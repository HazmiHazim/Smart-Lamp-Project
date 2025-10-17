#include "BLEDevice.h"
#include "BLEServer.h"
#include "BLEUtils.h"
#include "BLE2902.h"

// Setting PWM Properties
#define FREQUENCY 5000
#define RESOLUTION 8

// Define the Bluetooth device name
const char* BLE_NAME = "ESP32-Smart-Lamp";
const char* SERVICE_UUID = "UUID";

struct RGBPin {
  int red;
  int green;
  int blue;
  int rChannel;
  int gChannel;
  int bChannel;
};

struct LEDUUID {
  const char *rx;
  const char *tx;
};

class RGBLED {
  RGBPin pin;
  int brightness;

  public:
    RGBLED(RGBPin ledPin) : pin(ledPin), brightness(0) {}

    void begin() {
      // Configure LED PWM
      ledcAttachChannel(pin.red, FREQUENCY, RESOLUTION, pin.rChannel);
      ledcAttachChannel(pin.green, FREQUENCY, RESOLUTION, pin.gChannel);
      ledcAttachChannel(pin.blue, FREQUENCY, RESOLUTION, pin.bChannel);

      // Initialize OFF
      setBrightness(0);
    }

    void setBrightness(int value) {
      brightness = constrain(value, 0, 255);
      ledcWriteChannel(pin.rChannel, brightness);
      ledcWriteChannel(pin.gChannel, brightness);
      ledcWriteChannel(pin.bChannel, brightness);

      if (brightness > 0)
        Serial.printf("LED (R%d,G%d,B%d) → ON (%d)\n", pin.red, pin.green, pin.blue, brightness);
      else
        Serial.printf("LED (R%d,G%d,B%d) → OFF\n", pin.red, pin.green, pin.blue);
    }

    void turnOff() { setBrightness(0); }
    int getBrightness() const { return brightness; }
};

class BLELedController : public BLECharacteristicCallbacks, public BLEServerCallbacks {
  private:
    BLECharacteristic *txCharacteristics[3];
    BLECharacteristic *rxCharacteristics[3];
    RGBLED *leds[3];
    LEDUUID uuids[3];
    BLEService *pService;

  public:
    BLELedController(RGBLED *l1, RGBLED *l2, RGBLED *l3, const LEDUUID (&uuidList)[3]) : leds{l1, l2, l3} {
      for (int i = 0; i < 3; i++) {
        uuids[i] = uuidList[i];
      }
    }

    void begin() {
      BLEDevice::init(BLE_NAME);
      BLEServer *pServer = BLEDevice::createServer();
      pServer->setCallbacks(this);

      pService = pServer->createService(SERVICE_UUID);

      // Create 3 LED characteristic pairs
      for (int i = 0; i < 3; i++) {
        txCharacteristics[i] = pService->createCharacteristic(uuids[i].tx, BLECharacteristic::PROPERTY_NOTIFY);
        txCharacteristics[i]->addDescriptor(new BLE2902());

        rxCharacteristics[i] = pService->createCharacteristic(uuids[i].rx, BLECharacteristic::PROPERTY_WRITE);
        rxCharacteristics[i]->setCallbacks(this);
      }

      pService->start(); // Start the service
      BLEAdvertising *pAdvertising = pServer->getAdvertising();
      pAdvertising->addServiceUUID(SERVICE_UUID);
      pAdvertising->setScanResponse(true);
      pServer->getAdvertising()->start();

      Serial.println("BLE Ready — Waiting for client connection...");
    }

    // === BLEServerCallbacks ===
    void onConnect(BLEServer *pServer) override {
      Serial.println("Client connected.");
    }

    void onDisconnect(BLEServer *pServer) override {
      Serial.println("Client disconnected.");
    }

    // === BLECharacteristicCallbacks ===
    void onWrite(BLECharacteristic *pCharacteristic) override {
      for (int i = 0; i < 3; i++) {
        if (pCharacteristic == rxCharacteristics[i]) {
          handleWriteForLED(*leds[i], pCharacteristic, i);
          break;
        }
      }
    }

  private:
    void handleWriteForLED(RGBLED &led, BLECharacteristic *pCharacteristic, int index) {
      std::string valueStr = std::string(pCharacteristic->getValue().c_str());
      if (valueStr.empty()) return;

      int brightness = 0;
      try {
        brightness = std::stoi(valueStr);
      } catch (...) {
        brightness = 0;
      }

      if (brightness >= 0 && brightness <= 255) {
        led.setBrightness(brightness);
        sendNotification(index, brightness);
      } else {
        led.turnOff();
        sendNotification(index, 0);
      }
    }

    void sendNotification(int ledIndex, int brightness) {
      char msg[32];
      snprintf(msg, sizeof(msg), "LED%d brightness: %d", ledIndex + 1, brightness);
      txCharacteristics[ledIndex]->setValue(msg);
      txCharacteristics[ledIndex]->notify();
    }
};

// ======== GLOBAL OBJECTS ========
RGBLED led1({15, 2, 4, 0, 1, 2});
RGBLED led2({5, 18, 19, 3, 4, 5});
RGBLED led3({21, 22, 23, 6, 7, 8});

LEDUUID ledUUIDs[3] = {
  {"UUID", "UUID"},
  {"UUID", "UUID"},
  {"UUID", "UUID"}
};

BLELedController bleController(&led1, &led2, &led3, ledUUIDs);

void setup() {
  Serial.begin(115200);      // Initialize the serial port
  led1.begin();
  led2.begin();
  led3.begin();
  bleController.begin();
}

void loop() {
  // Nothing needed — BLE callbacks handle everything
}