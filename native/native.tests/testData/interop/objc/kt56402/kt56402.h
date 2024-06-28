#include <inttypes.h>
#include <objc/NSObject.h>

@interface OnDestroyHook: NSObject
-(instancetype)init:(void(^)(uintptr_t))onDestroy;
-(uintptr_t)identity;
@end

@protocol OnDestroyHook
-(uintptr_t)identity;
@end

void startApp(void(^task)());
uint64_t currentThreadId();
BOOL isMainThread();
void spin();

OnDestroyHook* newGlobal(void(^onDestroy)(uintptr_t));
void clearGlobal();

id<OnDestroyHook> newOnDestroyHook(void(^onDestroy)(uintptr_t));
