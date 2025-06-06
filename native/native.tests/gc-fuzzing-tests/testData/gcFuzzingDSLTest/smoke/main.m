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
    int32_t nextLocalsCount = localsCount + 2;
    if (!tryEnterFrame(nextLocalsCount)) {
        return nil;
    }
    globals.g3 = l0;
    [l1 storeField:0 value:[globals.g3 loadField:1]];
    spawnThread(^{
        int32_t nextLocalsCount = 0;
        fun7(nextLocalsCount, l1);
    });
    return nil;
}

id fun7(int32_t localsCount, id l0) {
    int32_t nextLocalsCount = localsCount + 4;
    if (!tryEnterFrame(nextLocalsCount)) {
        return nil;
    }
    id l1 = [[KtlibClass0 alloc] initWithF0:nil f1:nil];
    id l2 = [[Class1 alloc] initWithF0:nil f1:nil];
    id l3 = [KtlibKtlibKt fun6LocalsCount:nextLocalsCount l0:l0];
    return [[[l1 loadField:1] loadField:3] loadField:4];
}

int main() {
   globals = [Globals new];
   for (int i = 0; i < 100000; ++i) {
       [KtlibKtlibKt mainBody];
   }
   return 0;
}