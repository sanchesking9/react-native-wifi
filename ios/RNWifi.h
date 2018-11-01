
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import "voiceEncoder.h"
#import <AVFoundation/AVFoundation.h>

@interface RNWifi : NSObject <RCTBridgeModule> {
    int *freq;
    NSString *MyWiFiSSID;
    NSString *MyWiFiMac;
    NSInteger _times;
    NSThread *_voiceThread;// 播放声波的子线程
    VoiceEncoder *play;
    char *_mac;//用来播放的数组
    NSTimer *_voiceTimesTimer;//播放timer 用来循环播放
    NSString *MyPassword; //UITextField *MyPassword;
    unsigned int ip;
}

@end
