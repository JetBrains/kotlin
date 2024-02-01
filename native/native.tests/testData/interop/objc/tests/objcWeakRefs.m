#import "objcWeakRefs.h"

@implementation DeallocExecutor
-(void)dealloc {
    self.deallocListener.deallocated = YES;
}
@end

@implementation DeallocListener
-(BOOL)deallocExecutorIsNil {
    return self.deallocExecutor == nil;
}
@end