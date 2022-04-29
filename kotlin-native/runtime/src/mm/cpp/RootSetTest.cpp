/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RootSet.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ShadowStack.hpp"
#include "std_support/Memory.hpp"
#include "std_support/Vector.hpp"

using namespace kotlin;

namespace {

// TODO: All the test helpers to create the rootset should be abstracted out.

template <size_t LocalsCount>
class StackEntry : private Pinned {
public:
    static_assert(LocalsCount > 0, "Must have at least 1 object on stack");

    explicit StackEntry(mm::ShadowStack& shadowStack) : shadowStack_(shadowStack), value_(std_support::make_unique<ObjHeader>()) {
        // Fill `locals_` with some values.
        for (size_t i = 0; i < LocalsCount; ++i) {
            (*this)[i] = value_.get() + i;
        }

        shadowStack_.EnterFrame(data_.data(), 0, kTotalCount);
    }

    ~StackEntry() { shadowStack_.LeaveFrame(data_.data(), 0, kTotalCount); }

    ObjHeader*& operator[](size_t index) { return data_[kFrameOverlayCount + index]; }

private:
    mm::ShadowStack& shadowStack_;
    std_support::unique_ptr<ObjHeader> value_;

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

    std_support::vector<mm::ThreadRootSet::Value> actual;
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

    std_support::vector<mm::ThreadRootSet::Value> actual;
    for (auto object : iter) {
        actual.push_back(object);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(GlobalRootSetTest, Basic) {
    mm::GlobalsRegistry globals;
    mm::GlobalsRegistry::ThreadQueue globalsProducer(globals);
    ObjHeader* global1 = reinterpret_cast<ObjHeader*>(1);
    ObjHeader* global2 = reinterpret_cast<ObjHeader*>(2);
    globalsProducer.Insert(&global1);
    globalsProducer.Insert(&global2);

    mm::StableRefRegistry stableRefs;
    mm::StableRefRegistry::ThreadQueue stableRefsProducer(stableRefs);
    ObjHeader* stableRef1 = reinterpret_cast<ObjHeader*>(3);
    ObjHeader* stableRef2 = reinterpret_cast<ObjHeader*>(4);
    ObjHeader* stableRef3 = reinterpret_cast<ObjHeader*>(5);
    stableRefsProducer.Insert(stableRef1);
    stableRefsProducer.Insert(stableRef2);
    stableRefsProducer.Insert(stableRef3);

    globalsProducer.Publish();
    stableRefsProducer.Publish();

    mm::GlobalRootSet iter(globals, stableRefs);

    std_support::vector<mm::GlobalRootSet::Value> actual;
    for (auto object : iter) {
        actual.push_back(object);
    }

    auto asGlobal = [](ObjHeader*& object) -> mm::GlobalRootSet::Value { return {object, mm::GlobalRootSet::Source::kGlobal}; };
    auto asStableRef = [](ObjHeader*& object) -> mm::GlobalRootSet::Value { return {object, mm::GlobalRootSet::Source::kStableRef}; };
    EXPECT_THAT(
            actual,
            testing::ElementsAre(
                    asGlobal(global1), asGlobal(global2), asStableRef(stableRef1), asStableRef(stableRef2), asStableRef(stableRef3)));
}

TEST(GlobalRootSetTest, Empty) {
    mm::GlobalsRegistry globals;
    mm::StableRefRegistry stableRefs;

    mm::GlobalRootSet iter(globals, stableRefs);

    std_support::vector<mm::GlobalRootSet::Value> actual;
    for (auto object : iter) {
        actual.push_back(object);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}
