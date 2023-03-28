#include <Wire.h>  //For I2C/SMBus
#include <USB-MIDI.h> //Library for sending MIDI messages

USBMIDI_CREATE_DEFAULT_INSTANCE(); //required line to initialise MIDI communication

#define pinEcho 18 //Pin 18 on the Arduino Micro
#define pinTrig 19 //Pin 19 on the Arduino Micro, both used for the HC-SR04

#define range_cm 4 //range of each note
#define max_dist 8 * range_cm - 1  //max distance used = max note played, max_dist * 8 for white keys and max_dist * 13 for white + black keys
#define distance_offset 5 //offset for the initial reading of HC-SR04

#define buffer_size 50 //size of array buffer for the rolling average

#define sensor1_address 0x06  // Peripheral addresses used for SingleTact (SingleTact), default 0x04
#define sensor2_address 0x08
#define sensor3_address 0x0A

//the control change values that each SingleTact sensor will send
#define ST_left_CC 13
#define ST_middle_CC 12
#define ST_right_CC 7

#define enable_pitch_bend 0 //a future setting for only producing bended notes for a more Theremin like experience

//sensor measurements
int dist = 0;
short data1 = 0;
short data2 = 0;
short data3 = 0;

//checks and limits
int buffer[buffer_size] = { 0 }; //initialise an array of size buffer_size
int avg_dist = 0;
int weighted_avg_dist = 0;

//note data
int prev_note = 0;
int curr_note = 0;
int velocity = 0;

//for turning on or off the TMC based on input
char idle = 0;
bool on = true;

//customisations
int pitchBend_mode = enable_pitch_bend;
int pitchBend_val = 0;

//function initialisations
int* popBuffer(int data, int buffer[buffer_size]);
int calcAvg(int buffer[buffer_size]);

void setup() {
  setupUSR();
  MIDI.begin(1); //begin MIDI on channel 1
  Wire.begin();  // join i2c bus (address optional for master)
  Serial.begin(31250);  //serial for output, 31250 for correct MIDI output
  Serial.flush(); //waits for the transmission of outgoing serial data to complete
}

void loop() {

  measureAndCCsend();

  if (dist < max_dist) { //enter only if the measured distance is less than the max distance
    idle = 0;  //idle state set to false to signify it is working
    on = true; 

    avg_dist = calcAvg(popBuffer(dist, buffer)); //calculate the average distance
    weighted_avg_dist = (1.0 * avg_dist) + (0.0 * dist); //calculate the weighted distance

    curr_note = noteOut_Cmaj(weighted_avg_dist); //set the current note to be the value of the noteOut function with the weighted average distance

    if (curr_note != prev_note && curr_note > 20 && pitchBend_mode == 0) {  //>20 is to prevent other random notes occuring when the variables are empty

      MIDI.sendNoteOff(prev_note, 127, 1); //turn off the previous note
      MIDI.sendNoteOn(curr_note, 127, 1); //send the current note

    } else if (pitchBend_mode == 1) { //code only executes if pitch bend mode is enabled but no functionality is provided to the user

      MIDI.sendNoteOff(prev_note, 127, 1); //do the same thing as before but 
      curr_note = 48; //always set the current note to be number 48
      MIDI.sendNoteOn(curr_note, 127, 1);

      pitchBend_mode = 2; //set to 2 so that only pitch bend values get sent in the next iteration

    } else if (pitchBend_mode == 2) {

      pitchBend_val = map(weighted_avg_dist, 0, max_dist, MIDI_PITCHBEND_MIN, MIDI_PITCHBEND_MAX); ////use the map function to map the weighted distance between 0 to 16383

      MIDI.sendPitchBend(pitchBend_val, 1); //send the pitch bend value
    }
    prev_note = curr_note;  //gets stored before and after each check
  } else if (on) {          //if the distance is way too big it means no hand on top of USR
    idle++; //idle counter gets iterated
    if (idle > 100) { //if it reaches 100 
      reset(); //reset the TMC
      pitchBend_mode = enable_pitch_bend; //reset the pitch bend value to default
    }
  }
}

void setupUSR() { //function to set up the connections to the HC-SR04
  pinMode(pinEcho, INPUT); //set pin 18 to be an input pin
  pinMode(pinTrig, OUTPUT); //set pin 19 to be an output pin
}

int getDistance() { //function to measure distance using HC-SR)4
  int _dist = 0;
  digitalWrite(pinTrig, LOW);  //give a short low pulse to have a cleaner high pulse
  delayMicroseconds(2);
  digitalWrite(pinTrig, HIGH);
  delayMicroseconds(10);       //give 10 us high pulse on the trigger pin
  digitalWrite(pinTrig, LOW);  //reset to low

  //speed of sound is 343 m/s so it is 0.0343 cm/us, divided by 2 to find distance,
  _dist = (pulseIn(pinEcho, HIGH) * 0.0343) / 2.0;  //function returns microseconds

  _dist -= distance_offset; //subtract the distance offset to achieve the lowest measurement of 0
  if (_dist < 0) { _dist = 0; } //if it goes below 0 then set it to 0


  return _dist; 
}

short readDataFromSensor(short address) { //function to read data from SingleTact sensors
  int i2cPacketLength = 6;    //i2c packet length. Just need 6 bytes from each slave
  byte outgoingI2CBuffer[3];  //outgoing array buffer
  byte incomingI2CBuffer[6];  //incoming array buffer

  outgoingI2CBuffer[0] = 0x01;             //I2c read command
  outgoingI2CBuffer[1] = 128;              //Slave data offset
  outgoingI2CBuffer[2] = i2cPacketLength;  //require 6 bytes

  Wire.beginTransmission(address);             // transmit to device
  Wire.write(outgoingI2CBuffer, 3);            // send out command
  byte error = Wire.endTransmission();         // stop transmitting and check slave status
  if (error != 0) return -1;                   //if slave not exists or has error, return -1
  Wire.requestFrom(address, i2cPacketLength);  //require 6 bytes from slave

  byte incomeCount = 0;
  while (incomeCount < i2cPacketLength)  // slave may send less than requested
  {
    if (Wire.available()) {
      incomingI2CBuffer[incomeCount] = Wire.read();  // receive a byte as character
      incomeCount++;
    } else {
      delayMicroseconds(10);  //Wait 10us
    }
  }

  short rawData = (incomingI2CBuffer[4] << 8) + incomingI2CBuffer[5];  //get the raw data

  //used for debugging
  // Serial.println("Index 0:");
  // Serial.println(incomingI2CBuffer[0]);
  // Serial.println("Index 1:");
  // Serial.println(incomingI2CBuffer[1]);
  // Serial.println("Index 2:");
  // Serial.println(incomingI2CBuffer[2]);
  // Serial.println("Index 3:");
  // Serial.println(incomingI2CBuffer[3]);
  // Serial.println("Index 4:");
  // Serial.println(incomingI2CBuffer[4]);
  // Serial.println("Index 5:");
  // Serial.println(incomingI2CBuffer[5]);
  // Serial.println("\n");
  //Serial.println(rawData);

  return rawData;
}

void testSensorOutputs(int dist, short data1, short data2, short data3) { //function to give sensor outputs in Arduino IDE
  Serial.print("HC-SR04 Data:"); 
  Serial.print(dist); //output the corresponding variable with the correct label
  Serial.print("cm\n");

  Serial.print("I2C Sensor 1 Data:");
  Serial.print(data1);
  Serial.print("\n");

  Serial.print("I2C Sensor 2 Data:");
  Serial.print(data2);
  Serial.print("\n");

  Serial.print("I2C Sensor 3 Data:");
  Serial.print(data3);
  Serial.print("\n");
}

int noteOut(int dist) { //function that outputs the MIDI note value based on the distance measured
  if (dist <= range_cm * 0 && dist <= (1 * range_cm) - 1) { //range variable is used here to calculate the space each note will take
    return 48;  //C4
  } else if (dist <= range_cm * 1 && dist <= (2 * range_cm) - 1) {
    return 49;  //C#4
  } else if (dist <= range_cm * 2 && dist <= (3 * range_cm) - 1) {
    return 50;  //D4
  } else if (dist <= range_cm * 3 && dist <= (4 * range_cm) - 1) {
    return 51;  //D#4
  } else if (dist <= range_cm * 4 && dist <= (5 * range_cm) - 1) {
    return 52;  //E4
  } else if (dist <= range_cm * 5 && dist <= (6 * range_cm) - 1) {
    return 53;  //F4
  } else if (dist <= range_cm * 6 && dist <= (7 * range_cm) - 1) {
    return 54;  //F#4
  } else if (dist <= range_cm * 7 && dist <= (8 * range_cm) - 1) {
    return 55;  //G4
  } else if (dist <= range_cm * 8 && dist <= (9 * range_cm) - 1) {
    return 56;  //G#4
  } else if (dist <= range_cm * 9 && dist <= (10 * range_cm) - 1) {
    return 57;  //A4
  } else if (dist <= range_cm * 10 && dist <= (11 * range_cm) - 1) {
    return 58;  //A#4
  } else if (dist <= range_cm * 11 && dist <= (12 * range_cm) - 1) {
    return 59;  //B4
  } else if (dist <= range_cm * 12 && dist <= (13 * range_cm) - 1) {
    return 60;  //C5
  }
}

int noteOut_Cmaj(int dist) { //same as the noteOut function but here it is tuned to play C major
  if (dist <= range_cm * 0 && dist <= (1 * range_cm) - 1) {
    return 48;  //C3
  } else if (dist <= range_cm * 1 && dist <= (2 * range_cm) - 1) {
    return 50;  //D3
  } else if (dist <= range_cm * 2 && dist <= (3 * range_cm) - 1) {
    return 52;  //E3
  } else if (dist <= range_cm * 3 && dist <= (4 * range_cm) - 1) {
    return 53;  //F3
  } else if (dist <= range_cm * 4 && dist <= (5 * range_cm) - 1) {
    return 55;  //G3
  } else if (dist <= range_cm * 5 && dist <= (6 * range_cm) - 1) {
    return 57;  //A3
  } else if (dist <= range_cm * 6 && dist <= (7 * range_cm) - 1) {
    return 59;  //B3
  } else if (dist <= range_cm * 7 && dist < (8 * range_cm) - 1) {
    return 60;  //C4
  }
}

int* popBuffer(int data, int buffer[buffer_size]) { //function to populate the buffer array one value at a time
  for (int i = buffer_size - 1; i > 0; i--) {
    buffer[i] = buffer[i - 1]; //shift the value in index i, one to the right
  }
  buffer[0] = data; //assign the new data to the first value
  return buffer;
}

int calcAvg(int buffer[buffer_size]) { //calculate the average by taking the buffer array as an input
  int total = 0; //initialise the total to be 0
  for (int i = 0; i < buffer_size; i++) {
    total += buffer[i]; //add each value in the array to the total
  }
  return total / buffer_size; //divide by the buffer size to get the average
}

void reset() { //function to reset the TMC
  on = false;
  for (int i = 48; i <= 60; i++) {  //turn all notes off
    MIDI.sendNoteOff(i, 127, 1);
  }
}

void measureAndCCsend(){ //function that gets measurements from the sensors and sends the corresponing Control Change numbers
  dist = getDistance();
  data1 = map(readDataFromSensor(sensor1_address), 260, 1022, 0, 127);  //some calibration on the sensor data
  data2 = map(readDataFromSensor(sensor2_address), 256, 1022, 0, 127);
  data3 = map(readDataFromSensor(sensor3_address), 260, 1022, 0, 127);

  if (data3 > 4) { //only send data more than 4 to minimise noise
    MIDI.sendControlChange(7, data3, 1);
  } else {
    data3 = 0;
  }
  if (data2 > 4) {
    MIDI.sendControlChange(12, data2, 1);
  } else {
    data2 = 0;
  }
  if (data1 > 4) {
    MIDI.sendControlChange(13, data1, 1);
  } else {
    data1 = 0;
  } 
}