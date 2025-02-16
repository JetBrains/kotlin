/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <random>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "CustomAllocatorTestSupport.hpp"
#include "gtest/gtest.h"

#include "Cell.hpp"
#include "FixedBlockPage.hpp"

using namespace kotlin::alloc::test_support;

using testing::_;

namespace {

using FixedBlockPage = typename kotlin::alloc::FixedBlockPage;
using AllocationSize = typename kotlin::alloc::AllocationSize;

FakeObjectHeader* alloc(FixedBlockPage* page, size_t blockSizeCells) {
    auto blockSize = AllocationSize::cells(blockSizeCells);
    uint8_t* ptr = page->TryAllocate();
    if (ptr) {
        EXPECT_TRUE(ptr[0] == 0 && memcmp(ptr, ptr + 1, blockSize.inBytes() - 1) == 0);
        return new(ptr) FakeObjectHeader(blockSize.inBytes());
    }
    return nullptr;
}

} // namespace

TEST_F(CustomAllocatorTest, FixedBlockPageConsequtiveAlloc) {
    for (uint32_t size = 2; size <= FixedBlockPage::MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        FakeObjectHeader* prev = alloc(page, size);
        FakeObjectHeader* cur;
        while ((cur = alloc(page, size))) {
            uint64_t dist = abs(reinterpret_cast<uint8_t*>(cur) - reinterpret_cast<uint8_t*>(prev));
            EXPECT_EQ(dist, AllocationSize::cells(size).inBytes());
            prev = cur;
        }
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, FixedBlockPageSweepEmptyPage) {
    for (uint32_t size = 2; size <= FixedBlockPage::MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, FixedBlockPageSweepFullUnmarkedPage) {
    for (uint32_t size = 2; size <= FixedBlockPage::MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint32_t count = 0;
        while (alloc(page, size)) ++count;
        EXPECT_EQ(count, FixedBlockPage::cellCount() / size);
        EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, FixedBlockPageSweepSingleMarked) {
    for (uint32_t size = 2; size <= FixedBlockPage::MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        FakeObjectHeader* obj = alloc(page, size);
        obj->mark();
        EXPECT_TRUE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, FixedBlockPageSweepSingleReuse) {
    for (uint32_t size = 2; size <= FixedBlockPage::MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        FakeObjectHeader* obj = alloc(page, size);
        EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        EXPECT_EQ(alloc(page, size), obj);
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, FixedBlockPageSweepReuse) {
    for (uint32_t size = 2; size <= FixedBlockPage::MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        FakeObjectHeader* obj;
        for (int count = 0; (obj = alloc(page, size)); ++count) {
            if (count % 2 == 0) obj->mark();
        }
        EXPECT_TRUE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        uint32_t count = 0;
        for (; (obj = alloc(page, size)); ++count) {
            if (count % 2 == 0) obj->mark();
        }
        EXPECT_EQ(count, FixedBlockPage::cellCount() / size / 2);
        page->Destroy();
    }
}

TEST_F(CustomAllocatorTest, FixedBlockPageRandomExercise) {
    std::minstd_rand r(42);
    for (uint32_t size = 2; size <= FixedBlockPage::MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint32_t BLOCK_COUNT = FixedBlockPage::cellCount() / size;
        std::vector<FakeObjectHeader*> seen;
        while (FakeObjectHeader* obj = alloc(page, size)) seen.push_back(obj);
        EXPECT_EQ(seen.size(), BLOCK_COUNT);
        EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        std::unordered_set<FakeObjectHeader*> live;
        for (int gc = 0; gc < 10; gc++) {
            int createCount = r() % BLOCK_COUNT;
            while (createCount-- > 0) {
                FakeObjectHeader* obj = alloc(page, size);
                if (!obj) break;
                EXPECT_TRUE(live.insert(obj).second);
            }
            for (auto obj : seen) {
                if (live.find(obj) != live.end()) {
                    if (r() % 2) {
                        obj->mark();
                    } else {
                        live.erase(obj);
                    }
                }
            }
            EXPECT_EQ(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()), !live.empty());
            FakeObjectHeader* prev = nullptr;
            uint32_t allocCount = 0;
            for (auto* ptr : page->GetAllocatedBlocks()) {
                FakeObjectHeader* obj = FakeObjectHeader::at(ptr);
                EXPECT_LT(prev, obj);
                prev = obj;
                ++allocCount;
                EXPECT_NE(live.find(obj), live.end());
            }
            EXPECT_EQ(allocCount, live.size());
        }
        while (FakeObjectHeader* obj = alloc(page, size)) live.insert(obj);
        EXPECT_EQ(live.size(), BLOCK_COUNT);
        EXPECT_FALSE(page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue()));
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSchedulerNotification) {
    for (uint32_t size = 2; size <= FixedBlockPage::MAX_BLOCK_SIZE; ++size) {
        kotlin::alloc::test_support::WithSchedulerNotificationHook hookHandle;
        EXPECT_CALL(hookHandle.hook(), Call(_));

        FixedBlockPage* page = FixedBlockPage::Create(size);
        while (alloc(page, size)) {}
        page->Destroy();

        testing::Mock::VerifyAndClearExpectations(&hookHandle.hook());
    }
}
