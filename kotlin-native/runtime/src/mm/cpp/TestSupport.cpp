/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "GC.hpp"
#include "GCTestSupport.hpp"
#include "GlobalData.hpp"
#include "GlobalsRegistry.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

namespace {

template <typename T>
bool isEmpty(T& iterable) {
    return iterable.begin() == iterable.end();
}

template <typename T>
auto collectCopy(T& iterable) {
    std::vector<std::remove_reference_t<decltype(*iterable.begin())>> result;
    for (const auto &element : iterable) {
        result.push_back(element);
    }
    return result;
}

template <typename T>
auto collectPointers(T& iterable) {
    std::vector<const std::remove_reference_t<decltype(*iterable.begin())>*> result;
    for (const auto &element : iterable) {
        result.push_back(&element);
    }
    return result;
}
} // namespace

extern "C" void Kotlin_TestSupport_AssertClearGlobalState() {
    // Validate that global registries are empty.
    auto globals = mm::GlobalsRegistry::Instance().LockForIter();
    auto extraObjects = mm::GlobalData::Instance().extraObjectDataFactory().LockForIter();
    auto specialRefs = mm::SpecialRefRegistry::instance().lockForIter();
    auto threads = mm::ThreadRegistry::Instance().LockForIter();

    EXPECT_THAT(collectCopy(globals), testing::UnorderedElementsAre());
    EXPECT_THAT(collectPointers(extraObjects), testing::UnorderedElementsAre());
    EXPECT_THAT(collectPointers(specialRefs), testing::UnorderedElementsAre());
    EXPECT_THAT(collectPointers(threads), testing::UnorderedElementsAre());
    gc::AssertClear(mm::GlobalData::Instance().gc());
}

void kotlin::DeinitMemoryForTests(MemoryState* memoryState) {
    DeinitMemory(memoryState, false);
    mm::ThreadRegistry::ClearCurrentThreadData();
}

std::ostream& kotlin::operator<<(std::ostream& stream, ThreadState state) {
    return stream << ThreadStateName(state);
}
