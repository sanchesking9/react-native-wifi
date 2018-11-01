//
//  BoSmartLink.m
//  Eye4
//
//  Created by pengfeiV on 15/8/6.
//
//

#import "BoSmartLink.h"
#import "cooee.h"
@implementation BoSmartLink

+(void) setBoSmartLink:(NSString *) SSID setLen:(int) len setPassWord:(NSString *)PassWord setPwdLen:(int)pwdLen SetKey:(NSString *)key setKeyLen:(int)KeyLen SetIP:(unsigned int)IP{
    
    const char *KEY;
    KEY = [@"" UTF8String];
    const char *SID;
    SID = [SSID UTF8String];
    const char *PWD;
    PWD = [PassWord UTF8String];
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        
        send_cooee(SID,  (int)strlen(SID), PWD, (int)strlen(PWD), KEY, 0, IP);
        
    });
    
    
    
}
@end
