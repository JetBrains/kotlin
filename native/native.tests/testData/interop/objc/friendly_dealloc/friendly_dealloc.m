#include "friendly_dealloc.h"

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

void retain(uintptr_t obj) {
    [((id) obj) retain];
}

void release(uintptr_t obj) {
    [((id) obj) release];
}

void autorelease(uintptr_t obj) {
    [((id) obj) autorelease];
}
