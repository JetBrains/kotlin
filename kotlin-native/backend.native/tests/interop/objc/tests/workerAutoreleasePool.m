#import "workerAutoreleasePool.h"

@implementation CreateAutoreleaseDeallocated
@end;

@implementation CreateAutorelease {
    CreateAutoreleaseDeallocated* deallocated;
}
+(instancetype)create:(CreateAutoreleaseDeallocated*)deallocated {
    // __autoreleasing attribute prevents from early deallocation
    // and thus makes test behaviour identical on Intel and ARM CPUs.
    __autoreleasing CreateAutorelease* result = [self new];
    result->deallocated = deallocated;
    return result;
}

+(void)createAutorelease:(CreateAutoreleaseDeallocated*)deallocated {
    [self create:deallocated];
}

-(void)dealloc {
    deallocated.value = YES;
}
@end;
