#import <Foundation/NSObject.h>

@interface TestOverrideInit : NSObject
-(instancetype)initWithValue:(int)value NS_DESIGNATED_INITIALIZER;
+(instancetype)createWithValue:(int)value;
@end