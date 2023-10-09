#import <Foundation/NSObject.h>
#import <Foundation/NSDate.h>
#import <Foundation/NSUUID.h>

@interface ObjCClass : NSObject
- (NSString*)fooWithArg:(int)arg arg2:(NSString*)arg2;
- (NSString*)fooWithArg:(int)ohNoOtherName name2:(NSString*)name2;
- (NSString*)fooWithArg:(int)arg name3:(NSString*)name3;
@end

