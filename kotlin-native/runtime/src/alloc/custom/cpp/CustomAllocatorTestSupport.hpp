/*
 * Copyright 2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "AllocatedSizeTracker.hpp"
#include "AllocationSize.hpp"
#include "CustomFinalizerProcessor.hpp"
#include "FixedBlockPage.hpp"
#include "SingleObjectPage.hpp"
#include "GCStatistics.hpp"

namespace kotlin::alloc::test_support {

class FakeObjectHeader {
public:
    static FakeObjectHeader* at(uint8_t* ptr) {
        return reinterpret_cast<FakeObjectHeader*>(ptr);
    }

    explicit FakeObjectHeader(size_t size) : size_(size) {}

    size_t getSize() const { return size_; }

    bool isMarked() const { return marked_; }
    void mark() { marked_ = true; }
    void unmark() {
        RuntimeAssert(marked_, "Expected the object %p to be marked", this);
        marked_ = false;
    }

private:
    bool marked_ = false;
    size_t size_;
};


struct FakeSweepTraits {
    struct FakeGCSweepScope {};
    using GCSweepScope = FakeGCSweepScope;

    static GCSweepScope currentGCSweepScope(gc::GCHandle&) noexcept { return FakeGCSweepScope{}; }

    static bool trySweepElement(uint8_t* element, FinalizerQueue&, GCSweepScope&) noexcept {
        auto* obj = FakeObjectHeader::at(element);
        if (obj->isMarked()) {
            obj->unmark();
            return false;
        }
        return true;
    }

    static AllocationSize elementSize(uint8_t* element) {
        return AllocationSize::bytesAtLeast(FakeObjectHeader::at(element)->getSize());
    }
};

class CustomAllocatorTest : public ::testing::Test {
public:
    auto& gcHandle() { return gcHandle_; }
    auto& sweepHandle() { return sweepHandle_; }
    auto& finalizerQueue() { return finalizerQueue_; }
private:
    gc::GCHandle gcHandle_ = gc::GCHandle::createFakeForTests();
    FakeSweepTraits::GCSweepScope sweepHandle_ = FakeSweepTraits::currentGCSweepScope(gcHandle_);
    kotlin::alloc::FinalizerQueue finalizerQueue_;
};

class WithSchedulerNotificationHook {
public:
    WithSchedulerNotificationHook();
    ~WithSchedulerNotificationHook();

    auto& hook() { return schedulerNotificationHook_; }
private:
    testing::StrictMock<testing::MockFunction<void(std::size_t)>> schedulerNotificationHook_;
};

constexpr auto kExtraObjCellSize = AllocationSize::bytesExactly(sizeof(kotlin::alloc::ExtraObjectCell));

ExtraObjectCell* initExtraObjectCell(uint8_t* ptr);
ExtraObjectCell* allocExtraObjectCell(kotlin::alloc::FixedBlockPage* page);
ExtraObjectCell* allocExtraObjectCell(kotlin::alloc::SingleObjectPage* page);

}
