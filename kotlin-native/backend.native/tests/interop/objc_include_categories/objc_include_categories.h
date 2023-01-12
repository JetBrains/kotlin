#import <objc/NSObject.h>

@interface BaseClass : NSObject
- (id) init;
@property float floatProperty;
@end;

@interface BaseClass(IncludeCategory)
- (id)initWithFloat:(float)number;
- (float) multiplyBy:(int)value;
@end;

@interface DerivedClass : BaseClass
- (id) init;
@property int intProperty;
@end;

@interface DerivedClass(IncludeCategory)
- (id)initWithInt:(int)number;
@end;