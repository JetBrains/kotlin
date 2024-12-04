/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include <functional>
#include <objc/objc.h>

#include "ObjCForward.hpp"
#include "ObjectPtr.hpp"
#include "Utils.hpp"

OBJC_FORWARD_DECLARE(NSNotificationCenter);
OBJC_FORWARD_DECLARE(NSString);

namespace kotlin::objc_support {

class NSNotificationSubscription : private MoveOnly {
public:
    NSNotificationSubscription(NSNotificationCenter* center, NSString* name, std::function<void()> handler) noexcept;
    NSNotificationSubscription(NSString* name, std::function<void()> handler) noexcept;

    NSNotificationSubscription(NSNotificationSubscription&&) = default;
    NSNotificationSubscription& operator=(NSNotificationSubscription&&) = default;

    ~NSNotificationSubscription() { reset(); }

    void reset() noexcept;
    bool subscribed() const noexcept;
    explicit operator bool() const noexcept { return subscribed(); }

private:
    object_ptr<NSNotificationCenter> center_;
    // center_ will hold the strong reference to token_ anyway.
    id token_;
};

} // namespace kotlin::objc_support

#endif
