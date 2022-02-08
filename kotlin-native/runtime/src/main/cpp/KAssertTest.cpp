/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "KAssert.h"

#include "gtest/gtest.h"

TEST(TODODeathTest, EmptyTODO) {
    EXPECT_DEATH({
        TODO();
    }, "KAssertTest.cpp:12: runtime assert: Unimplemented");
}

TEST(TODODeathTest, TODOWithMessage) {
    EXPECT_DEATH({
        TODO("Nope");
    }, "KAssertTest.cpp:18: runtime assert: Nope");
}
