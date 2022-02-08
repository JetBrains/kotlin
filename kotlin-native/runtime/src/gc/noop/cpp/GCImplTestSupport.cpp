/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCTestSupport.hpp"

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "GCImpl.hpp"

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

void gc::AssertClear(GC& gc) noexcept {
    auto objects = gc.impl().objectFactory().LockForIter();
    EXPECT_THAT(collectCopy(objects), testing::UnorderedElementsAre());
}
