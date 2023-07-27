#include "objclib.h"

@implementation OnDestroyHook {
    void (^_onDestroy)(uintptr_t);
}

-(uintptr_t)identity {
    return (uintptr_t)self;
}

-(instancetype)init:(void (^)(uintptr_t))onDestroy {
    if (self = [super init]) {
        _onDestroy = [onDestroy retain];
    }
    return self;
}

-(void)dealloc {
    _onDestroy([self identity]);
    [_onDestroy release];
    [super dealloc];
}

@end

void retain(uint64_t obj) {
    [((id) obj) retain];
}

void release(uint64_t obj) {
    [((id) obj) release];
}

void autorelease(uint64_t obj) {
    [((id) obj) autorelease];
}
