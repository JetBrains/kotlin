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

template <typename T>
auto collectPointers(T& iterable) {
    std::vector<const std::remove_reference_t<decltype(*iterable.begin())>*> result;
    for (const auto& element : iterable) {
        result.push_back(&element);
    }
    return result;
}

} // namespace

void alloc::test_support::assertClear(Allocator& allocator) noexcept {
    auto objects = allocator.impl().objectFactory().LockForIter();
    auto extraObjects = allocator.impl().extraObjectDataFactory().LockForIter();
    EXPECT_THAT(collectCopy(objects), testing::UnorderedElementsAre());
    EXPECT_THAT(collectPointers(extraObjects), testing::UnorderedElementsAre());
}

std::vector<ObjHeader*> alloc::test_support::allocatedObjects(mm::ThreadData& threadData) noexcept {
    std::vector<ObjHeader*> objects;
    for (auto node : threadData.allocator().impl().objectFactoryThreadQueue()) {
        objects.push_back(node.GetObjHeader());
    }
    for (auto node : mm::GlobalData::Instance().allocator().impl().objectFactory().LockForIter()) {
        objects.push_back(node.GetObjHeader());
    }
    return objects;
}
