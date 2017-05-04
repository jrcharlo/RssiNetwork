#include <math.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_HMC5883_U.h>

/* Assign a unique ID to this sensor at the same time */
Adafruit_HMC5883_Unified mag = Adafruit_HMC5883_Unified(12345);

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
int readindex = 0;
volatile int xi[9];
volatile int yi[9];
volatile int xf[9];
volatile int yf[9];
volatile int i = 0;
double car_x=0;
double car_y=0;
double target_x;
double target_y;
int ABS = 185;
int ABS1 = 150;
double dis =10;
int rightDistance = 0;
int leftDistance = 0;
int middleDistance = 0;
float cx = 0;
float cy = 0;
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

  //initialize magSensor
  if(!mag.begin()){
    while (1);
  }
}

// constantly move car towards target
void loop() {
  if((abs(car_x - target_x) >= 10) || (abs(car_y - target_y) >= 10)){
    moveCar();
  }
}

float currentAngle(){  
  sensors_event_t event;
  mag.getEvent(&event);
  float heading = atan2(event.magnetic.y, event.magnetic.x);
  // Once you have your heading, you must then add your 'Declination Angle', which is the 'Error' of the magnetic field in your location.
  // Find yours here: http://www.magnetic-declination.com/
  // Mine is: -13* 2' W, which is ~13 Degrees, or (which we need) 0.22 radians
  // If you cannot find your Declination, comment out these two lines, your compass will be slightly off.
  float declinationAngle = 0.1868;
  heading += declinationAngle;
  // Correct for when signs are reversed.
  if (heading < 0)
    heading += 2 * PI;
  // Check for wrap due to addition of declination.
  if (heading > 2 * PI)
    heading -= 2 * PI;
  // Convert radians to degrees for readability.
  float headingDegrees = heading * 180 / M_PI;
  //Serial.print("Heading (degrees): "); Serial.println(headingDegrees);
  return headingDegrees;
}

void moveCar(){
  float cAngle = currentAngle();
  double target_angle = angleToTarget(car_x, car_y, target_x, target_y);
  
  Serial.println("Move car!");
  Serial.print("carX: ");
  Serial.println(car_x);
  Serial.print("carY: ");
  Serial.println(car_y);
  Serial.print("tarX: ");
  Serial.println(target_x);
  Serial.print("tarY: ");
  Serial.println(target_y);
  Serial.print("t angle: ");
  Serial.println(target_angle);
  Serial.print("c angle: ");
  Serial.println(cAngle);
  
  turning(cAngle, target_angle);
  moveTo(car_x, car_y, target_x, target_y);
 // _mForward(10);
 // updateCor();
}

void readWiFiPins(){
  // ----------------------------  receving data from esp8266
  if(i < 9){
    int xcar = digitalRead(CarX)==HIGH?1:0;
    int ycar = digitalRead(CarY)==HIGH?1:0;
    int xtarget = digitalRead(TargetX)==HIGH?1:0;
    int ytarget = digitalRead(TargetY)==HIGH?1:0;
    xi[i] = xcar;
    yi[i] = ycar;
    xf[i] = xtarget;
    yf[i++] = ytarget;
    //xi[i] = xi[i]<<i;
   // Serial.print(x);
   // Serial.print("x");Serial.print(i);Serial.print(": "); Serial.print(xi[i]);Serial.print("\t");    
  }
  if(i >= 9){
    i = 0;
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

double angleToTarget(double xi, double yi, double xf, double yf){
  double angle;
  if(xi != xf){
    if(yi<=yf){
      if(xi<=xf){
        angle = atan((yf-yi)/(xf-xi));
      }
      else{
        angle = atan((yf-yi)/(xf-xi))+PI;
      }
//      angle = (xi<xf)?atan((yf-yi)/(xf-xi)):(atan(yf-yi)/(xf-xi)+PI);
    }
    else{
      if(xi<=xf){
        angle = atan((yf-yi)/(xf-xi))+2*PI;
      }
      else{
        angle = atan((yf-yi)/(xf-xi))+PI;
      }
//      angle = (xi<xf)?(atan((yf-yi)/(xf-xi))+2*PI):(atan(yf-yi)/(xf-xi)+PI);
    }
  }
  else{
    if(yi <= yf){
      angle = PI/2;
    }
    else{
      angle = (3*PI)/2;
    }
  }
  return ((180/PI)*angle);
}

void turning(double angle1, double angle2){
  double delta = abs(angle1-angle2);
  if(delta >= 180){
    delta = 360 - delta;
  }
  while(delta >= 5){
    _mright(5);
    delta = abs(currentAngle()-angle2);
    if(delta >= 180){
      delta = 360 - delta;
    }
  }
}

void moveTo(double xi, double yi, double xf, double yf){
  _mForward(10);
  updateCor();
}

void updateCor(){
  double angle = currentAngle();
  car_x+=cos(angle)*10;
  car_y+=sin(angle)*10;
}

void _mForward(float d){
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

void _mBack(float d){
 analogWrite(ENA,ABS);
 analogWrite(ENB,ABS1);
 digitalWrite(in1,LOW);
 digitalWrite(in2,HIGH);
 digitalWrite(in3,LOW);
 digitalWrite(in4,HIGH);
 Serial.println("go back!");
 delay(int(d*30));
 _mStop();
}

void _mleft(float n){ //in 10 degree
 analogWrite(ENA,175);
 analogWrite(ENB,145);
 digitalWrite(in1,HIGH);
 digitalWrite(in2,LOW);
 digitalWrite(in3,LOW);
 digitalWrite(in4,HIGH);
 Serial.println("go left!");
 delay(int(7*n));
 _mStop();
}

void _mright(float n){ // in 10 degree
 analogWrite(ENA,175);
 analogWrite(ENB,145);
 digitalWrite(in1,LOW);
 digitalWrite(in2,HIGH);
 digitalWrite(in3,HIGH);
 digitalWrite(in4,LOW);
 Serial.println("go left!");
 delay(int(7*n));
 _mStop();
}

void _mStop(){
  digitalWrite(ENA,LOW);
  digitalWrite(ENB,LOW);
  Serial.println("Stop!");
} 
