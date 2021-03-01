/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "GlobalData.hpp"
#include "GlobalsRegistry.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

namespace {

template <typename T>
bool isEmpty(T& iterable) {
    return iterable.begin() == iterable.end();
}

} // namespace

extern "C" void Kotlin_TestSupport_AssertClearGlobalState() {
    // Validate that global registries are empty.
    auto globals = mm::GlobalsRegistry::Instance().Iter();
    auto objects = mm::GlobalData::Instance().objectFactory().Iter();
    auto stableRefs = mm::StableRefRegistry::Instance().Iter();
    auto threads = mm::ThreadRegistry::Instance().Iter();

    // TODO: Improve error reporting.
    EXPECT_TRUE(isEmpty(globals));
    EXPECT_TRUE(isEmpty(objects));
    EXPECT_TRUE(isEmpty(stableRefs));
    EXPECT_TRUE(isEmpty(threads));
}