IF EXIST pinball.dsk GOTO :dskok
xdm99.py pinball.dsk --initialize DSSD -n PINBALL
:dskok

xas99.py -R -L pinball.lst source/pinball.a99

xas99.py -R -i -o PINBALL source/pinball.a99

xdm99.py pinball.dsk -a pinball -n PBDEMO

java -jar tools/ea5tocart.jar pinball "PINBALL DEMO" > make.log

tools\pad.exe pinball8.bin pinball-16k.bin 16384
copy /b pinball-16k.bin + coll.bin pinball-demo-8.bin

