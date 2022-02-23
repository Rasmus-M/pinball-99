IF EXIST pinball.dsk GOTO :dskok
xdm99.py pinball.dsk --initialize DSSD -n PINBALL
:dskok

xas99.py -R -L pinball.lst source/pinball.a99

xas99.py -R -i -o PINBALL source/pinball.a99

xdm99.py pinball.dsk -a pinball -n PBDEMO

java -jar tools/ea5tocart.jar pinball "PINBALL DEMO" 0 16k > make.log
copy /b pinball8.bin + coll.bin pinball-demo-8.bin

java -jar tools/CopyHeader.jar pinball-demo-8.bin 60