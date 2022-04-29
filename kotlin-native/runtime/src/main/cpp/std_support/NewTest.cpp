/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/New.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

namespace {

class Class {
public:
    explicit Class(int32_t x = 17) : x_(x) {}

    int32_t x() const { return x_; }

private:
    int32_t x_;
};

class ClassThrows {
public:
    explicit ClassThrows(int32_t x = 17) : x_(x) { throw 13; }

    int32_t x() const { return x_; }

private:
    int32_t x_;
};

} // namespace

TEST(NewTest, NewDelete) {
    Class* ptr = new (std_support::kalloc) Class(42);
    EXPECT_THAT(ptr->x(), 42);
    std_support::kdelete(ptr);
}

TEST(NewTest, NewThrows) {
    EXPECT_THROW(new (std_support::kalloc) ClassThrows(42), int);
}
