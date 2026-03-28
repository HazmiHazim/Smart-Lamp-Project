#include "BLEDevice.h"
#include "BLEServer.h"
#include "BLEUtils.h"
#include "BLE2902.h"

// Setting PWM Properties
#define FREQUENCY 5000
#define RESOLUTION 8

// Define the Bluetooth device name
const char *BLE_NAME = "ESP32-Smart-Lamp";
const char *SERVICE_UUID = "UUID";

struct RGBPin
{
  int red;
  int green;
  int blue;
  int rChannel;
  int gChannel;
  int bChannel;
};

struct LEDUUID
{
  const char *rx;
  const char *tx;
};

class RGBLED
{
  RGBPin pin;
  int brightness;
  int red, green, blue;

public:
  RGBLED(RGBPin ledPin) : pin(ledPin), brightness(0), red(255), green(255), blue(255) {}

  void begin()
  {
    // Configure LED PWM
    ledcAttachChannel(pin.red, FREQUENCY, RESOLUTION, pin.rChannel);
    ledcAttachChannel(pin.green, FREQUENCY, RESOLUTION, pin.gChannel);
    ledcAttachChannel(pin.blue, FREQUENCY, RESOLUTION, pin.bChannel);

    applyLamp();
  }

  void applyLamp()
  {
    uint32_t redDuty = (red * brightness) / 255;
    uint32_t greenDuty = (green * brightness) / 255;
    uint32_t blueDuty = (blue * brightness) / 255;

    ledcWriteChannel(pin.rChannel, redDuty);
    ledcWriteChannel(pin.gChannel, greenDuty);
    ledcWriteChannel(pin.bChannel, blueDuty);
  }

  void setBrightness(int value)
  {
    brightness = constrain(value, 0, 255);
    applyLamp();
    Serial.printf("Brightness set to %d\n", brightness);
  }

  void setColour(int redValue, int greenValue, int blueValue)
  {
    red = redValue;
    green = greenValue;
    blue = blueValue;

    if (brightness == 0)
    {
      brightness = 255;
    }

    applyLamp();
    Serial.printf("Color set to R:%d G:%d B:%d\n", red, green, blue);
  }

  void turnOff()
  {
    brightness = 0;
    applyLamp();
    Serial.println("LED turned OFF");
  }

  int getBrightness() const { return brightness; }
};

class BLELedController : public BLECharacteristicCallbacks, public BLEServerCallbacks
{
private:
  BLECharacteristic *txCharacteristics[3];
  BLECharacteristic *rxCharacteristics[3];
  RGBLED *leds[3];
  LEDUUID uuids[3];
  BLEService *pService;

public:
  BLELedController(RGBLED *l1, RGBLED *l2, RGBLED *l3, const LEDUUID (&uuidList)[3]) : leds{l1, l2, l3}
  {
    for (int i = 0; i < 3; i++)
    {
      uuids[i] = uuidList[i];
    }
  }

  void begin()
  {
    BLEDevice::init(BLE_NAME);
    BLEServer *pServer = BLEDevice::createServer();
    pServer->setCallbacks(this);

    pService = pServer->createService(SERVICE_UUID);

    // Create 3 LED characteristic pairs
    for (int i = 0; i < 3; i++)
    {
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
  void onConnect(BLEServer *pServer) override
  {
    Serial.println("Client connected.");
  }

  void onDisconnect(BLEServer *pServer) override
  {
    Serial.println("Client disconnected.");
  }

  // === BLECharacteristicCallbacks ===
  void onWrite(BLECharacteristic *pCharacteristic) override
  {
    for (int i = 0; i < 3; i++)
    {
      if (pCharacteristic == rxCharacteristics[i])
      {
        handleWriteForLED(*leds[i], pCharacteristic, i);
        break;
      }
    }
  }

private:
  void handleWriteForLED(RGBLED &led, BLECharacteristic *pCharacteristic, int index)
  {
    String rawValue = pCharacteristic->getValue();
    size_t length = rawValue.length();
    Serial.printf("Received %d bytes\n", length);

    if (length < 3) {
      Serial.println("Not enough bytes, ignoring");
      return;
    }

    uint8_t red = (uint8_t)rawValue[0];
    uint8_t green = (uint8_t)rawValue[1];
    uint8_t blue = (uint8_t)rawValue[2];
    Serial.printf("RGB: %d, %d, %d\n", red, green, blue);

    // Turn off if all channels are zero
    if (red == 0 && green == 0 && blue == 0)
    {
      led.turnOff();
      sendNotification(index, 0);
      Serial.println("LED OFF (RGB = 0,0,0)");
      return;
    }

    // Color or Brightness (all values equal -> brightness)
    if (red == green && green == blue)
    {
      uint8_t brightness = red;
      led.setBrightness(brightness);
      sendNotification(index, brightness);
    }
    else
    {
      // Color: normal 3-channel RGB
      led.setColour(red, green, blue);
      sendNotification(index, red, green, blue);
    }
  }

  // For brightness mode
  void sendNotification(int ledIndex, int brightness)
  {
    char msg[32];
    snprintf(msg, sizeof(msg), "LED%d brightness: %d", ledIndex + 1, brightness);
    txCharacteristics[ledIndex]->setValue(msg);
    txCharacteristics[ledIndex]->notify();
  }

  // For colour mode
  void sendNotification(int ledIndex, uint8_t r, uint8_t g, uint8_t b)
  {
    char msg[32];
    snprintf(msg, sizeof(msg), "LED%d color: (%d,%d,%d)", ledIndex + 1, r, g, b);
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

void setup()
{
  Serial.begin(115200); // Initialize the serial port
  led1.begin();
  led2.begin();
  led3.begin();
  bleController.begin();
}

void loop()
{
  // Nothing needed — BLE callbacks handle everything
}