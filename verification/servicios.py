"""Prueba los dos JAR con bases temporales y cuentas ficticias. Requiere Java 21+."""
import hashlib
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import tempfile
import time
import uuid
from contextlib import ExitStack
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
def port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]

def call(base, path, method="GET", data=None, token=None, expected=200):
    headers = {"Content-Type": "application/octet-stream" if isinstance(data, bytes) else "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = data if isinstance(data, bytes) else None if data is None else json.dumps(data).encode()
    try:
        with urlopen(Request(base+path, body, headers, method=method), timeout=15) as resp:
            assert resp.status == expected
            raw = resp.read()
            if "application/octet-stream" in resp.headers.get("Content-Type",""):
                return raw
            return json.loads(raw) if raw else None
    except HTTPError as exc:
        if exc.code != expected:
            raise AssertionError((method, path, exc.code, exc.read().decode())) from exc
        return None

def run(accounts, field, password):
    def credentials(email):
        return {"email":email+"@example.invalid","password":password,"name":email+" prueba"}
    def login(email):
        return call(accounts,"/accounts/login","POST",credentials(email))["token"]
    admin = login("admin")
    users = {}
    for name,role in [("jefe","Jefatura"),("tecnico","Técnico"),("cliente","Cliente"),("ajeno","Cliente")]:
        recovery=call(accounts,"/accounts/register","POST",credentials(name))
        assert len(recovery["recovery"])>=32
        if name=="cliente":
            recovered=call(accounts,"/accounts/recover","POST",{**credentials(name),**recovery})
            assert recovered != recovery
        users[name]=call(accounts,"/accounts/me",token=login(name))["id"]
    def assign(name,role,scope=""):
        call(accounts,"/accounts/"+users[name]+"/access","PUT",{"role":role,"scope":scope},admin)
    assign("jefe","Jefatura")
    boss=login("jefe")
    site,technician=(str(uuid.uuid4()) for _ in range(2))
    for key,kind in [(site,"sites"),(technician,"technicians")]:
        entry={"id":key,"kind":kind,"name":kind,"address":"Calle de prueba 123","client":"Empresa","email":"t@example.invalid","active":True}
        call(field,"/field/catalog/"+key,"PUT",entry,boss)
    eq=str(uuid.uuid4())
    call(field,"/field/equipment/"+eq,"PUT",{"id":eq,"site":site,"name":"Unidad exterior","location":"Azotea","active":True,"lastService":0},boss)
    assert call(field,"/field/equipment?site="+site,token=boss)[0]["name"]=="Unidad exterior"
    assign("tecnico","Técnico",technician)
    assign("cliente","Cliente",site)
    tech,client,other=map(login,["tecnico","cliente","ajeno"])
    call(accounts,"/accounts/me","PATCH",{"name":"Técnico de prueba"},tech)
    call(accounts,"/accounts",token=other,expected=403)
    plan={"site":site,"technician":technician,"scheduled":"2026-12-01","latitude":-33.45,"longitude":-70.65,"slaHours":24}
    order={"clientOrderId":str(uuid.uuid4()),"site":"sites","address":"Calle de prueba 123","technician":"technicians",
      "equipment":"Unidad exterior","priority":"Alta","type":"Preventiva","problem":"Visita de prueba","stage":"ASSIGNED","evidenceCount":0}
    def write(revision,body,photos=None):
        return {"operation":str(uuid.uuid4()),"revision":revision,"plan":plan,"order":body,"photos":photos or []}
    create=write(0,order)
    record=call(field,"/field/orders","POST",create,boss)
    ident=record["order"]["id"]
    assert call(field,"/field/orders","POST",create,boss)["order"]["id"]==ident
    path="/field/orders/"+str(ident)
    assert len(call(field,"/field/orders",token=tech))==1
    assert call(field,"/field/orders",token=other)==[]
    call(field,path+"?revision=1","DELETE",token=tech,expected=403)
    def step(token,photos=None,**changes):
        nonlocal record
        request=write(record["revision"],{**record["order"],**changes},photos)
        record=call(field,path,"PUT",request,token)
        assert call(field,path,"PUT",request,token)["revision"]==record["revision"]
    step(tech,stage="ACCEPTED")
    step(tech,stage="ACTIVE",arrival=1000)
    image=b"evidencia ficticia"
    digest=hashlib.sha256(image).hexdigest()
    call(field,path+"/photos/"+digest,"PUT",image,tech)
    step(tech,[digest],stage="DONE",departure=2000,diagnosis="Revisión del equipo",work="Limpieza del filtro",evidenceCount=1)
    assert call(field,path+"/photos/"+digest,token=client)==image
    call(field,path+"/photos/"+digest,token=other,expected=404)
    step(client,[digest],customer="Local de prueba",customerConfirmed=True)
    step(boss,[digest],stage="CLOSED")
    assert call(field,path+"/history",token=client)[-1]["name"]=="jefe prueba"
    assert call(field,"/field/equipment?site="+site,token=boss)[0]["lastService"]==2000
    call(field,path+"?revision="+str(record["revision"]),"DELETE",token=boss)
    assert call(field,"/field/orders",token=client)==[]
    print("OK: cuentas, recuperación, permisos, catálogos, visita, reintentos, evidencia, conformidad, cierre y eliminación.")

def main():
    ap,fp=port(),port()
    accounts,field=f"http://127.0.0.1:{ap}",f"http://127.0.0.1:{fp}"
    password=secrets.token_urlsafe(24)
    java=Path(os.environ["JAVA_HOME"])/"bin"/("java.exe" if os.name=="nt" else "java")
    with tempfile.TemporaryDirectory() as folder, ExitStack() as stack:
        for module,p in [("cuentas",ap),("acis-microservicio",fp)]:
            jar=next((ROOT/module/"target").glob("*.jar"))
            log=stack.enter_context(open(Path(folder)/(module+".log"),"w+"))
            env=dict(os.environ,SERVER_PORT=str(p),PORT=str(p),ACIS_ADMIN_EMAIL="admin@example.invalid",ACIS_ADMIN_PASSWORD=password,
                     ACIS_ACCOUNTS_URL=accounts,ACIS_ACCOUNTS_DB="jdbc:h2:mem:accounts",ACIS_DB_URL="jdbc:h2:mem:orders")
            proc=subprocess.Popen([str(java),"-jar",str(jar)],cwd=folder,env=env,stdout=log,stderr=subprocess.STDOUT)
            def stop(proc=proc):
                proc.terminate()
                proc.wait(timeout=20)
            stack.callback(stop)
            for _ in range(180):
                if proc.poll() is not None:
                    log.seek(0)
                    raise RuntimeError(log.read()[-2000:])
                try:
                    with socket.create_connection(("127.0.0.1",p),timeout=0.3): break
                except OSError: time.sleep(0.3)
            else: raise RuntimeError("El servicio no inició")
        run(accounts,field,password)
if __name__=="__main__":
    main()
