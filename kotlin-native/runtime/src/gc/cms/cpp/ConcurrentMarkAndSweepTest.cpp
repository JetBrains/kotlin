/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMarkAndSweep.hpp"

#include <mutex>

#include "gtest/gtest.h"

#include "GCImpl.hpp"
#include "GlobalData.hpp"
#include "StableRef.hpp"
#include "TestSupport.hpp"
#include "TracingGCTest.hpp"

using namespace kotlin;

namespace {

class ConcurrentMarkAndSweepTest {
public:
    ~ConcurrentMarkAndSweepTest() {
        mm::GlobalsRegistry::Instance().ClearForTests();
        mm::SpecialRefRegistry::instance().clearForTests();
        mm::GlobalData::Instance().gc().ClearForTests();
        mm::GlobalData::Instance().allocator().clearForTests();
    }

    void SetUp() {}
};

} // namespace

REGISTER_TYPED_TEST_SUITE_WITH_LISTS(TracingGCTest, TRACING_GC_TEST_LIST);
INSTANTIATE_TYPED_TEST_SUITE_P(CMS, TracingGCTest, ConcurrentMarkAndSweepTest);
