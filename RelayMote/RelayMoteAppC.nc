#include "RssiNetworkMessages.h"

configuration RelayMoteAppC {
} implementation {
  components ActiveMessageC, MainC;
  components new AMSenderC(AM_RSSIMSG);
  components new AMReceiverC(AM_RSSIMSG);
  components LedsC;
  components CC2420ActiveMessageC;

  components RelayMoteC as App;

  App.Boot -> MainC;
  App.Leds -> LedsC;
  App.Packet -> AMSenderC;
  App.AMPacket -> AMSenderC;
  App.AMSend -> AMSenderC;
  App.AMControl -> ActiveMessageC;
  App.Receive -> AMReceiverC;
  App.CC2420Packet -> CC2420ActiveMessageC;
}
