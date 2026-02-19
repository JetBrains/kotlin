#import <Foundation/NSObject.h>

@protocol BlockProvider
@required
-(int (^)(int)) block;
@end

int callProvidedBlock(id<BlockProvider> blockProvider, int argument) {
  return [blockProvider block](argument);
}

@protocol BlockConsumer
@required
-(int)callBlock:(int (^)(int))block argument:(int)argument;
@end

int callPlusOneBlock(id<BlockConsumer> blockConsumer, int argument) {
  return [blockConsumer callBlock:^int(int p) { return p + 1; } argument:argument];
}

@interface Blocks : NSObject
+(BOOL)blockIsNull:(void (^)(void))block;
+(int (^)(int, int, int, int))same:(int (^)(int, int, int, int))block;

@property (class) void (^nullBlock)(void);
@property (class) void (^notNullBlock)(void);
@end
