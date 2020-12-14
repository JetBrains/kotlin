#import <Foundation/NSObject.h>

extern BOOL kt42482Deallocated;
extern id kt42482Global;

@interface KT42482 : NSObject
-(int)fortyTwo;
@end;

void kt42482Swizzle(id obj);
