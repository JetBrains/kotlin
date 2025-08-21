#include <stdbool.h>
#include <stdint.h>
#import <Foundation/Foundation.h>

@protocol ObjCIndexAccess
- (id)loadObjCField:(int32_t)index;
- (void)storeObjCField:(int32_t)index value:(id)value;
@end

bool tryRegisterThread();
void unregisterThread();

bool updateAllocBlocker();

@interface Class1 : NSObject<ObjCIndexAccess>
@property id f0;
@property id f1;
@property id f2;
@property id f3;
@property id f4;
@property id f5;
@property id f6;
@property id f7;
@property id f8;
@property id f9;
- (instancetype)initWithF0:(id)f0 f1:(id)f1 f2:(id)f2 f3:(id)f3 f4:(id)f4 f5:(id)f5 f6:(id)f6 f7:(id)f7 f8:(id)f8 f9:(id)f9;
@end

id fun3(int32_t localsCount, id l0);
