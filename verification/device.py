import subprocess, time, re, xml.etree.ElementTree as ET
from pathlib import Path
ADB = "C:/Users/nicod/AppData/Local/Android/Sdk/platform-tools/adb.exe"
def adb(*args):
 return subprocess.check_output([ADB,"-s","emulator-5554",*args],stderr=subprocess.STDOUT)
def nodes():
 for attempt in range(4):
  try:
   adb("shell","uiautomator","dump","/sdcard/acis-check.xml")
   return list(ET.fromstring(adb("shell","cat","/sdcard/acis-check.xml")).iter("node"))
  except Exception:
   time.sleep(2)
 raise RuntimeError("No se obtuvo la interfaz")
def click(text,scroll=True):
 for _ in range(7 if scroll else 1):
  for n in nodes():
   if n.get("text")==text or n.get("content-desc")==text:
    x1,y1,x2,y2=map(int,re.findall(r"\d+",n.get("bounds")))
    if y2-y1>8:
     adb("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2));time.sleep(.4);return
  adb("shell","input","swipe","540","1900","540","800","350")
 raise RuntimeError("No se encuentra: "+text)
def fill(label,value):
 click(label);adb("shell","input","text",value.replace(" ","%s"));adb("shell","input","keyevent","4");time.sleep(.3)
def shot(name):
 Path("verification/"+name+".png").write_bytes(adb("exec-out","screencap","-p"))
