#include "KT38234_override.h"

@implementation KT38234_Base
-(int)foo {
    return 1;
}
-(int)callFoo {
    return [self foo];
}
@end
