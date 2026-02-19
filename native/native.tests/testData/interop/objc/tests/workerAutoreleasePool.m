#import "workerAutoreleasePool.h"

@implementation CreateAutoreleaseDeallocated
@end

@implementation CreateAutorelease {
    CreateAutoreleaseDeallocated* deallocated;
}

+(void)createAutorelease:(CreateAutoreleaseDeallocated*)deallocated {
    // __autoreleasing attribute prevents from early deallocation
    // and thus makes test behaviour identical on Intel and ARM CPUs.
    __autoreleasing CreateAutorelease* result = [self new];
    result->deallocated = deallocated;
}

-(void)dealloc {
    deallocated.value = YES;
}
@end
