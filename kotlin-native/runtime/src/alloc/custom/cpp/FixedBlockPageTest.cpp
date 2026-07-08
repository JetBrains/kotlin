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

// Regression for the generational remembered-set drain (gms Finding #4). The drain resolves a
// recorded slot back to its containing object via FixedBlockPage::objectContainingInteriorPointer; a
// slot can outlive its object (the object was freed since the slot was recorded), so the resolver
// MUST reject a pointer into a free cell. Without the free-list guard it would hand back the freed
// cell as a "container", and the drain would then read that cell's first word -- a FixedCellRange
// free-list link whose low bits can spoof isOld() -- and dereference the stale slot. Every allocated
// block must resolve to a stable non-null object (identical for any interior byte); every freed block
// and every out-of-range pointer must resolve to nullptr.
TEST_F(CustomAllocatorTest, FixedBlockPageInteriorPointerRejectsFreeCells) {
    for (uint32_t size = 2; size <= 6; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        const size_t blockBytes = AllocationSize::cells(size).inBytes();

        std::vector<uint8_t*> blocks;
        while (FakeObjectHeader* obj = alloc(page, size)) blocks.push_back(reinterpret_cast<uint8_t*>(obj));
        ASSERT_GT(blocks.size(), 2u);

        // Every allocated block resolves to a non-null object, and every interior byte of a block
        // resolves to the same object as its first byte.
        for (uint8_t* block : blocks) {
            ObjHeader* fromStart = page->objectContainingInteriorPointer(block);
            EXPECT_NE(fromStart, nullptr) << "size=" << size;
            EXPECT_EQ(page->objectContainingInteriorPointer(block + blockBytes - 1), fromStart) << "size=" << size;
        }

        // Keep the even blocks, free the odd ones.
        for (size_t i = 0; i < blocks.size(); i += 2) FakeObjectHeader::at(blocks[i])->mark();
        page->Sweep<FakeSweepTraits>(sweepHandle(), finalizerQueue());

        // Live blocks still resolve; freed blocks must resolve to nullptr -- this is the free-cell guard.
        for (size_t i = 0; i < blocks.size(); ++i) {
            ObjHeader* resolved = page->objectContainingInteriorPointer(blocks[i]);
            if (i % 2 == 0) {
                EXPECT_NE(resolved, nullptr) << "size=" << size << " live block " << i << " lost";
            } else {
                EXPECT_EQ(resolved, nullptr) << "size=" << size << " freed block " << i << " spoofed a container";
            }
        }

        // Pointers outside the page's cell range resolve to nullptr (below and at/above the limit).
        EXPECT_EQ(page->objectContainingInteriorPointer(blocks.front() - blockBytes), nullptr) << "size=" << size;
        EXPECT_EQ(page->objectContainingInteriorPointer(blocks.back() + blockBytes), nullptr) << "size=" << size;

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
