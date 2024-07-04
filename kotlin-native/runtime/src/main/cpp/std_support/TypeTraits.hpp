/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

namespace kotlin::std_support {

constexpr bool is_constant_evaluated() {
    return __builtin_is_constant_evaluated();
}

static_assert(is_constant_evaluated());

}
