/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AllocatorTestSupport.hpp"

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "AllocatorImpl.hpp"

using namespace kotlin;

namespace {

template <typename T>
auto collectCopy(T& iterable) {
    std::vector<std::remove_reference_t<decltype(*iterable.begin())>> result;
    for (const auto& element : iterable) {
        result.push_back(element);
    }
    return result;
}

} // namespace

void alloc::test_support::assertClear(Allocator& allocator) noexcept {
    auto objects = allocator.impl().heap().GetAllocatedObjects();
    EXPECT_THAT(collectCopy(objects), testing::UnorderedElementsAre());
}

std::vector<ObjHeader*> alloc::test_support::allocatedObjects(mm::ThreadData& threadData) noexcept {
    return threadData.allocator().impl().alloc().heap().GetAllocatedObjects();
}

void alloc::test_support::detachAndDestroyExtraObjectData(mm::ExtraObjectData& extraObject) noexcept {
    extraObject.ClearRegularWeakReferenceImpl();
    extraObject.UnlinkFromBaseObject();
    destroyExtraObjectData(extraObject);
    extraObject.setFlag(mm::ExtraObjectData::FLAGS_SWEEPABLE);
}