/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FreezeHooks.hpp"

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "FreezeHooksTestSupport.hpp"
#include "Memory.h"
#include "ObjectTestSupport.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

struct EmptyPayload {
    using Field = ObjHeader* EmptyPayload::*;
    static constexpr std::array<Field, 0> kFields{};
};

class FreezeHooksTest : public testing::Test {
public:
    testing::MockFunction<void(ObjHeader*)>& freezeHook() { return freezeHooks_.freezeHook(); }

private:
    FreezeHooksTestSupport freezeHooks_;
};

} // namespace

TEST_F(FreezeHooksTest, TypeWithFreezeHook) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>().addFlag(TF_HAS_FREEZE_HOOK)};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    ObjHeader* obj = object.header();
    EXPECT_CALL(freezeHook(), Call(obj));
    RunFreezeHooks(obj);
}

TEST_F(FreezeHooksTest, TypeWithoutFreezeHook) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    ObjHeader* obj = object.header();
    EXPECT_CALL(freezeHook(), Call(_)).Times(0);
    RunFreezeHooks(obj);
}
