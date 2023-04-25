/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>

#include "Cell.hpp"
#include "CustomAllocConstants.hpp"
#include "gtest/gtest.h"
#include "FixedBlockPage.hpp"
#include "TypeInfo.h"

namespace {

using FixedBlockPage = typename kotlin::alloc::FixedBlockPage;

TypeInfo fakeType = {.flags_ = 0}; // a type without a finalizer

void mark(void* obj) {
    reinterpret_cast<uint64_t*>(obj)[0] = 1;
}

uint8_t* alloc(FixedBlockPage* page, size_t blockSize) {
    uint8_t* ptr = page->TryAllocate();
    if (ptr) {
        memset(ptr, 0, 8 * blockSize);
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
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        EXPECT_FALSE(page->Sweep(gcScope));
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepFullUnmarkedPage) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint32_t count = 0;
        while (alloc(page, size)) ++count;
        EXPECT_EQ(count, FIXED_BLOCK_PAGE_CELL_COUNT / size);
        EXPECT_FALSE(page->Sweep(gcScope));
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepSingleMarked) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint8_t* ptr = alloc(page, size);
        mark(ptr);
        EXPECT_TRUE(page->Sweep(gcScope));
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepSingleReuse) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint8_t* ptr = alloc(page, size);
        EXPECT_FALSE(page->Sweep(gcScope));
        EXPECT_EQ(alloc(page, size), ptr);
        page->Destroy();
    }
}

TEST(CustomAllocTest, FixedBlockPageSweepReuse) {
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    for (uint32_t size = 2; size <= FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE; ++size) {
        FixedBlockPage* page = FixedBlockPage::Create(size);
        uint8_t* ptr;
        for (int count = 0; (ptr = alloc(page, size)); ++count) {
            if (count % 2 == 0) mark(ptr);
        }
        EXPECT_TRUE(page->Sweep(gcScope));
        uint32_t count = 0;
        for (; (ptr = alloc(page, size)); ++count) {
            if (count % 2 == 0) mark(ptr);
        }
        EXPECT_EQ(count, FIXED_BLOCK_PAGE_CELL_COUNT / size / 2);
        page->Destroy();
    }
}
} // namespace
