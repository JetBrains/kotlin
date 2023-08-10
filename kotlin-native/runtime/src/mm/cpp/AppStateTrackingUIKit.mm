/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_UIKIT_FRAMEWORK

#include "AppStateTracking.hpp"

#include <functional>

#import <UIKit/UIApplication.h>

#include "CompilerConstants.hpp"
#include "objc_support/NSNotificationSubscription.hpp"

using namespace kotlin;

class mm::AppStateTracking::Impl : private Pinned {
public:
    explicit Impl(std::function<void(State)> handler) noexcept :
        handler_(std::move(handler)),
        didEnterBackground_(UIApplicationDidEnterBackgroundNotification, [this] { handler_(State::kBackground); }),
        willEnterForeground_(UIApplicationWillEnterForegroundNotification, [this] { handler_(State::kForeground); }) {}

private:
    std::function<void(State)> handler_;
    objc_support::NSNotificationSubscription didEnterBackground_;
    objc_support::NSNotificationSubscription willEnterForeground_;
};

mm::AppStateTracking::AppStateTracking() noexcept {
    switch (compiler::appStateTracking()) {
        case compiler::AppStateTracking::kDisabled:
            break;
        case compiler::AppStateTracking::kEnabled:
            impl_ = std::make_unique<Impl>([this](State state) noexcept { setState(state); });
            break;
    }
}

mm::AppStateTracking::~AppStateTracking() = default;

#endif
