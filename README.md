# MKkbd
Programmable keyboard controller.

Project based on V-USB library, firmware precompiled for atmega8a with 12MHz quartz.

v1.x - controller supports up to 13 (17) keys with 4 layouts, stored in EEMEM (you can switch between layouts by installing jumper link on board).

## Installation
Software part (charset changer) comes in 2 variants. 

First one is standalone package with jre included and doesn't require any third-party apps, so no there's need to install app, just unpack and launch. 

Second package contains only app itself and you must have jre 1.8 installed.

## Hardware/firmaware notice
PCB and schematic projects made in DipTrace software.
Main project is Atmel Studio 7
When flashing avr make sure you using right fuses (there 2 xml files included, one for use with bootloader and another without)
Bootloader made by obdev (https://www.obdev.at/products/vusb/bootloadhid.html) and just configured for my project, to activate bootloader set up jumper in appropriate position.

### Jumper positions:
**J2** | **J2** | **J2** | **J2** | **J2**  
------ | ------ | ------ | ------ | ------ 
░ ░<br>░ ░ | ░ ░<br>█ █ | █ █<br>░ ░ | █ █<br>█ █ | ░ █<br>░ █
lay1 | lay2 | lay3 | lay4 | boot
 
## TODO
code nice button debounce
make v2.x board, that supply only 12 keys but includes programmable led lights on buttons.

## License
GNU GPL V3 (see LICENSE)
