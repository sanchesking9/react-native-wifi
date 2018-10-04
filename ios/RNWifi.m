#import "RNWifi.h"
#import <NetworkExtension/NetworkExtension.h>
#import <SystemConfiguration/CaptiveNetwork.h>
#import "voiceEncoder.h"
// If using official settings URL
//#import <UIKit/UIKit.h>

@implementation WifiManager
RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(connectToSSID:(NSString*)ssid
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    if (@available(iOS 11.0, *)) {
        NEHotspotConfiguration* configuration = [[NEHotspotConfiguration alloc] initWithSSID:ssid];
        configuration.joinOnce = true;
        
        [[NEHotspotConfigurationManager sharedManager] applyConfiguration:configuration completionHandler:^(NSError * _Nullable error) {
            if (error != nil) {
                reject(@"nehotspot_error", @"Error while configuring WiFi", error);
            } else {
                resolve(nil);
            }
        }];
        
    } else {
        reject(@"ios_error", @"Not supported in iOS<11.0", nil);
    }
}

RCT_EXPORT_METHOD(connectToProtectedSSID:(NSString*)ssid
                  withPassphrase:(NSString*)passphrase
                  isWEP:(BOOL)isWEP
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    if (@available(iOS 11.0, *)) {
        NEHotspotConfiguration* configuration = [[NEHotspotConfiguration alloc] initWithSSID:ssid passphrase:passphrase isWEP:isWEP];
        configuration.joinOnce = true;
        
        [[NEHotspotConfigurationManager sharedManager] applyConfiguration:configuration completionHandler:^(NSError * _Nullable error) {
            if (error != nil) {
                reject(@"nehotspot_error", @"Error while configuring WiFi", error);
            } else {
                resolve(nil);
            }
        }];
        
    } else {
        reject(@"ios_error", @"Not supported in iOS<11.0", nil);
    }
}

RCT_EXPORT_METHOD(disconnectFromSSID:(NSString*)mac
                  (NSString*)wifi
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    if (@available(iOS 11.0, *)) {
        [[NEHotspotConfigurationManager sharedManager] getConfiguredSSIDsWithCompletionHandler:^(NSArray<NSString *> *ssids) {
            if (ssids != nil && [ssids indexOfObject:ssid] != NSNotFound) {
                [[NEHotspotConfigurationManager sharedManager] removeConfigurationForSSID:ssid];
            }
            resolve(nil);
        }];
    } else {
        reject(@"ios_error", @"Not supported in iOS<11.0", nil);
    }
    
}

RCT_EXPORT_METHOD(list
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    if (@available(iOS 11.0, *)) {
        // TODO: give list of wifi spots
        resolve(nil);
    } else {
        reject(@"ios_error", @"Not supported in iOS<11.0", nil);
    }
    
}

RCT_REMAP_METHOD(getCurrentWifiSSID,
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
    
    NSString *kSSID = (NSString*) kCNNetworkInfoKeySSID;
    
    NSArray *ifs = (__bridge_transfer id)CNCopySupportedInterfaces();
    for (NSString *ifnam in ifs) {
        NSDictionary *info = (__bridge_transfer id)CNCopyCurrentNetworkInfo((__bridge CFStringRef)ifnam);
        if (info[kSSID]) {
            resolve(info[kSSID]);
            return;
        }
    }
    
    reject(@"cannot_detect_ssid", @"Cannot detect SSID", nil);
}

RCT_EXPORT_METHOD(sendSonic:(NSString*)ssid
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
    if (@available(iOS 11.0, *)) {
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
        resolve(nil);
    } else {
        reject(@"ios_error", @"Not supported in iOS<11.0", nil);
    }
}

- (NSDictionary*)constantsToExport {
    // Officially better to use UIApplicationOpenSettingsURLString
    return @{
             @"settingsURL": @"App-Prefs:root=WIFI"
             };
}

@end

