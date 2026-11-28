/*
 * JWS TV Masjid Al-Gaffar - ESP32 8-Channel Relay Controller
 * Mendukung:
 * 1. USB OTG Serial (Protokol: "{pin}:{1|0}\n", Baud Rate default: 9600)
 * 2. WiFi HTTP Relay Opsional (GET http://<IP>/relay/{1..8}/{on|off})
 *
 * Pin Mapping ESP32 (aman dari boot strapping & bukan pin input-only):
 *   Pin 1 (Lampu Dalam)  -> GPIO 16
 *   Pin 2 (Lampu Luar)   -> GPIO 17
 *   Pin 3 (AC 1)         -> GPIO 18
 *   Pin 4 (AC 2)         -> GPIO 19
 *   Pin 5 (AC 3)         -> GPIO 21
 *   Pin 6 (Audio Luar)   -> GPIO 22
 *   Pin 7 (Audio Dalam)  -> GPIO 23
 *   Pin 8 (Cadangan)     -> GPIO 25
 */

#define RELAY_COUNT       8
#define SERIAL_BAUD_RATE  9600
#define RELAY_ACTIVE_LOW  true   // Ubah ke false jika modul relay Anda Active-HIGH

// Set ke 1 jika ingin mengaktifkan kontrol lewat WiFi HTTP selain USB Serial
#define ENABLE_WIFI_HTTP  0

#if ENABLE_WIFI_HTTP
  #include <WiFi.h>
  #include <WebServer.h>
  const char* WIFI_SSID = "NAMA_WIFI_MASJID";
  const char* WIFI_PASS = "PASSWORD_WIFI";
  WebServer server(80);
#endif

const uint8_t RELAY_PINS[RELAY_COUNT] = {16, 17, 18, 19, 21, 22, 23, 25};
bool relayStates[RELAY_COUNT] = {false};
String serialBuffer = "";

void setRelay(uint8_t pinNumber, bool turnOn) {
  if (pinNumber < 1 || pinNumber > RELAY_COUNT) return;
  uint8_t idx = pinNumber - 1;
  relayStates[idx] = turnOn;
  uint8_t level = RELAY_ACTIVE_LOW ? (turnOn ? LOW : HIGH) : (turnOn ? HIGH : LOW);
  digitalWrite(RELAY_PINS[idx], level);

  Serial.print("OK ");
  Serial.print(pinNumber);
  Serial.print(":");
  Serial.println(turnOn ? 1 : 0);
}

void processSerialLine(String line) {
  line.trim();
  if (line.length() == 0) return;

  int sep = line.indexOf(':');
  if (sep <= 0) return;

  int pinNum = line.substring(0, sep).toInt();
  int stateVal = line.substring(sep + 1).toInt();
  if (pinNum >= 1 && pinNum <= RELAY_COUNT) {
    setRelay((uint8_t)pinNum, stateVal == 1);
  }
}

#if ENABLE_WIFI_HTTP
void setupWifiHttp() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  unsigned long startAttempt = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startAttempt < 10000) {
    delay(250);
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("WIFI_IP=");
    Serial.println(WiFi.localIP());
  }

  for (uint8_t p = 1; p <= RELAY_COUNT; p++) {
    String onPath = "/relay/" + String(p) + "/on";
    String offPath = "/relay/" + String(p) + "/off";
    server.on(onPath.c_str(), HTTP_GET, [p]() {
      setRelay(p, true);
      server.send(200, "text/plain", "ON");
    });
    server.on(offPath.c_str(), HTTP_GET, [p]() {
      setRelay(p, false);
      server.send(200, "text/plain", "OFF");
    });
  }
  server.begin();
}
#endif

void setup() {
  Serial.begin(SERIAL_BAUD_RATE);
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    pinMode(RELAY_PINS[i], OUTPUT);
    digitalWrite(RELAY_PINS[i], RELAY_ACTIVE_LOW ? HIGH : LOW);
  }
  serialBuffer.reserve(32);

#if ENABLE_WIFI_HTTP
  setupWifiHttp();
#endif
}

void loop() {
  while (Serial.available() > 0) {
    char c = (char)Serial.read();
    if (c == '\n' || c == '\r') {
      processSerialLine(serialBuffer);
      serialBuffer = "";
    } else if (serialBuffer.length() < 30) {
      serialBuffer += c;
    }
  }

#if ENABLE_WIFI_HTTP
  server.handleClient();
#endif
}
