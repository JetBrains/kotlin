#import "allocNoRetain.h"

@implementation TestAllocNoRetain
-(instancetype)init {
    __weak id weakSelf = self;
    self = [TestAllocNoRetain alloc];
    if (self = [super init]) {
        // Ensure that original self value was deallocated:
        self.ok = (weakSelf == nil);
        // So it's RC was 1, which means there wasn't redundant retain applied to it.
    }
    return self;
}
@end