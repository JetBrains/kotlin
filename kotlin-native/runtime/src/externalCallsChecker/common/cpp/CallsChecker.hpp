/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Common.h"
#include "Utils.hpp"

#define NO_EXTERNAL_CALLS_CHECK __attribute__((annotate("no_external_calls_check")))

namespace kotlin {

// Ignore thread state checker for the scope.
//
// Can be used to ignore specific call sites that are known to be safe.
class CallsCheckerIgnoreGuard : private Pinned {
public:
    ALWAYS_INLINE CallsCheckerIgnoreGuard() noexcept;
    ALWAYS_INLINE ~CallsCheckerIgnoreGuard();
};

} // namespace kotlin
