#include "cinterop.h"

#include <stdatomic.h>

#import <Foundation/Foundation.h>
#include <mach/mach.h>

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

static NSLock* allocBlockerLock = nil;
static atomic_bool allocBlocker = false;

static size_t footprint() {
    struct task_vm_info info;
    mach_msg_type_number_t vmInfoCount = TASK_VM_INFO_COUNT;
    kern_return_t err = task_info(mach_task_self(), TASK_VM_INFO, (task_info_t)&info, &vmInfoCount);
    if (err != KERN_SUCCESS) {
        [NSException raise:NSGenericException format:@"Failed to get the footprint err=%d", err];
    }
    return info.phys_footprint;
}

enum MemoryPressureLevel {
    LOW_PRESSURE,
    MEDIUM_PRESSURE,
    HIGH_PRESSURE,
};

static enum MemoryPressureLevel currentMemoryPressureLevel() {
    size_t currentFootprint = footprint();
    if (currentFootprint < 2684354560)
        return LOW_PRESSURE;
    if (currentFootprint <= 3221225472)
        return MEDIUM_PRESSURE;
    return HIGH_PRESSURE;
}

static bool allocBlockerInNormalMode() {
    switch (currentMemoryPressureLevel()) {
        case LOW_PRESSURE:
        case MEDIUM_PRESSURE:
            return false;
        case HIGH_PRESSURE:
            break;
    }
    [KtlibKtlibKt performGC];
    switch (currentMemoryPressureLevel()) {
        case LOW_PRESSURE:
        case MEDIUM_PRESSURE:
            return false;
        case HIGH_PRESSURE:
            return true;
    }
}

static bool allocBlockerInHazardMode() {
    switch (currentMemoryPressureLevel()) {
        case LOW_PRESSURE:
            return false;
        case MEDIUM_PRESSURE:
        case HIGH_PRESSURE:
            break;
    }
    [KtlibKtlibKt performGC];
    switch (currentMemoryPressureLevel()) {
        case LOW_PRESSURE:
            return false;
        case MEDIUM_PRESSURE:
        case HIGH_PRESSURE:
            return true;
    }
}

bool updateAllocBlocker() {
    [allocBlockerLock lock];
    bool result = allocBlocker ? allocBlockerInNormalMode() : allocBlockerInHazardMode();
    allocBlocker = result;
    KtlibKtlibKt.allocBlocker = result;
    [allocBlockerLock unlock];
    return result;
}

static void allocBlockerUpdater() {
    allocBlockerLock = [NSLock new];
    [NSThread detachNewThreadWithBlock:^{
        while (true) {
            updateAllocBlocker();
            [NSThread sleepForTimeInterval:1.0];
        }
    }];
}

static inline id alloc(id (^block)()) {
    if (!atomic_load_explicit(&allocBlocker, memory_order_relaxed) || !updateAllocBlocker())
        return block();
    return nil;
}

@interface Globals : NSObject
@end

@implementation Globals
@end
static Globals* globals = nil;

int main() {
   globals = [Globals new];
   allocBlockerUpdater();
   for (int i = 0; i < 100000; ++i) {
       [KtlibKtlibKt mainBody];
   }
   return 0;
}