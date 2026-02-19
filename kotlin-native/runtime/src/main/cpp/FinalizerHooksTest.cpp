/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FinalizerHooks.hpp"

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "FinalizerHooksTestSupport.hpp"
#include "Memory.h"
#include "ObjectTestSupport.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

struct EmptyPayload {
    static constexpr test_support::NoRefFields<EmptyPayload> kFields{};
};

class FinalizerHooksTest : public testing::Test {
public:
    testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHooks_.finalizerHook(); }

private:
    FinalizerHooksTestSupport finalizerHooks_;
};

} // namespace

TEST_F(FinalizerHooksTest, TypeWithFinalizerHookWithoutExtra) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>().addFlag(TF_HAS_FINALIZER)};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    ObjHeader* obj = object.header();
    ASSERT_FALSE(obj->has_meta_object());

    EXPECT_TRUE(HasFinalizers(obj));
    EXPECT_CALL(finalizerHook(), Call(obj));
    RunFinalizers(obj);
    EXPECT_FALSE(obj->has_meta_object());
}

TEST_F(FinalizerHooksTest, TypeWithFinalizerHookWithExtra) {
    ScopedMemoryInit init;
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>().addFlag(TF_HAS_FINALIZER)};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    ObjHeader* obj = object.header();
    ObjHeader::createMetaObject(obj);
    ASSERT_TRUE(obj->has_meta_object());

    EXPECT_TRUE(HasFinalizers(obj));
    EXPECT_CALL(finalizerHook(), Call(obj));
    RunFinalizers(obj);
    EXPECT_FALSE(obj->has_meta_object());
}

TEST_F(FinalizerHooksTest, TypeWithoutFinalizerHookWithoutExtra) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    ObjHeader* obj = object.header();
    ASSERT_FALSE(obj->has_meta_object());

    EXPECT_FALSE(HasFinalizers(obj));
    EXPECT_CALL(finalizerHook(), Call(_)).Times(0);
    RunFinalizers(obj);
    EXPECT_FALSE(obj->has_meta_object());
}

TEST_F(FinalizerHooksTest, TypeWithoutFinalizerHookWithExtra) {
    ScopedMemoryInit init;
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    ObjHeader* obj = object.header();
    ObjHeader::createMetaObject(obj);
    ASSERT_TRUE(obj->has_meta_object());

    EXPECT_TRUE(HasFinalizers(obj));
    EXPECT_CALL(finalizerHook(), Call(_)).Times(0);
    RunFinalizers(obj);
    EXPECT_FALSE(obj->has_meta_object());
}
