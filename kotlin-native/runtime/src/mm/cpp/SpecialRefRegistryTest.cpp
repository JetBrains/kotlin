/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SpecialRefRegistry.hpp"

#include <condition_variable>
#include <mutex>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ExternalRCRef.hpp"
#include "TestSupport.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

namespace {

class Waiter : private Pinned {
public:
    void allow() noexcept {
        {
            std::unique_lock guard(mutex_);
            allow_ = true;
        }
        cv_.notify_all();
    };

    void wait() noexcept {
        std::unique_lock guard(mutex_);
        cv_.wait(guard, [this] { return allow_; });
    }

private:
    bool allow_ = false;
    std::mutex mutex_;
    std::condition_variable cv_;
};

struct Payload {
    static constexpr test_support::NoRefFields<Payload> kFields = {};
};

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
using Object = test_support::Object<Payload>;

} // namespace

class SpecialRefRegistryTest : public testing::Test {
public:
    ~SpecialRefRegistryTest() {
        // Clean up safely.
        roots();
        all();
    }

    void publish() noexcept { mm::ThreadRegistry::Instance().CurrentThreadData()->specialRefRegistry().publish(); }

    template <typename... Invalidated>
    std::vector<ObjHeader*> all(Invalidated&&... invalidated) noexcept {
        std::set<ObjHeader*> invalidatedSet({std::forward<Invalidated>(invalidated)...});
        std::vector<ObjHeader*> result;
        for (auto obj : mm::SpecialRefRegistry::instance().lockForIter()) {
            if (invalidatedSet.find(obj) != invalidatedSet.end()) {
                obj = nullptr;
            }
            result.push_back(obj);
        }
        return result;
    }

    std::vector<ObjHeader*> roots() noexcept {
        std::vector<ObjHeader*> result;
        for (auto* obj : mm::SpecialRefRegistry::instance().roots()) {
            result.push_back(obj);
        }
        return result;
    }

    ObjHeader* tryRef(mm::RawExternalRCRef* weakRef) noexcept {
        ObjHeader* result;
        return mm::tryRefExternalRCRef(weakRef, &result);
    }
};

TEST_F(SpecialRefRegistryTest, RegisterRetainedRefWithoutPublish) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref = mm::createRetainedExternalRCRef(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(mm::dereferenceExternalRCRef(ref), obj);

        mm::releaseAndDisposeExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, RegisterRetainedRef) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref = mm::createRetainedExternalRCRef(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(mm::dereferenceExternalRCRef(ref), obj);

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(mm::dereferenceExternalRCRef(ref), obj);

        mm::releaseAndDisposeExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, RegisterUnretainedRefWithoutPublish) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref = mm::createUnretainedExternalRCRef(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(tryRef(ref), obj);

        mm::disposeExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, RegisterUnretainedRef) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref = mm::createUnretainedExternalRCRef(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(tryRef(ref), obj);

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(tryRef(ref), obj);

        mm::disposeExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, RegisterAllRefsWithoutPublish) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref1 = mm::createRetainedExternalRCRef(obj);
        auto ref2 = mm::createUnretainedExternalRCRef(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj, obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        mm::releaseAndDisposeExternalRCRef(ref1);
        mm::disposeExternalRCRef(ref2);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, RegisterAllRefs) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref1 = mm::createRetainedExternalRCRef(obj);
        auto ref2 = mm::createUnretainedExternalRCRef(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj, obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj, obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj, obj, obj));

        mm::disposeExternalRCRef(ref1);
        mm::disposeExternalRCRef(ref2);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, TryUnretainedRef) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref = mm::createUnretainedExternalRCRef(obj);
        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(tryRef(ref), obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(obj), testing::UnorderedElementsAre(nullptr));
        EXPECT_THAT(tryRef(ref), nullptr);

        mm::disposeExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, TryRetainedRef) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref = mm::createRetainedExternalRCRef(obj);
        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(tryRef(ref), obj);
        EXPECT_THAT(mm::dereferenceExternalRCRef(ref), obj);

        mm::releaseExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(tryRef(ref), obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(obj), testing::UnorderedElementsAre(nullptr));
        EXPECT_THAT(tryRef(ref), nullptr);

        mm::disposeExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, TryRetainedBeforePublishRef) {
    RunInNewThread([this] {
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        auto ref = mm::createUnretainedExternalRCRef(obj);
        mm::retainExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(mm::dereferenceExternalRCRef(ref), obj);

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(mm::dereferenceExternalRCRef(ref), obj);

        mm::releaseAndDisposeExternalRCRef(ref);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(SpecialRefRegistryTest, StressRetainedRef) {
    Object object(typeHolder.typeInfo());
    KRef obj = object.header();
    Waiter waiter;
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&, this] {
            ScopedMemoryInit scope;
            ObjHolder holder(obj);
            waiter.wait();
            auto ref = mm::createRetainedExternalRCRef(obj);
            publish();
            mm::releaseAndDisposeExternalRCRef(ref);
        });
    }
    waiter.allow();
    mutators.clear();
    EXPECT_THAT(roots(), testing::UnorderedElementsAre());
    EXPECT_THAT(all(), testing::UnorderedElementsAre());
}

TEST_F(SpecialRefRegistryTest, StressUnretainedRef) {
    Object object(typeHolder.typeInfo());
    KRef obj = object.header();
    Waiter waiter;
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&, this] {
            ScopedMemoryInit scope;
            ObjHolder holder(obj);
            waiter.wait();
            auto ref = mm::createUnretainedExternalRCRef(obj);
            publish();
            mm::disposeExternalRCRef(ref);
        });
    }
    waiter.allow();
    mutators.clear();
    EXPECT_THAT(roots(), testing::UnorderedElementsAre());
    EXPECT_THAT(all(), testing::UnorderedElementsAre());
}

TEST_F(SpecialRefRegistryTest, StressRetainRelease) {
    RunInNewThread([this] {
        constexpr int kGCCycles = 10000;
        constexpr int kRefsCount = 3;
        Object object(typeHolder.typeInfo());
        KRef obj = object.header();
        ObjHolder holder(obj);
        Waiter waiter;
        std::atomic<bool> canStop = false;
        std::vector<mm::RawExternalRCRef*> refs;
        for (int i = 0; i < kRefsCount; ++i) {
            refs.emplace_back(mm::createUnretainedExternalRCRef(obj));
        }
        publish();
        std::vector<ScopedThread> threads;
        // GC thread
        threads.emplace_back([&, this] {
            waiter.wait();
            for (int i = 0; i < kGCCycles; ++i) {
                roots();
                all();
            }
            canStop.store(true, std::memory_order_release);
        });
        for (int i = 0; i < kDefaultThreadCount; ++i) {
            // Mutator thread
            threads.emplace_back([i, obj, &refs, &waiter, &canStop] {
                ScopedMemoryInit scope;
                ObjHolder holder(obj);
                waiter.wait();
                auto& ref = refs[i % kRefsCount];
                while (!canStop.load(std::memory_order_acquire)) {
                    mm::retainExternalRCRef(ref);
                    mm::releaseExternalRCRef(ref);
                }
            });
        }
        waiter.allow();
        threads.clear();
        for (auto& ref : refs) {
            mm::disposeExternalRCRef(ref);
        }
        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}
