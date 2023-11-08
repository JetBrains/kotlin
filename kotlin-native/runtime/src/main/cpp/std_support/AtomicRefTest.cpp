/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AtomicRef.hpp"

#include <iterator>
#include <type_traits>

#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "ScopedThread.hpp"

using namespace kotlin;

namespace {

} // namespace

TEST(AtomicRefTest, Load) {
    int x = 42;
    std_support::atomic_ref aref{x};
    x = 32;
    EXPECT_THAT(aref.load(), x);
}

TEST(AtomicRefTest, Store) {
    int x = 42;
    std_support::atomic_ref aref{x};
    aref.store(37);
    EXPECT_THAT(x, 37);
}

TEST(AtomicRefTest, LoadOperator) {
    int x = 42;
    std_support::atomic_ref aref{x};
    x = 32;
    EXPECT_THAT(aref, x);
}

TEST(AtomicRefTest, Assignment) {
    int x = 42;
    std_support::atomic_ref aref{x};
    aref = 37;
    EXPECT_THAT(x, 37);
}

TEST(AtomicRefTest, Exchange) {
    int x = 42;
    std_support::atomic_ref aref{x};
    EXPECT_THAT(aref.exchange(37), 42);
    EXPECT_THAT(x, 37);
}

TEST(AtomicRefTest, CompareExchangeWeak) {
    int x = 42;
    std_support::atomic_ref aref{x};
    int expected = 37;
    EXPECT_FALSE(aref.compare_exchange_weak(expected, 108));
    EXPECT_THAT(x, 42);
    expected = 42;
    while (!aref.compare_exchange_weak(expected, 108)) {}
    EXPECT_THAT(x, 108);
}

TEST(AtomicRefTest, CompareExchangeStrong) {
    int x = 42;
    std_support::atomic_ref aref{x};
    int expected = 37;
    EXPECT_FALSE(aref.compare_exchange_strong(expected, 108));
    EXPECT_THAT(x, 42);
    expected = 42;
    EXPECT_TRUE(aref.compare_exchange_strong(expected, 108));
    EXPECT_THAT(x, 108);
}

TEST(AtomicRefTest, ReleaseAcquire) {
    int data = 0;
    bool available = false;
    auto producer = ScopedThread([&]() {
        data = 42;
        std_support::atomic_ref{available}.store(true, std::memory_order_release);
    });
    auto consumer = ScopedThread([&]() {
        while (!std_support::atomic_ref{available}.load(std::memory_order_acquire)) {}
        EXPECT_THAT(data, 42);
    });
}
