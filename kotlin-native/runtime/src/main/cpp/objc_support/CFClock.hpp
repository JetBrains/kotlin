/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include <chrono>
#include <CoreFoundation/CFDate.h>

namespace kotlin::objc_support {

// A clock like `std::chrono::system_clock`, but uses CoreFoundation under the hood.
// Unlike `system_clock`, the epoch of this clock is 1 Jan 2001 00:00:00 GMT
class cf_clock {
public:
    using rep = CFTimeInterval;
    using period = std::ratio<1>;
    using duration = std::chrono::duration<rep, period>;
    using time_point = std::chrono::time_point<cf_clock>;

    static constexpr bool is_steady = false;

    static time_point now() noexcept { return fromCFAbsoluteTime(CFAbsoluteTimeGetCurrent()); }

    static CFAbsoluteTime toCFAbsoluteTime(const time_point& t) noexcept { return t.time_since_epoch().count(); }

    static time_point fromCFAbsoluteTime(CFAbsoluteTime t) noexcept { return time_point(duration(t)); }
};

} // namespace kotlin::objc_support

#endif