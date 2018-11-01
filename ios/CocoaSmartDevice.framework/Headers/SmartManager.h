//
//  SmartDeviceManager.h
//  CocoaSmartDevice
//
//  Created by 杨柳 on 2018/2/25.
//  Copyright © 2018年 com.vstarcam. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef void (^ScanWifiCallback)(BOOL success, NSString *wifi);
typedef void (^ConfigDeviceCallback)(BOOL success, NSString *device_id,NSString *master_address);
typedef void (^AddSmartDeviceCallback)(BOOL success, NSString *device_id,NSString *master_address);

/**
 在线状态更改协议
 */
@protocol SmartDeviceProtocol <NSObject>

@required

/**
 状态更改
 
 @param device_id 设备ID
 @param state 设备状态
 */
- (void)connectState:(NSString *)device_id State:(int) state;

/**
 设备接收消息
 
 @param device_id 设备ID
 @param message 消息
 */
- (void)publishMessage:(NSString *)device_id Message:(NSData *) message;
/**
 设备响应消息
 
 @param device_id 设备ID
 @param message 消息
 */
- (void)responseMessage:(NSString *)device_id Message:(NSData *) message;

@end

@interface SmartManager : NSObject{
}

@property(readonly,weak)id<SmartDeviceProtocol> delegate;
/**
 获取单例
 
 @return 单例对象
 */
+(instancetype) shareInstance;

/**
 根据用户ID和用户Token初始化
 
 @param user_id 用户ID
 @param user_token 用户Token
 @return 是否成功
 */
-(BOOL) initWithUserId:(long) user_id Token:(NSString *)user_token;

/**
 设置回调
 
 @param delegate 协议回调
 */
-(void) setSmartDeviceDelegate:(id<SmartDeviceProtocol>) delegate;
//
///**
// 搜索设备附件的WIFI
// 
// @param callback 回调
// @return 是否成功
// */
//+(BOOL)scanWifi:(ScanWifiCallback) callback;
//
///**
// 配置设备
// 
// @param ssid WIFI ssid
// @param ssidPassword WIFI 密码
// @param serverPassword 服务器密码
// @param devicePassword 设备密码
// @param callback 回调
// @return 是否成功
// */
//+(BOOL)configDeviceWithSSID:(NSString *)ssid SSIDPassword:(NSString *)ssidPassword ServerPassword:(NSString *)serverPassword DevicePassword:(NSString *)devicePassword Callback:(ConfigDeviceCallback) callback;

/**
 添加设备

 @param device_id 设备ID
 @param device_password 设备密码
 @param apply_name 设备OEM名称
 @param callback 回调
 @return 成功与否
 */
+(BOOL)addSmartDeviceWithDeviceId:(NSString *)device_id DevicePassword:(NSString*) device_password ApplyName:(NSString*) apply_name Callback:(AddSmartDeviceCallback) callback;

/**
 移除设备

 @param device_id 设备ID
 @return 成功与否
 */
+(BOOL)removeSmartDeviceWithDeviceId:(NSString*) device_id;

/**
 设置设备密码

 @param device_id 设备ID
 @param password 设备密码
 @return 成功与否
 */
+(BOOL)setDevicePassword:(NSString *)device_id Password:(NSString *) password;

/**
 关闭MQTT连接

 @return 成功与否
 */
+(BOOL)closeMQTT;

/**
 打开MQTT连接

 @return 成功与否
 */
+(BOOL)startMQTT;

/**
 发送数据

 @param device_id 设备ID
 @param message 数据
 @return 是否成功
 */
+(BOOL)actionDevice:(NSString *)device_id Message:(NSData *) message;

/**
 请求数据

 @param device_id 设备ID
 @param message 数据
 @return 是否成功
 */
+(BOOL)requestDevice:(NSString *)device_id Message:(NSData *)message;

@end
