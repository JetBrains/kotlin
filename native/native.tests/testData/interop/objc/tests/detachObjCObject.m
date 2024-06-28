#import "detachObjCObject.h"

@implementation DeallocFlagHolder
@end

@implementation ObjectWithDeallocFlag
- (instancetype)init {
    if (self = [super init]) {
        self.deallocFlagHolder = [DeallocFlagHolder new];
        self.deallocFlagHolder.deallocated = NO;
    }
    return self;
}
- (void)dealloc {
    self.deallocFlagHolder.deallocated = YES;
}

- (instancetype _Nonnull)sameObject; {
    return self;
}
@end