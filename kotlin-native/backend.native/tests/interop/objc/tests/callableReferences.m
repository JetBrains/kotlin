#import "callableReferences.h"

@implementation TestCallableReferences
- (int)instanceMethod {
    return self.value;
}

+ (int)classMethod:(int)first :(int)second {
    return first + second;
}
@end