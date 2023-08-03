/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include <list>

#include "IntrusiveList.hpp"

#include "ParallelProcessor.hpp"

#include "std_support/Vector.hpp"
#include "SingleThreadExecutor.hpp"
#include "TestSupport.hpp"

using ::testing::_;
using namespace kotlin;

namespace {

class Element {
public:
    Element() : Element(0) {}
    explicit Element(int value) : a(value), b(value), c(value), d(value) {}

    bool isValid() const {
        return a == b && b == c && c == d;
    }

private:
    std::size_t a;
    std::size_t b;
    std::size_t c;
    std::size_t d;
};

} // namespace


TEST(BoundedQueueTest, ConcurrentEnqueue) {
    constexpr auto kThreadCount = 16;
    constexpr auto kElemsPerThread = 1024;
    BoundedQueue<Element, kThreadCount * kElemsPerThread> queue;

    std::atomic<bool> start = false;
    std::list<ScopedThread> threads;
    for (int t = 0; t < kThreadCount; ++t) {
        threads.emplace_back([&, t]() {
            while (!start) {
                std::this_thread::yield();
            }
            for (int e = 0; e < kElemsPerThread; ++e) {
                queue.enqueue(Element(t + e));
            }
        });
    }
    start = true;

    threads.clear();

    while (auto elem = queue.dequeue()) {
        EXPECT_TRUE(elem->isValid());
    }
}

TEST(BoundedQueueTest, ConcurrentDequeue) {
    constexpr auto kThreadCount = 16;
    constexpr auto kElemsPerThread = 1024;
    BoundedQueue<Element, kThreadCount * kElemsPerThread> queue;

    for (int i = 0; i < kThreadCount * kElemsPerThread; ++i) {
        queue.enqueue(Element(i));
    }

    std::atomic<bool> start = false;
    std::list<ScopedThread> threads;
    for (int t = 0; t < kThreadCount; ++t) {
        threads.emplace_back([&]() {
            while (!start) {
                std::this_thread::yield();
            }
            while (auto elem = queue.dequeue()) {
                EXPECT_TRUE(elem->isValid());
            }
        });
    }
    start = true;
}

TEST(BoundedQueueTest, PingPongWithOverflow) {
    constexpr auto kElemsPerThread = 1024;
    BoundedQueue<Element, kElemsPerThread / 2> queue;

    std::atomic<bool> start = false;
    std::list<ScopedThread> writers;
    for (std::size_t t = 0; t < kDefaultThreadCount; ++t) {
        writers.emplace_back([&]() {
            while (!start) {
                std::this_thread::yield();
            }
            for (int i = 0; i < kElemsPerThread; ++i) {
                while (!queue.enqueue(Element(i))) {
                    std::this_thread::yield();
                }
            }
        });
    }

    std::atomic<bool> allWritten = false;
    std::list<ScopedThread> readers;
    for (std::size_t t = 0; t < kDefaultThreadCount; ++t) {
        readers.emplace_back([&]() {
            while (!start) {
                std::this_thread::yield();
            }
            while (!allWritten) {
                while (auto elem = queue.dequeue()) {
                    EXPECT_TRUE(elem->isValid());
                }
            }
        });
    }

    start = true;
    writers.clear();
    allWritten = true;
}
