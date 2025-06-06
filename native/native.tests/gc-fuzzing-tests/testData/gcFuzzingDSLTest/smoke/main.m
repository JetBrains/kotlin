#include "cinterop.h"

#include <stdbool.h>

#import <Foundation/Foundation.h>

#include "ktlib.h"

@implementation NSObject (LoadStoreFieldsAdditions)

- (id)loadField:(int32_t)index {
    id this = self;
    if (!this) return nil;
    if ([this respondsToSelector:@selector(loadKotlinFieldIndex:)]) {
        return [this loadKotlinFieldIndex:index];
    }
    if ([this respondsToSelector:@selector(loadObjCField:)]) {
        return [this loadObjCField:index];
    }
    @throw [NSException exceptionWithName:NSGenericException reason:@"Invalid loadField call" userInfo:nil];
}

- (void)storeField:(int32_t)index value:(id)value {
    id this = self;
    if (!this) return;
    if ([this respondsToSelector:@selector(storeKotlinFieldIndex:value:)]) {
        [this storeKotlinFieldIndex:index value:value];
    }
    if ([this respondsToSelector:@selector(storeObjCField:value:)]) {
        [this storeObjCField:index value:value];
    }
    @throw [NSException exceptionWithName:NSGenericException reason:@"Invalid storeField call" userInfo:nil];
}

@end

static void spawnThread(void (^block)()) {
    [NSThread detachNewThreadWithBlock:block];
}

static _Thread_local int64_t frameCount = 100;

static bool tryEnterFrame(void) {
    if (frameCount-- <= 0) {
        ++frameCount;
        return false;
    }
    return true;
}

static void leaveFrame(void) {
    ++frameCount;
}

int main() {
   [KtlibKtlibKt mainBody];
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

static id g3 = nil;

id fun5(id l0, id l1) {
    if (!tryEnterFrame()) {
        return nil;
    }
    g3 = l0;
    [l1 storeField:0 value:[g3 loadField:1]];
    spawnThread(^{
        fun7(l1);
    });
    leaveFrame();
    return nil;
}

id fun7(id l0) {
    if (!tryEnterFrame()) {
        return nil;
    }
    id l1 = [[KtlibClass0 alloc] initWithF0:nil f1:nil];
    id l2 = [[Class1 alloc] initWithF0:nil f1:nil];
    id l3 = [KtlibKtlibKt fun6L0:l0];
    leaveFrame();
    return [[[l1 loadField:1] loadField:3] loadField:4];
}
