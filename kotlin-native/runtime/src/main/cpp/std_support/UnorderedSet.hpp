/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <unordered_set>

#include "std_support/Memory.hpp"

namespace kotlin::std_support {

template <typename Key, typename Hash = std::hash<Key>, typename KeyEqual = std::equal_to<Key>, typename Allocator = allocator<Key>>
using unordered_set = std::unordered_set<Key, Hash, KeyEqual, Allocator>;

template <typename Key, typename Hash = std::hash<Key>, typename KeyEqual = std::equal_to<Key>, typename Allocator = allocator<Key>>
using unordered_multiset = std::unordered_multiset<Key, Hash, KeyEqual, Allocator>;

} // namespace kotlin::std_support
