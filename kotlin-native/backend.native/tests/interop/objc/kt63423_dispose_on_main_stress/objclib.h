#include <objc/NSObject.h>

@interface OnDestroyHook : NSObject
- (instancetype)init;
@end

#ifdef __cplusplus
extern "C" {
#endif

void startApp(void (^task)());
BOOL isMainThread();
void spin();

#ifdef __cplusplus
}
#endif