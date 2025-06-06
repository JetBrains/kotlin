#include "cinterop.h"

#include <stdatomic.h>
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

static atomic_int maxThreadsCount = 100;

static void spawnThread(void (^block)()) {
    int allowedThreads = atomic_fetch_sub(&maxThreadsCount, 1);
    if (allowedThreads <= 0) {
        atomic_fetch_add(&maxThreadsCount, 1);
        return;
    }
    [NSThread detachNewThreadWithBlock:^{
        block();
        atomic_fetch_add(&maxThreadsCount, 1);
    }];
}

static bool tryEnterFrame(int32_t localsCount) {
    return localsCount < 500;
}

int main() {
   for (int i = 0; i < 100000; ++i) {
       [KtlibKtlibKt mainBody];
   }
   return 0;
}

static id g2 = nil;

id fun4(int32_t localsCount, id l0) {
    int32_t nextLocalsCount = localsCount + 4;
    if (!tryEnterFrame(nextLocalsCount)) {
        return nil;
    }
    id l1 = [[KtlibClass0 alloc] initWithF0:l0];
    id l2 = [[KtlibClass0 alloc] initWithF0:l1];
    id l3 = [[KtlibClass0 alloc] initWithF0:l0];
    return nil;
}

id fun6(int32_t localsCount, id l0) {
    int32_t nextLocalsCount = localsCount + 4;
    if (!tryEnterFrame(nextLocalsCount)) {
        return nil;
    }
    id l1 = [KtlibKtlibKt fun3LocalsCount:nextLocalsCount l0:l0];
    id l2 = [KtlibKtlibKt fun3LocalsCount:nextLocalsCount l0:l1];
    id l3 = [KtlibKtlibKt fun3LocalsCount:nextLocalsCount l0:l0];
    return nil;
}

id fun8(int32_t localsCount, id l0) {
    int32_t nextLocalsCount = localsCount + 4;
    if (!tryEnterFrame(nextLocalsCount)) {
        return nil;
    }
    id l1 = l0;
    id l2 = l1;
    id l3 = l0;
    return nil;
}

id fun10(int32_t localsCount, id l0) {
    int32_t nextLocalsCount = localsCount + 2;
    if (!tryEnterFrame(nextLocalsCount)) {
        return nil;
    }
    id l1 = nil;
    l1 = nil;
    l1 = nil;
    l1 = nil;
    return nil;
}
