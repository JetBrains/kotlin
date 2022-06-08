#import <Foundation/NSObject.h>

@protocol ExceptionThrower
-(void)throwException;
@end

@interface ExceptionThrowerManager : NSObject
+(void)throwExceptionWith:(id<ExceptionThrower>)thrower;
@end
