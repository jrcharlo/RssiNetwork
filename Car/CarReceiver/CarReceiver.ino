#include <math.h>

// Pin Definitions
const byte interruptPin = 2;
int Echo = A4;  
int Trig = A5; 
int in1 = 6;
int in2 = 7;
int in3 = 8;
int in4 = 9;
int ENA = 5;
int ENB = 11;
int CarX = 3;
int CarY = 4;
int TargetX = 10;
int TargetY = 12;

// Variable declarations
int intIndex = 0;
volatile int xi[9];
volatile int yi[9];
volatile int xf[9];
volatile int yf[9];
int dir = 0; // car's direction, initially 0 = +x
double car_x=0;
double car_y=0;
double target_x;
double target_y;
double dis = 10.0; //distance to move in one go, in cm
int ABS = 185;
int ABS1 = 150;
//float orientation = 90;

void setup() {
  Serial.begin(115200);
  pinMode(CarX,INPUT);
  pinMode(CarY,INPUT);
  pinMode(TargetX,INPUT);
  pinMode(TargetY,INPUT);
  pinMode(interruptPin, INPUT_PULLUP);

  //initialize interrupt
  attachInterrupt(digitalPinToInterrupt(interruptPin), readWiFiPins, RISING);
  
  car_x=0;
  car_y=0;
  target_x=0;
  target_y=0;
}

// constantly move car towards target
void loop() {
  moveCar();
}

void moveCar(){
  Serial.println("Move car!");
  Serial.print("carX: ");
  Serial.println(car_x);
  Serial.print("carY: ");
  Serial.println(car_y);
  Serial.print("tarX: ");
  Serial.println(target_x);
  Serial.print("tarY: ");
  Serial.println(target_y);
  
  double dx = target_x - car_x;
  double dy = target_y - car_y;
  if(abs(dx) >= dis){
    if(dx > 0){ //set dir = 0
      if(dir == 1){
        _mright();
      }
      else if(dir == 2){
        _mright();
        _mright();
      }
      else if(dir == 3){
        _mleft();
      }
      // car now facing +x (dir = 0)
      dir = 0;
      _mForward(dis);
      car_x += dis;
    }
    else{ //set dir = 2
      if(dir == 0){
        _mleft();
        _mleft();
      }
      else if(dir == 1){
        _mleft();
      }
      else if(dir == 3){
        _mright();
      }
      // car now facing -x (dir = 2)
      dir = 2;
      _mForward(dis);
      car_x -= dis;
    }
  }
  else if(abs(dy) >= dis){
    if(dy > 0){ // set dir = 1
      if(dir == 0){
        _mleft();
      }
      else if(dir == 2){
        _mright();
      }
      else if(dir == 3){
        _mright();
        _mright();
      }
      // car now facing +y (dir = 1)
      dir = 1;
      _mForward(dis);
      car_y += dis;
    }
    else{ // set dir = 3
      if(dir == 0){
        _mright();
      }
      else if(dir == 1){
        _mleft();
        _mleft();
      }
      else if(dir == 2){
        _mleft();
      }
      // car now facing -y (dir = 3)
      dir = 3;
      _mForward(dis);
      car_y -= dis;
    }
  }
}

void readWiFiPins(){
  // ----------------------------  receving data from esp8266
  if(intIndex < 9){
    int xcar = digitalRead(CarX)==HIGH?1:0;
    int ycar = digitalRead(CarY)==HIGH?1:0;
    int xtarget = digitalRead(TargetX)==HIGH?1:0;
    int ytarget = digitalRead(TargetY)==HIGH?1:0;
    xi[intIndex] = xcar;
    yi[intIndex] = ycar;
    xf[intIndex] = xtarget;
    yf[intIndex++] = ytarget;
    //xi[i] = xi[i]<<i;
   // Serial.print(x);
   // Serial.print("x");Serial.print(i);Serial.print(": "); Serial.print(xi[i]);Serial.print("\t");    
  }
  if(intIndex >= 9){
    intIndex = 0;
   // car_x = computeInt(xi);
    Serial.println();
    Serial.print("car_x: ");
    Serial.println(car_x);
   // car_y = computeInt(yi);
    Serial.print("car_y: ");
    Serial.println(car_y);
    target_x = computeInt(xf);
    Serial.print("target_x: ");
    Serial.println(target_x);
    target_y = computeInt(yf);
    Serial.print("target_y: ");
    Serial.println(target_y);  }
}

double computeInt(int a[]){
  double x = 0;
  for(int j = 0; j <9; j++){
    if(a[j]){
      x += pow(2,j);
    }
  }
  return x;
}

void _mForward(double d){
 analogWrite(ENA,ABS);
 analogWrite(ENB,ABS1);
 digitalWrite(in1,HIGH);
 digitalWrite(in2,LOW);
 digitalWrite(in3,HIGH);
 digitalWrite(in4,LOW);
 Serial.println("go forward!");
 delay(int(d*20));
 _mStop();
}

void _mBack(double d){
 analogWrite(ENA,ABS);
 analogWrite(ENB,ABS1);
 digitalWrite(in1,LOW);
 digitalWrite(in2,HIGH);
 digitalWrite(in3,LOW);
 digitalWrite(in4,HIGH);
 Serial.println("go back!");
 delay(int(d*10));
 _mStop();
}

void _mleft(){ //in 90 degree
 analogWrite(ENA,175);
 analogWrite(ENB,145);
 digitalWrite(in1,HIGH);
 digitalWrite(in2,LOW);
 digitalWrite(in3,LOW);
 digitalWrite(in4,HIGH);
 Serial.println("go left!");
 delay(630);
 _mStop();
}

void _mright(){ // in 90 degree
 analogWrite(ENA,175);
 analogWrite(ENB,145);
 digitalWrite(in1,LOW);
 digitalWrite(in2,HIGH);
 digitalWrite(in3,HIGH);
 digitalWrite(in4,LOW);
 Serial.println("go left!");
 delay(630);
 _mStop();
}

void _mStop(){
  digitalWrite(ENA,LOW);
  digitalWrite(ENB,LOW);
  Serial.println("Stop!");
} 
