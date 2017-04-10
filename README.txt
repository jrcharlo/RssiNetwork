Useful commands:
To see list of connected motes:
motelist

To flash/install:
1)
make telosb
make telosb reinstall bsl,/dev/ttyUSB0
2)
make telosb install,x
(where x is the desired node number)
(note that method 2 will select first telosb mote listed by motelist)

To use the Listen tool:
1) default:
java net.tinyos.tools.Listen -comm serial@/dev/ttyUSB0:telosb
2) RSSI in dBm listening tool, is located in the java folder
java Listen -comm serial@/dev/ttyUSB0:telosb

Node ID notation (to be used when flashing motes):
1 = base station
2 = sending mote (target)
3+ = relay nodes (sensors that track target)

