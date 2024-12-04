/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ParallelMarkConcurrentSweep.hpp"

#include <mutex>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "GCImpl.hpp"
#include "GlobalData.hpp"
#include "SafePoint.hpp"
#include "StableRef.hpp"
#include "TestSupport.hpp"
#include "TracingGCTest.hpp"

using namespace kotlin;

namespace {

template<std::size_t kMaxParallelism, bool kCooperativeMutators, std::size_t kAuxGCThreads>
class ParallelMarkConcurrentSweepTest {
public:
    ParallelMarkConcurrentSweepTest() {
        if (supportedConfiguration()) {
            mm::GlobalData::Instance().gc().impl().gc().reconfigure(kMaxParallelism, kCooperativeMutators, kAuxGCThreads);
        }
    }

    ~ParallelMarkConcurrentSweepTest() {
        mm::GlobalsRegistry::Instance().ClearForTests();
        mm::SpecialRefRegistry::instance().clearForTests();
        mm::GlobalData::Instance().gc().ClearForTests();
        mm::GlobalData::Instance().allocator().clearForTests();
    }

    void SetUp() {
        if (!supportedConfiguration()) {
            GTEST_SKIP() << "Unsupported parallelism configuration";
        }
    }

    static std::string getName() {
        using namespace std::string_literals;
        auto base = ""s;
        auto parallelism = std::to_string(kMaxParallelism) + "Parallel";
        auto withMutators = kCooperativeMutators ? "WithMutators" : "";
        auto withAux = kAuxGCThreads > 0 ? "WithGCThreads" : "";
        return base + parallelism + withMutators + withAux;
    }

private:
    bool supportedConfiguration() const {
        return !compiler::gcMarkSingleThreaded() || (!kCooperativeMutators && kAuxGCThreads == 0);
    }
};

} // namespace

using ParallelismConfigs = ::testing::Types<
        ParallelMarkConcurrentSweepTest<kDefaultThreadCount * 3, false, 0>,
        ParallelMarkConcurrentSweepTest<kDefaultThreadCount * 3, true, 0>
#if !__has_feature(thread_sanitizer) // TODO: Fix auxilary threads with tsan.
        , ParallelMarkConcurrentSweepTest<kDefaultThreadCount * 3, false, kDefaultThreadCount>,
        ParallelMarkConcurrentSweepTest<kDefaultThreadCount * 3, true, kDefaultThreadCount>,
        ParallelMarkConcurrentSweepTest<kDefaultThreadCount / 2, false, kDefaultThreadCount>,
        ParallelMarkConcurrentSweepTest<kDefaultThreadCount / 2 * 3, true, kDefaultThreadCount>
#endif
>;
struct NameGenerator {
    template <typename T>
    static std::string GetName(int) {
        return T::getName();
    }
};

REGISTER_TYPED_TEST_SUITE_WITH_LISTS(TracingGCTest, TRACING_GC_TEST_LIST);
INSTANTIATE_TYPED_TEST_SUITE_P(PMCS, TracingGCTest, ParallelismConfigs, NameGenerator);

REGISTER_TYPED_TEST_SUITE_WITH_LISTS(STWMarkGCTest, STW_MARK_GC_TEST_LIST);
INSTANTIATE_TYPED_TEST_SUITE_P(PMCS, STWMarkGCTest, ParallelismConfigs, NameGenerator);
