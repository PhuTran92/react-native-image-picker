//
//  ImagePickerBridge.m
//  react-native-image-picker
//
//  Created by Phu Tran on 3/26/22.
//

#import <Foundation/Foundation.h>
#import "UIKit/UIKit.h"

#import <React/RCTBridgeModule.h>


@interface RCT_EXTERN_MODULE(ImagePickerManager, NSObject)

RCT_EXTERN_METHOD(launchImageLibrary:(NSDictionary *)options callback:(RCTResponseSenderBlock *)callback)
RCT_EXTERN_METHOD(launchCamera:(NSDictionary *)options callback:(RCTResponseSenderBlock *)callback)

@end
