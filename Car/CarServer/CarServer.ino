#include <ESP8266WiFi.h>
#include <math.h>

//pin definitions
const int pin_led = D0;
const int tx = D1;
const int xip = D2;
const int yip = D3;
const int xfp = D4;
const int yfp = D5;

//wifi stuff
const char WiFiAPPSK[] = "eerssi";
WiFiServer server(8989);

//coordinates
int xi;
int yi;
int xf;
int yf;

void setup() {
  initHardware();
  setupWiFi();
  server.begin();
}

void loop() {
  // Check if a client has connected
  WiFiClient client = server.available();
  if (!client) {
    return;
  }
  
  Serial.println("Client connected!");
  
  String coords = client.readString();
//  Serial.println(coords);
  
  client.flush();

  // parse string here, send over data ports
  String sxi = "";
  String syi = "";
  String sxf = "";
  String syf = "";
  int i = 0;
  while(coords[i] != ' '){
    sxi += coords[i++];
  }
  i++;
  while(coords[i] != ' '){
    syi += coords[i++];
  }
  i++;
  while(coords[i] != ' '){
    sxf += coords[i++];
  }
  i++;
  while(coords[i] != ' '){
    syf += coords[i++];
  }
/*
  Serial.println("Strings received:");
  Serial.println(sxi);
  Serial.println(syi);
  Serial.println(sxf);
  Serial.println(syf);*/
  xi = sxi.toInt();
  yi = syi.toInt();
  xf = sxf.toInt();
  yf = syf.toInt();
/*  Serial.println("Ints received:");
  Serial.println(xi);
  Serial.println(yi);
  Serial.println(xf);
  Serial.println(yf);*/
  sendCoordinates();

  delay(1);
  Serial.println("Client disconnected\n");
}

void sendCoordinates(){
  for(int i = 0; i < 9; i++){
    digitalWrite(tx, 0);
    
    byte bxi = xi&1;
    byte byi = yi&1;
    byte bxf = xf&1;
    byte byf = yf&1;
    xi = xi>>1;
    yi = yi>>1;
    xf = xf>>1;
    yf = yf>>1;

    if(bxi){
      digitalWrite(xip, HIGH);
    }
    else{
      digitalWrite(xip, LOW);
    }
    if(byi){
      digitalWrite(yip, HIGH);
    }
    else{
      digitalWrite(yip, LOW);
    }
    if(bxf){
      digitalWrite(xfp, HIGH);
    }
    else{
      digitalWrite(xfp, LOW);
    }
    if(byf){
      digitalWrite(yfp, HIGH);
    }
    else{
      digitalWrite(yfp, LOW);
    }

    delay(100);
    digitalWrite(tx, 1);
    delay(150);
  }
}

void setupWiFi(){
  WiFi.mode(WIFI_AP);

  // Do a little work to get a unique-ish name. Append the
  // last two bytes of the MAC (HEX'd) to "Thing-":
  uint8_t mac[WL_MAC_ADDR_LENGTH];
  WiFi.softAPmacAddress(mac);
  String macID = String(mac[WL_MAC_ADDR_LENGTH - 2], HEX) + String(mac[WL_MAC_ADDR_LENGTH - 1], HEX);
  macID.toUpperCase();
  String AP_NameString = "ESP8266 Target Tracking" + macID;

  char AP_NameChar[AP_NameString.length() + 1];
  memset(AP_NameChar, 0, AP_NameString.length() + 1);

  for (int i=0; i<AP_NameString.length(); i++){
    AP_NameChar[i] = AP_NameString.charAt(i);
  }

  WiFi.softAP(AP_NameChar, WiFiAPPSK);
}

void initHardware(){
  Serial.begin(115200);
  pinMode(pin_led, OUTPUT);
  pinMode(tx, OUTPUT);
  pinMode(xip, OUTPUT); 
  pinMode(yip, OUTPUT);  
  pinMode(xfp, OUTPUT);  
  pinMode(yfp, OUTPUT);  
}

