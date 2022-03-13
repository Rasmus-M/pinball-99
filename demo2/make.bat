xas99.py -R -L pinball.lst -i -q -o PINBALL source/pinball.a99

java -jar tools/ea5tocart.jar pinball "PINBALL DEMO II" > make.log

copy /b pinball8.bin + ^
    coll.bin + ^
    flippers.bin + ^
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

java -jar tools/CopyHeader.jar pinball-demo2-8.bin 60 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22
