#define LCD_CS A3 // Chip Select goes to Analog 3
#define LCD_CD A2 // Command/Data goes to Analog 2
#define LCD_WR A1 // LCD Write goes to Analog 1
#define LCD_RD A0 // LCD Read goes to Analog 0
#define LCD_RESET A4 // Can alternately just connect to Arduino's reset pin

#define SERIAL_RX_BUFFER_SIZE 256

#include <Adafruit_GFX.h> // Hardware-specific library
#include <MCUFRIEND_kbv.h>

MCUFRIEND_kbv tft;

String msg = "";
byte currentState = 0;
byte rcvd[2];

void setup(void) {
	tft.reset();
	Serial.begin(115200);
	tft.begin(tft.readID());
	tft.setRotation(0);
	tft.fillScreen(0);
	tft.setTextSize(2);
	tft.setTextColor(0xFFFF);
	tft.setCursor(0, 0);
}

void loop(void) {
	if (Serial.available() > 0) {
		char c = (char)Serial.read();
		switch (currentState) {
		case 0:
			if (c == 36) {
				currentState++;
				msg = "";
			}
			break;
		case 1:
			if (c == 38) currentState++;
			else msg += c;
			break;
		case 2:
			break;
		default:
			break;
		}
	}
	if (currentState == 2) {
		int vars[5] = { 0,0,0,0,0 };
		int index = 0;
		for (int i = 0; i < msg.length(); i++) {
			if (msg[i] > 47 && msg[i] < 58) {
				vars[index] *= 10;
				vars[index] += (msg[i] - 48);
			} else if (msg[i] == 44) index++;
		}
		int x = vars[0];
		int y = vars[1];
		int s = vars[4];
		int w = vars[2] * s;
		int h = vars[3] * s;
		tft.setCursor(vars[0], vars[1]);
		while (Serial.available()) {
			for (int16_t j = y; j < h+y; j+=s) {
				for (int16_t i = x; i < w+x; i+=s) {
					Serial.readBytes(rcvd, 2);
					uint16_t color = rcvd[1] + (rcvd[0] << 8);
					if (s > 1) tft.writeFillRect(i, j, s, s, color);
					else tft.writePixel(i, j, color);
				}
			}
			break;
		}
		currentState = 0;
	}
}
