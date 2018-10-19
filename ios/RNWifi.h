
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import "voiceEncoder.h"

@interface RNWifi : NSObject <RCTBridgeModule> {
    NSInteger _times;
    NSThread *_voiceThread;// 播放声波的子线程
    NSString *MyPassword;
    
    NSString *MyWiFiMac;
    NSString *MyWiFiSSID;
    NSTimer *_voiceTimesTimer;//播放timer 用来循环播放
    int *freq;
    
    char *_mac;//用来播放的数组
    
    VoiceEncoder *play;
}

@end
