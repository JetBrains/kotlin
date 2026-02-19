/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <random>

#include "CustomAllocatorTestSupport.hpp"
#include "gtest/gtest.h"

#include "NextFitPage.hpp"
#include "SingleObjectPage.hpp"
#include "TypeInfo.h"
#include "GCApi.hpp"

using namespace kotlin::alloc::test_support;

using testing::_;

namespace {

constexpr auto kMinBlockSize = kotlin::alloc::NextFitPage::cellCount(); // FIXME??

auto alloc(uint64_t blockSize) {
    auto* page = kotlin::alloc::SingleObjectPage::Create(blockSize);
    uint8_t* ptr = page->Allocate();
    EXPECT_TRUE(ptr[0] == 0 && memcmp(ptr, ptr + 1, blockSize * 8 - 1) == 0);
    auto objSize = kotlin::alloc::AllocationSize::cells(blockSize);
    FakeObjectHeader* obj = new(ptr) FakeObjectHeader(objSize.inBytes());
    return std::make_pair(page, obj);
}

}

TEST_F(CustomAllocatorTest, SingleObjectPageSweepEmptyPage) {
    auto [page, obj] = alloc(kMinBlockSize);
    EXPECT_FALSE(page->SweepAndDestroy<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
}

TEST_F(CustomAllocatorTest, SingleObjectPageSweepFullPage) {
    auto [page, obj] = alloc(kMinBlockSize);
    obj->mark();
    EXPECT_TRUE(page->SweepAndDestroy<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
}

TEST(CustomAllocTest, SingleObjectPageSchedulerNotification) {
    kotlin::alloc::test_support::WithSchedulerNotificationHook hookHandle;
    EXPECT_CALL(hookHandle.hook(), Call(_));
    alloc(kMinBlockSize);
    testing::Mock::VerifyAndClearExpectations(&hookHandle.hook());
}
