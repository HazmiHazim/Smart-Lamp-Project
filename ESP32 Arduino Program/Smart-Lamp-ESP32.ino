// Access Wifi Library & Asynchronous Web Server for ESP32
#include <WiFi.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>

// LED 1
#define LED1_RED 15   // GPIO 15
#define LED1_GREEN 2  // GPIO 2
#define LED1_BLUE 4   // GPIO 4

// LED 2
#define LED2_RED 5     // GPIO 5
#define LED2_GREEN 18  // GPIO 18
#define LED2_BLUE 19   // GPIO 19

// LED 3
#define LED3_RED 21    // GPIO 21
#define LED3_GREEN 22  // GPIO 22
#define LED3_BLUE 23   // GPIO 23

// Setting PWM Properties
#define FREQUENCY 5000
// Channel for LED 1
#define LED1_CHANNEL_RED 0      // Channel 0
#define LED1_CHANNEL_GREEN 1    // Channel 1
#define LED1_CHANNEL_BLUE 2     // Channel 2
// Channel for LED 2
#define LED2_CHANNEL_RED 3      // Channel 3
#define LED2_CHANNEL_GREEN 4    // Channel 4
#define LED2_CHANNEL_BLUE 5     // Channel 5
// Channel for LED 3
#define LED3_CHANNEL_RED 6      // Channel 6
#define LED3_CHANNEL_GREEN 7    // Channel 7
#define LED3_CHANNEL_BLUE 8     // Channel 8
#define RESOLUTION 8

// Create a Server on Port 80
AsyncWebServer server(80);

// Setting Configuration for Network ESP32
const char *ssid = "ESP32-SMART-LAMP";
const char *password = "123456789";

// Set Parameter String To Be Received from HTTP Request
const char *REQUEST_VALUE = "value";
const char *REQUEST_TIMER_VALUE = "timer";
const char *REQUEST_STOP_TIMER = "stop";
const char *REQUEST_RED_VALUE = "red";
const char *REQUEST_GREEN_VALUE = "green";
const char *REQUEST_BLUE_VALUE = "blue";

// Define hw_timer_t to Configure The Timer
hw_timer_t *timer = NULL;

// Global Volatile Variable To Store The Shared LED Channel Value
volatile int LEDChannelRed;
volatile int LEDChannelGreen;
volatile int LEDChannelBlue;

void setup() {
  // Callback Function for Wi-Fi Connection
  wifiConnection();
  // Set LED 1  Pin Mode
  setLEDPinMode(LED1_RED, LED1_GREEN, LED1_BLUE, OUTPUT);
  // Set LED 2 Pin Mode
  setLEDPinMode(LED2_RED, LED2_GREEN, LED2_BLUE, OUTPUT);
  // Set LED 3 Pin Mode
  setLEDPinMode(LED3_RED, LED3_GREEN, LED3_BLUE, OUTPUT);

  // Set LED 1 "OFF" By Default
  setDefaultLEDState(LED1_RED, LED1_GREEN, LED1_BLUE, LOW);
  // Set LED 2 "OFF" By Default
  setDefaultLEDState(LED2_RED, LED2_GREEN, LED2_BLUE, LOW);
  // Set LED 3 "OFF" By Default
  setDefaultLEDState(LED3_RED, LED3_GREEN, LED3_BLUE, LOW);

  // Configure Each LED Channel to It's PWM Functionalities
  pwmConfiguration(LED1_CHANNEL_RED, LED1_CHANNEL_GREEN, LED1_CHANNEL_BLUE, FREQUENCY, RESOLUTION);
  pwmConfiguration(LED2_CHANNEL_RED, LED2_CHANNEL_GREEN, LED2_CHANNEL_BLUE, FREQUENCY, RESOLUTION);
  pwmConfiguration(LED3_CHANNEL_RED, LED3_CHANNEL_GREEN, LED3_CHANNEL_BLUE, FREQUENCY, RESOLUTION);

  // Set Each LED to It's Channel
  pwmAttachPin(LED1_RED, LED1_GREEN, LED1_BLUE, LED1_CHANNEL_RED, LED1_CHANNEL_GREEN, LED1_CHANNEL_BLUE);
  pwmAttachPin(LED2_RED, LED2_GREEN, LED2_BLUE, LED2_CHANNEL_RED, LED2_CHANNEL_GREEN, LED2_CHANNEL_BLUE);
  pwmAttachPin(LED3_RED, LED3_GREEN, LED3_BLUE, LED3_CHANNEL_RED, LED3_CHANNEL_GREEN, LED3_CHANNEL_BLUE);

  // Received Request for Connection with Client
  server.on("/", HTTP_GET, [](AsyncWebServerRequest *request) {
    getClient(request);
  });

  // Turn On LED 1
  server.on("/lamp1/on", HTTP_POST, [](AsyncWebServerRequest *request) {
    controlLED(request, LED1_CHANNEL_RED, LED1_CHANNEL_GREEN, LED1_CHANNEL_BLUE);
  });
  // Turn Off LED 1
  server.on("/lamp1/off", HTTP_POST, [](AsyncWebServerRequest *request) {
    controlLED(request, LED1_CHANNEL_RED, LED1_CHANNEL_GREEN, LED1_CHANNEL_BLUE);
  });

  // Turn On LED 2
  server.on("/lamp2/on", HTTP_POST, [](AsyncWebServerRequest *request) {
    controlLED(request, LED2_CHANNEL_RED, LED2_CHANNEL_GREEN, LED2_CHANNEL_BLUE);
  });
  // Turn Off LED 2
  server.on("/lamp2/off", HTTP_POST, [](AsyncWebServerRequest *request) {
    controlLED(request, LED2_CHANNEL_RED, LED2_CHANNEL_GREEN, LED2_CHANNEL_BLUE);
  });

  // Turn On LED 3
  server.on("/lamp3/on", HTTP_POST, [](AsyncWebServerRequest *request) {
    controlLED(request, LED3_CHANNEL_RED, LED3_CHANNEL_GREEN, LED3_CHANNEL_BLUE);
  });
  // Turn Off LED 3
  server.on("/lamp3/off", HTTP_POST, [](AsyncWebServerRequest *request) {
    controlLED(request, LED3_CHANNEL_RED, LED3_CHANNEL_GREEN, LED3_CHANNEL_BLUE);
  });

  // Set Colour for LED 1
  server.on("/lamp1/colour", HTTP_POST, [](AsyncWebServerRequest *request) {
    setColour(request, LED1_CHANNEL_RED, LED1_CHANNEL_GREEN, LED1_CHANNEL_BLUE);
  });
  // Set Colour for LED 2
  server.on("/lamp2/colour", HTTP_POST, [](AsyncWebServerRequest *request) {
    setColour(request, LED2_CHANNEL_RED, LED2_CHANNEL_GREEN, LED2_CHANNEL_BLUE);
  });
  // Set Colour for LED 3
  server.on("/lamp3/colour", HTTP_POST, [](AsyncWebServerRequest *request) {
    setColour(request, LED3_CHANNEL_RED, LED3_CHANNEL_GREEN, LED3_CHANNEL_BLUE);
  });

  // Set Time for LED 1 to Turn Off
  server.on("/lamp1", HTTP_POST, [](AsyncWebServerRequest *request) {
    setTimer(request, LED1_CHANNEL_RED, LED1_CHANNEL_GREEN, LED1_CHANNEL_BLUE);
  });
  // Set Time for LED 2 to Turn Off
  server.on("/lamp2", HTTP_POST, [](AsyncWebServerRequest *request) {
    setTimer(request, LED2_CHANNEL_RED, LED2_CHANNEL_GREEN, LED2_CHANNEL_BLUE);
  });
  // Set Time for LED 3 to Turn Off
  server.on("/lamp3", HTTP_POST, [](AsyncWebServerRequest *request) {
    setTimer(request, LED3_CHANNEL_RED, LED3_CHANNEL_GREEN, LED3_CHANNEL_BLUE);
  });

  // Stop Timer Interrupt if Requested
  server.on("/timer", HTTP_POST, [](AsyncWebServerRequest *request) {
    stopTimerInterrupt(request);
  });
}

void loop() {
  // No Code Used to Loop the LEDs.
}

// Function for Connecting to Wi-Fi Network with SSID and Password
void wifiConnection() {
  Serial.begin(115200);
  delay(1000);
  WiFi.mode(WIFI_STA);
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.softAP(ssid, password);
  Serial.println("");
  Serial.println("Wifi connected.");
  Serial.println("IP address: ");
  Serial.println(WiFi.softAPIP());
  Serial.println("");
  // Start The Server
  server.begin();
  Serial.println("HTTP server started.");
}

// Function to Set Pin Mode
void setLEDPinMode(int red, int green, int blue, uint8_t mode) {
  pinMode(red, mode);
  pinMode(green, mode);
  pinMode(blue, mode);
}

// Function to Set Default State of LEDs
void setDefaultLEDState(int red, int green, int blue, int state) {
  digitalWrite(red, state);
  digitalWrite(green, state);
  digitalWrite(blue, state);
}

// Function to Configure LED PWM Functionalitites
void pwmConfiguration(int rChannel, int gChannel, int bChannel, int frequency, int resolution) {
  ledcSetup(rChannel, frequency, resolution);
  ledcSetup(gChannel, frequency, resolution);
  ledcSetup(bChannel, frequency, resolution);
}

// Function to Attach the Channel to The GPIO to be Controlled
void pwmAttachPin(int red, int green, int blue, int rChannel, int gChannel, int bChannel) {
  ledcAttachPin(red, rChannel);
  ledcAttachPin(green, gChannel);
  ledcAttachPin(blue, bChannel);
}

// Function to Received Connection Using HTTP GET
void getClient(AsyncWebServerRequest *request) {
  if (request->method() == HTTP_GET) {
    request->send(200, "text/plain", "Connected!");
    Serial.println("Connected!");
  }
}

// Function to Controlled The Lamp Using Application
void controlLED(AsyncWebServerRequest *request, int rChannel, int gChannel, int bChannel) {
  if (request->method() == HTTP_POST) {
    if (request->hasParam(REQUEST_VALUE)) {
      String value = request->getParam(REQUEST_VALUE)->value();
      if (value.toInt() > 0 && value.toInt() <= 255) {
        ledcWrite(rChannel, value.toInt());
        ledcWrite(gChannel, value.toInt());
        ledcWrite(bChannel, value.toInt());
        switch (rChannel) {
          case 0:
            Serial.println("LED 1 Turn On.");
            break;
          case 3:
            Serial.println("LED 2 Turn On.");
            break;
          case 6:
            Serial.println("LED 3 Turn On.");
            break;
          default:
            Serial.println("Wrong Channel!");
        }
      }
    } else {
      ledcWrite(rChannel, 0);
      ledcWrite(gChannel, 0);
      ledcWrite(bChannel, 0);
      switch (rChannel) {
        case 0:
          Serial.println("LED 1 Turn Off.");
          break;
        case 3:
          Serial.println("LED 2 Turn Off.");
          break;
        case 6:
          Serial.println("LED 3 Turn Off.");
          break;
        default:
          Serial.println("Wrong Channel!");
      }
    }
  }
  request->send(200, "text/plain", "OK");
}

// If Timer is on Time Start Interupting
void IRAM_ATTR onTimer() {
  ledcWrite(LEDChannelRed, 0);
  ledcWrite(LEDChannelGreen, 0);
  ledcWrite(LEDChannelBlue, 0);
  switch (LEDChannelRed) {
    case 0:
      Serial.println("LED 1 Turn Off.");
      break;
    case 3:
      Serial.println("LED 2 Turn Off.");
      break;
    case 6:
      Serial.println("LED 3 Turn Off.");
      break;
    default:
      Serial.println("Wrong Channel!");
  }
  // Detach the Interrupt From the Timer if The Timer is Running
  if (timer) {
    timerDetachInterrupt(timer);
    timer = NULL;
    Serial.println("Interrupt Detached.");
  }
}

// Function to Set Timer for Lamp Using Application
void setTimer(AsyncWebServerRequest *request, int rChannel, int gChannel, int bChannel) {
  if (request->method() == HTTP_POST) {
    if (request->hasParam(REQUEST_TIMER_VALUE)) {
      // Detach the Interrupt From the Timer if The Timer is Running
      if (timer) {
        timerDetachInterrupt(timer);
        timer = NULL;
        Serial.println("Interrupt Detached.");
      }
      String value = request->getParam(REQUEST_TIMER_VALUE)->value();
      int timerValue = value.toInt();
      int timerValueMicro = timerValue * 1000000;
      LEDChannelRed = rChannel;
      LEDChannelGreen = gChannel;
      LEDChannelBlue = bChannel;
      timer = timerBegin(0, 80, true);
      timerAttachInterrupt(timer, &onTimer, true);
      timerAlarmWrite(timer, timerValueMicro, false);  // 3rd Param Set to False to Make the Timer Running Only Once and Not Repeating Again.
      timerAlarmEnable(timer);
      Serial.print("Time Set: ");
      Serial.print(timerValue);
      Serial.print(" seconds for ");
      switch (LEDChannelRed) {
        case 0:
          Serial.print("LED 1.");
          break;
        case 3:
          Serial.print("LED 2.");
          break;
        case 6:
          Serial.print("LED 3.");
          break;
        default:
          Serial.print("Wrong Channel.");
      }
      Serial.println("");
    }
  }
  request->send(200, "text/plain", "OK");
}

// Function to Stop Timer by Request Using Application
void stopTimerInterrupt(AsyncWebServerRequest *request) {
  if (request->method() == HTTP_POST) {
    if (request->hasParam(REQUEST_STOP_TIMER)) {
      timerDetachInterrupt(timer);
      timer = NULL;
      Serial.println("Timer Stopped.");
    }
  }
  request->send(200, "text/plain", "Timer Stopped.");
}

// Function to Set Colour by Request Using Application
void setColour(AsyncWebServerRequest *request, int rChannel, int gChannel, int bChannel) {
  if (request->method() == HTTP_POST) {
    if (request->hasParam(REQUEST_RED_VALUE) && request->hasParam(REQUEST_GREEN_VALUE) && request->hasParam(REQUEST_BLUE_VALUE)) {
      String redStringValue = request->getParam(REQUEST_RED_VALUE)->value();
      String greenStringValue = request->getParam(REQUEST_GREEN_VALUE)->value();
      String blueStringValue = request->getParam(REQUEST_BLUE_VALUE)->value();
      int redValue = redStringValue.toInt();
      int greenValue = greenStringValue.toInt();
      int blueValue = blueStringValue.toInt();
      ledcWrite(rChannel, redValue);
      ledcWrite(gChannel, greenValue);
      ledcWrite(bChannel, blueValue);
      switch (rChannel) {
        case 0:
          Serial.print("Change Colour to RGB(");
          Serial.print(redValue);
          Serial.print(", ");
          Serial.print(greenValue);
          Serial.print(", ");
          Serial.print(blueValue);
          Serial.print(") For LED 1.");
          break;
        case 3:
          Serial.print("Change Colour to RGB(");
          Serial.print(redValue);
          Serial.print(", ");
          Serial.print(greenValue);
          Serial.print(", ");
          Serial.print(blueValue);
          Serial.print(") For LED 2.");
          break;
        case 6:
          Serial.print("Change Colour to RGB(");
          Serial.print(redValue);
          Serial.print(", ");
          Serial.print(greenValue);
          Serial.print(", ");
          Serial.print(blueValue);
          Serial.print(") For LED 3.");
          break;
        default:
          Serial.print("Wrong Channel.");
      }
      Serial.println("");
    }
  }
  request->send(200, "text/plain", "OK");
}