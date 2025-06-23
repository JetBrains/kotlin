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

