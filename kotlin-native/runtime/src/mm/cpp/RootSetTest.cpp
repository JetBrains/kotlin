/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RootSet.hpp"

#include <memory>
#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ExternalRCRef.hpp"
#include "ObjectTestSupport.hpp"
#include "ShadowStack.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

namespace {

struct Payload {
    static constexpr test_support::NoRefFields<Payload> kFields = {};
};

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};

using Object = test_support::Object<Payload>;

std::unique_ptr<Object> allocateObject() noexcept {
    return std::make_unique<Object>(typeHolder.typeInfo());
}

class Global : private Pinned {
public:
    explicit Global(mm::ThreadData& threadData) noexcept {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(threadData, &location_);
        location_ = allocateObject().release()->header();
    }

    KRef& operator*() noexcept { return location_; }

    ~Global() {
        // Delete the allocated global.
        std::unique_ptr<Object> obj(&Object::FromObjHeader(location_));
        location_ = nullptr;
    }
private:
    ObjHeader* location_ = nullptr;
};

// TODO: All the test helpers to create the rootset should be abstracted out.

template <size_t LocalsCount>
class StackEntry : private Pinned {
public:
    static_assert(LocalsCount > 0, "Must have at least 1 object on stack");

    explicit StackEntry(mm::ShadowStack& shadowStack) : shadowStack_(shadowStack) {
        objects_.reserve(LocalsCount);
        // Fill `locals_` with some values.
        for (size_t i = 0; i < LocalsCount; ++i) {
            auto object = allocateObject();
            (*this)[i] = object->header();
            objects_.push_back(std::move(object));
        }

        shadowStack_.EnterFrame(data_.data(), 0, kTotalCount);
    }

    ~StackEntry() { shadowStack_.LeaveFrame(data_.data(), 0, kTotalCount); }

    ObjHeader*& operator[](size_t index) { return data_[kFrameOverlayCount + index]; }

private:
    mm::ShadowStack& shadowStack_;
    std::vector<std::unique_ptr<Object>> objects_;

    // The following is what the compiler creates on the stack.
    static inline constexpr int kFrameOverlayCount = sizeof(FrameOverlay) / sizeof(ObjHeader**);
    static inline constexpr int kTotalCount = kFrameOverlayCount + LocalsCount;
    std::array<ObjHeader*, kTotalCount> data_;
};

struct TLSKey {};

} // namespace

TEST(ThreadRootSetTest, Basic) {
    mm::ShadowStack stack;
    StackEntry<2> entry(stack);

    TLSKey key;
    mm::ThreadLocalStorage tls;
    tls.AddRecord(&key, 3);
    tls.Commit();

    mm::ThreadRootSet iter(stack, tls);

    std::vector<mm::ThreadRootSet::Value> actual;
    for (auto object : iter) {
        actual.push_back(object);
    }

    auto asStack = [](ObjHeader*& object) -> mm::ThreadRootSet::Value { return {object, mm::ThreadRootSet::Source::kStack}; };
    auto asTLS = [](ObjHeader*& object) -> mm::ThreadRootSet::Value { return {object, mm::ThreadRootSet::Source::kTLS}; };
    EXPECT_THAT(
            actual,
            testing::ElementsAre(
                    asStack(entry[0]), asStack(entry[1]), asTLS(*tls.Lookup(&key, 0)), asTLS(*tls.Lookup(&key, 1)),
                    asTLS(*tls.Lookup(&key, 2))));
}

TEST(ThreadRootSetTest, Empty) {
    mm::ShadowStack stack;
    mm::ThreadLocalStorage tls;

    mm::ThreadRootSet iter(stack, tls);

    std::vector<mm::ThreadRootSet::Value> actual;
    for (auto object : iter) {
        actual.push_back(object);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(GlobalRootSetTest, Basic) {
    RunInNewThread([](mm::ThreadData& threadData) {
        Global global1(threadData);
        Global global2(threadData);

        mm::ExternalRCRefRegistry externalRCRefsRegistry;
        mm::ExternalRCRefRegistry::ThreadQueue stableRefsProducer(externalRCRefsRegistry);
        auto stableRef1 = allocateObject();
        auto stableRef2 = allocateObject();
        auto stableRef3 = allocateObject();
        mm::OwningExternalRCRef stableRefHandle1(stableRefsProducer.createExternalRCRefImpl(stableRef1->header(), 1).toRaw());
        mm::OwningExternalRCRef stableRefHandle2(stableRefsProducer.createExternalRCRefImpl(stableRef2->header(), 1).toRaw());
        mm::OwningExternalRCRef stableRefHandle3(stableRefsProducer.createExternalRCRefImpl(stableRef3->header(), 1).toRaw());

        threadData.globalsThreadQueue().Publish();
        stableRefsProducer.publish();

        mm::GlobalRootSet iter(mm::GlobalsRegistry::Instance(), externalRCRefsRegistry);

        std::vector<mm::GlobalRootSet::Value> actual;
        for (auto object : iter) {
            actual.push_back(object);
        }

        auto asGlobal = [](Global& global) -> mm::GlobalRootSet::Value { return {*global, mm::GlobalRootSet::Source::kGlobal}; };
        auto asStableRef = [](std::unique_ptr<Object>& object) -> mm::GlobalRootSet::Value { return {object->header(), mm::GlobalRootSet::Source::kStableRef}; };
        EXPECT_THAT(
                actual,
                testing::UnorderedElementsAre(
                        asGlobal(global1), asGlobal(global2), asStableRef(stableRef1), asStableRef(stableRef2), asStableRef(stableRef3)));
        mm::GlobalsRegistry::Instance().ClearForTests();
    });
}

TEST(GlobalRootSetTest, Empty) {
    RunInNewThread([](mm::ThreadData& threadData) {
        mm::GlobalsRegistry globals;
        mm::ExternalRCRefRegistry externalRCRefsRegistry;

        mm::GlobalRootSet iter(globals, externalRCRefsRegistry);

        std::vector<mm::GlobalRootSet::Value> actual;
        for (auto object : iter) {
            actual.push_back(object);
        }

        EXPECT_THAT(actual, testing::IsEmpty());
    });
}
