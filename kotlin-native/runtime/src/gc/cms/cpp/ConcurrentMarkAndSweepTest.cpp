/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ConcurrentMarkAndSweep.hpp"

#include <mutex>
#include <atomic>

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

TYPED_TEST_P(TracingGCTest, WeakResurrectionAtMarkTermination) {
    std::vector<Mutator> mutators(kDefaultThreadCount);
    std::vector<test_support::RegularWeakReferenceImpl*> weaks(kDefaultThreadCount);
    std::vector<test_support::Object<Payload>*> roots(kDefaultThreadCount);

    // initialize threads
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([&, i](mm::ThreadData& threadData, Mutator& mutator) {
                    roots[i] = &AllocateObject(threadData);
                    mutator.AddGlobalRoot(roots[i]->header());

                    auto& weakReferee = AllocateObject(threadData);
                    auto& weakRef = [&threadData, &weakReferee]() -> test_support::RegularWeakReferenceImpl& {
                        ObjHolder holder;
                        return test_support::InstallWeakReference(threadData, weakReferee.header(), holder.slot());
                    }();
                    EXPECT_NE(weakRef.get(), nullptr);
                    weaks[i] = &weakRef;
                    mutator.AddGlobalRoot(weakRef.header());
                })
                .wait();
    }

    auto epoch = mm::GlobalData::Instance().gc().Schedule();
    std::atomic gcDone = false;

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    std::vector<std::future<void>> mutatorFutures;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutatorFutures.emplace_back(mutators[i].Execute([&, i](mm::ThreadData& threadData, Mutator& mutator) noexcept {
            while (!gc::mark::test_support::flushActionRequested() && !gcDone.load(std::memory_order_relaxed)) {
                safePoint(threadData);
            }

            threadData.gc().impl().gc().mark().onSafePoint();

            auto weakReferee = weaks[i]->get();
            (*roots[i])->field2 = weakReferee;
            bool resurrected = weakReferee != nullptr;

            while (!gcDone.load(std::memory_order_relaxed)) {
                safePoint(threadData);
            }

            if (resurrected) {
                EXPECT_NE(weaks[i]->get(), nullptr);
            } else {
                EXPECT_EQ(weaks[i]->get(), nullptr);
            }
        }));
    }

    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
    gcDone = true;

    for (auto& future : mutatorFutures) {
        future.wait();
    }

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([&, i](mm::ThreadData&, Mutator&) noexcept {
                    if (auto weakReferee = weaks[i]->get()) {
                        auto& extraObj = *mm::ExtraObjectData::Get(weakReferee);
                        extraObj.ClearRegularWeakReferenceImpl();
                        extraObj.Uninstall();
                        alloc::destroyExtraObjectData(extraObj);
                    }
                })
                .wait();
    }
}

TYPED_TEST_P(TracingGCTest, ReleaseStableRefDuringRSCollection) {
    std::vector<Mutator> mutators(kDefaultThreadCount);

    std::atomic<size_t> readyThreads = 0;
    std::atomic<bool> gcDone = false;

    std::vector<std::future<void>> mutatorFutures;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutatorFutures.push_back(mutators[i].Execute([&](mm::ThreadData& threadData, Mutator& mutator) {
            auto& obj = AllocateObject(threadData);
            auto stableRef = mm::StableRef::create(obj.header());

            ++readyThreads;
            while (!mm::IsThreadSuspensionRequested()) {}

            mm::safePoint();

            mutator.AddStackRoot(obj.header());
            std::move(stableRef).dispose();

            while (!gcDone) { mm::safePoint(); }

            EXPECT_THAT(mutator.Alive(), testing::Contains(obj.header()));
        }));
    }

    while (readyThreads < kDefaultThreadCount) {}

    mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
    gcDone = true;

    for (auto& future : mutatorFutures) {
        future.wait();
    }
}

REGISTER_TYPED_TEST_SUITE_WITH_LISTS(TracingGCTest, TRACING_GC_TEST_LIST, WeakResurrectionAtMarkTermination, ReleaseStableRefDuringRSCollection);
INSTANTIATE_TYPED_TEST_SUITE_P(CMS, TracingGCTest, ConcurrentMarkAndSweepTest);
