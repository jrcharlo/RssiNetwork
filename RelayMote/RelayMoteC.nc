#include "../RssiNetworkMessages.h"

module RelayMoteC {
  uses interface Boot;
  uses interface Leds;
  uses interface Packet;
  uses interface AMPacket;
  uses interface AMSend;
  uses interface SplitControl as AMControl;
  uses interface Receive;
  uses interface CC2420Packet;
} implementation {
  bool busy = FALSE;
  RssiMsg* rmsg;

  uint16_t getRssi(message_t *msg){
    return (uint16_t) call CC2420Packet.getRssi(msg);
  }

  event void Boot.booted() {
    call Leds.led0Toggle();
    call AMControl.start();
  }

  event void AMControl.startDone(error_t err) {
    if(err == SUCCESS){
      call Leds.led0Toggle();
    }
    else{
      call AMControl.start();
    }
  }

  event void AMControl.stopDone(error_t err) {
  }

  event void AMSend.sendDone(message_t* msg, error_t error) {
    if (error == SUCCESS) {
      busy = FALSE;
      call Leds.led1Toggle();
    }
    else{
      call Leds.led2Toggle();
    }
  }

  event message_t* Receive.receive(message_t* msg, void* payload, uint8_t len) {
    if(len == sizeof(RssiMsg)){
      if(call AMPacket.isForMe(msg)){
        rmsg = (RssiMsg*) call AMSend.getPayload(msg, sizeof(RssiMsg));
        rmsg->nodeid = TOS_NODE_ID;
        rmsg->rssi = getRssi(msg);
        if(!busy){
          //call AMPacket.setDestination(&msg, rnode);
          if (call AMSend.send(/*AM_BROADCAST_ADDR*/(am_addr_t) 0, msg, sizeof(RssiMsg)) == SUCCESS) {
            call Leds.led1Toggle();
            busy = TRUE;
          }
        }
      }
    }
    return msg;
  }
}
