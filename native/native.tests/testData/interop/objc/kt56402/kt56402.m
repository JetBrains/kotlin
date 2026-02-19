#include "kt56402.h"

#include <assert.h>
#include <dispatch/dispatch.h>
#include <pthread.h>
#import <AppKit/NSApplication.h>
#import <Foundation/NSRunLoop.h>
#import <Foundation/NSThread.h>

@implementation OnDestroyHook {
    void (^onDestroy_)(uintptr_t);
}

- (uintptr_t)identity {
    return (uintptr_t)self;
}

- (instancetype)init:(void (^)(uintptr_t))onDestroy {
    if (self = [super init]) {
        onDestroy_ = onDestroy;
    }
    return self;
}

- (void)dealloc {
    onDestroy_([self identity]);
}

@end

@interface OnDestroyHookPrivate : NSObject<OnDestroyHook>
@end

@implementation OnDestroyHookPrivate {
    void (^onDestroy_)(uintptr_t);
}

- (uintptr_t)identity {
    return (uintptr_t)self;
}

- (instancetype)init:(void (^)(uintptr_t))onDestroy {
    if (self = [super init]) {
        onDestroy_ = onDestroy;
    }
    return self;
}

- (void)dealloc {
    onDestroy_([self identity]);
}

@end

void startApp(void (^task)()) {
    dispatch_async(dispatch_get_main_queue(), ^{
        // At this point all other scheduled main queue tasks were already executed.
        // Executing via performBlock to allow a recursive run loop in `spin()`.
        [[NSRunLoop currentRunLoop] performBlock:^{
            task();
            [NSApp terminate:NULL];
        }];
    });
    [[NSApplication sharedApplication] run];
}

uint64_t currentThreadId() {
    uint64_t result;
    int ret = pthread_threadid_np(NULL, &result);
    assert(ret == 0);
    return result;
}

BOOL isMainThread() {
    return [NSThread isMainThread];
}

void spin() {
    if ([NSRunLoop currentRunLoop] != [NSRunLoop mainRunLoop]) {
        fprintf(stderr, "Must spin main run loop\n");
        exit(1);
    }
    [[NSRunLoop currentRunLoop]
           runMode:NSDefaultRunLoopMode
        beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
}

OnDestroyHook* global = NULL;

OnDestroyHook* newGlobal(void(^onDestroy)(uintptr_t)) {
    global = [[OnDestroyHook alloc] init:onDestroy];
    return global;
}

void clearGlobal() {
    global = NULL;
}

id<OnDestroyHook> newOnDestroyHook(void(^onDestroy)(uintptr_t)) {
    return [[OnDestroyHookPrivate alloc] init:onDestroy];
}
