/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#pragma once

#if __has_include(<optional>)
#include <optional>
#elif __has_include(<experimental/optional>)
// TODO: Remove when wasm32 is gone.
#include <experimental/optional>
namespace std {
template <typename T>
using optional = std::experimental::optional<T>;
inline constexpr auto nullopt = std::experimental::nullopt;
} // namespace std
#else
#error "No <optional>"
#endif
