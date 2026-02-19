/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "NSNotificationSubscription.hpp"

#import <Foundation/Foundation.h>

using namespace kotlin;

objc_support::NSNotificationSubscription::NSNotificationSubscription(
        NSNotificationCenter* center, NSString* name, std::function<void()> handler) noexcept :
    center_([center retain]),
    token_([center addObserverForName:name
                               object:nil
                                queue:nil
                           usingBlock:^(NSNotification* notification) {
                               handler();
                           }]) {}

objc_support::NSNotificationSubscription::NSNotificationSubscription(NSString* name, std::function<void()> handler) noexcept :
    NSNotificationSubscription([NSNotificationCenter defaultCenter], name, std::move(handler)) {}

bool objc_support::NSNotificationSubscription::subscribed() const noexcept {
    return token_ != nil;
}

void objc_support::NSNotificationSubscription::reset() noexcept {
    @autoreleasepool {
        [*center_ removeObserver:token_];
        center_.reset();
        token_ = nil;
    }
}

#endif
