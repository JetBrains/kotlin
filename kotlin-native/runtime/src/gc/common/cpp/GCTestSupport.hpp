/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "GC.hpp"

namespace kotlin {
namespace gc {

void AssertClear(GC& gc) noexcept;

}
} // namespace kotlin
