/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <set>

#include "std_support/Memory.hpp"

namespace kotlin::std_support {

template <typename Key, typename Compare = std::less<Key>, typename Allocator = allocator<Key>>
using set = std::set<Key, Compare, Allocator>;

template <typename Key, typename Compare = std::less<Key>, typename Allocator = allocator<Key>>
using multiset = std::multiset<Key, Compare, Allocator>;

} // namespace kotlin::std_support
