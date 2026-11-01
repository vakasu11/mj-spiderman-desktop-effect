"""Dependency-free Java Android build; use Google SDK Build Tools 36 and JDK 21.

python android/build_apk.py --jdk PATH --platform PATH --tools PATH --out PATH
Requires a working local Java installation and an SDK license obtained from Google.
Output is signed with a local DEVELOPMENT key. Never commit that key.
"""
from pathlib import Path
import argparse
import subprocess
import shutil
import zipfile
import hashlib
import json
import os
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
for name in ['jdk','platform','tools','out']:
    parser.add_argument('--'+name, type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parent
work = root.parent / 'work/android-build'
work.mkdir(parents=True, exist_ok=True)
out = args.out.resolve()
out.mkdir(parents=True, exist_ok=True)
classes, gen, assets = [work/name for name in ['classes','gen','assets']]
for folder in [classes,gen,assets]: folder.mkdir(exist_ok=True)
jdk, tools = args.jdk.resolve(), args.tools.resolve()
android_jar = args.platform.resolve() / 'android.jar'
env = dict(os.environ, JAVA_HOME=str(jdk))
env['PATH'] = str(jdk/'bin') + os.pathsep + env['PATH']
def run(*command):
    print('Running', str(command[0]), flush=True)
    subprocess.run([str(v) for v in command], check=True, env=env)
java, javac = jdk/'bin/java.exe', jdk/'bin/javac.exe'
for name in ['frames','frames2']:
    shutil.copytree(root.parent/'桌面特效'/name, assets/name, dirs_exist_ok=True)
for name in ['audio.wav','audio2.wav']:
    shutil.copy2(root.parent/'桌面特效'/name, assets/name)
compiled = work/'resources.zip'
run(tools/'aapt2.exe','compile','--dir',root/'res','-o',compiled)
base = work/'base.apk'
run(tools/'aapt2.exe','link','-o',base,'-I',android_jar,'--manifest',root/'AndroidManifest.xml',
    '--java',gen,'-A',assets,'-0','wav',compiled)
sources = [*root.glob('src/**/*.java'),*gen.glob('**/*.java')]
source_list = work/'sources.txt'
source_list.write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in sources),encoding='utf-8')
run(javac,'-encoding','UTF-8','--release','8','-classpath',android_jar,
    '-d',classes,'@'+str(source_list))
tests = work/'test-classes'
tests.mkdir(exist_ok=True)
run(javac,'-encoding','UTF-8','-d',tests,
    root/'src/com/vakasu11/mjspiderman/TextTrigger.java',root/'tests/TextTriggerTest.java',
    root/'src/com/vakasu11/mjspiderman/ServiceSelection.java',root/'tests/ServiceSelectionTest.java')
for test_name in ['TextTriggerTest','ServiceSelectionTest']:
    run(java,'-cp',tests,'com.vakasu11.mjspiderman.'+test_name)
jar = work/'classes.jar'
with zipfile.ZipFile(jar,'w') as z:
    for p in classes.rglob('*.class'): z.write(p,p.relative_to(classes).as_posix())
run(java,'-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--lib',android_jar,
    '--min-api','30','--output',work,jar)
unsigned = work/'unsigned.apk'
with zipfile.ZipFile(base) as source, zipfile.ZipFile(unsigned,'w',zipfile.ZIP_DEFLATED) as z:
    # Repack entries so ZIP local and central headers use identical flags.
    for info in source.infolist():
        z.writestr(info.filename, source.read(info), compress_type=info.compress_type)
    z.write(work/'classes.dex','classes.dex')
aligned = work/'aligned.apk'
run(tools/'zipalign.exe','-f','-p','4',unsigned,aligned)
key = root.parent/'work/android-development.keystore'
if not key.exists():
    run(jdk/'bin/keytool.exe','-genkeypair','-keystore',key,'-storepass','android',
        '-keypass','android','-alias','androiddebugkey','-keyalg','RSA','-keysize','2048',
        '-validity','10000','-dname','CN=MJ Local Development')
version = ET.parse(root/'AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
apk = out/f'mj-spiderman-android-{version}.apk'
signer = tools/'lib/apksigner.jar'
run(java,'-jar',signer,'sign','--ks',key,'--ks-key-alias','androiddebugkey',
    '--ks-pass','pass:android','--key-pass','pass:android','--out',apk,aligned)
run(java,'-jar',signer,'verify','--verbose','--print-certs',apk)
run(tools/'zipalign.exe','-c','4',apk)
with zipfile.ZipFile(apk) as z:
    assert z.testzip() is None
    assert len([n for n in z.namelist() if n.startswith('assets/frames/') and n.endswith('.png')])==166
    assert len([n for n in z.namelist() if n.startswith('assets/frames2/') and n.endswith('.png')])==80
    assert all('assets/'+n in z.namelist() for n in ['audio.wav','audio2.wav'])
sha = hashlib.sha256(apk.read_bytes()).hexdigest()
(out/'SHA256.txt').write_text(sha+'  '+apk.name+'\n',encoding='ascii')
print(json.dumps({'apk':str(apk),'sha256':sha,'bytes':apk.stat().st_size,'device_tested':False}),flush=True)
