#import <Foundation/NSObject.h>

@interface TestCallableReferences : NSObject
@property int value;
- (int)instanceMethod;
+ (int)classMethod:(int)first :(int)second;
@end