#include "RssiNetworkMessages.h"

configuration SendingMoteAppC {
} implementation {
  components ActiveMessageC, MainC;
  components new AMSenderC(AM_RSSIMSG) as RssiMsgSender;
  components new TimerMilliC() as SendTimer;
  components LedsC;

  components SendingMoteC as App;

  App.Boot -> MainC;
  App.SendTimer -> SendTimer;
  App.Leds -> LedsC;

  App.RssiMsgSend -> RssiMsgSender;
  App.AMPacket -> RssiMsgSender;
  App.RadioControl -> ActiveMessageC;
}
