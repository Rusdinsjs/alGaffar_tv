/*
 * ============================================================================
 * JWS TV Masjid Al-Gaffar - ESP32 8-Channel Smart Relay Controller (v2.0)
 * ============================================================================
 * Mendukung 3 Mode Konektivitas Sekaligus (Tanpa Perlu Edit/Upload Ulang Kode):
 *
 * 1. USB OTG Serial (Kabel USB langsung ke TV Box)
 *    - Protokol Relay : "{pin}:{1|0}\n" (contoh: "1:1\n", Baud Rate: 9600)
 *    - Provisioning WiFi via USB dari JWS TV:
 *      "WIFI:ONLINE:<ssid>:<password>:<static_last_octet>\n"
 *      "WIFI:OFFLINE\n"
 *      "WIFI:STATUS\n"
 *
 * 2. Mode WiFi OFFLINE (Access Point / Hotspot Mandiri Tanpa Router)
 *    - ESP32 memancarkan WiFi sendiri:
 *      SSID Default     : JWS_Relay_AP
 *      Password Default : algaffar123
 *      IP Address Tetap : 192.168.4.1
 *    - Cocok untuk masjid tanpa WiFi/Router, atau ketika TV Box memakai
 *      Kabel LAN untuk Internet (YouTube Live) dan WiFi TV Box konek ke ESP32.
 *
 * 3. Mode WiFi ONLINE (Dynamic Station + Dual AP Backup + Auto Static IP)
 *    - Nama WiFi (SSID) & Password tiap masjid disimpan permanen di NVS Flash
 *      (Preferences) dan bisa diubah kapan saja lewat:
 *      a) Web Portal HP / Browser : http://192.168.4.1 atau http://<IP_RELAY>
 *      b) Menu Pengaturan JWS TV  : Kirim SSID & Password via USB atau HTTP
 *    - Mendukung Auto-Lock Static IP (misal akhiran .250 -> 192.168.1.250)
 *      agar IP Relay tidak pernah berubah walau mati lampu, sambil membiarkan
 *      TV Box tetap terhubung ke WiFi Internet Masjid (Fitur YouTube Live &
 *      Keuangan Online tetap jalan 100%!).
 *
 * Pin Mapping ESP32 (Bebas konflik boot strapping):
 *   Pin 1 (Lampu Dalam)  -> GPIO 16
 *   Pin 2 (Lampu Luar)   -> GPIO 17
 *   Pin 3 (AC 1)         -> GPIO 18
 *   Pin 4 (AC 2)         -> GPIO 19
 *   Pin 5 (AC 3)         -> GPIO 21
 *   Pin 6 (Audio Luar)   -> GPIO 22
 *   Pin 7 (Audio Dalam)  -> GPIO 23
 *   Pin 8 (Cadangan)     -> GPIO 25
 * ============================================================================
 */

#include <WiFi.h>
#include <WebServer.h>
#include <Preferences.h>

#define RELAY_COUNT          8
#define SERIAL_BAUD_RATE     9600
#define RELAY_ACTIVE_LOW     true   // Ubah ke false jika modul relay Anda Active-HIGH

// Default Hotspot AP ESP32 (Selalu aktif untuk Setup HP & Mode Offline)
#define DEFAULT_AP_SSID      "JWS_Relay_AP"
#define DEFAULT_AP_PASS      "algaffar123"

const uint8_t RELAY_PINS[RELAY_COUNT] = {16, 17, 18, 19, 21, 22, 23, 25};
bool relayStates[RELAY_COUNT] = {false};
String serialBuffer = "";

Preferences prefs;
WebServer server(80);

// Konfigurasi yang disimpan di NVS Flash ESP32
bool cfgOnlineMode = false;          // false = OFFLINE (AP Only), true = ONLINE (AP + Station WiFi Masjid)
String cfgStaSsid = "";              // Nama WiFi Masjid / Hotspot TV Box
String cfgStaPass = "";              // Password WiFi Masjid
int cfgStaticOctet = 250;            // 0 = DHCP biasa, 2..254 = Kunci ujung IP otomatis (misal .250)
bool staStaticApplied = false;
unsigned long lastWifiCheckMs = 0;

void addCorsHeaders() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
}

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

void loadConfig() {
  prefs.begin("jws_relay", true);
  cfgOnlineMode = prefs.getBool("online", false);
  cfgStaSsid = prefs.getString("ssid", "");
  cfgStaPass = prefs.getString("pass", "");
  cfgStaticOctet = prefs.getInt("octet", 250);
  prefs.end();
}

void saveConfig(bool online, String ssid, String pass, int staticOctet) {
  cfgOnlineMode = online;
  cfgStaSsid = ssid;
  cfgStaPass = pass;
  cfgStaticOctet = (staticOctet >= 2 && staticOctet <= 254) ? staticOctet : 0;

  prefs.begin("jws_relay", false);
  prefs.putBool("online", cfgOnlineMode);
  prefs.putString("ssid", cfgStaSsid);
  prefs.putString("pass", cfgStaPass);
  prefs.putInt("octet", cfgStaticOctet);
  prefs.end();
}

String currentStatusJson() {
  bool staConnected = (WiFi.status() == WL_CONNECTED);
  String staIp = staConnected ? WiFi.localIP().toString() : "0.0.0.0";
  String apIp = WiFi.softAPIP().toString();

  String json = "{";
  json += "\"board\":\"ESP32_8CH\",";
  json += "\"relayCount\":" + String(RELAY_COUNT) + ",";
  json += "\"mode\":\"" + String(cfgOnlineMode ? "online" : "offline") + "\",";
  json += "\"staSsid\":\"" + cfgStaSsid + "\",";
  json += "\"staConnected\":" + String(staConnected ? "true" : "false") + ",";
  json += "\"staIp\":\"" + staIp + "\",";
  json += "\"apSsid\":\"" + String(DEFAULT_AP_SSID) + "\",";
  json += "\"apIp\":\"" + apIp + "\",";
  json += "\"staticOctet\":" + String(cfgStaticOctet) + ",";
  json += "\"relays\":[";
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    if (i > 0) json += ",";
    json += (relayStates[i] ? "1" : "0");
  }
  json += "]}";
  return json;
}

void applyWifiConnection() {
  staStaticApplied = false;
  WiFi.disconnect(false);
  delay(100);

  // Selalu nyalakan AP darurat/setup (192.168.4.1) agar selalu bisa diakses dari HP
  if (cfgOnlineMode && cfgStaSsid.length() > 0) {
    WiFi.mode(WIFI_AP_STA);
    WiFi.softAP(DEFAULT_AP_SSID, DEFAULT_AP_PASS);
    WiFi.begin(cfgStaSsid.c_str(), cfgStaPass.c_str());

    unsigned long startAttempt = millis();
    while (WiFi.status() != WL_CONNECTED && millis() - startAttempt < 10000) {
      delay(250);
    }

    // Jika berhasil konek via DHCP dan fitur Auto Static Octet aktif (misal .250),
    // kunci IP sesuai subnet router masjid agar tidak berubah-ubah saat mati lampu.
    if (WiFi.status() == WL_CONNECTED && cfgStaticOctet >= 2 && cfgStaticOctet <= 254) {
      IPAddress curIp = WiFi.localIP();
      IPAddress gw = WiFi.gatewayIP();
      IPAddress mask = WiFi.subnetMask();
      IPAddress dns = WiFi.dnsIP();
      if (curIp[3] != cfgStaticOctet && gw[0] != 0) {
        IPAddress targetIp(curIp[0], curIp[1], curIp[2], cfgStaticOctet);
        if (WiFi.config(targetIp, gw, mask, dns)) {
          staStaticApplied = true;
          delay(200);
        }
      }
    }
  } else {
    WiFi.mode(WIFI_AP);
    WiFi.softAP(DEFAULT_AP_SSID, DEFAULT_AP_PASS);
  }

  Serial.print("WIFI_STATUS ");
  Serial.println(currentStatusJson());
}

void registerHttpRoutes() {
  // 1. Halaman Web Portal Pengaturan WiFi & Test Saklar (http://192.168.4.1)
  server.on("/", HTTP_GET, []() {
    bool staConnected = (WiFi.status() == WL_CONNECTED);
    String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>";
    html += "<meta name='viewport' content='width=device-width,initial-scale=1'>";
    html += "<title>JWS TV Relay Setup (ESP32 8-CH)</title>";
    html += "<style>body{font-family:sans-serif;background:#0f172a;color:#f8fafc;padding:18px;max-width:520px;margin:auto}";
    html += ".card{background:#1e293b;border:1px solid #334155;border-radius:14px;padding:16px;margin-bottom:16px}";
    html += "h2{margin-top:0;color:#38bdf8}label{display:block;font-size:13px;color:#94a3b8;margin-top:10px}";
    html += "input,select{width:100%;padding:10px;margin-top:4px;border-radius:8px;border:1px solid #475569;background:#0f172a;color:#fff;box-sizing:border-box}";
    html += "button{background:#0284c7;color:#fff;border:none;padding:11px 16px;border-radius:8px;font-weight:bold;cursor:pointer;margin-top:12px;width:100%}";
    html += ".grid{display:grid;grid-template-columns:repeat(2,1fr);gap:8px;margin-top:10px}";
    html += ".on{background:#059669}.off{background:#475569}.badge{display:inline-block;padding:4px 10px;border-radius:99px;font-size:12px;font-weight:bold}</style></head><body>";

    html += "<div class='card'><h2>JWS TV Relay - ESP32 (8 Pin)</h2>";
    html += "<p>Mode Aktif: <span class='badge' style='background:" + String(cfgOnlineMode ? "#0284c7" : "#d97706") + "'>" + String(cfgOnlineMode ? "ONLINE (WiFi Masjid + AP)" : "OFFLINE (Hotspot AP)") + "</span></p>";
    html += "<p>IP Hotspot AP (Tetap): <b>" + WiFi.softAPIP().toString() + "</b> (SSID: <b>" + String(DEFAULT_AP_SSID) + "</b>)</p>";
    html += "<p>Status WiFi Masjid: <b>" + String(staConnected ? ("TERHUBUNG (" + WiFi.localIP().toString() + ")") : "TIDAK TERHUBUNG") + "</b></p></div>";

    html += "<div class='card'><h2>Pengaturan Mode & WiFi Masjid</h2>";
    html += "<form method='POST' action='/api/wifi-config'>";
    html += "<label>Pilih Mode Koneksi WiFi</label><select name='mode'>";
    html += "<option value='offline'" + String(!cfgOnlineMode ? " selected" : "") + ">OFFLINE - Hotspot AP Mandiri (192.168.4.1)</option>";
    html += "<option value='online'" + String(cfgOnlineMode ? " selected" : "") + ">ONLINE - Gabung ke WiFi Masjid / Hotspot TV Box</option></select>";
    html += "<label>Nama WiFi Masjid / Hotspot TV Box (SSID)</label>";
    html += "<input type='text' name='ssid' value='" + cfgStaSsid + "' placeholder='Contoh: WiFi_Masjid_AlGaffar'>";
    html += "<label>Password WiFi Masjid</label>";
    html += "<input type='password' name='pass' value='" + cfgStaPass + "' placeholder='Masukkan password WiFi'>";
    html += "<label>Kunci Akhiran IP Statis Otomatis (2 - 254, atau 0 = DHCP)</label>";
    html += "<input type='number' name='octet' value='" + String(cfgStaticOctet) + "' min='0' max='254'>";
    html += "<small style='color:#94a3b8'>Contoh: nilai <b>250</b> akan mengunci IP ESP32 di <b>192.168.x.250</b> agar IP tidak berubah walau mati lampu.</small>";
    html += "<button type='submit'>Simpan & Hubungkan WiFi</button></form></div>";

    html += "<div class='card'><h2>Uji Coba Saklar Relay (1 - 8)</h2><div class='grid'>";
    for (uint8_t p = 1; p <= RELAY_COUNT; p++) {
      bool isOn = relayStates[p - 1];
      String target = isOn ? "off" : "on";
      html += "<a href='/relay/" + String(p) + "/" + target + "' style='text-decoration:none'>";
      html += "<button type='button' class='" + String(isOn ? "on" : "off") + "'>PIN " + String(p) + ": " + String(isOn ? "ON" : "OFF") + "</button></a>";
    }
    html += "</div></div></body></html>";

    addCorsHeaders();
    server.send(200, "text/html", html);
  });

  // 2. Endpoint Status JSON (/status & /api/status)
  auto statusHandler = []() {
    addCorsHeaders();
    server.send(200, "application/json", currentStatusJson());
  };
  server.on("/status", HTTP_GET, statusHandler);
  server.on("/api/status", HTTP_GET, statusHandler);

  // 3. Endpoint Simpan Konfigurasi WiFi Dinamis dari HP atau Aplikasi JWS TV (/api/wifi-config)
  server.on("/api/wifi-config", HTTP_ANY, []() {
    if (server.method() == HTTP_OPTIONS) {
      addCorsHeaders();
      server.send(204);
      return;
    }
    String modeStr = server.hasArg("mode") ? server.arg("mode") : "online";
    String ssid = server.hasArg("ssid") ? server.arg("ssid") : cfgStaSsid;
    String pass = server.hasArg("pass") ? server.arg("pass") : cfgStaPass;
    int octet = server.hasArg("octet") ? server.arg("octet").toInt() : cfgStaticOctet;

    bool online = !modeStr.equalsIgnoreCase("offline");
    ssid.trim();
    saveConfig(online, ssid, pass, octet);
    applyWifiConnection();

    addCorsHeaders();
    if (server.hasArg("json") && server.arg("json") == "1") {
      server.send(200, "application/json", currentStatusJson());
    } else {
      server.sendHeader("Location", "/");
      server.send(303);
    }
  });

  // 4. Endpoint Kontrol Saklar Relay (/relay/{1..8}/{on|off})
  for (uint8_t p = 1; p <= RELAY_COUNT; p++) {
    String onPath = "/relay/" + String(p) + "/on";
    String offPath = "/relay/" + String(p) + "/off";
    server.on(onPath.c_str(), HTTP_GET, [p]() {
      setRelay(p, true);
      addCorsHeaders();
      server.send(200, "text/plain", "ON");
    });
    server.on(offPath.c_str(), HTTP_GET, [p]() {
      setRelay(p, false);
      addCorsHeaders();
      server.send(200, "text/plain", "OFF");
    });
  }
}

/*
 * Mendukung perintah USB Serial:
 * - "{pin}:{1|0}" -> contoh: "1:1" atau "1:0"
 * - "WIFI:ONLINE:<ssid>:<pass>:<octet>" -> Mengatur WiFi Masjid secara dinamis lewat kabel USB!
 * - "WIFI:OFFLINE" -> Mengubah ESP32 ke mode Hotspot AP Offline (192.168.4.1)
 * - "WIFI:STATUS" -> Menampilkan JSON status & IP ESP32
 */
void processSerialLine(String line) {
  line.trim();
  if (line.length() == 0) return;

  if (line.startsWith("WIFI:")) {
    String cmd = line.substring(5);
    if (cmd.equalsIgnoreCase("STATUS")) {
      Serial.print("WIFI_STATUS ");
      Serial.println(currentStatusJson());
      return;
    }
    if (cmd.equalsIgnoreCase("OFFLINE")) {
      saveConfig(false, cfgStaSsid, cfgStaPass, cfgStaticOctet);
      applyWifiConnection();
      return;
    }
    if (cmd.startsWith("ONLINE:")) {
      String rest = cmd.substring(7);
      int p1 = rest.indexOf(':');
      int p2 = rest.lastIndexOf(':');
      if (p1 > 0) {
        String ssid = rest.substring(0, p1);
        String pass = "";
        int octet = cfgStaticOctet;
        if (p2 > p1) {
          pass = rest.substring(p1 + 1, p2);
          octet = rest.substring(p2 + 1).toInt();
        } else {
          pass = rest.substring(p1 + 1);
        }
        saveConfig(true, ssid, pass, octet);
        applyWifiConnection();
      }
      return;
    }
  }

  int sep = line.indexOf(':');
  if (sep <= 0) return;

  int pinNum = line.substring(0, sep).toInt();
  int stateVal = line.substring(sep + 1).toInt();
  if (pinNum >= 1 && pinNum <= RELAY_COUNT) {
    setRelay((uint8_t)pinNum, stateVal == 1);
  }
}

void setup() {
  Serial.begin(SERIAL_BAUD_RATE);
  for (uint8_t i = 0; i < RELAY_COUNT; i++) {
    pinMode(RELAY_PINS[i], OUTPUT);
    digitalWrite(RELAY_PINS[i], RELAY_ACTIVE_LOW ? HIGH : LOW);
  }
  serialBuffer.reserve(128);

  loadConfig();
  applyWifiConnection();
  registerHttpRoutes();
  server.begin();
}

void loop() {
  while (Serial.available() > 0) {
    char c = (char)Serial.read();
    if (c == '\n' || c == '\r') {
      processSerialLine(serialBuffer);
      serialBuffer = "";
    } else if (serialBuffer.length() < 120) {
      serialBuffer += c;
    }
  }

  server.handleClient();

  // Auto-reconnect ke WiFi Masjid setiap 20 detik jika mode ONLINE aktif namun sinyal sempat putus
  if (cfgOnlineMode && cfgStaSsid.length() > 0) {
    unsigned long now = millis();
    if (now - lastWifiCheckMs >= 20000) {
      lastWifiCheckMs = now;
      if (WiFi.status() != WL_CONNECTED) {
        WiFi.reconnect();
      }
    }
  }
}
