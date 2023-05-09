/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <algorithm>
#include <cstdint>
#include <random>

#include "ExtraObjectPage.hpp"
#include "SingleObjectPage.hpp"
#include "gtest/gtest.h"
#include "Heap.hpp"
#include "FixedBlockPage.hpp"

namespace {

using Heap = typename kotlin::alloc::Heap;
using FixedBlockPage = typename kotlin::alloc::FixedBlockPage;
using NextFitPage = typename kotlin::alloc::NextFitPage;
using SingleObjectPage = typename kotlin::alloc::SingleObjectPage;

inline constexpr int MIN_BLOCK_SIZE = 2;

void mark(void* obj) {
    reinterpret_cast<uint64_t*>(obj)[0] = 1;
}

TEST(CustomAllocTest, HeapReuseFixedBlockPages) {
    Heap heap;
    const int MIN = MIN_BLOCK_SIZE;
    const int MAX = FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE + 1;
    FixedBlockPage* pages[MAX];
    kotlin::alloc::FinalizerQueue finalizerQueue;
    for (int blocks = MIN; blocks < MAX; ++blocks) {
        pages[blocks] = heap.GetFixedBlockPage(blocks, finalizerQueue);
        void* obj = pages[blocks]->TryAllocate();
        mark(obj); // to make the page survive a sweep
    }
    heap.PrepareForGC();
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    heap.Sweep(gcHandle);
    for (int blocks = MIN; blocks < MAX; ++blocks) {
        EXPECT_EQ(pages[blocks], heap.GetFixedBlockPage(blocks, finalizerQueue));
    }
}

TEST(CustomAllocTest, HeapReuseNextFitPages) {
    Heap heap;
    const uint32_t BLOCKSIZE = FIXED_BLOCK_PAGE_MAX_BLOCK_SIZE + 42;
    kotlin::alloc::FinalizerQueue finalizerQueue;
    NextFitPage* page = heap.GetNextFitPage(BLOCKSIZE, finalizerQueue);
    void* obj = page->TryAllocate(BLOCKSIZE);
    mark(obj); // to make the page survive a sweep
    heap.PrepareForGC();
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    heap.Sweep(gcHandle);
    EXPECT_EQ(page, heap.GetNextFitPage(0, finalizerQueue));
}

} // namespace
