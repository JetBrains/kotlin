#import "lib.h"

@implementation ObjCClass {
}

- (NSString*)fooWithArg:(int)arg arg2:(NSString*)arg2 {
  return @"A";
}

- (NSString*)fooWithArg:(int)ohNoOtherName name2:(NSString*)name2 {
  return @"B";  
}

- (NSString*)fooWithArg:(int)arg name3:(NSString*)name3 {
  return @"C";  
}

@end