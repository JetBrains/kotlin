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

@interface Globals : NSObject
@property id g2;
@end

@implementation Globals
@end
static Globals* globals = nil;

id fun4(int32_t localsCount, id l0) {
    id l1 = [[KtlibClass0 alloc] initWithF0:l0];
    id l2 = [[KtlibClass0 alloc] initWithF0:l1];
    id l3 = [[KtlibClass0 alloc] initWithF0:l0];
    return nil;
}

id fun6(int32_t localsCount, id l0) {
    id l1 = call(localsCount, 4, ^(int32_t localsCount) { return [KtlibKtlibKt fun3LocalsCount:localsCount l0:l0]; });
    id l2 = call(localsCount, 4, ^(int32_t localsCount) { return [KtlibKtlibKt fun3LocalsCount:localsCount l0:l1]; });
    id l3 = call(localsCount, 4, ^(int32_t localsCount) { return [KtlibKtlibKt fun3LocalsCount:localsCount l0:l0]; });
    return nil;
}

id fun8(int32_t localsCount, id l0) {
    id l1 = l0;
    id l2 = l1;
    id l3 = l0;
    return nil;
}

id fun10(int32_t localsCount, id l0) {
    id l1 = nil;
    l1 = nil;
    l1 = nil;
    l1 = nil;
    return nil;
}

int main() {
   globals = [Globals new];
   for (int i = 0; i < 100000; ++i) {
       [KtlibKtlibKt mainBody];
   }
   return 0;
}