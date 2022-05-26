xas99.py -S -L pinball.lst -i -q -o bin/PINBALL source/pinball.a99
xas99.py -b -q source/speech-data.a99 -o bin/speech-data.bin

java -jar tools/ea5tocart.jar bin\pinball "PINBALL 99" > make.log

copy /b bin\pinball8.bin + ^
    bin\coll.bin + ^
    bin\flippers.bin + ^
    bin\speech-data.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin + ^
    bin\empty.bin ^
    pinball99-8.bin

java -jar tools/CopyHeader.jar pinball99-8.bin 60 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26
