#include "objclib.h"

#include <cinttypes>
#include <dispatch/dispatch.h>
#include <map>
#import <AppKit/NSApplication.h>
#import <Foundation/NSRunLoop.h>
#import <Foundation/NSThread.h>

std::map<uintptr_t, bool> dictionary;

@implementation OnDestroyHook
- (instancetype)init {
    if (self = [super init]) {
        dictionary[(uintptr_t)self] = true;
    }
    return self;
}

- (void)dealloc {
    dictionary[(uintptr_t)self] = false;
}

@end

extern "C" void startApp(void (^task)()) {
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

extern "C" BOOL isMainThread() {
    return [NSThread isMainThread];
}

extern "C" void spin() {
    if ([NSRunLoop currentRunLoop] != [NSRunLoop mainRunLoop]) {
        fprintf(stderr, "Must spin main run loop\n");
        exit(1);
    }
    while (true) {
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
        bool done = true;
        for (auto kvp : dictionary) {
            if (kvp.second) {
                done = false;
                break;
            }
        }
        if (done) return;
    }
}
