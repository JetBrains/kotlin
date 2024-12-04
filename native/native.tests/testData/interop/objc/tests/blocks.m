#import "blocks.h"

@implementation Blocks
+(BOOL)blockIsNull:(void (^)(void))block {
    return block == nil;
}

+(int (^)(int, int, int, int))same:(int (^)(int, int, int, int))block {
    return block;
}

+(void (^)(void)) nullBlock {
    return nil;
}

+(void (^)(void)) notNullBlock {
    return ^{};
}

@end