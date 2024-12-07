#import "initWithCustomSelector.h"

@implementation TestInitWithCustomSelector

-(instancetype)initCustom {
    if (self = [super init]) {
        self.custom = YES;
    }
    return self;
}

-(instancetype)init {
    if (self = [super init]) {
        self.custom = NO;
    }
    return self;
}

+(instancetype)createCustom {
    return [[self alloc] initCustom];
}

@end