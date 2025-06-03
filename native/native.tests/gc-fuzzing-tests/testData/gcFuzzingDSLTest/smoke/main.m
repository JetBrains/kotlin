#include "cinterop.h"

#import <Foundation/Foundation.h>

#include "KotlinObjCFramework.h"

void spawnThread(void (^block)()) {
    [NSThread detachNewThreadWithBlock:block];
}

int main() {
   [KOCFLibKt mainBody];
   return 0;
}

@implementation Class1
- (instancetype)initWithF0:(id)f0 f1:(id)f1 {
    self = [super init];
    if (self) {
        self.f0 = f0;
        self.f1 = f1;
    }
    return self;
}
- (id)loadObjCField:(int32_t)index {
    switch(index % 2) {
        case 0: return self.f0;
        case 1: return self.f1;
        default: return nil;
    }
}
- (void)storeObjCField:(int32_t)index value:(id)value {
    switch(index % 2) {
        case 0: self.f0 = value;
        case 1: self.f1 = value;
    }
}
@end
id g3 = nil;
id fun5(id l0, id l1) {
    g3 = l0;
    [l1 storeObjCField:0 value:[g3 loadObjCField:1]];
    spawnThread(^{
        fun7(l1);
    });
    return nil;
}
id fun7(id l0) {
    id l1 = [[KOCFClass0 alloc] initWithF0:nil f1:nil ];
    id l2 = [[Class1 alloc] initWithF0:nil f1:nil ];
    id l3 = [KOCFLibKt fun6L0:l0 ];
    return [[[l1 loadObjCField:1] loadObjCField:3] loadObjCField:4];
}
