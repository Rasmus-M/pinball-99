xas99.py -R -L pinball.lst -i -q -o bin/PINBALL source/pinball.a99

java -jar tools/ea5tocart.jar bin\pinball "PINBALL" > make.log

copy /b bin\pinball8.bin + ^
    bin\coll.bin + ^
    bin\flippers.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin ^
    pinball-8.bin

java -jar tools/CopyHeader.jar pinball-8.bin 60 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22
