from device import *
import uuid
local = "Local Prueba " + uuid.uuid4().hex[:6]
adb("shell","am","force-stop","cl.acis.app")
adb("shell","am","start","-n","cl.acis.app/.MainActivity");time.sleep(6)
shot("inicio")
click("Jefatura");click("+ Crear orden")
fill("Local",local)
fill("Dirección","Avenida Central 123")
fill("Equipo / identificación","Aire acondicionado EQ-01")
fill("Técnico asignado","Ana Perez")
fill("Motivo de la visita","Revision preventiva mensual")
click("Crear orden");time.sleep(1)
shot("orden")
assert any(n.get("text")==local for n in nodes())
adb("shell","am","force-stop","cl.acis.app");adb("shell","am","start","-n","cl.acis.app/.MainActivity");time.sleep(6)
assert any(n.get("text")==local for n in nodes()), "Persistencia fallida"
click("Técnico");click(local);time.sleep(1);click("Aceptar orden");click("Registrar llegada e iniciar");click("Registrar intervención")
fill("Diagnóstico","Filtro con suciedad acumulada")
fill("Trabajo realizado","Limpieza y revision del equipo")
click("Guardar avance");time.sleep(1)
assert any(n.get("text")=="Filtro con suciedad acumulada" for n in nodes())
shot("intervencion")
print("PASS: creación, persistencia tras reinicio, aceptación, llegada y guardado de intervención",flush=True)
