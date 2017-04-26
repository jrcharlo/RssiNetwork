#include "../RssiNetworkMessages.h"

module SendingMoteC {
  uses interface Boot;
  uses interface Timer<TMilli> as SendTimer;

  uses interface Leds;
  uses interface AMSend as RssiMsgSend;
  uses interface AMPacket;
  uses interface SplitControl as RadioControl;
} implementation {
  message_t msg;
  int rnode;
  int numnodes;
  int numrelaynodes;
  float send_interval;

  event void Boot.booted(){
    rnode  = 3; // first relay node
    numnodes = 7;
    numrelaynodes = numnodes - 2;
    send_interval = 1000/numrelaynodes;
    call RadioControl.start();
  }

  event void RadioControl.startDone(error_t result){
    call SendTimer.startPeriodic(send_interval);
  }

  event void RadioControl.stopDone(error_t result){}


  event void SendTimer.fired(){
    RssiMsg *rmsg = (RssiMsg*) call RssiMsgSend.getPayload(&msg, sizeof(RssiMsg));
    rmsg->nodeid = TOS_NODE_ID;
    rmsg->rssi = 0;
    rmsg->onodeid = TOS_NODE_ID;
    call RssiMsgSend.send((am_addr_t) rnode, &msg, sizeof(RssiMsg));
  }

  event void RssiMsgSend.sendDone(message_t *m, error_t error){
    call Leds.led1Toggle();
    if(++rnode > numnodes){
      rnode = TOS_NODE_ID+1;
    }
  }
}
