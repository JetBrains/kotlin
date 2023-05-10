/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <random>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "Cell.hpp"
#include "CustomAllocConstants.hpp"
#include "ExtraObjectPage.hpp"
#include "gtest/gtest.h"
#include "FixedBlockPage.hpp"
#include "TypeInfo.h"

namespace {

using FixedBlockPage = typename kotlin::alloc::FixedBlockPage;

TypeInfo fakeType = {.typeInfo_ = &fakeType, .flags_ = 0}; // a type without a finalizer

void mark(void* obj) {
    reinterpret_cast<uint64_t*>(obj)[0] = 1;
}

uint8_t* alloc(FixedBlockPage* page, size_t blockSize) {
    uint8_t* ptr = page->TryAllocate();
    if (ptr) {
        EXPECT_TRUE(ptr[0] == 0 && memcmp(ptr, ptr + 1, blockSize * 8 - 1) == 0);
        reinterpret_cast<uint64_t*>(ptr)[1] = reinterpret_cast<uint64_t>(&fakeType);
    }
    return ptr;
}

TEST(CustomAllocTest, FixedBlockPageConsequtiveAlloc) {
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint8_t* prev = alloc(page, size);
        uint8_t* cur;
        while ((cur = alloc(page, size))) {
            EXPECT_EQ(prev + sizeof(kotlin::alloc::Cell) * size, cur);
            prev = cur;
        }
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepEmptyPage) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepFullUnmarkedPage) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint32_t count = 0;
        while (alloc(page, size)) ++count;
        EXPECT_EQ(count, FIXED_BLOCK_PAGE_CELL_COUNT / size);
        EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepSingleMarked) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint8_t* ptr = alloc(page, size);
        mark(ptr);
        EXPECT_TRUE(page->Sweep(gcScope, finalizerQueue));
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepSingleReuse) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint8_t* ptr = alloc(page, size);
        EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
        EXPECT_EQ(alloc(page, size), ptr);
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepReuse) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint8_t* ptr;
        for (int count = 0; (ptr = alloc(page, size)); ++count) {
            if (count % 2 == 0) mark(ptr);
        }
        EXPECT_TRUE(page->Sweep(gcScope, finalizerQueue));
        uint32_t count = 0;
        for (; (ptr = alloc(page, size)); ++count) {
            if (count % 2 == 0) mark(ptr);
        }
        EXPECT_EQ(count, FIXED_BLOCK_PAGE_CELL_COUNT / size / 2);
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageRandomExercise) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    std::minstd_rand r(42);
    uint8_t* ptr;
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint32_t BLOCK_COUNT = FIXED_BLOCK_PAGE_CELL_COUNT / size;
        std::vector<uint8_t*> seen;
        while ((ptr = alloc(page, size))) seen.push_back(ptr);
        EXPECT_EQ(seen.size(), BLOCK_COUNT);
        EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
        std::unordered_set<uint8_t*> live;
        for (int gc = 0; gc < 10; gc++) {
            int createCount = r() % BLOCK_COUNT;
            while (createCount-- > 0) {
                ptr = alloc(page, size);
                if (!ptr) break;
                EXPECT_TRUE(live.insert(ptr).second);
            }
            for (auto obj : seen) {
                if (live.find(obj) != live.end()) {
                    if (r() % 2) {
                        mark(obj);
                    } else {
                        live.erase(obj);
                    }
                }
            }
            EXPECT_EQ(page->Sweep(gcScope, finalizerQueue), !live.empty());
        }
        while ((ptr = alloc(page, size))) live.insert(ptr);
        EXPECT_EQ(live.size(), BLOCK_COUNT);
        EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
        page->Destroy();
    }
}

} // namespace
