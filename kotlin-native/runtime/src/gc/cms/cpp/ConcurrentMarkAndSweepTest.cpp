/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMarkAndSweep.hpp"

#include <mutex>

#include "gtest/gtest.h"

#include "GCImpl.hpp"
#include "GlobalData.hpp"
#include "SafePoint.hpp"
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

TYPED_TEST_P(TracingGCTest, CMSMultipleMutatorsWeak) {
    std::vector<Mutator> mutators(kDefaultThreadCount);
    ObjHeader* globalRoot = nullptr;
    test_support::RegularWeakReferenceImpl* weak = nullptr;

    mutators[0]
            .Execute([&weak, &globalRoot](mm::ThreadData& threadData, Mutator& mutator) {
                auto& global = mutator.AddGlobalRoot();

                auto& object = AllocateObject(threadData);
                auto& objectWeak = ([&threadData, &object]() -> test_support::RegularWeakReferenceImpl& {
                    ObjHolder holder;
                    return InstallWeakReference(threadData, object.header(), holder.slot());
                })();
                global->field1 = objectWeak.header();
                weak = &objectWeak;
                globalRoot = global.header();
            })
            .wait();

    // Make sure all mutators are initialized.
    for (int i = 1; i < kDefaultThreadCount; ++i) {
        mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) {}).wait();
    }

    std::vector<std::future<void>> gcFutures;
    auto epoch = mm::GlobalData::Instance().gc().Schedule();
    std::atomic<bool> gcDone = false;

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {}

    for (auto& mutator : mutators) {
        gcFutures.emplace_back(mutator.Execute([&](mm::ThreadData& threadData, Mutator& mutator) noexcept {
            bool dead = false;
            while (!gcDone.load(std::memory_order_relaxed)) {
                mm::safePoint(threadData);
                auto weakReferee = weak->get();
                if (dead) {
                    EXPECT_THAT(weakReferee, nullptr);
                } else if (weakReferee == nullptr) {
                    dead = true;
                }
            }
            EXPECT_THAT(weak->get(), nullptr);
        }));
    }

    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
    gcDone.store(true, std::memory_order_relaxed);

    for (auto& future : gcFutures) {
        future.wait();
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAre(globalRoot, weak->header()));
    }
}

REGISTER_TYPED_TEST_SUITE_WITH_LISTS(TracingGCTest, TRACING_GC_TEST_LIST, CMSMultipleMutatorsWeak);
INSTANTIATE_TYPED_TEST_SUITE_P(CMS, TracingGCTest, ConcurrentMarkAndSweepTest);
