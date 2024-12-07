#import "overrideInit.h"

@implementation TestOverrideInit
-(instancetype)initWithValue:(int)value {
    return self = [super init];
}

+(instancetype)createWithValue:(int)value {
    return [[self alloc] initWithValue:value];
}
@end
