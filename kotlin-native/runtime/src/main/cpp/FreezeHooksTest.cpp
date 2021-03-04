/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FreezeHooks.hpp"

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "FreezeHooksTestSupport.hpp"
#include "Memory.h"

using namespace kotlin;

using ::testing::_;

namespace {

class FreezeHooksTest : public testing::Test {
public:
    testing::MockFunction<void(ObjHeader*)>& freezeHook() { return freezeHooks_.freezeHook(); }

private:
    FreezeHooksTestSupport freezeHooks_;
};

} // namespace

TEST_F(FreezeHooksTest, TypeWithFreezeHook) {
    TypeInfo type;
    type.typeInfo_ = &type;
    type.flags_ |= TF_HAS_FREEZE_HOOK;
    ObjHeader obj = {&type};
    EXPECT_CALL(freezeHook(), Call(&obj));
    RunFreezeHooks(&obj);
}

TEST_F(FreezeHooksTest, TypeWithoutFreezeHook) {
    TypeInfo type;
    type.typeInfo_ = &type;
    type.flags_ &= ~TF_HAS_FREEZE_HOOK;
    ObjHeader obj = {&type};
    EXPECT_CALL(freezeHook(), Call(_)).Times(0);
    RunFreezeHooks(&obj);
}
