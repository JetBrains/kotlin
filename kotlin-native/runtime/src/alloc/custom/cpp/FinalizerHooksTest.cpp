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
#include "mm/ObjectOps.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

struct EmptyPayload {
    static constexpr test_support::NoRefFields<EmptyPayload> kFields{};
};

class FinalizerHooksTest : public testing::Test {
public:
    testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHooks_.finalizerHook(); }

    void TearDown() override { mm::GlobalData::Instance().allocator().clearForTests(); }

private:
    FinalizerHooksTestSupport finalizerHooks_;
};

} // namespace

TEST_F(FinalizerHooksTest, TypeWithFinalizerHook) {
    RunInNewThread([this](mm::ThreadData& thread) {
        test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>().addFlag(TF_HAS_FINALIZER)};
        ObjHolder resultHolder;
        ObjHeader* obj = mm::AllocateObject(&thread, type.typeInfo(), resultHolder.slot());

        EXPECT_TRUE(HasFinalizers(obj));
        EXPECT_CALL(finalizerHook(), Call(obj));
        RunFinalizers(obj);
    });
}

TEST_F(FinalizerHooksTest, TypeWithoutFinalizerHookWithExtra) {
    RunInNewThread([this](mm::ThreadData& thread) {
        test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
        test_support::Object<EmptyPayload> object(type.typeInfo());
        ObjHolder resultHolder;
        ObjHeader* obj = mm::AllocateObject(&thread, type.typeInfo(), resultHolder.slot());
        ObjHeader::createMetaObject(obj);

        EXPECT_TRUE(HasFinalizers(obj));
        EXPECT_CALL(finalizerHook(), Call(_)).Times(0);
        RunFinalizers(obj);
    });
}

TEST_F(FinalizerHooksTest, TypeWithoutFinalizerHookWithoutExtra) {
    RunInNewThread([this](mm::ThreadData& thread) {
        test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
        test_support::Object<EmptyPayload> object(type.typeInfo());
        ObjHolder resultHolder;
        ObjHeader* obj = mm::AllocateObject(&thread, type.typeInfo(), resultHolder.slot());

        EXPECT_FALSE(HasFinalizers(obj));
        EXPECT_CALL(finalizerHook(), Call(_)).Times(0);
        RunFinalizers(obj);
    });
}
