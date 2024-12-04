#include "objc_wrap.h"

#import <Foundation/Foundation.h>

void raiseExc(id name, id reason) {
    [NSException raise:name format:@"%@", reason];
}

id logExc(id exc) {
    assert([exc isKindOfClass:[NSException class]]);
    return ((NSException*)exc).name;
}

@implementation Foo : NSObject
- (void) instanceMethodThrow:(id)name reason:(id)reason {
    raiseExc(name, reason);
}
+ (void) classMethodThrow:(id)name reason:(id)reason {
    raiseExc(name, reason);
}
@end
