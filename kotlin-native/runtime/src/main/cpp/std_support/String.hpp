/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <string>

#include "std_support/Memory.hpp"

namespace kotlin::std_support {

template <typename CharT, typename Traits = std::char_traits<CharT>, typename Allocator = allocator<CharT>>
using basic_string = std::basic_string<CharT, Traits, Allocator>;

using string = basic_string<char>;
using wstring = basic_string<wchar_t>;
using u16string = basic_string<char16_t>;
using u32string = basic_string<char32_t>;

} // namespace kotlin::std_support
