"""Comprueba las líneas de lógica Android con los datos de JaCoCo generados por Gradle."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"app/src/main/java"
REPORT=ROOT/"app/build/reports/coverage/test/debug/report.xml"

def measure():
    report=ET.parse(REPORT)
    totals=[0,0]
    for pkg in report.findall("package"):
        name=pkg.get("name")
        if name.split("/")[-1] not in {"model","data","remote","viewmodel","connected"}:
            continue
        for entry in pkg.findall("sourcefile"):
            path=SOURCE/name/entry.get("name")
            if not path.exists() or "@Composable" in path.read_text(encoding="utf-8"):
                continue
            lines=entry.find("counter[@type='LINE']")
            if lines is not None:
                totals[0]+=int(lines.get("covered"))
                totals[1]+=int(lines.get("missed"))
    hit,miss=totals
    assert hit+miss>0, "Informe de cobertura vacío"
    print(f"Lógica Android: {hit}/{hit+miss} líneas ({100*hit/(hit+miss):.1f}%).")
    if hit/(hit+miss)<0.8: raise SystemExit("La cobertura no alcanza el 80%.")
if __name__=="__main__":measure()
