
#import "RNWifi.h"
#import <React/RCTLog.h>
#import "SmartLink.h"
#import <SystemConfiguration/CaptiveNetwork.h>

@implementation RNWifi

-(void)PlayVoice
{   _times = 0;//控制播放次数
    NSThread *voiceThread = [[NSThread alloc]initWithTarget:self selector:@selector(VoiceThread) object:nil];
    _voiceThread = voiceThread;
    [voiceThread start];
    
    //smartLink
    [SmartLink StopSmartLink];
    [SmartLink setSmartLink:MyWiFiSSID setAuthmod:@"0" setPassWord:MyPassword];
    
    
}

//获取当前WiFi名字以及Mac地址
- (NSString *)GetCurrentWiFiSSID {
    NSArray *ifs = (__bridge id)CNCopySupportedInterfaces();
    
    
    id info = nil;
    for (NSString *ifnam in ifs)
    {
        info = (__bridge id)CNCopyCurrentNetworkInfo((__bridge CFStringRef)ifnam);
        
        if (info && [info count])
        {
            break;
        }
    }
    
    NSDictionary *dctySSID = (NSDictionary* )info;
    NSString *ssid = [dctySSID objectForKey:@"SSID"];
    MyWiFiSSID =[[NSString alloc] initWithFormat:@"%@", ssid];
    
    NSString *Bssid = [dctySSID objectForKey:@"BSSID"];
    MyWiFiMac =[[NSString alloc] initWithFormat:@"%@", Bssid];
    NSLog(@"________%@",MyWiFiMac);
    
    NSString *tempSSID = [[NSString alloc] initWithFormat:@"%@+%@", ssid, Bssid];
    
    return tempSSID;
}

- (void)DidLoad {
    //1.获取当前WIFI
    [self GetCurrentWiFiSSID];
    
    //音波频率
    int i;
    freq = (int*)malloc(sizeof(int)*19);
    freq[0] = 6500;
    for (i = 0; i < 18; i++) {
        freq[i + 1] = freq[i] + 200;
    }
}

-(void)VoiceThread
{
    play = [[VoiceEncoder alloc] init];
    
    
    NSArray *array = [NSArray array];
    array = [MyWiFiMac componentsSeparatedByString:@":"];
    NSLog(@"array[4]:%@,array[5]%@",array[4],array[5]);
    
    NSString *str1 = [NSString string];
    str1 = array[5];
    
    NSString *str = [NSString string];
    NSString *str2 = [NSString string];
    NSString *astr;
    NSString *bstr;
    NSString *cstr;
    unsigned long red = 0;
    unsigned long blue = 0;
    unsigned long yellow;
    
    
    if ([array[5] isEqualToString:@"0"]) {
        str = array[3];
        str2 = array[4];
        
        bstr = [NSString stringWithFormat:@"0x%@",str];
        cstr = [NSString stringWithFormat:@"0x%@",str2];
        
        blue = strtoul([cstr UTF8String],0,0);
        red = strtoul([bstr UTF8String],0,0);
        
    }
    astr = [NSString stringWithFormat:@"0x%@",str1];
    yellow = strtoul([astr UTF8String],0,0);
    
    if (MyWiFiMac) {
        
        [play setFreqs:freq freqCount:19];
        
        if ([array[5] isEqualToString:@"0"]){
            
            char mac[2] = {red,blue};
            _mac = mac;
            
            NSRunLoop *runLoop = [NSRunLoop currentRunLoop];
            _voiceTimesTimer = [NSTimer scheduledTimerWithTimeInterval:0.5f target:self selector:@selector(startPlay:) userInfo:[NSNumber numberWithInt:2] repeats:YES] ;
            [runLoop run];
        }else{
            
            char mac[1] = {yellow};
            _mac = mac;
            
            NSRunLoop *runLoop = [NSRunLoop currentRunLoop];
            _voiceTimesTimer = [NSTimer scheduledTimerWithTimeInterval:0.5f target:self selector:@selector(startPlay:) userInfo:[NSNumber numberWithInt:1] repeats:YES] ;
            
            [runLoop run];
            
            
        }
        
    }
    
    
}

- (void)startPlay:(NSTimer *)aTimer {
    @autoreleasepool {
        
        [play playWiFi:_mac macLen:1 pwd:MyPassword playCount:[[aTimer userInfo] integerValue] muteInterval:8000];
        while (![play isStopped]) {
            usleep(600*4000);
        }
        _times ++;
        if (_times == 10) {
            [_voiceTimesTimer invalidate];
            _voiceTimesTimer = nil;
            play = nil;
            
            [_voiceThread cancel];
            [SmartLink StopSmartLink];
        }
    }
}




RCT_EXPORT_MODULE(WIFIMan);

RCT_EXPORT_METHOD(list:(RCTResponseSenderBlock)callback) {
    [self DidLoad];
    NSArray *wifiList = @[MyWiFiSSID];
    callback(@[[NSNull null], wifiList]);
}

RCT_EXPORT_METHOD(sendSonic:(NSString *)pwd) {
    [self DidLoad];
    MyPassword = pwd;
    NSThread *playVoice = [[NSThread alloc]initWithTarget:self selector:@selector(PlayVoice) object:nil];
    [playVoice start];
}

@end
