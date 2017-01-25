#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/pgmspace.h>
#include <avr/wdt.h>
#include <avr/eeprom.h>
#include <util/delay.h>
#include <string.h>

#include "usbdrv.h"
#include "oddebug.h"

/* ----------------------- hardware I/O abstraction ------------------------ */

/* pin assignments:
PD5	Key 1
PD6	Key 2
PD7	Key 3

PB0	Key 4
PB1	Key 5
PB2 Key 6
PB3 Key 14
PB4 Key 15
PB5 Key 16

PC0	Key 7
PC1	Key 8
PC2	Key 9
PC3	Key 10
PC4	Key 11
PC5	Key 12
PC6	Key 17

PD1 Key 13

PD0	USB-
PD2	USB+ (int0)

PD3 jumper 1
PD4 jumper 2
*/

#define REPORT_COUNT		128			//длина пакета
#define NUM_KEYS			13			//дефолтное количество клавиш (максимум 13)
#define MOD_CONTROL_LEFT    (1<<0)
#define MOD_SHIFT_LEFT      (1<<1)
#define MOD_ALT_LEFT        (1<<2)
#define MOD_GUI_LEFT        (1<<3)
#define MOD_CONTROL_RIGHT   (1<<4)
#define MOD_SHIFT_RIGHT     (1<<5)
#define MOD_ALT_RIGHT       (1<<6)
#define MOD_GUI_RIGHT       (1<<7)
#define COUNT_END			50
#define COUNT_PERIOD		15

uchar dKey = 4;						//дефолтное значение клавиши
uchar keyCount = NUM_KEYS;			//количество клавиш для формирования пакета
uchar keyReport[NUM_KEYS + 1][2];	//массив клавиш
uchar active_layout = 1;			//активная раскладка

uchar eeprom_init = 0;
uchar eeprom_len = 1;
uchar eeprom_def = 2;
uchar eeprom_lay1 = 3;
uchar eeprom_lay2 = 3 + NUM_KEYS;
uchar eeprom_lay3 = 3 + NUM_KEYS * 2;
uchar eeprom_lay4 = 3 + NUM_KEYS * 3;

static uchar    reportBuffer[3];    /* пакет для клавиатуры */
static uchar    idleRate;           /* in 4 ms units */
static uchar	currentAddress;
static uchar	bytesRemaining;
static uchar	msgbuf[REPORT_COUNT+1];	//пакет настроек

static void hardwareInit(void)
{
uchar	i, j;

    PORTB = 0xff;   /* activate all pull-ups */
    DDRB = 0;       /* all pins input */
    PORTC = 0xff;   /* activate all pull-ups */
    DDRC = 0;       /* all pins input */
    PORTD = 0xfa;   /* 1111 1010 bin: activate pull-ups except on USB lines */
    DDRD = 0x05;    /* 0000 0101 bin: all pins input except USB (-> USB reset) */
	j = 0;
	while(--j){     /* USB Reset by device only required on Watchdog Reset */
		i = 0;
		while(--i); /* delay >10ms for USB reset */
	}
	DDRD = 0x00;   
    /* configure timer 0 for a rate of 12M/(1024 * 256) = 45.78 Hz (~22ms) */
    TCCR0 = 5;      /* timer 0 prescaler: 1024 */
}

/* ------------------------------------------------------------------------- */

/* The following function returns an index for the first key pressed. It
 * returns 0 if no key is pressed.
 */
static uchar    keyPressed(void)
{
	uchar   i, mask, ip, jp;

	mask = 1;
	for(i=0;i<6;i++){
		if((PINB & mask) == 0){
			ip = 0;
			jp = 0;
			while (ip < COUNT_END) {
				if((PINB & mask) == 0) {
					ip++;
					jp = 0;
				} else {
					ip = 0;
					jp++;
				}
				if (jp > COUNT_PERIOD) break;
			}
			if(ip>=COUNT_END)
			switch (i) {
				case 0:
					return 4;
				case 1:
					return 5;
				case 2:
					return 6;
/*				case 3:
					return 14;
				case 4:
					return 15;
				case 5:
					return 16;
*/				default:
					break;
			}
		}
		mask <<= 1;
	}
	mask = 1;
	for(i=0;i<7;i++){
		if((PINC & mask) == 0){
			ip = 0;
			jp = 0;
			while (ip < COUNT_END) {
				if((PINC & mask) == 0) {
					ip++;
					jp = 0;
				} else {
					ip = 0;
					jp++;
				}
				if (jp > COUNT_PERIOD) break;
			}
			if(ip>=COUNT_END)
			switch (i) {
				case 0:
					return 7;
				case 1:
					return 8;
				case 2:
					return 9;
				case 3:
					return 10;
				case 4:
					return 11;
				case 5:
					return 12;
/*				case 6:
					return 17;
*/				default:
					break;
			}
		}
		mask <<= 1;
	}
	mask = 1;
	for(i=0;i<8;i++){
		if((PIND & mask) == 0){
			ip = 0;
			jp = 0;
			while (ip < COUNT_END) {
				if((PIND & mask) == 0) {
					ip++;
					jp = 0;
				} else {
					ip = 0;
					jp++;
				}
				if (jp > COUNT_PERIOD) break;
			}
			if(ip>=COUNT_END)
			switch (i) {
				case 1:
					return 13;
				case 5:
					return 1;
				case 6:
					return 2;
				case 7:
					return 3;
				default:
					break;
			}
		}
		mask <<= 1;
	}
	return 0;
}

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
    0xc0,                          // END_COLLECTION
    0x06, 0x00, 0xff,              // USAGE_PAGE (Generic Desktop)
    0x09, 0x01,                    // USAGE (Vendor Usage 1)
    0xa1, 0x01,                    // COLLECTION (Application)
    0x85, 0x02,                    //   REPORT_ID (2)
    0x15, 0x00,                    //   LOGICAL_MINIMUM (0)
    0x26, 0xff, 0x00,              //   LOGICAL_MAXIMUM (255)
    0x75, 0x08,                    //   REPORT_SIZE (8)
    0x95, REPORT_COUNT,                    //   REPORT_COUNT (128)
    0x09, 0x00,                    //   USAGE (Undefined)
    0x91, 0x00,                    //   OUTPUT (Data,Ary,Abs)
    0x75, 0x08,                    //   REPORT_SIZE (8)
    0x95, REPORT_COUNT,                    //   REPORT_COUNT (128)
    0x09, 0x00,                    //   USAGE (Undefined)
    0xb1, 0x00,                    //   FEATURE (Data,Ary,Abs)
    0xc0                           // END_COLLECTION
};

//проверка установленных перемычек - ни одной перемычки для 1 раскладки, левая для 2, правая для 3, обе для 4
static void readConf() {
	char p;
	p = 4;
	p -= (PIND & (1 << 3)) >> 3;
	p -= (PIND & (1 << 4)) >> 3;
	active_layout = p;
}

//запись указанной раскладки в память
static void writeLayout(uchar *layout, uchar layNum) {
	switch (layNum) {
		case 1:
			//eeprom_write_block(layout, (void *) eeprom_lay1, NUM_KEYS);
			for (int i = 0; i < NUM_KEYS; i++) {
				eeprom_write_byte( (uint8_t *) (eeprom_lay1 + i), layout[i]);
			}
			break;
		case 2:
			//eeprom_write_block(layout, (void *) eeprom_lay2, NUM_KEYS);
			for (int i = 0; i < NUM_KEYS; i++) {
				eeprom_write_byte( (uint8_t *) (eeprom_lay2 + i), layout[i]);
			}
			break;
		case 3:
			//eeprom_write_block(layout, (void *) eeprom_lay3, NUM_KEYS);
			for (int i = 0; i < NUM_KEYS; i++) {
				eeprom_write_byte( (uint8_t *) (eeprom_lay3 + i), layout[i]);
			}
			break;
		case 4:
			//eeprom_write_block(layout, (void *) eeprom_lay4, NUM_KEYS);
			for (int i = 0; i < NUM_KEYS; i++) {
				eeprom_write_byte( (uint8_t *) (eeprom_lay4 + i), layout[i]);
			}
			break;
	}
}

//чтение указанной раскладки из памяти
static void readLayout() {
	uchar keys[NUM_KEYS];
	switch (active_layout) {
		case 1:
			eeprom_read_block(keys, (const void *) (uintptr_t) eeprom_lay1, NUM_KEYS);
			break;
		case 2:
			eeprom_read_block(keys, (const void *) (uintptr_t) eeprom_lay2, NUM_KEYS);
			break;
		case 3:
			eeprom_read_block(keys, (const void *) (uintptr_t) eeprom_lay3, NUM_KEYS);
			break;
		case 4:
			eeprom_read_block(keys, (const void *) (uintptr_t) eeprom_lay4, NUM_KEYS);
			break;
		default:
			for (int i=0; i<NUM_KEYS; i++) {
				keys[i] = dKey;
			}
	}
//заполнение массива клавиш выбранной раскладкой
	keyReport[0][0] = 0;
	keyReport[0][1] = 0;
	for (int i=0; i < NUM_KEYS; i++) {
		keyReport[i+1][0] = 0;
		keyReport[i+1][1] = keys[i];
	}
}

//инициализация настроек при первом запуске
static void firstRun() {
	uchar initial[NUM_KEYS];
	//массив дефолтных клавиш
	/*for(uchar i=0; i<NUM_KEYS; i++) {
		initial[i] = dKey + i;
	}*/
	initial[1] = 15;
	initial[2] = 4;
	initial[3] = 28;
	initial[4] = 18;
	initial[5] = 24;
	initial[6] = 23;
	for(uchar i=7; i<NUM_KEYS; i++) {
		initial[i] = 41;
	}
	//установка флага инициализации в памяти
	eeprom_write_byte( (uint8_t *) (uintptr_t) eeprom_init, 1);
	//запись количества кнопок в память
	eeprom_write_byte( (uint8_t *) (uintptr_t) eeprom_len, NUM_KEYS);
	//запись дефолтной клавиши в память
	eeprom_write_byte( (uint8_t *) (uintptr_t) eeprom_def, dKey);
	//запись раскладок
	initial[0] = 30;
	writeLayout(initial, 1);
	initial[0] = 31;
	writeLayout(initial, 2);
	initial[0] = 32;
	writeLayout(initial, 3);
	initial[0] = 33;
	writeLayout(initial, 4);
	//заполнение массива клавиш
	readConf();
	readLayout();
}

static void buildReport(uchar key)
{
	reportBuffer[2] = keyReport[key][1];
	reportBuffer[1] = keyReport[key][0];
	reportBuffer[0] = 1;
}

//построение пакета с настройками
static void buildFeatureReport() {
	uchar l1[NUM_KEYS];
	uchar l2[NUM_KEYS];
	uchar l3[NUM_KEYS];
	uchar l4[NUM_KEYS];
	
	eeprom_read_block(l1, (const void *) (uintptr_t) eeprom_lay1, NUM_KEYS);
	eeprom_read_block(l2, (const void *) (uintptr_t) eeprom_lay2, NUM_KEYS);
	eeprom_read_block(l3, (const void *) (uintptr_t) eeprom_lay3, NUM_KEYS);
	eeprom_read_block(l4, (const void *) (uintptr_t) eeprom_lay4, NUM_KEYS);
	
	//обнуление буфера, 1 байт - кол-во клавиш, начиная со 2 байта раскладки
	memset(&msgbuf[0], 0xFF, sizeof(msgbuf));
	memcpy(&msgbuf[0], &keyCount, sizeof(uchar));
	memcpy(&msgbuf[1], &active_layout, sizeof(uchar));
	memcpy(&msgbuf[2], l1, sizeof(uchar)*NUM_KEYS);
	memcpy(&msgbuf[2+NUM_KEYS], l2, sizeof(uchar)*NUM_KEYS);
	memcpy(&msgbuf[2+NUM_KEYS*2], l3, sizeof(uchar)*NUM_KEYS);
	memcpy(&msgbuf[2+NUM_KEYS*3], l4, sizeof(uchar)*NUM_KEYS);
	
}

//разбираем пришедший пакет с настройками
static void parseFeatureReport() {
	//первый байт - команда, 1 - запись настроек
	if (msgbuf[1] == 0x01) {
		uchar l1[NUM_KEYS];
		uchar l2[NUM_KEYS];
		uchar l3[NUM_KEYS];
		uchar l4[NUM_KEYS];
	
		memcpy(&l1, &msgbuf[2], NUM_KEYS);
		memcpy(&l2, &msgbuf[2+NUM_KEYS], NUM_KEYS);
		memcpy(&l3, &msgbuf[2+NUM_KEYS*2], NUM_KEYS);
		memcpy(&l4, &msgbuf[2+NUM_KEYS*3], NUM_KEYS);
	
		eeprom_write_block(l1, (void *) (uintptr_t) eeprom_lay1, NUM_KEYS);
		eeprom_write_block(l2, (void *) (uintptr_t) eeprom_lay2, NUM_KEYS);
		eeprom_write_block(l3, (void *) (uintptr_t) eeprom_lay3, NUM_KEYS);
		eeprom_write_block(l4, (void *) (uintptr_t) eeprom_lay4, NUM_KEYS);
		
		readLayout();
	}
}

uchar	usbFunctionSetup(uchar data[8]) {
	usbRequest_t    *rq = (void *)data;

	if((rq->bmRequestType & USBRQ_TYPE_MASK) == USBRQ_TYPE_CLASS){
		if(rq->bRequest == USBRQ_HID_GET_REPORT){
			bytesRemaining = REPORT_COUNT;
			currentAddress = 0;
			return USB_NO_MSG;
		}else if(rq->bRequest == USBRQ_HID_SET_REPORT){
			bytesRemaining = REPORT_COUNT;
			currentAddress = 0;
			return USB_NO_MSG;
		}else if(rq->bRequest == USBRQ_HID_GET_IDLE){
			usbMsgPtr = &idleRate;
			return 1;
		}else if(rq->bRequest == USBRQ_HID_SET_IDLE){
			idleRate = rq->wValue.bytes[1];
		}
	}else{
	/* ignore vendor type requests, we don't use any */
	}
	return 0;
}

uchar usbFunctionRead(uchar *data, uchar len) {
	//строим пакет перед началом отправки
	if(bytesRemaining == REPORT_COUNT){
		buildFeatureReport();
	}
	if(len > bytesRemaining)
		len = bytesRemaining;
	memcpy( data, msgbuf + currentAddress, len);
    currentAddress += len;
    bytesRemaining -= len;
    return len;
}

uchar usbFunctionWrite(uchar *data, uchar len) {
    if(bytesRemaining == 0){
		parseFeatureReport();
		return 1;            
	}
    if(len > bytesRemaining)
		len = bytesRemaining;
	memcpy( msgbuf+currentAddress, data, len );
    currentAddress += len;
    bytesRemaining -= len;
    if(bytesRemaining == 0){
		parseFeatureReport();
		return 1;
	}
	return bytesRemaining == 0;
}

/* ------------------------------------------------------------------------- */

int	main(void)
{
uchar   key, lastKey = 0, keyDidChange = 0, idleCounter = 0;
	wdt_enable(WDTO_1S);
    hardwareInit();
	//если флаг инициализации стоит - загружаем настройки
	if(eeprom_read_byte( (uint8_t *) (uintptr_t) eeprom_init) != 1) {
		firstRun();
	} else {
		readConf();
		readLayout();
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
			buildReport(key);
			usbSetInterrupt(reportBuffer, sizeof(reportBuffer));
			keyDidChange = 0;
        }
	}
	return 0;
}

/* ------------------------------------------------------------------------- */
