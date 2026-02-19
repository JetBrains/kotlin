#include "cinterop.h"
#include <dispatch/dispatch.h>
#import <AppKit/NSApplication.h>
#import <Foundation/NSRunLoop.h>
#import <Foundation/NSThread.h>

uintptr_t useArray(NSArray* arr) {
    uintptr_t r = 0;
    for (id object in arr) {
        r += [object hash];
    }
    return r;
}

void task(id (^produceFinalizables)(), void(^collect)(), void(^schedule)(), void(^consume)(id)) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [[NSRunLoop currentRunLoop] performBlock:^{
            for (int i = 0; i < 10; ++i) {
                @autoreleasepool {
                    id x = produceFinalizables();
                    consume(x);
                }
                collect();
                schedule();
                [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:1]];
            }
            [NSApp terminate:NULL];
            [NSApp stop:NULL];
        }];
    });
    [[NSApplication sharedApplication] run];
}
