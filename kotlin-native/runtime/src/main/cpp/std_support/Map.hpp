/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <map>

#include "std_support/Memory.hpp"

namespace kotlin::std_support {

template <typename Key, typename T, typename Compare = std::less<Key>, typename Allocator = allocator<std::pair<const Key, T>>>
using map = std::map<Key, T, Compare, Allocator>;

template <typename Key, typename T, typename Compare = std::less<Key>, typename Allocator = allocator<std::pair<const Key, T>>>
using multimap = std::multimap<Key, T, Compare, Allocator>;

} // namespace kotlin::std_support
