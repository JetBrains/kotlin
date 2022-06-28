/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_UIKIT_FRAMEWORK

#include "AppStateTracking.hpp"

#include <functional>

// Workaround from https://youtrack.jetbrains.com/issue/KT-48807#focus=Comments-27-5210791.0-0
// TODO: remove this once our clang supports NSAttributedString as a valid return from the formatter function.
#define NS_FORMAT_ARGUMENT(x)
#import <UIKit/UIApplication.h>
#undef NS_FORMAT_ARGUMENT

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
            impl_ = std_support::make_unique<Impl>([this](State state) noexcept { setState(state); });
            break;
    }
}

mm::AppStateTracking::~AppStateTracking() = default;

#endif
