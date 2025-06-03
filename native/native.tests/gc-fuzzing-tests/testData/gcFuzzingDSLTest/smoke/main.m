#include "cinterop.h"

#include <stdatomic.h>

#import <Foundation/Foundation.h>

#include "ktlib.h"

@implementation NSObject (LoadStoreFieldsAdditions)

- (id)loadField:(int32_t)index {
    id this = self;
    if (!this) return nil;
    if ([this respondsToSelector:@selector(loadObjCField:)]) {
        return [this loadObjCField:index];
    }
    return [this loadKotlinFieldIndex:index];
}

- (void)storeField:(int32_t)index value:(id)value {
    id this = self;
    if (!this) return;
    if ([this respondsToSelector:@selector(storeObjCField:value:)]) {
        [this storeObjCField:index value:value];
        return;
    }
    [this storeKotlinFieldIndex:index value:value];
    return;
}

@end

static atomic_int allowedThreadsCount = 100;

bool tryRegisterThread() {
    if (atomic_fetch_sub_explicit(&allowedThreadsCount, 1, memory_order_relaxed) <= 0) {
        atomic_fetch_add_explicit(&allowedThreadsCount, 1, memory_order_relaxed);
        return false;
    }
    return true;
}

void unregisterThread() {
    atomic_fetch_add_explicit(&allowedThreadsCount, 1, memory_order_relaxed);
}

static void spawnThread(void (^block)()) {
    if (!tryRegisterThread())
        return;
    [NSThread detachNewThreadWithBlock:^{
        block();
        unregisterThread();
    }];
}

static inline id call(int32_t localsCount, int32_t blockLocalsCount, id (^block)(int32_t)) {
    int32_t nextLocalsCount = localsCount + blockLocalsCount;
    if (nextLocalsCount > 500) {
        return nil;
    }
    return block(nextLocalsCount);
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

@interface Globals : NSObject
@property id g3;
@end

@implementation Globals
@end
static Globals* globals = nil;

id fun5(int32_t localsCount, id l0, id l1) {
    globals.g3 = l0;
    [l1 storeField:0 value:[globals.g3 loadField:1]];
    spawnThread(^{
        int32_t localsCount = 0;
        call(localsCount, 4, ^(int32_t localsCount) { return fun7(localsCount, l1); });
    });
    return nil;
}

id fun7(int32_t localsCount, id l0) {
    id l1 = [[KtlibClass0 alloc] initWithF0:nil f1:nil];
    id l2 = [[Class1 alloc] initWithF0:nil f1:nil];
    id l3 = call(localsCount, 4, ^(int32_t localsCount) { return [KtlibKtlibKt fun6LocalsCount:localsCount l0:l0]; });
    return [[[l1 loadField:1] loadField:3] loadField:4];
}

int main() {
   globals = [Globals new];
   for (int i = 0; i < 100000; ++i) {
       [KtlibKtlibKt mainBody];
   }
   return 0;
}