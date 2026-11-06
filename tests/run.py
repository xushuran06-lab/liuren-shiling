from pathlib import Path
import subprocess,argparse
r=Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser();parser.add_argument('--java',type=Path,default=r.parent/'.android-toolchain/amazon-corretto-21.jdk/Contents/Home');a=parser.parse_args();j=a.java/'bin'
b=r/'test-build';b.mkdir(exist_ok=True)
sources=list((r/'vendor/lunar-java/src/main/java').rglob('*.java'))+[r/'android/src/dev/liuren/pocket/CalendarEngine.java',r/'android/src/dev/liuren/pocket/ChartEngine.java',r/'tests/EngineTest.java',r/'tests/ChartTest.java']
subprocess.run([str(j/'javac'),'-encoding','UTF-8','-d',str(b),*[str(p) for p in sources]],check=True)
subprocess.run([str(j/'java'),'-cp',str(b),'EngineTest'],check=True)
subprocess.run([str(j/'java'),'-cp',str(b),'ChartTest'],check=True)
