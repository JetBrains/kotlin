/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <random>

#include "CustomAllocConstants.hpp"
#include "ExtraObjectPage.hpp"
#include "gtest/gtest.h"
#include "SingleObjectPage.hpp"
#include "TypeInfo.h"

namespace {

using SingleObjectPage = typename kotlin::alloc::SingleObjectPage;

TypeInfo fakeType = {.typeInfo_ = &fakeType, .flags_ = 0}; // a type without a finalizer

#define MIN_BLOCK_SIZE NEXT_FIT_PAGE_CELL_COUNT

void mark(void* obj) {
    reinterpret_cast<uint64_t*>(obj)[0] = 1;
}

SingleObjectPage* alloc(uint64_t blockSize) {
    SingleObjectPage* page = SingleObjectPage::Create(blockSize);
    uint8_t* ptr = page->TryAllocate();
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
    EXPECT_FALSE(page->Sweep(gcScope, finalizerQueue));
    page->Destroy();
}

TEST(CustomAllocTest, SingleObjectPageSweepFullPage) {
    SingleObjectPage* page = alloc(MIN_BLOCK_SIZE);
    EXPECT_TRUE(page);
    EXPECT_TRUE(page->Data());
    mark(page->Data());
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    auto gcScope = gcHandle.sweep();
    kotlin::alloc::FinalizerQueue finalizerQueue;
    EXPECT_TRUE(page->Sweep(gcScope, finalizerQueue));
    page->Destroy();
}

#undef MIN_BLOCK_SIZE
} // namespace
