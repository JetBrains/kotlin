/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <random>

#include "Cell.hpp"
#include "CustomAllocConstants.hpp"
#include "ExtraObjectPage.hpp"
#include "gtest/gtest.h"
#include "NextFitPage.hpp"
#include "TypeInfo.h"

namespace {

using NextFitPage = typename kotlin::alloc::NextFitPage;
using Cell = typename kotlin::alloc::Cell;

TypeInfo fakeType = {.typeInfo_ = &fakeType, .flags_ = 0}; // a type without a finalizer

inline constexpr const size_t MIN_BLOCK_SIZE = FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE + 1;

void mark(void* obj) {
    reinterpret_cast<uint64_t*>(obj)[0] = 1;
}

uint8_t* alloc(NextFitPage* page, uint32_t blockSize) {
    uint8_t* ptr = page->TryAllocate(blockSize);
    if (!page->CheckInvariants()) {
        ADD_FAILURE();
        return nullptr;
    }
    if (ptr == nullptr) return nullptr;
    EXPECT_TRUE(ptr[0] == 0 && memcmp(ptr, ptr + 1, blockSize * 8 - 1) == 0);
    reinterpret_cast<uint64_t*>(ptr)[1] = reinterpret_cast<uint64_t>(&fakeType);
    if (!page->CheckInvariants()) {
        ADD_FAILURE();
        return nullptr;
    }
    return ptr;
}

TEST(CustomAllocTest, NextFitPageAlloc) {
    NextFitPage* page = NextFitPage::Create(MIN_BLOCK_SIZE);
    uint8_t* p1 = alloc(page, MIN_BLOCK_SIZE);
    uint8_t* p2 = alloc(page, MIN_BLOCK_SIZE);
    uint64_t dist = abs(p1 - p2);
    EXPECT_EQ(dist, (MIN_BLOCK_SIZE + 1) * sizeof(kotlin::alloc::Cell));
    page->Destroy();
}

TEST(CustomAllocTest, NextFitPageSweepEmptyPage) {
    NextFitPage* page = NextFitPage::Create(MIN_BLOCK_SIZE);
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
    page->Destroy();
}

TEST(CustomAllocTest, NextFitPageSweepFullUnmarkedPage) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (uint32_t seed = 0xC0FFEE0; seed <= 0xC0FFEEF; ++seed) {
        std::minstd_rand r(seed);
        NextFitPage* page = NextFitPage::Create(MIN_BLOCK_SIZE);
        while (alloc(page, MIN_BLOCK_SIZE + r() % 100)) {}
        EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
        page->Destroy();
    }
}

TEST(CustomAllocTest, NextFitPageSweepSingleMarked) {
    NextFitPage* page = NextFitPage::Create(MIN_BLOCK_SIZE);
    mark(alloc(page, MIN_BLOCK_SIZE));
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    EXPECT_TRUE(page->Sweep(gcScope, finalizerQueue));
    page->Destroy();
}

TEST(CustomAllocTest, NextFitPageSweepSingleReuse) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (uint32_t seed = 0xC0FFEE0; seed <= 0xC0FFEEF; ++seed) {
        std::minstd_rand r(seed);
        NextFitPage* page = NextFitPage::Create(MIN_BLOCK_SIZE);
        int count1 = 0;
        while (alloc(page, MIN_BLOCK_SIZE + r() % 100)) ++count1;
        EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
        r.seed(seed);
        int count2 = 0;
        while (alloc(page, MIN_BLOCK_SIZE + r() % 100)) ++count2;
        EXPECT_EQ(count1, count2);
        page->Destroy();
    }
}

TEST(CustomAllocTest, NextFitPageSweepReuse) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (uint32_t seed = 0xC0FFEE0; seed <= 0xC0FFEEF; ++seed) {
        std::minstd_rand r(seed);
        NextFitPage* page = NextFitPage::Create(MIN_BLOCK_SIZE);
        int unmarked = 0;
        while (uint8_t* ptr = alloc(page, MIN_BLOCK_SIZE)) {
            if (r() & 1) {
                mark(ptr);
            } else {
                ++unmarked;
            }
        }
        page->Sweep(gcScope, finalizerQueue);
        int freed = 0;
        while (alloc(page, MIN_BLOCK_SIZE)) ++freed;
        EXPECT_EQ(freed, unmarked);
        page->Destroy();
    }
}

TEST(CustomAllocTest, NextFitPageSweepCoallesce) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    NextFitPage* page = NextFitPage::Create(MIN_BLOCK_SIZE);
    EXPECT_TRUE(alloc(page, (NEXT_FIT_PAGE_CELL_COUNT-1) / 2 - 1));
    EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
    EXPECT_TRUE(alloc(page, (NEXT_FIT_PAGE_CELL_COUNT-1) - 1));
    page->Destroy();
}

#undef MIN_BLOCK_SIZE
} // namespace
