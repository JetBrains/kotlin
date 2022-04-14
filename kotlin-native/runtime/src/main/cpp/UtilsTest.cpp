/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Utils.hpp"

#include <type_traits>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

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
    static_assert(std::is_nothrow_default_constructible_v<MoveOnlyImpl>, "Must be nothrow default constructible");
    static_assert(std::is_nothrow_destructible_v<MoveOnlyImpl>, "Must be nothrow destructible");
    static_assert(!std::is_copy_constructible_v<MoveOnlyImpl>, "Must not be copy constructible");
    static_assert(!std::is_copy_assignable_v<MoveOnlyImpl>, "Must not be copy assignable");
    static_assert(std::is_nothrow_move_constructible_v<MoveOnlyImpl>, "Must be nothrow move constructible");
    static_assert(std::is_nothrow_move_assignable_v<MoveOnlyImpl>, "Must be nothrow move assignable");
    static_assert(sizeof(MoveOnlyImpl) == sizeof(A), "Must not increase size");
}

TEST(UtilsTest, PinnedImpl) {
    static_assert(std::is_nothrow_default_constructible_v<PinnedImpl>, "Must be nothrow default constructible");
    static_assert(std::is_nothrow_destructible_v<PinnedImpl>, "Must be nothrow destructible");
    static_assert(!std::is_copy_constructible_v<PinnedImpl>, "Must not be copy constructible");
    static_assert(!std::is_copy_assignable_v<PinnedImpl>, "Must not be copy assignable");
    static_assert(!std::is_move_constructible_v<PinnedImpl>, "Must not be move constructible");
    static_assert(!std::is_move_assignable_v<PinnedImpl>, "Must not be move assignable");
    static_assert(sizeof(PinnedImpl) == sizeof(A), "Must not increase size");
}

namespace {

class Container {
public:
    static Container& fromX(int32_t& x) { return ownerOf(Container, x_, x); }

    static Container& fromY(void*& y) { return ownerOf(Container, y_, y); }

    int32_t& x() { return x_; }
    void*& y() { return y_; }

private:
    int32_t x_ = 0;
    void* y_ = nullptr;
};

} // namespace

TEST(UtilsTest, OwnerOf) {
    Container c;
    EXPECT_THAT(&c, &Container::fromX(c.x()));
    EXPECT_THAT(&c, &Container::fromY(c.y()));
}
