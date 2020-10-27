#import "tryRetainGC.h"
#import <Foundation/NSArray.h>

@implementation WeakRefHolder
-(void)loadManyTimes {
    NSMutableArray* array = [NSMutableArray new];
    for (int i = 0; i < 10000; ++i) {
        [array addObject:self.obj];
    }
}
@end;