/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <algorithm>
#include <cstdint>
#include <random>

#include "CustomAllocatorTestSupport.hpp"
#include "gtest/gtest.h"

#include "FixedBlockPage.hpp"
#include "GCApi.hpp"
#include "gc/GCTestSupport.hpp"
#include "Heap.hpp"
#include "SingleObjectPage.hpp"

using namespace kotlin::alloc::test_support;

namespace {

using Heap = typename kotlin::alloc::Heap;
using FixedBlockPage = typename kotlin::alloc::FixedBlockPage;
using NextFitPage = typename kotlin::alloc::NextFitPage;
using SingleObjectPage = typename kotlin::alloc::SingleObjectPage;

inline constexpr int MIN_BLOCK_SIZE = 2;

void mark(kotlin::alloc::CustomHeapObject& obj) {
    ASSERT_TRUE(kotlin::gc::test_support::tryMark(obj.object()));
}

kotlin::alloc::CustomHeapObject& installType(uint8_t* obj, TypeInfo* typeInfo) {
    auto descriptor = kotlin::alloc::CustomHeapObject::descriptorFrom(typeInfo);
    auto& heapObject = *descriptor.construct(obj);
    ObjHeader* object = heapObject.object();
    object->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    return heapObject;
}

TEST_F(CustomAllocatorTest, HeapReuseFixedBlockPages) {
    Heap heap;
    const int MIN = MIN_BLOCK_SIZE;
    const int MAX = FixedBlockPage::MAX_BLOCK_SIZE + 1;
    TypeInfo fakeTypes[MAX];
    for (int i = MIN; i < MAX; ++i) {
        fakeTypes[i].typeInfo_ = &fakeTypes[i];
        fakeTypes[i].instanceSize_ = 8 * (i - 1);
        fakeTypes[i].flags_ = 0;
    }
    FixedBlockPage* pages[MAX];
    for (int blocks = MIN; blocks < MAX; ++blocks) {
        pages[blocks] = heap.GetFixedBlockPage(blocks, finalizerQueue());
        auto& obj = installType(pages[blocks]->TryAllocate(), &fakeTypes[blocks]);
        mark(obj); // to make the page survive a sweep
    }
    heap.PrepareForGC();
    auto gcHandle = kotlin::gc::GCHandle::createFakeForTests();
    heap.Sweep(gcHandle);
    for (int blocks = MIN; blocks < MAX; ++blocks) {
        EXPECT_EQ(pages[blocks], heap.GetFixedBlockPage(blocks, finalizerQueue()));
    }
}

TEST_F(CustomAllocatorTest, HeapReuseNextFitPages) {
    Heap heap;
    const uint32_t BLOCKSIZE = FixedBlockPage::MAX_BLOCK_SIZE + 42;
    NextFitPage* page = heap.GetNextFitPage(BLOCKSIZE, finalizerQueue());
    TypeInfo fakeType{};
    fakeType.typeInfo_ = &fakeType;
    fakeType.instanceSize_ = 8 * (BLOCKSIZE - 1);
    fakeType.flags_ = 0;
    auto& obj = installType(page->TryAllocate(BLOCKSIZE), &fakeType);
    mark(obj); // to make the page survive a sweep
    heap.PrepareForGC();
    heap.Sweep(gcHandle());
    EXPECT_EQ(page, heap.GetNextFitPage(0, finalizerQueue()));
}

TEST_F(CustomAllocatorTest, TraverseAllocatedExtraObjects) {
    Heap heap;

    std::vector<kotlin::mm::ExtraObjectData*> allocatedExtraObjects;
    if (kotlin::compiler::pagedAllocator()) {
        FixedBlockPage* page = heap.GetFixedBlockExtraObjectPage(finalizerQueue());
        while (kotlin::alloc::ExtraObjectCell* cell = allocExtraObjectCell(page)) {
            allocatedExtraObjects.push_back(cell->Data());
        }
    } else {
        kotlin::alloc::ExtraObjectCell* inSingleObjPage = allocExtraObjectCell(heap.GetSingleExtraObjectPage());
        allocatedExtraObjects.push_back(inSingleObjPage->Data());
    }

    std::vector<kotlin::mm::ExtraObjectData*> foundExtraObjects;
    heap.TraverseAllocatedExtraObjects([&](kotlin::mm::ExtraObjectData* extraObject){
        foundExtraObjects.push_back(extraObject);
    });

    EXPECT_THAT(foundExtraObjects, testing::ElementsAreArray(allocatedExtraObjects));
}

} // namespace
