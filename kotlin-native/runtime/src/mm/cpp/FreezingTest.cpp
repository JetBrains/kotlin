/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Freezing.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "FreezeHooksTestSupport.hpp"
#include "Memory.h"
#include "ObjectTestSupport.hpp"
#include "Utils.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

struct WithFreezeHook {
    static constexpr bool hasFreezeHook = true;
};

struct NoFreezeHook {
    static constexpr bool hasFreezeHook = false;
};

struct EmptyPayload {
    static constexpr test_support::NoRefFields<EmptyPayload> kFields{};
};

struct Payload {
    mm::RefField field1;
    mm::RefField field2;
    mm::RefField field3;

    static constexpr std::array kFields{
            &Payload::field1,
            &Payload::field2,
            &Payload::field3,
    };
};

template <typename Payload, typename Traits>
class ObjectHolder : private Pinned {
public:
    static test_support::TypeInfoHolder::ObjectBuilder<Payload> TypeBuilder() {
        auto builder = test_support::TypeInfoHolder::ObjectBuilder<Payload>();
        if (Traits::hasFreezeHook) {
            builder.addFlag(TF_HAS_FREEZE_HOOK);
        }
        return builder;
    }

    ObjectHolder() : type_(TypeBuilder()), object_(type_.typeInfo()) {}

    ~ObjectHolder() {
        if (header()->has_meta_object()) {
            ObjHeader::destroyMetaObject(header());
        }
    }

    ObjHeader* header() { return object_.header(); }

    mm::RefField& operator[](size_t field) { return object_.fields()[field]; }

    void MakePermanent() { header()->typeInfoOrMeta_ = setPointerBits(header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER); }

private:
    test_support::TypeInfoHolder type_;
    test_support::Object<Payload> object_;
};

// Arrays types are predetermined, and none of them have freeze hooks.
template <size_t Elements>
class ArrayHolder : private Pinned {
public:
    ArrayHolder() {}

    ~ArrayHolder() {
        if (header()->has_meta_object()) {
            ObjHeader::destroyMetaObject(header());
        }
    }

    ObjHeader* header() { return array_.header(); }

    mm::RefField& operator[](size_t index) { return array_.elements()[index]; }

    void MakePermanent() { header()->typeInfoOrMeta_ = setPointerBits(header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER); }

private:
    test_support::ObjectArray<Elements> array_;
};

class FreezingTest : public testing::Test {
public:
    testing::MockFunction<void(ObjHeader*)>& freezeHook() { return freezeHooks_.freezeHook(); }

private:
    FreezeHooksTestSupport freezeHooks_;
};

class TypesNames {
public:
    template <typename T>
    static std::string GetName(int i) {
        switch (i) {
            case 0:
                return "object";
            case 1:
                return "array";
            default:
                return "unknown";
        }
    }
};

template <typename T>
class FreezingEmptyNoHookTest : public FreezingTest {};
using EmptyNoHookTypes = testing::Types<ObjectHolder<EmptyPayload, NoFreezeHook>, ArrayHolder<0>>;
TYPED_TEST_SUITE(FreezingEmptyNoHookTest, EmptyNoHookTypes, TypesNames);

template <typename T>
class FreezingEmptyWithHookTest : public FreezingTest {};
using EmptyWithHookTypes = testing::Types<ObjectHolder<EmptyPayload, WithFreezeHook>>;
TYPED_TEST_SUITE(FreezingEmptyWithHookTest, EmptyWithHookTypes, TypesNames);

template <typename T>
class FreezingNoHookTest : public FreezingTest {};
using NoHookTypes = testing::Types<ObjectHolder<Payload, NoFreezeHook>, ArrayHolder<3>>;
TYPED_TEST_SUITE(FreezingNoHookTest, NoHookTypes, TypesNames);

template <typename T>
class FreezingWithHookTest : public FreezingTest {};
using WithHookTypes = testing::Types<ObjectHolder<Payload, WithFreezeHook>>;
TYPED_TEST_SUITE(FreezingWithHookTest, WithHookTypes, TypesNames);

} // namespace

TYPED_TEST(FreezingEmptyNoHookTest, UnfrozenByDefault) {
    TypeParam object;
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, PermanentIsFrozen) {
    TypeParam object;
    object.MakePermanent();
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, FailToEnsureNeverFrozen) {
    ScopedMemoryInit init;
    TypeParam object;
    ASSERT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    ASSERT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::EnsureNeverFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, FailToEnsureNeverFrozenPermanent) {
    TypeParam object;
    object.MakePermanent();
    ASSERT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::EnsureNeverFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, Freeze) {
    ScopedMemoryInit init;
    TypeParam object;
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, FreezePermanent) {
    ScopedMemoryInit init;
    TypeParam object;
    object.MakePermanent();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, FreezeTwice) {
    ScopedMemoryInit init;
    TypeParam object;
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, FreezeForbidden) {
    ScopedMemoryInit init;
    TypeParam object;
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyWithHookTest, Freeze) {
    ScopedMemoryInit init;
    TypeParam object;
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyWithHookTest, FreezePermanent) {
    ScopedMemoryInit init;
    TypeParam object;
    object.MakePermanent();
    EXPECT_CALL(this->freezeHook(), Call(_)).Times(0);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyWithHookTest, FreezeTwice) {
    ScopedMemoryInit init;
    TypeParam object;
    // Only called for the first freeze.
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    testing::Mock::VerifyAndClearExpectations(&this->freezeHook());
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyWithHookTest, FreezeForbidden) {
    ScopedMemoryInit init;
    TypeParam object;
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, Freeze) {
    ScopedMemoryInit init;
    TypeParam object;
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezePermanent) {
    ScopedMemoryInit init;
    TypeParam object;
    object.MakePermanent();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTwice) {
    ScopedMemoryInit init;
    TypeParam object;
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeForbidden) {
    ScopedMemoryInit init;
    TypeParam object;
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTree) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreePermanent) {
    ScopedMemoryInit init;
    TypeParam object;
    object.MakePermanent();
    TypeParam field1;
    field1.MakePermanent();
    TypeParam field2;
    field2.MakePermanent();
    TypeParam field3;
    field3.MakePermanent();
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreePermanentLeaf) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    field1.MakePermanent();
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeTwice) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeForbidden) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
    EXPECT_FALSE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeForbiddenByField) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ASSERT_TRUE(mm::EnsureNeverFrozen(field2.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), field2.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
    EXPECT_FALSE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeRecursive) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam inner1;
    TypeParam inner2;
    object[0] = inner1.header();
    inner1[0] = inner2.header();
    inner2[0] = object.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(inner1.header()));
    EXPECT_TRUE(mm::IsFrozen(inner2.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeRecursivePermanent) {
    ScopedMemoryInit init;
    TypeParam object;
    object.MakePermanent();
    TypeParam inner1;
    inner1.MakePermanent();
    TypeParam inner2;
    inner2.MakePermanent();
    object[0] = inner1.header();
    inner1[0] = inner2.header();
    inner2[0] = object.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(inner1.header()));
    EXPECT_TRUE(mm::IsFrozen(inner2.header()));
}

TYPED_TEST(FreezingWithHookTest, Freeze) {
    ScopedMemoryInit init;
    TypeParam object;
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezePermanent) {
    ScopedMemoryInit init;
    TypeParam object;
    object.MakePermanent();
    EXPECT_CALL(this->freezeHook(), Call(_)).Times(0);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTwice) {
    ScopedMemoryInit init;
    TypeParam object;
    // Only called for the first freeze.
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    testing::Mock::VerifyAndClearExpectations(&this->freezeHook());
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeForbidden) {
    ScopedMemoryInit init;
    TypeParam object;
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTree) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreePermanent) {
    ScopedMemoryInit init;
    TypeParam object;
    object.MakePermanent();
    TypeParam field1;
    field1.MakePermanent();
    TypeParam field2;
    field2.MakePermanent();
    TypeParam field3;
    field3.MakePermanent();
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_CALL(this->freezeHook(), Call(_)).Times(0);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreePermanentLeaf) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    field1.MakePermanent();
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header())).Times(0);
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeTwice) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    // Only called for the first freeze.
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    testing::Mock::VerifyAndClearExpectations(&this->freezeHook());
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeForbidden) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
    EXPECT_FALSE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeForbiddenByField) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ASSERT_TRUE(mm::EnsureNeverFrozen(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), field2.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
    EXPECT_FALSE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeRecursive) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam inner1;
    TypeParam inner2;
    object[0] = inner1.header();
    inner1[0] = inner2.header();
    inner2[0] = object.header();
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(inner1.header()));
    EXPECT_CALL(this->freezeHook(), Call(inner2.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(inner1.header()));
    EXPECT_TRUE(mm::IsFrozen(inner2.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeRecursivePermanent) {
    ScopedMemoryInit init;
    TypeParam object;
    object.MakePermanent();
    TypeParam inner1;
    inner1.MakePermanent();
    TypeParam inner2;
    inner2.MakePermanent();
    object[0] = inner1.header();
    inner1[0] = inner2.header();
    inner2[0] = object.header();
    EXPECT_CALL(this->freezeHook(), Call(_)).Times(0);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(inner1.header()));
    EXPECT_TRUE(mm::IsFrozen(inner2.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeWithHookRewrite) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    TypeParam oldInner;
    TypeParam newInner;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    field2[0] = oldInner.header();
    ON_CALL(this->freezeHook(), Call(field2.header())).WillByDefault([&field2, &newInner](ObjHeader* obj) {
        field2[0] = newInner.header();
    });
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_CALL(this->freezeHook(), Call(newInner.header()));
    EXPECT_CALL(this->freezeHook(), Call(oldInner.header())).Times(0);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
    EXPECT_TRUE(mm::IsFrozen(newInner.header()));
    EXPECT_FALSE(mm::IsFrozen(oldInner.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeForbiddenByHook) {
    ScopedMemoryInit init;
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ON_CALL(this->freezeHook(), Call(field2.header())).WillByDefault([](ObjHeader* obj) { EXPECT_TRUE(mm::EnsureNeverFrozen(obj)); });
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), field2.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
}
