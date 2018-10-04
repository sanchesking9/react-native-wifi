//
//  ViewController.m
//  OneKeyWifi
//
//  Created by 莫晓文 on 16/7/18.
//  Copyright © 2016年 VSTARTCAM. All rights reserved.
//



#import "ViewController.h"
#import <SystemConfiguration/CaptiveNetwork.h>
#import "voiceEncoder.h"
#import "SmartLink.h"

@interface ViewController ()

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    //1.获取当前WIFI
    [self GetCurrentWiFiSSID];
    
    //音波频率
    int i;
    freq = (int*)malloc(sizeof(int)*19);
    freq[0] = 6500;
    for (i = 0; i < 18; i++) {
        freq[i + 1] = freq[i] + 200;
    }
    
    
    [self createUI];
    
    
}

-(void)createUI
{
    
    CGRect bounds = [[UIScreen mainScreen] bounds];
    MyWifi = [[UITextField alloc]initWithFrame:CGRectMake(20, 74, bounds.size.width-30, 40)];
    MyWifi.text = MyWiFiSSID;
    MyWifi.borderStyle = UITextBorderStyleRoundedRect;
    [self.view addSubview:MyWifi];
    
    MyPassword  = [[UITextField alloc]initWithFrame:CGRectMake(20, 134, bounds.size.width-30, 40)];
    MyPassword.placeholder =@"请输入WIFI密码";
    MyPassword.borderStyle = UITextBorderStyleRoundedRect;
    MyPassword.secureTextEntry = NO;
    [self.view addSubview:MyPassword];
    
    Sendbtn =[[UIButton alloc]initWithFrame:CGRectMake(35, 194, 85, 45)];
    [Sendbtn setTitle:@"Send" forState:UIControlStateNormal];
    [Sendbtn setTintColor:[UIColor whiteColor]];
    [Sendbtn setBackgroundColor:[UIColor blackColor]];
    [Sendbtn addTarget:self action:@selector(PlayVoice) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:Sendbtn];
    
    
    Cancelbtn =[[UIButton alloc]initWithFrame:CGRectMake(200, 194, 85, 45)];
    [Cancelbtn setTitle:@"Cancel" forState:UIControlStateNormal];
    [Cancelbtn setTintColor:[UIColor whiteColor]];
    [Cancelbtn setBackgroundColor:[UIColor blackColor]];
    [Cancelbtn addTarget:self action:@selector(StopVoice) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:Cancelbtn];
    

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
    
    
}

-(void)PlayVoice
{   _times = 0;//控制播放次数
    NSThread *voiceThread = [[NSThread alloc]initWithTarget:self selector:@selector(VoiceThread) object:nil];
    _voiceThread = voiceThread;
    [voiceThread start];
    
    //smartLink
    [SmartLink StopSmartLink];
    [SmartLink setSmartLink:MyWiFiSSID setAuthmod:@"0" setPassWord:MyPassword.text];
    
    
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
        
        [play playWiFi:_mac macLen:1 pwd:MyPassword.text playCount:[[aTimer userInfo] integerValue] muteInterval:8000];
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



- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
