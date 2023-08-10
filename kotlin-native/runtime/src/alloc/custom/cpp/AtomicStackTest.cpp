/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AtomicStack.hpp"

#include <array>

#include "TestSupport.hpp"
#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ScopedThread.hpp"

using namespace kotlin;

namespace {

struct Element {
    std::atomic<Element*> next_ = nullptr;
};

} // namespace

TEST(AtomicStackTest, StressPushPop) {
    alloc::AtomicStack<Element> ready;
    alloc::AtomicStack<Element> used;
    std::vector<Element> elements(1000);
    std::vector<Element*> expected;
    for (auto& element : elements) {
        ready.Push(&element);
        expected.push_back(&element);
    }

    std::atomic<bool> canStart = false;
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&]() NO_INLINE {
            while (!canStart.load(std::memory_order_relaxed)) {
            }
            while (auto* element = ready.Pop()) {
                used.Push(element);
            }
        });
    }

    canStart.store(true, std::memory_order_relaxed);
    mutators.clear();

    std::vector<Element*> actual;
    while (auto* element = ready.Pop()) {
        actual.push_back(element);
    }
    while (auto* element = used.Pop()) {
        actual.push_back(element);
    }
    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}
