/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "TestSupport.hpp"
#include "CooperativeWorkLists.hpp"

#include "std_support/Deque.hpp"
#include "std_support/Vector.hpp"
#include "std_support/List.hpp"

using ::testing::_;
using namespace kotlin;

namespace {

struct Task {
    template<typename WorkList>
    static void workLoop(WorkList& workList) {
        while (Task* task = workList.tryPop()) {
            RuntimeAssert(!task->done_.load(), "Tasks are not idempotent");
            task->done_ = true;
        }
    }

    Task* next() const noexcept { return next_; }
    void setNext(Task* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        next_ = next;
    }
    bool trySetNext(Task* next) noexcept {
        RuntimeAssert(next, "next cannot be nullptr");
        if (next_ == nullptr) {
            next_ = next;
            return true;
        }
        return false;
    }

    std::atomic<bool> done_ = false;
    Task* next_ = nullptr;
};

auto workBatch(std::size_t size) {
    std::list<Task> batch;
    for (size_t i = 0; i < size; ++i) {
        batch.emplace_back();
    }
    return batch;
}

template <typename WorkList, typename Iterable>
void offerWork(WorkList& wl, Iterable& batch) {
    for (auto& task: batch) {
        bool accepted = wl.tryPush(task);
        RuntimeAssert(accepted, "Must be accepted");
    }
}

using ListImpl = intrusive_forward_list<Task>;

}

// SharableQueuePerWorker

struct SharableQueuePerWorkerTest : public testing::Test {

    static constexpr auto kMinSizeToShare = 256;

    struct FakeParallelProcessor;

    using WorkList = SharableQueuePerWorker<FakeParallelProcessor, ListImpl, kMinSizeToShare, kMinSizeToShare / 2, kotlin::internal::ShareOn::kPop>;

    struct FakeParallelProcessor : public std::list<typename WorkList::LocalQueue> {
        auto& workerQueues() {
            return *this;
        }

        void onShare(std::size_t) {}
    };
};

TEST_F(SharableQueuePerWorkerTest, UncontendedStealing) {
    FakeParallelProcessor processor;
    WorkList::CommonStorage common(processor);

    auto& wl1 = processor.emplace_back(common);
    auto& wl2 = processor.emplace_back(common);

    auto work = workBatch(kMinSizeToShare * 2);
    offerWork(wl1, work);

    EXPECT_TRUE(wl2.empty());

    // trigger sharing
    EXPECT_NE(wl1.tryPop(), nullptr);

    EXPECT_TRUE(wl2.tryAcquireWork());

    EXPECT_FALSE(wl2.empty());
}

TEST_F(SharableQueuePerWorkerTest, ShareEventually) {
    FakeParallelProcessor processor;
    WorkList::CommonStorage common(processor);

    auto& wl1 = processor.emplace_back(common);
    auto& wl2 = processor.emplace_back(common);

    auto work = workBatch(kMinSizeToShare * 2);
    offerWork(wl1, work);

    EXPECT_FALSE(wl2.tryAcquireWork());

    Task::workLoop(wl1);

    EXPECT_TRUE(wl2.tryAcquireWork());
}
