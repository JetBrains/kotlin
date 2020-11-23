/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Utils.hpp"

#include "gtest/gtest.h"

#include "CppSupport.hpp"

using namespace kotlin;

namespace {

struct A {
    int field;
};

class MoveOnlyImpl : private MoveOnly {
public:
    A a;
};

class PinnedImpl : private Pinned {
public:
    A a;
};

} // namespace

TEST(UtilsTest, MoveOnlyImpl) {
    static_assert(std_support::is_nothrow_default_constructible_v<MoveOnlyImpl>, "Must be nothrow default constructible");
    static_assert(std_support::is_nothrow_destructible_v<MoveOnlyImpl>, "Must be nothrow destructible");
    static_assert(!std_support::is_copy_constructible_v<MoveOnlyImpl>, "Must not be copy constructible");
    static_assert(!std_support::is_copy_assignable_v<MoveOnlyImpl>, "Must not be copy assignable");
    static_assert(std_support::is_nothrow_move_constructible_v<MoveOnlyImpl>, "Must be nothrow move constructible");
    static_assert(std_support::is_nothrow_move_assignable_v<MoveOnlyImpl>, "Must be nothrow move assignable");
    static_assert(sizeof(MoveOnlyImpl) == sizeof(A), "Must not increase size");
}

TEST(UtilsTest, PinnedImpl) {
    static_assert(std_support::is_nothrow_default_constructible_v<PinnedImpl>, "Must be nothrow default constructible");
    static_assert(std_support::is_nothrow_destructible_v<PinnedImpl>, "Must be nothrow destructible");
    static_assert(!std_support::is_copy_constructible_v<PinnedImpl>, "Must not be copy constructible");
    static_assert(!std_support::is_copy_assignable_v<PinnedImpl>, "Must not be copy assignable");
    static_assert(!std_support::is_move_constructible_v<PinnedImpl>, "Must not be move constructible");
    static_assert(!std_support::is_move_assignable_v<PinnedImpl>, "Must not be move assignable");
    static_assert(sizeof(PinnedImpl) == sizeof(A), "Must not increase size");
}
