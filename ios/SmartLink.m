

#import "SmartLink.h"
#import "smtiot.h"

@implementation SmartLink

+(BOOL) setSmartLink:(NSString *) SSID setAuthmod:(NSString *) Authmod setPassWord:(NSString *)PassWord
{
    //smartLink
    const char *ssid = [SSID UTF8String];
    const char *s_authmode = [Authmod cStringUsingEncoding:NSASCIIStringEncoding];
    int authmode = atoi(s_authmode);
    const char *password = [PassWord cStringUsingEncoding:NSUTF8StringEncoding];
    NSLog(@"OnStart: ssid = %s, authmode = %d, password = %s", ssid, authmode, password);
    InitSmartConnection();
    StartSmartConnection(ssid, password, "", authmode);
    return true;
}


+(void)StopSmartLink
{
    StopSmartConnection();
}

@end
