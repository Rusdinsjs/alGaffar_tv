/*
 * JWS TV Masjid Al-Gaffar - Wemos D1 Mini (ESP8266) 4 / 2-Channel Relay Controller
 * Mendukung:
 * 1. USB OTG Serial (Protokol: "{pin}:{1|0}\n", Baud Rate default: 9600)
 * 2. WiFi HTTP Relay Opsional (GET http://<IP>/relay/{1..4}/{on|off})
 *
 * Ubah RELAY_COUNT menjadi 4 (untuk modul relay 4 pin) atau 2 (untuk modul relay 2 pin).
 *
 * Pin Mapping Wemos D1 Mini (menggunakan pin paling aman, bebas konflik boot):
 *   Pin 1 -> D1 (GPIO 5)
 *   Pin 2 -> D2 (GPIO 4)
 *   Pin 3 -> D5 (GPIO 14)  [aktif jika RELAY_COUNT == 4]
 *   Pin 4 -> D6 (GPIO 12)  [aktif jika RELAY_COUNT == 4]
 */

#define RELAY_COUNT       4      // Ubah ke 2 jika menggunakan modul relay 2 channel
#define SERIAL_BAUD_RATE  9600
#define RELAY_ACTIVE_LOW  true   // Ubah ke false jika modul relay Anda Active-HIGH

// Set ke 1 jika ingin mengaktifkan kontrol lewat WiFi HTTP selain USB Serial
#define ENABLE_WIFI_HTTP  0

#if ENABLE_WIFI_HTTP
  #include <ESP8266WiFi.h>
  #include <ESP8266WebServer.h>
  const char* WIFI_SSID = "NAMA_WIFI_MASJID";
  const char* WIFI_PASS = "PASSWORD_WIFI";
  ESP8266WebServer server(80);
#endif

const uint8_t ALL_WEMOS_PINS[4] = {D1, D2, D5, D6};
bool relayStates[4] = {false};
String serialBuffer = "";

void setRelay(uint8_t pinNumber, bool turnOn) {
  if (pinNumber < 1 || pinNumber > RELAY_COUNT) return;
  uint8_t idx = pinNumber - 1;
  relayStates[idx] = turnOn;
  uint8_t level = RELAY_ACTIVE_LOW ? (turnOn ? LOW : HIGH) : (turnOn ? HIGH : LOW);
  digitalWrite(ALL_WEMOS_PINS[idx], level);

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
    pinMode(ALL_WEMOS_PINS[i], OUTPUT);
    digitalWrite(ALL_WEMOS_PINS[i], RELAY_ACTIVE_LOW ? HIGH : LOW);
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
