/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>
#include <random>

#include "CustomAllocator.hpp"
#include "Memory.h"
#include "gtest/gtest.h"
#include "Heap.hpp"
#include "TypeInfo.h"

namespace {

using Heap = typename kotlin::alloc::Heap;
using FixedBlockPage = typename kotlin::alloc::FixedBlockPage;
using NextFitPage = typename kotlin::alloc::NextFitPage;
using CustomAllocator = typename kotlin::alloc::CustomAllocator;

inline constexpr int MIN_BLOCK_SIZE = 2;

TEST(CustomAllocTest, SmallAllocNonNull) {
    const int N = 200;
    TypeInfo fakeTypes[N];
    for (int i = 1; i < N; ++i) {
        fakeTypes[i] = {.typeInfo_ = &fakeTypes[i], .instanceSize_ = 8 * i, .flags_ = 0};
    }
    Heap heap;
    CustomAllocator ca(heap);
    ObjHeader* obj[N];
    for (int i = 1; i < N; ++i) {
        TypeInfo* type = fakeTypes + i;
        obj[i] = ca.CreateObject(type);
        EXPECT_TRUE(obj[i]);
    }
}

TEST(CustomAllocTest, SmallAllocSameFixedBlockPage) {
    const int N = FixedBlockPage::cellCount() / FixedBlockPage::MAX_BLOCK_SIZE;
    for (int blocks = MIN_BLOCK_SIZE; blocks < FixedBlockPage::MAX_BLOCK_SIZE; ++blocks) {
        Heap heap;
        CustomAllocator ca(heap);
        TypeInfo fakeType = {.typeInfo_ = &fakeType, .instanceSize_ = 8 * blocks, .flags_ = 0};
        uint8_t* first = reinterpret_cast<uint8_t*>(ca.CreateObject(&fakeType));
        for (int i = 1; i < N; ++i) {
            uint8_t* obj = reinterpret_cast<uint8_t*>(ca.CreateObject(&fakeType));
            uint64_t dist = abs(obj - first);
            EXPECT_TRUE(dist < kotlin::alloc::FixedBlockPage::SIZE);
        }
    }
}

TEST(CustomAllocTest, FixedBlockPageThreshold) {
    std::list<TypeInfo> types;
    Heap heap;
    CustomAllocator ca(heap);
    const int FROM = FixedBlockPage::MAX_BLOCK_SIZE - 10;
    const int TO = FixedBlockPage::MAX_BLOCK_SIZE + 10;
    for (int blocks = FROM; blocks <= TO; ++blocks) {
        auto& type = types.emplace_back();
        type = {.typeInfo_ = &type, .instanceSize_ = 8 * blocks, .flags_ = 0};
        ca.CreateObject(&type);
    }
}

TEST(CustomAllocTest, NextFitPageThreshold) {
    std::list<TypeInfo> types;
    Heap heap;
    CustomAllocator ca(heap);
    const int MAX_BLOCK_SIZE = NextFitPage::maxBlockSize();
    const int FROM = MAX_BLOCK_SIZE - 10;
    const int TO = MAX_BLOCK_SIZE + 10;
    for (int blocks = FROM; blocks <= TO; ++blocks) {
        auto& type = types.emplace_back();
        type = {.typeInfo_ = &type, .instanceSize_ = 8 * blocks, .flags_ = 0};
        ca.CreateObject(&type);
    }
}

TEST(CustomAllocTest, TwoAllocatorsDifferentPages) {
    for (int blocks = MIN_BLOCK_SIZE; blocks < 2000; ++blocks) {
        Heap heap;
        CustomAllocator ca1(heap);
        CustomAllocator ca2(heap);
        TypeInfo fakeType = {.typeInfo_ = &fakeType, .instanceSize_ = 8 * blocks, .flags_ = 0};
        uint8_t* obj1 = reinterpret_cast<uint8_t*>(ca1.CreateObject(&fakeType));
        uint8_t* obj2 = reinterpret_cast<uint8_t*>(ca2.CreateObject(&fakeType));
        uint64_t dist = abs(obj2 - obj1);
        EXPECT_TRUE(dist >= kotlin::alloc::FixedBlockPage::SIZE);
    }
}

using Data = typename kotlin::mm::ExtraObjectData;

TEST(CustomAllocTest, AllocExtraObjectNonNullZeroed) {
    Heap heap;
    CustomAllocator ca(heap);
    for (int i = 1; i < 10; ++i) {
        uint8_t* obj = reinterpret_cast<uint8_t*>(ca.CreateExtraObject());
        EXPECT_TRUE(obj);
        for (size_t j = 0; j < sizeof(Data); ++j) {
            EXPECT_FALSE(obj[j]);
        }
    }
}

#undef MIN_BLOCK_SIZE
} // namespace
