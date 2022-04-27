/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <unordered_map>

#include "std_support/Memory.hpp"

namespace kotlin::std_support {

template <
        typename Key,
        typename T,
        typename Hash = std::hash<Key>,
        typename KeyEqual = std::equal_to<Key>,
        typename Allocator = allocator<std::pair<const Key, T>>>
using unordered_map = std::unordered_map<Key, T, Hash, KeyEqual, Allocator>;

template <
        typename Key,
        typename T,
        typename Hash = std::hash<Key>,
        typename KeyEqual = std::equal_to<Key>,
        typename Allocator = allocator<std::pair<const Key, T>>>
using unordered_multimap = std::unordered_multimap<Key, T, Hash, KeyEqual, Allocator>;

} // namespace kotlin::std_support
