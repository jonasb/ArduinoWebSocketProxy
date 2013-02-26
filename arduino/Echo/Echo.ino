#include <AndroidAccessory.h>

AndroidAccessory usb("Wigwam Labs",
                      "Arduino WebSocket proxy", 
                      "Enables HTML pages to communicate with an Arduino",
                      "1.0",
                      "http://wigwamlabs.com",
                      "1");

int pinConnectionLed = 13;

void setup() {
  usb.begin();
  pinMode(pinConnectionLed, OUTPUT);
}

void loop() {
  if (usb.isConnected()) {
    digitalWrite(pinConnectionLed, HIGH);
    if (usb.available() > 0) {
      int d = usb.read();
      usb.write(d);
    }
  } else {
    digitalWrite(pinConnectionLed, LOW);
  }
}

