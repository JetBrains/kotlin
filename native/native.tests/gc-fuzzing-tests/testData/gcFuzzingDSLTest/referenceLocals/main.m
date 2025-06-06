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
   for (int i = 0; i < 100000; ++i) {
       [KtlibKtlibKt mainBody];
   }
   return 0;
}

static id g2 = nil;

id fun4(id l0) {
    if (!tryEnterFrame()) {
        return nil;
    }
    id l1 = [[KtlibClass0 alloc] initWithF0:l0];
    id l2 = [[KtlibClass0 alloc] initWithF0:l1];
    id l3 = [[KtlibClass0 alloc] initWithF0:l0];
    leaveFrame();
    return nil;
}

id fun6(id l0) {
    if (!tryEnterFrame()) {
        return nil;
    }
    id l1 = [KtlibKtlibKt fun3L0:l0];
    id l2 = [KtlibKtlibKt fun3L0:l1];
    id l3 = [KtlibKtlibKt fun3L0:l0];
    leaveFrame();
    return nil;
}

id fun8(id l0) {
    if (!tryEnterFrame()) {
        return nil;
    }
    id l1 = l0;
    id l2 = l1;
    id l3 = l0;
    leaveFrame();
    return nil;
}

id fun10(id l0) {
    if (!tryEnterFrame()) {
        return nil;
    }
    id l1 = nil;
    l1 = nil;
    l1 = nil;
    l1 = nil;
    leaveFrame();
    return nil;
}
