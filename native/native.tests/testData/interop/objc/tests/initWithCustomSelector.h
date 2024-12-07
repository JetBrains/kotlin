#import <Foundation/NSObject.h>

@interface TestInitWithCustomSelector : NSObject
-(instancetype)initCustom;
@property BOOL custom;

+(instancetype _Nonnull)createCustom;
@end