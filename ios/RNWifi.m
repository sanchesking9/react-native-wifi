
#import "RNWifi.h"
#import <SystemConfiguration/CaptiveNetwork.h>
#import "voiceEncoder.h"
#import "SmartLink.h"
#import <Foundation/Foundation.h>
#include <ifaddrs.h>
#import "BoSmartLink.h"
#include <ifaddrs.h>
#include <arpa/inet.h>
#import <ifaddrs.h>

@interface RNWifi ()
//@property(nonatomic, strong) RACCommand *startBoSmartCommand;  //博通
@property(nonatomic,strong) NSTimer *SendBoSmartLinkTimer;
@end

@implementation RNWifi

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

    NSDictionary *dctySSID = (NSDictionary *)info;
    NSString *ssid = [dctySSID objectForKey:@"SSID"];
    MyWiFiSSID =[[NSString alloc] initWithFormat:@"%@", ssid];

    NSString *Bssid = [dctySSID objectForKey:@"BSSID"];
    MyWiFiMac =[[NSString alloc] initWithFormat:@"%@", Bssid];
    NSLog(@"________%@",MyWiFiMac);

    NSString *tempSSID = [[NSString alloc] initWithFormat:@"%@+%@", ssid, Bssid];

    return tempSSID;
}

-(void)PlayVoice
{   _times = 0;//控制播放次数
    NSThread *voiceThread = [[NSThread alloc]initWithTarget:self selector:@selector(VoiceThread) object:nil];
    _voiceThread = voiceThread;
    [voiceThread start];

    //smartLink
    [SmartLink StopSmartLink];
    [SmartLink setSmartLink:MyWiFiSSID setAuthmod:@"0" setPassWord:MyPassword];

    struct in_addr addr;
    inet_aton([[self getIPAddress] UTF8String], &addr);
    ip = CFSwapInt32BigToHost(ntohl(addr.s_addr));


    //    dispatch_async(dispatch_get_main_queue(), ^{
    //        self.SendBoSmartLinkTimer = [NSTimer scheduledTimerWithTimeInterval:1 target:self selector:@selector(StartCooee) userInfo:nil repeats:YES];
    //        [[NSRunLoop currentRunLoop]addTimer:self.SendBoSmartLinkTimer forMode:NSRunLoopCommonModes];
    //    });
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(6 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        //[self StartCooee];
        self.SendBoSmartLinkTimer = [NSTimer scheduledTimerWithTimeInterval:1 target:self selector:@selector(StartCooee) userInfo:nil repeats:YES];
    });
    //[BoSmartLink setBoSmartLink:MyWiFiSSID setLen:(int)strlen([MyWiFiSSID UTF8String]) setPassWord:MyWiFiPwd setPwdLen://(int)strlen([MyWiFiPwd UTF8String]) SetKey:@"" setKeyLen:0 SetIP:ip];

}

-(void)StartCooee {

    struct in_addr addr;
    inet_aton([[self getIPAddress] UTF8String], &addr);
    ip = CFSwapInt32BigToHost(ntohl(addr.s_addr));

    NSLog(@"MyWiFiSSID %@",MyWiFiSSID);
    NSLog(@"MyWiFiPwd %@",MyPassword);

    //NSLog(@"WIFISSID:%@,WIFILen:%d,WiFIPWD:%@,WIFIPwdLen:%d,IP:%@",MyWiFiSSID,(int)strlen([MyWiFiSSID UTF8String]),MyWiFiPwd,(int)strlen([MyWiFiPwd UTF8String]),ip);

    [BoSmartLink setBoSmartLink:MyWiFiSSID setLen:(int)strlen([MyWiFiSSID UTF8String]) setPassWord:MyPassword setPwdLen:(int)strlen([MyPassword UTF8String]) SetKey:@"" setKeyLen:0 SetIP:ip];


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

- (NSString *)getIPAddress
{
    NSString *address = @"error";
    struct ifaddrs *interfaces = NULL;
    struct ifaddrs *temp_addr = NULL;
    int success = 0;

    // retrieve the current interfaces - returns 0 on success
    success = getifaddrs(&interfaces);
    if (success == 0)
    {
        // Loop through linked list of interfaces
        temp_addr = interfaces;
        while(temp_addr != NULL)
        {
            if(temp_addr->ifa_addr->sa_family == AF_INET)
            {
                // Check if interface is en0 which is the wifi connection on the iPhone
                if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"])
                {
                    // Get NSString from C String
                    address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
                }
            }
            temp_addr = temp_addr->ifa_next;
        }
    }

    // Free memory
    freeifaddrs(interfaces);

    return address;
}

-(void)StopVoice
{
    [play isStopped];

    play = nil;

    if ([_voiceTimesTimer isValid])
    {
        [_voiceTimesTimer invalidate];
        _voiceTimesTimer = nil;
    }

    if (_voiceThread)
    {
        [_voiceThread cancel];
        _voiceThread = nil;
    }

    [SmartLink StopSmartLink];

    [_SendBoSmartLinkTimer invalidate];

    _SendBoSmartLinkTimer = nil;


}

RCT_EXPORT_MODULE(WIFIMan);

RCT_EXPORT_METHOD(list:(RCTResponseSenderBlock)callback) {
    [self DidLoad];
    NSArray *wifiList = @[];
    if(![MyWiFiSSID isEqualToString: @"(null)"]) {
        wifiList = @[MyWiFiSSID];
    }
    callback(@[[NSNull null], wifiList]);
}

RCT_EXPORT_METHOD(sendSonic:(NSString *)pwd) {
    [self DidLoad];
    MyPassword = pwd;
    [self PlayVoice];
}

RCT_EXPORT_METHOD(stopConnect) {
    [self StopVoice];
}

@end
