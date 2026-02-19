/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <random>

#include "CustomAllocatorTestSupport.hpp"
#include "gtest/gtest.h"

#include "Cell.hpp"
#include "FixedBlockPage.hpp"
#include "NextFitPage.hpp"
#include "TypeInfo.h"
#include "AllocationSize.hpp"

using namespace kotlin::alloc::test_support;

using testing::_;

namespace {

using NextFitPage = typename kotlin::alloc::NextFitPage;
using FixedBlockPage = typename kotlin::alloc::FixedBlockPage;
using Cell = typename kotlin::alloc::Cell;

constexpr size_t kMinBlockSize = FixedBlockPage::MAX_BLOCK_SIZE + 1;

FakeObjectHeader* alloc(NextFitPage* page, uint32_t blockSizeCells) {
    auto blockSize = kotlin::alloc::AllocationSize::cells(blockSizeCells);
    uint8_t* ptr = page->TryAllocate(blockSizeCells);
    if (!page->CheckInvariants()) {
        ADD_FAILURE();
        return nullptr;
    }
    if (ptr == nullptr) return nullptr;
    EXPECT_TRUE(ptr[0] == 0 && memcmp(ptr, ptr + 1, blockSize.inBytes() - 1) == 0);
    auto* obj = new(ptr) FakeObjectHeader(blockSize.inBytes());
    if (!page->CheckInvariants()) {
        ADD_FAILURE();
        return nullptr;
    }
    return obj;
}

} // namespace

TEST_F(CustomAllocatorTest, NextFitPageAlloc) {
    NextFitPage* page = NextFitPage::Create(kMinBlockSize);
    FakeObjectHeader* p1 = alloc(page, kMinBlockSize);
    FakeObjectHeader* p2 = alloc(page, kMinBlockSize);
    uint64_t dist = abs(reinterpret_cast<uint8_t*>(p1) - reinterpret_cast<uint8_t*>(p2));
    EXPECT_EQ(dist, kotlin::alloc::AllocationSize::cells(kMinBlockSize + 1).inBytes());
    page->Destroy();
}

TEST_F(CustomAllocatorTest, NextFitPageSweepEmptyPage) {
    NextFitPage* page = NextFitPage::Create(kMinBlockSize);
    EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
    page->Destroy();
}

TEST_F(CustomAllocatorTest, NextFitPageSweepFullUnmarkedPage) {
    for (uint32_t seed = 0xC0FFEE0; seed <= 0xC0FFEEF; ++seed) {
        std::minstd_rand r(seed);
        NextFitPage* page = NextFitPage::Create(kMinBlockSize);
        while (alloc(page, kMinBlockSize + r() % 100)) {}
        EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, NextFitPageSweepSingleMarked) {
    NextFitPage* page = NextFitPage::Create(kMinBlockSize);
    alloc(page, kMinBlockSize)->mark();
    EXPECT_TRUE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
    page->Destroy();
}

TEST_F(CustomAllocatorTest, NextFitPageSweepSingleReuse) {
    for (uint32_t seed = 0xC0FFEE0; seed <= 0xC0FFEEF; ++seed) {
        std::minstd_rand r(seed);
        NextFitPage* page = NextFitPage::Create(kMinBlockSize);
        int count1 = 0;
        while (alloc(page, kMinBlockSize + r() % 100)) ++count1;
        EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        r.seed(seed);
        int count2 = 0;
        while (alloc(page, kMinBlockSize + r() % 100)) ++count2;
        EXPECT_EQ(count1, count2);
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, NextFitPageSweepReuse) {
    for (uint32_t seed = 0xC0FFEE0; seed <= 0xC0FFEEF; ++seed) {
        std::minstd_rand r(seed);
        NextFitPage* page = NextFitPage::Create(kMinBlockSize);
        int unmarked = 0;
        while (FakeObjectHeader* obj = alloc(page, kMinBlockSize)) {
            if (r() & 1) {
                obj->mark();
            } else {
                ++unmarked;
            }
        }
        page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue());
        int freed = 0;
        while (alloc(page, kMinBlockSize)) ++freed;
        EXPECT_EQ(freed, unmarked);
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, NextFitPageSweepCoallesce) {
    NextFitPage* page = NextFitPage::Create(kMinBlockSize);
    EXPECT_TRUE(alloc(page, (NextFitPage::cellCount() - 1) / 2 - 1));
    EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
    EXPECT_TRUE(alloc(page, (NextFitPage::cellCount() - 1) - 1));
    page->Destroy();
}


TEST(CustomAllocTest, NextFitPageSchedulerNotification) {
    kotlin::alloc::test_support::WithSchedulerNotificationHook hookHandle;
    EXPECT_CALL(hookHandle.hook(), Call(_));

    NextFitPage * page = NextFitPage ::Create(kMinBlockSize);
    while (alloc(page, kMinBlockSize)) {}
    page->Destroy();

    testing::Mock::VerifyAndClearExpectations(&hookHandle.hook());
}
