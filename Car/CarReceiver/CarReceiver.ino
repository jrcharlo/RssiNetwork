#include <math.h>

//pin definitions
const byte interruptPin = 2;

int readindex = 0;
volatile int sxi[9];
volatile int syi[9];
volatile int sxf[9];
volatile int syf[9];
volatile int state = 0;
double xi;
double yi;
double xf;
double yf;

void setup() {
  Serial.begin(115200);
  pinMode(4,INPUT);
  pinMode(interruptPin, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(interruptPin), readWiFiPins, RISING);
}

void loop() {}

void readWiFiPins(){
  if(state < 9){
    int bxi = digitalRead(4)==HIGH?1:0;
    sxi[state++] = bxi;
      
  }
  if(state >= 9){
    state = 0;
    xi = computeInt();
    Serial.println();
    Serial.print("received: ");
    Serial.println(x);
  }
  
}

double computeInt(){
  double x = 0;
  for(int j = 0; j < 9; j++){
    if(xi[j]){
      x += pow(2,j);
    }
  }
  return x;
}
