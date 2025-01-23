/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExternalRCRefRegistry.hpp"

#include <condition_variable>
#include <mutex>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ExternalRCRef.hpp"
#include "ObjectTestSupport.hpp"
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

test_support::TypeInfoHolder typeInfoHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};

struct HeapObject : test_support::Object<Payload> {
    HeapObject() noexcept : Object(typeInfoHolder.typeInfo()) {}
};

struct PermanentObject : test_support::Object<Payload> {
    PermanentObject() noexcept : Object(typeInfoHolder.typeInfo()) {
        header()->typeInfoOrMeta_ = setPointerBits(header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        RuntimeAssert(header()->permanent(), "Must be permanent");
    }
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

    template <typename Traits>
    ObjHeader* tryRef(mm::ExternalRCRef<Traits>& weakRef) noexcept {
        ObjHeader* result;
        return weakRef.tryRef(&result);
    }
};

TEST_F(ExternalRCRefRegistryTest, RegisterRefsWithoutPublish) {
    RunInNewThread([this] {
        HeapObject object1;
        HeapObject object2;
        PermanentObject object3;
        PermanentObject object4;

        auto ref1 = mm::OwningExternalRCRef(object1.header());
        auto ref2 = mm::WeakExternalRCRef(object2.header());
        auto ref3 = mm::OwningExternalRCRef(object3.header());
        auto ref4 = mm::WeakExternalRCRef(object4.header());

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(object1.header()));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        EXPECT_THAT(*ref1, object1.header());
        EXPECT_THAT(tryRef(ref1), object1.header());
        EXPECT_THAT(tryRef(ref2), object2.header());
        EXPECT_THAT(*ref3, object3.header());
        EXPECT_THAT(tryRef(ref3), object3.header());
        EXPECT_THAT(tryRef(ref4), object4.header());

        ref1.reset();
        ref2.reset();
        ref3.reset();
        ref4.reset();

        EXPECT_THAT(*ref1, nullptr);
        EXPECT_THAT(tryRef(ref1), nullptr);
        EXPECT_THAT(tryRef(ref2), nullptr);
        EXPECT_THAT(*ref3, nullptr);
        EXPECT_THAT(tryRef(ref3), nullptr);
        EXPECT_THAT(tryRef(ref4), nullptr);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RegisterAllRefs) {
    RunInNewThread([this] {
        HeapObject object1;
        HeapObject object2;
        PermanentObject object3;
        PermanentObject object4;

        auto ref1 = mm::OwningExternalRCRef(object1.header());
        auto ref2 = mm::WeakExternalRCRef(object2.header());
        auto ref3 = mm::OwningExternalRCRef(object3.header());
        auto ref4 = mm::WeakExternalRCRef(object4.header());

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(object1.header()));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());

        EXPECT_THAT(*ref1, object1.header());
        EXPECT_THAT(tryRef(ref1), object1.header());
        EXPECT_THAT(tryRef(ref2), object2.header());
        EXPECT_THAT(*ref3, object3.header());
        EXPECT_THAT(tryRef(ref3), object3.header());
        EXPECT_THAT(tryRef(ref4), object4.header());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(object1.header()));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(object1.header(), object2.header()));

        EXPECT_THAT(*ref1, object1.header());
        EXPECT_THAT(tryRef(ref1), object1.header());
        EXPECT_THAT(tryRef(ref2), object2.header());
        EXPECT_THAT(*ref3, object3.header());
        EXPECT_THAT(tryRef(ref3), object3.header());
        EXPECT_THAT(tryRef(ref4), object4.header());

        ref1.reset();
        ref2.reset();
        ref3.reset();
        ref4.reset();

        EXPECT_THAT(*ref1, nullptr);
        EXPECT_THAT(tryRef(ref1), nullptr);
        EXPECT_THAT(tryRef(ref2), nullptr);
        EXPECT_THAT(*ref3, nullptr);
        EXPECT_THAT(tryRef(ref3), nullptr);
        EXPECT_THAT(tryRef(ref4), nullptr);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, InvalidateWeakRef) {
    RunInNewThread([this] {
        HeapObject object;
        auto ref = mm::WeakExternalRCRef(object.header());
        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(object.header()), testing::UnorderedElementsAre(nullptr));
        EXPECT_THAT(tryRef(ref), nullptr);

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(nullptr));
        EXPECT_THAT(tryRef(ref), nullptr);

        ref.reset();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RetainWeakRef) {
    RunInNewThread([this] {
        HeapObject object;
        auto ref = mm::WeakExternalRCRef(object.header());
        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(object.header()));
        EXPECT_THAT(tryRef(ref), object.header());

        mm::retainExternalRCRef(ref.get());

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(object.header()));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(object.header()));

        mm::releaseExternalRCRef(ref.get());

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(object.header()));
        EXPECT_THAT(tryRef(ref), object.header());

        ref.reset();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, RetainWeakRefBeforePublish) {
    RunInNewThread([this] {
        HeapObject object;
        auto ref = mm::WeakExternalRCRef(object.header());

        mm::retainExternalRCRef(ref.get());

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(object.header()));
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
        EXPECT_THAT(tryRef(ref), object.header());

        publish();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre(object.header()));
        EXPECT_THAT(all(), testing::UnorderedElementsAre(object.header()));
        EXPECT_THAT(tryRef(ref), object.header());

        mm::releaseExternalRCRef(ref.get());

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre(object.header()));
        EXPECT_THAT(tryRef(ref), object.header());

        ref.reset();

        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}

TEST_F(ExternalRCRefRegistryTest, StressOwningRef) {
    HeapObject object;
    Waiter waiter;
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&, this] {
            ScopedMemoryInit scope;
            waiter.wait();
            auto ref = mm::OwningExternalRCRef(object.header());
            publish();
            ref.reset();
        });
    }
    waiter.allow();
    mutators.clear();
    EXPECT_THAT(roots(), testing::UnorderedElementsAre());
    EXPECT_THAT(all(), testing::UnorderedElementsAre());
}

TEST_F(ExternalRCRefRegistryTest, StressWeakRef) {
    HeapObject object;
    Waiter waiter;
    std::vector<ScopedThread> mutators;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators.emplace_back([&, this] {
            ScopedMemoryInit scope;
            waiter.wait();
            auto ref = mm::WeakExternalRCRef(object.header());
            publish();
            ref.reset();
        });
    }
    waiter.allow();
    mutators.clear();
    EXPECT_THAT(roots(), testing::UnorderedElementsAre());
    EXPECT_THAT(all(), testing::UnorderedElementsAre());
}

TEST_F(ExternalRCRefRegistryTest, StressWeakRefRetainRelease) {
    RunInNewThread([this] {
        constexpr int kGCCycles = 10000;
        constexpr int kRefsCount = 3;
        HeapObject object;
        KRef obj = object.header();
        Waiter waiter;
        std::atomic<bool> canStop = false;
        std::vector<mm::WeakExternalRCRef> refs;
        for (int i = 0; i < kRefsCount; ++i) {
            refs.emplace_back(mm::WeakExternalRCRef(obj));
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
            mutators.emplace_back([i, &refs, &waiter, &canStop] {
                ScopedMemoryInit scope;
                waiter.wait();
                auto& ref = refs[i % kRefsCount];
                while (!canStop.load(std::memory_order_acquire)) {
                    mm::retainExternalRCRef(ref.get());
                    mm::releaseExternalRCRef(ref.get());
                }
            });
        }
        waiter.allow();
        mutators.clear();
        for (auto& ref : refs) {
            ref.reset();
        }
        EXPECT_THAT(roots(), testing::UnorderedElementsAre());
        EXPECT_THAT(all(), testing::UnorderedElementsAre());
    });
}
