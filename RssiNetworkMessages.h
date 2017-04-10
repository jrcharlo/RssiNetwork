#ifndef RSSINETWORKMESSAGES_H__
#define RSSINETWORKMESSAGES_H__

enum {
  AM_RSSIMSG = 10
};

typedef nx_struct RssiMsg{
  nx_int16_t nodeid;
  nx_int16_t rssi;
} RssiMsg;

#endif //RSSINETWORKMESSAGES_H__
