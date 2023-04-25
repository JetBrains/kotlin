#import "direct.h"

#define TEST_METHOD_IMPL(NAME) (uint64_t)NAME:(uint64_t)arg { return arg; }

@implementation CallingConventions : NSObject

+ TEST_METHOD_IMPL(regular);
- TEST_METHOD_IMPL(regular);

+ TEST_METHOD_IMPL(direct);
- TEST_METHOD_IMPL(direct);

@end

@implementation CallingConventions(Ext)

+ TEST_METHOD_IMPL(regularExt);
- TEST_METHOD_IMPL(regularExt);

+ TEST_METHOD_IMPL(directExt);
- TEST_METHOD_IMPL(directExt);

@end

@implementation CallingConventionsHeir

@end
