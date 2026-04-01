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

@interface Class3 : NSObject<ObjCIndexAccess>
@property id f0;
@property(weak) id f1;
- (instancetype)initWithF0:(id)f0 f1:(id)f1;
@end

id fun5(int32_t localsCount);
