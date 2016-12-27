/* Name: main.c
 * Project: HID-Test
 * Author: Christian Starkjohann
 * Creation Date: 2006-02-02
 * Tabsize: 4
 * Copyright: (c) 2006 by OBJECTIVE DEVELOPMENT Software GmbH
 * License: GNU GPL v2 (see License.txt) or proprietary (CommercialLicense.txt)
 * This Revision: $Id$
 */

#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/pgmspace.h>
#include <avr/wdt.h>
#include <avr/eeprom.h>
#include <util/delay.h>

#include "usbdrv.h"
#include "oddebug.h"

/* ----------------------- hardware I/O abstraction ------------------------ */

/* pin assignments:
PB0	Key 1
PB1	Key 2
PB2	Key 3
PB3	Key 4
PB4	Key 5
PB5 Key 6

PC0	Key 7
PC1	Key 8
PC2	Key 9
PC3	Key 10
PC4	Key 11
PC5	Key 12

PD0	USB-
PD1	debug tx
PD2	USB+ (int0)
PD3	Key 13
PD4	Key 14
PD5	Key 15
PD6	Key 16
PD7	Key 17
*/

static void hardwareInit(void)
{
uchar	i, j;

    PORTB = 0xff;   /* activate all pull-ups */
    DDRB = 0;       /* all pins input */
    PORTC = 0xff;   /* activate all pull-ups */
    DDRC = 0;       /* all pins input */
    PORTD = 0xfa;   /* 1111 1010 bin: activate pull-ups except on USB lines */
    DDRD = 0x07;    /* 0000 0111 bin: all pins input except USB (-> USB reset) */
	j = 0;
	while(--j){     /* USB Reset by device only required on Watchdog Reset */
		i = 0;
		while(--i); /* delay >10ms for USB reset */
	}
    DDRD = 0x02;    /* 0000 0010 bin: remove USB reset condition */
    /* configure timer 0 for a rate of 12M/(1024 * 256) = 45.78 Hz (~22ms) */
    TCCR0 = 5;      /* timer 0 prescaler: 1024 */
}

/* ------------------------------------------------------------------------- */

/* The following function returns an index for the first key pressed. It
 * returns 0 if no key is pressed.
 */
static uchar    keyPressed(void)
{
	uchar   i, mask, x;

	x = PINB;
	mask = 1;
	for(i=0;i<3;i++){
		if((x & mask) == 0)
		switch (i) {
			case 0:
				return 10;
			case 1:
				return 9;
			case 2:
				return 8;
			default:
				break;
		}
		mask <<= 1;
	}
	x = PINC;
	mask = 1;
	for(i=0;i<6;i++){
		if((x & mask) == 0)
		switch (i) {
			case 0:
				return 7;
			case 1:
				return 6;
			case '2':
				return 5;
			case '3':
				return 4;
			case '4':
				return 3;
			case '5':
				return 2;
			default:
				break;
		}
		mask <<= 1;
	}
	x = PIND;
	mask = 1;
	for(i=0;i<8;i++){
		if((x & mask) == 0)
		switch (i) {
			case '1':
				return 1;
			case '5':
				return 13;
			case '6':
				return 12;
			case '7':
				return 11;
			default:
				break;
		}
		mask <<= 1;
	}
	return 0;
}

/*static uchar    keyPressed(void)
{
	uchar   i, mask, x;

	x = PINB;
	mask = 1;
	for(i=0;i<3;i++){
		if((x & mask) == 0)
			switch (i) {
				case 0:
				return 10;
				case 1:
				return 9;
				case 2:
				return 8;
				default:
				break;
			}
		mask <<= 1;
	}
	return 0;
}*/

/* ------------------------------------------------------------------------- */
/* ----------------------------- USB interface ----------------------------- */
/* ------------------------------------------------------------------------- */

static uchar    reportBuffer[3];    /* buffer for HID reports */
static uchar    idleRate;           /* in 4 ms units */
static uchar	currentAddress;
static uchar	bytesRemainig;

const PROGMEM char usbHidReportDescriptor[USB_CFG_HID_REPORT_DESCRIPTOR_LENGTH] = {   /* USB report descriptor */
    0x05, 0x01,                    // USAGE_PAGE (Generic Desktop)
    0x09, 0x06,                    // USAGE (Keyboard)
    0xa1, 0x01,                    // COLLECTION (Application)
	0x85, 0x01,                    //   REPORT_ID (1)
    0x05, 0x07,                    //   USAGE_PAGE (Keyboard)
    0x19, 0xe0,                    //   USAGE_MINIMUM (Keyboard LeftControl)
    0x29, 0xe7,                    //   USAGE_MAXIMUM (Keyboard Right GUI)
    0x15, 0x00,                    //   LOGICAL_MINIMUM (0)
    0x25, 0x01,                    //   LOGICAL_MAXIMUM (1)
    0x75, 0x01,                    //   REPORT_SIZE (1)
    0x95, 0x08,                    //   REPORT_COUNT (8)
    0x81, 0x02,                    //   INPUT (Data,Var,Abs)
    0x95, 0x01,                    //   REPORT_COUNT (1)
    0x75, 0x08,                    //   REPORT_SIZE (8)
	0x25, 0x65,                    //   LOGICAL_MAXIMUM (101)
    0x19, 0x00,                    //   USAGE_MINIMUM (Reserved (no event indicated))
    0x29, 0x65,                    //   USAGE_MAXIMUM (Keyboard Application)
    0x81, 0x00,                    //   INPUT (Data,Ary,Abs)
    0xc0,                          // END_COLLECTION //37bytes
    0x06, 0x00, 0xff,              // USAGE_PAGE (Generic Desktop)
    0x09, 0x01,                    // USAGE (Vendor Usage 1)
    0xa1, 0x01,                    // COLLECTION (Application)
    0x85, 0x02,                    //   REPORT_ID (2)
    0x15, 0x00,                    //   LOGICAL_MINIMUM (0)
    0x26, 0xff, 0x00,              //   LOGICAL_MAXIMUM (255)
    0x75, 0x08,                    //   REPORT_SIZE (8)
    0x95, 0x40,                    //   REPORT_COUNT (64)
    0x09, 0x00,                    //   USAGE (Undefined)
    0xB1, 0x00,					   //   INPUT (Data,Var,Abs)
	0x09, 0x00,					   //	USAGE (Undefined)
	0x91, 0x00,					   //	OUTPUT (Data,Ary,Abs)
    0xc0                           // END_COLLECTION                        
};
/* We use a simplifed keyboard report descriptor which does not support the
 * boot protocol. We don't allow setting status LEDs and we only allow one
 * simultaneous key press (except modifiers). We can therefore use short
 * 2 byte input reports.
 * The report descriptor has been created with usb.org's "HID Descriptor Tool"
 * which can be downloaded from http://www.usb.org/developers/hidpage/.
 * Redundant entries (such as LOGICAL_MINIMUM and USAGE_PAGE) have been omitted
 * for the second INPUT item.
 */

/* Keyboard usage values, see usb.org's HID-usage-tables document, chapter
 * 10 Keyboard/Keypad Page for more codes.
 */
#define MOD_CONTROL_LEFT    (1<<0)
#define MOD_SHIFT_LEFT      (1<<1)
#define MOD_ALT_LEFT        (1<<2)
#define MOD_GUI_LEFT        (1<<3)
#define MOD_CONTROL_RIGHT   (1<<4)
#define MOD_SHIFT_RIGHT     (1<<5)
#define MOD_ALT_RIGHT       (1<<6)
#define MOD_GUI_RIGHT       (1<<7)

#define NUM_KEYS 13
uchar dKey=4;
uchar keyReport[NUM_KEYS + 1][2];

uchar EEMEM initFlag;
uchar EEMEM length;
uchar EEMEM defaultKey;
uchar EEMEM lay1[NUM_KEYS];
uchar EEMEM lay2[NUM_KEYS];
uchar EEMEM lay3[NUM_KEYS];
uchar EEMEM lay4[NUM_KEYS];

static int readConf() {
	char p = 0;
	
	if (PIND & (1 << PD3)){
		p |= (PIND & (1 << PD3)) >> PD3;
	}
	
	if (PIND & (1 << PD4)){
		p |= (PIND & (1 << PD4)) >> PD3;
	}
	
	return p;
}

static void writeLayout(uchar *layout, uchar layNum) {
	switch (layNum) {
		case 1:
			eeprom_write_block(layout, & lay1, NUM_KEYS);
			break;
		case 2:
			eeprom_write_block(layout, & lay2, NUM_KEYS);
			break;
		case 3:
			eeprom_write_block(layout, & lay3, NUM_KEYS);
			break;
		case 4:
			eeprom_write_block(layout, & lay4, NUM_KEYS);
			break;
	}
}

static void readLayout(char p) {
	uchar keys[NUM_KEYS];
	if (p == 0)
	p = readConf()+1;
	switch (p) {
		case 1:
			eeprom_read_block(keys, &lay1, NUM_KEYS);
			break;
		case 2:
			eeprom_read_block(keys, &lay2, NUM_KEYS);
			break;
		case 3:
			eeprom_read_block(keys, &lay3, NUM_KEYS);
			break;
		case 4:
			eeprom_read_block(keys, &lay4, NUM_KEYS);
			break;
		default:
		for (int i=0; i<NUM_KEYS; i++) {
			keys[i] = dKey;
		}
	}
	keyReport[0][0] = 0;
	keyReport[0][1] = 0;
	for (int i=0; i < NUM_KEYS; i++) {
		keyReport[i+1][0] = 0;
		keyReport[i+1][1] = keys[i];
	}
}

static void firstRun() {
	uchar initial[NUM_KEYS];
	for(uchar i=0; i<NUM_KEYS; i++) {
		initial[i] = dKey + i;
	}
	eeprom_write_byte(&initFlag, 1);
	eeprom_write_byte(&length, NUM_KEYS);
	eeprom_write_byte(&defaultKey, dKey);
	writeLayout(initial, 1);
	writeLayout(initial, 2);
	writeLayout(initial, 3);
	writeLayout(initial, 4);
	keyReport[0][0] = 0;
	keyReport[0][1] = 0;
	for (int i=0; i < NUM_KEYS; i++) {
		keyReport[i+1][0] = 0;
		keyReport[i+1][1] = initial[i];
	}
}

static void buildReport(uchar key)
{
	reportBuffer[2] = keyReport[key][1];
	reportBuffer[1] = keyReport[key][0];
	reportBuffer[0] = 1;
}

uchar	usbFunctionSetup(uchar data[8])
{
usbRequest_t    *rq = (void *)data;

    usbMsgPtr = reportBuffer;
    if((rq->bmRequestType & USBRQ_TYPE_MASK) == USBRQ_TYPE_CLASS){    /* class request type */
        if(rq->bRequest == USBRQ_HID_GET_REPORT){  /* wValue: ReportType (highbyte), ReportID (lowbyte) */
            if (rq->wValue.bytes[0] == 2) {
				bytesRemainig = 64;
				currentAddress = 0;
				return USB_NO_MSG;
            }
        } else if(rq->bRequest == USBRQ_HID_SET_REPORT){
			if (rq->wValue.bytes[0] == 2) {
				bytesRemainig = 64;
				currentAddress = 0;
				return USB_NO_MSG;
			}
		}else if(rq->bRequest == USBRQ_HID_GET_IDLE){
            usbMsgPtr = &idleRate;
            return 1;
        }else if(rq->bRequest == USBRQ_HID_SET_IDLE){
            idleRate = rq->wValue.bytes[1];
        }
    }else{
        /* no vendor specific requests implemented */
    }
	return 0;
}

uchar usbFunctionRead(uchar *data, uchar len) {
	if(len > bytesRemainig)
		len = bytesRemainig;
	eeprom_read_block(data, (uchar *)0 + currentAddress, len);
	currentAddress += len;
	bytesRemainig -= len;
	return bytesRemainig == 0;
}

uchar usbFunctionWrite(uchar *data, uchar len) {
	return 1;
}

/* ------------------------------------------------------------------------- */

int	main(void)
{
uchar   key, lastKey = 0, keyDidChange = 0;
uchar   idleCounter = 0;

	wdt_enable(WDTO_1S);
    hardwareInit();
	if(eeprom_read_byte(&initFlag) != 1) {
		firstRun();
	} else {
		readLayout(0);
	}
	odDebugInit();
	DBG1(0x00, 0, 0);
	usbInit();
	sei();
    DBG1(0x01, 0, 0);
    for(;;){	/* main event loop */
		DBG1(0x02, 0, 0);
		wdt_reset();
		usbPoll();
        key = keyPressed();
        if(lastKey != key){
            lastKey = key;
            keyDidChange = 1;
        }
        if(TIFR & (1<<TOV0)){   /* 22 ms timer */
            TIFR = 1<<TOV0;
            if(idleRate != 0){
                if(idleCounter > 4){
                    idleCounter -= 5;   /* 22 ms in units of 4 ms */
                }else{
                    idleCounter = idleRate;
                    keyDidChange = 1;
                }
            }
        }
        if(keyDidChange && usbInterruptIsReady()){
            keyDidChange = 0;
            /* use last key and not current key status in order to avoid lost
               changes in key status. */
            buildReport(lastKey);
            usbSetInterrupt(reportBuffer, sizeof(reportBuffer));
        }
	}
	return 0;
}

/* ------------------------------------------------------------------------- */
