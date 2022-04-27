/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <forward_list>

#include "std_support/Memory.hpp"

namespace kotlin::std_support {

template <typename T, typename Allocator = allocator<T>>
using forward_list = std::forward_list<T, Allocator>;

} // namespace kotlin::std_support
