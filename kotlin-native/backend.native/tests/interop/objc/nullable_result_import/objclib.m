#include "objclib.h"

@implementation Resource
-(void)async_get_result:(void (^)(NSString *_Nullable_result result, NSError *_Nullable err))completionHandler {
    completionHandler(@"Hello", NULL);
}
@end