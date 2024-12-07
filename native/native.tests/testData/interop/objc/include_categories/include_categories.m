#include "include_categories.h"

@implementation BaseClass
-(id) init {
    self.floatProperty = 3.0f;
    return self;
}
@end;

@implementation BaseClass(IncludeCategory)
- (id)initWithFloat:(float)number {
    self.floatProperty = number;
    return self;
}
- (float) multiplyBy:(int)number {
    return self.floatProperty * 2;
}
@end;

@implementation DerivedClass
-(id) init {
    self = [super init];
    self.intProperty = 3;
    return self;
}
@end;

@implementation DerivedClass(IncludeCategory)
- (id)initWithInt:(int)number {
    self = [self init];
    self.intProperty = number;
    return self;
}
@end;