/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "GlobalData.hpp"
#include "GlobalsRegistry.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

namespace {

template <typename T>
bool isEmpty(T& iterable) {
    return iterable.begin() == iterable.end();
}

template <typename E, typename T>
std::vector<E> collect(T& iterable) {
    std::vector<E> result;
    for (E element : iterable) {
        result.push_back(element);
    }
    return std::move(result);
}

std::vector<mm::ThreadData*> collect(mm::ThreadRegistry::Iterable& iterable) {
    std::vector<mm::ThreadData*> result;
    for (mm::ThreadData& element : iterable) {
        result.push_back(&element);
    }
    // Do not use std::move because clang complains that it prevents copy elision.
    return result;
}

} // namespace

extern "C" void Kotlin_TestSupport_AssertClearGlobalState() {
    // Validate that global registries are empty.
    auto globals = mm::GlobalsRegistry::Instance().Iter();
    auto objects = mm::GlobalData::Instance().objectFactory().Iter();
    auto stableRefs = mm::StableRefRegistry::Instance().Iter();
    auto threads = mm::ThreadRegistry::Instance().Iter();

    EXPECT_THAT(collect<ObjHeader**>(globals), testing::UnorderedElementsAre());
    EXPECT_THAT(collect<mm::ObjectFactory<mm::GC>::NodeRef>(objects), testing::UnorderedElementsAre());
    EXPECT_THAT(collect<ObjHeader*>(stableRefs), testing::UnorderedElementsAre());
    EXPECT_THAT(collect(threads), testing::UnorderedElementsAre());
}

void kotlin::DeinitMemoryForTests(MemoryState* memoryState) {
    DeinitMemory(memoryState, false);
    mm::ThreadRegistry::TestSupport::ClearCurrentThreadData();
}