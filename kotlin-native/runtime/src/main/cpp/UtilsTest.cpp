/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Utils.hpp"

#include <type_traits>

#include "gtest/gtest.h"

namespace {

// TODO: use std variants when we move to C++17
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

struct A {
    int field;
};

class MoveOnlyImpl : private kotlin::MoveOnly {
public:
    A a;
};

class PinnedImpl : private kotlin::Pinned {
public:
    A a;
};

} // namespace

TEST(UtilsTest, MoveOnlyImpl) {
    static_assert(is_nothrow_default_constructible_v<MoveOnlyImpl>, "Must be nothrow default constructible");
    static_assert(is_nothrow_destructible_v<MoveOnlyImpl>, "Must be nothrow destructible");
    static_assert(!is_copy_constructible_v<MoveOnlyImpl>, "Must not be copy constructible");
    static_assert(!is_copy_assignable_v<MoveOnlyImpl>, "Must not be copy assignable");
    static_assert(is_nothrow_move_constructible_v<MoveOnlyImpl>, "Must be nothrow move constructible");
    static_assert(is_nothrow_move_assignable_v<MoveOnlyImpl>, "Must be nothrow move assignable");
    static_assert(sizeof(MoveOnlyImpl) == sizeof(A), "Must not increase size");
}

TEST(UtilsTest, PinnedImpl) {
    static_assert(is_nothrow_default_constructible_v<PinnedImpl>, "Must be nothrow default constructible");
    static_assert(is_nothrow_destructible_v<PinnedImpl>, "Must be nothrow destructible");
    static_assert(!is_copy_constructible_v<PinnedImpl>, "Must not be copy constructible");
    static_assert(!is_copy_assignable_v<PinnedImpl>, "Must not be copy assignable");
    static_assert(!is_move_constructible_v<PinnedImpl>, "Must not be move constructible");
    static_assert(!is_move_assignable_v<PinnedImpl>, "Must not be move assignable");
    static_assert(sizeof(PinnedImpl) == sizeof(A), "Must not increase size");
}
