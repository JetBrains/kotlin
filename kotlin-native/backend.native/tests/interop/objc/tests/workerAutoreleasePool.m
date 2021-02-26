#import "workerAutoreleasePool.h"

@implementation CreateAutoreleaseDeallocated
@end;

@implementation CreateAutorelease {
    CreateAutoreleaseDeallocated* deallocated;
}
+(instancetype)create:(CreateAutoreleaseDeallocated*)deallocated {
    CreateAutorelease* result = [self new];
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
