"""Recorrido real con PostgreSQL. Crea y retira un proyecto Docker exclusivo de esta prueba."""
from pathlib import Path
from subprocess import run as command
import os
import secrets
import time
import uuid
from urllib.error import URLError, HTTPError
from urllib.request import urlopen
from servicios import port, run

root=Path(__file__).resolve().parents[1]
name="acis-qa-"+uuid.uuid4().hex[:12]
ap,fp=port(),port()
password=secrets.token_urlsafe(32)
env=dict(os.environ,ACIS_ADMIN_EMAIL="admin@example.invalid",ACIS_ADMIN_PASSWORD=password,
         ACIS_DB_PASSWORD=secrets.token_hex(32),ACIS_ACCOUNTS_PASSWORD=secrets.token_hex(32),
         ACIS_ACCOUNTS_PORT=str(ap),ACIS_FIELD_PORT=str(fp))
compose=["docker","compose","--project-name",name]
try:
    command(compose+["up","--no-build","-d"],cwd=root,env=env,check=True)
    for address in [f"http://127.0.0.1:{ap}/accounts",f"http://127.0.0.1:{fp}/field/orders"]:
        for _ in range(120):
            try: urlopen(address,timeout=2)
            except HTTPError as e:
                if e.code in (400,401,403): break
            except (URLError,ConnectionError): pass
            time.sleep(1)
        else: raise RuntimeError("Servicio sin respuesta")
    run(f"http://127.0.0.1:{ap}",f"http://127.0.0.1:{fp}",password)
finally:
    command(compose+["down","--volumes"],cwd=root,env=env,check=True)
