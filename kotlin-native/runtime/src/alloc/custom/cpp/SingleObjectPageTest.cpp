/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <random>

#include "CustomAllocatorTestSupport.hpp"
#include "gtest/gtest.h"

#include "ExtraObjectPage.hpp"
#include "NextFitPage.hpp"
#include "SingleObjectPage.hpp"
#include "TypeInfo.h"

using testing::_;

namespace {

using SingleObjectPage = typename kotlin::alloc::SingleObjectPage;

TypeInfo fakeType = {.typeInfo_ = &fakeType, .flags_ = 0}; // a type without a finalizer

#define MIN_BLOCK_SIZE kotlin::alloc::NextFitPage::cellCount()

void mark(void* obj) {
    reinterpret_cast<uint64_t*>(obj)[0] = 1;
}

SingleObjectPage* alloc(uint64_t blockSize) {
    SingleObjectPage* page = SingleObjectPage::Create(blockSize);
    uint8_t* ptr = page->Allocate();
    EXPECT_TRUE(ptr[0] == 0 && memcmp(ptr, ptr + 1, blockSize * 8 - 1) == 0);
    reinterpret_cast<uint64_t*>(ptr)[1] = reinterpret_cast<uint64_t>(&fakeType);
    return page;
}

TEST(CustomAllocTest, SingleObjectPageSweepEmptyPage) {
    SingleObjectPage* page = alloc(MIN_BLOCK_SIZE);
    EXPECT_TRUE(page);
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    EXPECT_FALSE(page->SweepAndDestroy(gcScope, finalizerQueue));
}

TEST(CustomAllocTest, SingleObjectPageSweepFullPage) {
    SingleObjectPage* page = alloc(MIN_BLOCK_SIZE);
    EXPECT_TRUE(page);
    EXPECT_TRUE(page->Data());
    mark(page->Data());
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    EXPECT_TRUE(page->SweepAndDestroy(gcScope, finalizerQueue));
}

TEST(CustomAllocTest, SingleObjectPageSchedulerNotification) {
    kotlin::alloc::test_support::WithSchedulerNotificationHook hookHandle;
    EXPECT_CALL(hookHandle.hook(), Call(_));
    alloc(MIN_BLOCK_SIZE);
    testing::Mock::VerifyAndClearExpectations(&hookHandle.hook());
}

#undef MIN_BLOCK_SIZE
} // namespace
