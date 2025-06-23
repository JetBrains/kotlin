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

@implementation Class1

- (instancetype)initWithF0:(id)f0 f1:(id)f1 f2:(id)f2 f3:(id)f3 f4:(id)f4 f5:(id)f5 f6:(id)f6 f7:(id)f7 f8:(id)f8 f9:(id)f9 {
    self = [super init];
    if (self) {
        self.f0 = f0;
        self.f1 = f1;
        self.f2 = f2;
        self.f3 = f3;
        self.f4 = f4;
        self.f5 = f5;
        self.f6 = f6;
        self.f7 = f7;
        self.f8 = f8;
        self.f9 = f9;
    }
    return self;
}

- (id)loadObjCField:(int32_t)index {
    switch(index % 10) {
        case 0: return self.f0;
        case 1: return self.f1;
        case 2: return self.f2;
        case 3: return self.f3;
        case 4: return self.f4;
        case 5: return self.f5;
        case 6: return self.f6;
        case 7: return self.f7;
        case 8: return self.f8;
        case 9: return self.f9;
        default: return nil;
    }
}

- (void)storeObjCField:(int32_t)index value:(id)value {
    switch(index % 10) {
        case 0: self.f0 = value;
        case 1: self.f1 = value;
        case 2: self.f2 = value;
        case 3: self.f3 = value;
        case 4: self.f4 = value;
        case 5: self.f5 = value;
        case 6: self.f6 = value;
        case 7: self.f7 = value;
        case 8: self.f8 = value;
        case 9: self.f9 = value;
    }
}

@end

@interface Globals : NSObject
@end

@implementation Globals
@end
static Globals* globals = nil;

id fun3(int32_t localsCount, id l0) {
    id l1 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:0 value:l1];
    id l2 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:1 value:l2];
    id l3 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:2 value:l3];
    id l4 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:3 value:l4];
    id l5 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:4 value:l5];
    id l6 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:5 value:l6];
    id l7 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:6 value:l7];
    id l8 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:7 value:l8];
    id l9 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:8 value:l9];
    id l10 = alloc(^{ return [[Class1 alloc] initWithF0:nil f1:nil f2:nil f3:nil f4:nil f5:nil f6:nil f7:nil f8:nil f9:nil]; });
    [l0 storeField:9 value:l10];
    return [l0 loadField:9];
}

int main() {
   globals = [Globals new];
   allocBlockerUpdater();
   for (int i = 0; i < 100000; ++i) {
       [KtlibKtlibKt mainBody];
   }
   return 0;
}
