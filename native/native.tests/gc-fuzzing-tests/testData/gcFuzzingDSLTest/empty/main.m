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

