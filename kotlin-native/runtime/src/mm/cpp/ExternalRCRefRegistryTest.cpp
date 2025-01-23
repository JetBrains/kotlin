/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExternalRCRefRegistry.hpp"

#include <condition_variable>
#include <mutex>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ObjCBackRef.hpp"
#include "StableRef.hpp"
#include "TestSupport.hpp"
#include "ThreadRegistry.hpp"
#include "WeakRef.hpp"

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

} // namespace

class ExternalRCRefRegistryTest : public testing::Test {
public:
    ~ExternalRCRefRegistryTest() {
        // Clean up safely.
        roots();
        all();
    }

    void publish() noexcept { mm::ThreadRegistry::Instance().CurrentThreadData()->externalRCRefRegistry().publish(); }

    template <typename... Invalidated>
    std::vector<ObjHeader*> all(Invalidated&&... invalidated) noexcept {
        std::set<ObjHeader*> invalidatedSet({std::forward<Invalidated>(invalidated)...});
        std::vector<ObjHeader*> result;
        for (auto obj : mm::ExternalRCRefRegistry::instance().lockForIter()) {
            if (invalidatedSet.find(obj) != invalidatedSet.end()) {
                obj = nullptr;
            }
            result.push_back(obj);
        }
        return result;
    }

    std::vector<ObjHeader*> roots() noexcept {
        std::vector<ObjHeader*> result;
        for (auto* obj : mm::ExternalRCRefRegistry::instance().roots()) {
            result.push_back(obj);
        }
        return result;
    }

    ObjHeader* tryRef(mm::WeakRef& weakRef) noexcept {
        ObjHeader* result;
        return weakRef.tryRef(&result);
    }
};

TEST_F(ExternalRCRefRegistryTest, RegisterStableRefWithoutPublish) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::StableRef::create(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(*ref, obj);

        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RegisterStableRef) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::StableRef::create(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(*ref, obj);

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(*ref, obj);

        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RegisterWeakRefWithoutPublish) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::WeakRef::create(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(tryRef(ref), obj);

        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RegisterWeakRef) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::WeakRef::create(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(tryRef(ref), obj);

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(tryRef(ref), obj);

        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RegisterObjCRefWithoutPublish) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::ObjCBackRef::create(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(*ref, obj);

        ref.release();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(*ref, obj);

        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RegisterObjCRef) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::ObjCBackRef::create(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(*ref, obj);

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(*ref, obj);

        ref.release();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(*ref, obj);

        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RegisterAllRefsWithoutPublish) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref1 = mm::StableRef::create(obj);
        auto ref2 = mm::WeakRef::create(obj);
        auto ref3 = mm::ObjCBackRef::create(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj, obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        std::move(ref1).dispose();
        std::move(ref2).dispose();
        ref3.release();
        std::move(ref3).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RegisterAllRefs) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref1 = mm::StableRef::create(obj);
        auto ref2 = mm::WeakRef::create(obj);
        auto ref3 = mm::ObjCBackRef::create(obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj, obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj, obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj, obj, obj));

        std::move(ref1).dispose();
        std::move(ref2).dispose();
        ref3.release();
        std::move(ref3).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, InvalidateWeakRef) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::WeakRef::create(obj);
        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(obj), testing::UnorderedElementsAre(nullptr));
        EXPECT_THAT(tryRef(ref), nullptr);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(nullptr));
        EXPECT_THAT(tryRef(ref), nullptr);

        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, InvalidateObjCRef) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::ObjCBackRef::create(obj);
        ref.release();
        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(obj), testing::UnorderedElementsAre(nullptr));
        EXPECT_FALSE(ref.tryRetainForTests());

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(nullptr));

        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, TryObjCRef) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::ObjCBackRef::create(obj);
        ref.release();
        publish();

        EXPECT_TRUE(ref.tryRetainForTests());
        EXPECT_THAT(*ref, obj);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(*ref, obj);

        ref.release();
        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, ReRetainObjCRefBeforePublish) {
    RunInNewThread([this] {
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        auto ref = mm::ObjCBackRef::create(obj);
        ref.release();
        ref.retain();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(*ref, obj);

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(obj));
        EXPECT_THAT(*ref, obj);

        ref.release();
        std::move(ref).dispose();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, StressStableRef) {
    ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
    Waiter waiter;
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&, this] {
            ScopedMemoryInit scope;
            ObjHolder holder(obj);
            waiter.wait();
            auto ref = mm::StableRef::create(obj);
            publish();
            std::move(ref).dispose();
        });
    }
    waiter.allow();
    mutators.clear();
    EXPECT_THAT(roots(), testing::UnorderedElementsAre());
    EXPECT_THAT(all(), testing::UnorderedElementsAre());
}

TEST_F(ExternalRCRefRegistryTest, StressWeakRef) {
    ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
    Waiter waiter;
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&, this] {
            ScopedMemoryInit scope;
            ObjHolder holder(obj);
            waiter.wait();
            auto ref = mm::WeakRef::create(obj);
            publish();
            std::move(ref).dispose();
        });
    }
    waiter.allow();
    mutators.clear();
    EXPECT_THAT(roots(), testing::UnorderedElementsAre());
    EXPECT_THAT(all(), testing::UnorderedElementsAre());
}

TEST_F(ExternalRCRefRegistryTest, StressObjCRef) {
    ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
    Waiter waiter;
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&, this] {
            ScopedMemoryInit scope;
            ObjHolder holder(obj);
            waiter.wait();
            auto ref = mm::ObjCBackRef::create(obj);
            publish();
            ref.release();
            std::move(ref).dispose();
        });
    }
    waiter.allow();
    mutators.clear();
    EXPECT_THAT(roots(), testing::UnorderedElementsAre());
    EXPECT_THAT(all(), testing::UnorderedElementsAre());
}

TEST_F(ExternalRCRefRegistryTest, StressObjCRefRetainRelease) {
    RunInNewThread([this] {
        constexpr int kGCCycles = 10000;
        constexpr int kRefsCount = 3;
        ObjHeader* obj = reinterpret_cast<ObjHeader*>(1);
        ObjHolder holder(obj);
        Waiter waiter;
        std::atomic<bool> canStop = false;
        std::vector<mm::ObjCBackRef> refs;
        for (int i = 0; i < kRefsCount; ++i) {
            refs.emplace_back(mm::ObjCBackRef::create(obj));
            refs.back().release();
        }
        publish();
        std::vector<ScopedThread> mutators;
        mutators.emplace_back([&, this] {
            waiter.wait();
            for (int i = 0; i < kGCCycles; ++i) {
                roots();
                all();
            }
            canStop.store(true, std::memory_order_release);
        });
        for (int i = 0; i < kDefaultThreadCount; ++i) {
            mutators.emplace_back([i, obj, &refs, &waiter, &canStop] {
                ScopedMemoryInit scope;
                ObjHolder holder(obj);
                waiter.wait();
                auto& ref = refs[i % kRefsCount];
                while (!canStop.load(std::memory_order_acquire)) {
                    ref.retain();
                    ref.release();
                }
            });
        }
        waiter.allow();
        mutators.clear();
        for (auto& ref : refs) {
            std::move(ref).dispose();
        }
        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}
