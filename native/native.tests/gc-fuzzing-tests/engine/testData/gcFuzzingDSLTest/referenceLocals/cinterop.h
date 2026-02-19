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

id fun4(int32_t localsCount, id l0);
id fun6(int32_t localsCount, id l0);
id fun8(int32_t localsCount, id l0);
id fun10(int32_t localsCount, id l0);
