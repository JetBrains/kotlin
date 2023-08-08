#include <inttypes.h>
#include <objc/NSObject.h>

@interface OnDestroyHook: NSObject
-(instancetype)init:(void(^)(uintptr_t))onDestroy;
-(uintptr_t)identity;
@end

void retain(uintptr_t);
void release(uintptr_t);
void autorelease(uintptr_t);
