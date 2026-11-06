#!/usr/bin/env python3
"""Offline native APK build; SDK paths can be overridden. No Gradle/network required."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, zipfile
ROOT=Path(__file__).resolve().parent.parent
SDK=ROOT.parent/'.android-toolchain'
p=argparse.ArgumentParser()
p.add_argument('--java',type=Path,default=SDK/'amazon-corretto-21.jdk/Contents/Home')
p.add_argument('--tools',type=Path,default=SDK/'tools/android-16')
p.add_argument('--platform',type=Path,default=SDK/'platform/android-36')
a=p.parse_args(); j=a.java.resolve(); tools=a.tools.resolve(); platform=a.platform.resolve()
env=dict(os.environ);env['JAVA_HOME']=str(j);env['PATH']=str(j/'bin')+os.pathsep+env.get('PATH','')
def run(*cmd): subprocess.run([str(c) for c in cmd],env=env,cwd=ROOT,check=True)
b=ROOT/'android/build'
if b.exists():shutil.rmtree(b)
b.mkdir()
for d in ['classes','generated','dex','assets']:(b/d).mkdir()
shutil.copy2(ROOT/'vendor/lunar-java/LICENSE',b/'assets/LUNAR-LICENSE.txt')
shutil.copy2(ROOT/'vendor/lunar-java/COMMIT',b/'assets/LUNAR-COMMIT.txt')
shutil.copy2(ROOT/'LICENSE',b/'assets/APP-LICENSE.txt')
run(tools/'aapt2','compile','--dir',ROOT/'android/res','-o',b/'resources.zip')
run(tools/'aapt2','link','-o',b/'base.apk','-I',platform/'android.jar','--manifest',ROOT/'android/AndroidManifest.xml','-A',b/'assets','--java',b/'generated',b/'resources.zip')
sources=sorted((ROOT/'android/src').rglob('*.java'))+sorted((ROOT/'vendor/lunar-java/src/main/java').rglob('*.java'))+sorted((b/'generated').rglob('*.java'))
run(j/'bin/javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-bootclasspath',str(tools/'core-lambda-stubs.jar')+os.pathsep+str(platform/'android.jar'),'-d',b/'classes',*sources)
run(j/'bin/java','-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--min-api','26','--lib',platform/'android.jar','--output',b/'dex',*sorted((b/'classes').rglob('*.class')))
shutil.copy2(b/'base.apk',b/'unsigned.apk')
with zipfile.ZipFile(b/'unsigned.apk','a',compression=zipfile.ZIP_DEFLATED) as z:
    for f in sorted((b/'dex').glob('*.dex')):z.write(f,f.name)
run(tools/'zipalign','-P','16','-f','4',b/'unsigned.apk',b/'aligned.apk')
s=ROOT/'android/.signing';s.mkdir(mode=0o700,exist_ok=True);key=s/'dev.jks'
if not key.exists():
    run(j/'bin/keytool','-genkeypair','-noprompt','-keystore',key,'-storetype','JKS','-storepass','android','-keypass','android','-alias','liuren-dev','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Liuren Pocket Development,C=CN');key.chmod(0o600)
release=ROOT/'releases';release.mkdir(exist_ok=True);output=release/'Liuren-Shiling-0.2.0.apk'
run(j/'bin/java','-jar',tools/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','liuren-dev','--ks-pass','pass:android','--key-pass','pass:android','--v4-signing-enabled','false','--out',output,b/'aligned.apk')
run(j/'bin/java','-jar',tools/'lib/apksigner.jar','verify','--verbose',output)
run(tools/'zipalign','-c','-P','16','4',output)
sha=hashlib.sha256(output.read_bytes()).hexdigest();output.with_suffix('.apk.sha256').write_text(f'{sha}  {output.name}\n')
print(json.dumps({'apk':str(output),'bytes':output.stat().st_size,'sha256':sha},ensure_ascii=False))
