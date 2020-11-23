/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_CPP_SUPPORT_H
#define RUNTIME_CPP_SUPPORT_H

#include <type_traits>
#include <memory>

// A collection of backported utilities from future C++ versions.

namespace kotlin {
namespace std_support {

////////////////////////// C++14 //////////////////////////

template <typename T, typename... Args>
std::unique_ptr<T> make_unique(Args&&... args) {
    return std::unique_ptr<T>(new T(std::forward<Args>(args)...));
}

template <typename T>
using make_unsigned_t = typename std::make_unsigned<T>::type;

////////////////////////// C++17 //////////////////////////

template <typename T>
constexpr bool is_trivially_destructible_v = std::is_trivially_destructible<T>::value;
template <typename T>
constexpr bool is_nothrow_default_constructible_v = std::is_nothrow_default_constructible<T>::value;
template <typename T>
constexpr bool is_nothrow_destructible_v = std::is_nothrow_destructible<T>::value;
template <typename T>
constexpr bool is_copy_constructible_v = std::is_copy_constructible<T>::value;
template <typename T>
constexpr bool is_copy_assignable_v = std::is_copy_assignable<T>::value;
template <typename T>
constexpr bool is_move_constructible_v = std::is_move_constructible<T>::value;
template <typename T>
constexpr bool is_move_assignable_v = std::is_move_assignable<T>::value;
template <typename T>
constexpr bool is_nothrow_move_constructible_v = std::is_nothrow_move_constructible<T>::value;
template <typename T>
constexpr bool is_nothrow_move_assignable_v = std::is_nothrow_move_assignable<T>::value;

} // namespace std_support
} // namespace kotlin

#endif // RUNTIME_CPP_SUPPORT_H
