#import "exceptions.h"

@implementation ExceptionThrowerManager
+(void)throwExceptionWith:(id<ExceptionThrower>)thrower {
    [thrower throwException];
}
@end