/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <algorithm>
#include <cstdint>
#include <random>

#include "LargePage.hpp"
#include "gtest/gtest.h"
#include "Heap.hpp"
#include "SmallPage.hpp"

namespace {

using Heap = typename kotlin::alloc::Heap;
using SmallPage = typename kotlin::alloc::SmallPage;
using MediumPage = typename kotlin::alloc::MediumPage;
using LargePage = typename kotlin::alloc::LargePage;

inline constexpr int MIN_BLOCK_SIZE = 2;

void mark(void* obj) {
    reinterpret_cast<uint64_t*>(obj)[0] = 1;
}

TEST(CustomAllocTest, HeapReuseSmallPages) {
    Heap heap;
    const int MIN = MIN_BLOCK_SIZE;
    const int MAX = SMALL_PAGE_MAX_BLOCK_SIZE + 1;
    SmallPage* pages[MAX];
    for (int blocks = MIN; blocks < MAX; ++blocks) {
        pages[blocks] = heap.GetSmallPage(blocks);
        void* obj = pages[blocks]->TryAllocate();
        mark(obj); // to make the page survive a sweep
    }
    heap.PrepareForGC();
    heap.Sweep();
    for (int blocks = MIN; blocks < MAX; ++blocks) {
        EXPECT_EQ(pages[blocks], heap.GetSmallPage(blocks));
    }
}

TEST(CustomAllocTest, HeapReuseMediumPages) {
    Heap heap;
    const uint32_t BLOCKSIZE = SMALL_PAGE_MAX_BLOCK_SIZE + 42;
    MediumPage* page = heap.GetMediumPage(BLOCKSIZE);
    void* obj = page->TryAllocate(BLOCKSIZE);
    mark(obj); // to make the page survive a sweep
    heap.PrepareForGC();
    heap.Sweep();
    EXPECT_EQ(page, heap.GetMediumPage(BLOCKSIZE));
}

} // namespace
