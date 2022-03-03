xas99.py -R -L pinball.lst -i -q -o PINBALL source/pinball.a99

java -jar tools/ea5tocart.jar pinball "PINBALL DEMO 2" > make.log

copy /b pinball8.bin + ^
    coll.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin + ^
    empty.bin ^
    pinball-demo2-8.bin

rem java -jar tools/CopyHeader.jar pinball-demo2-8.bin
