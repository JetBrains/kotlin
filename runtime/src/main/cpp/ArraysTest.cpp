/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gtest/gtest.h"

#include "Porting.h"

TEST(ArraysTest, GoodTest) {
    konan::consolePrintf("I'm a good test\n");
    EXPECT_EQ(true, true);
}

TEST(ArraysTest, BadTest) {
    konan::consolePrintf("I'm a bad test\n");
    EXPECT_EQ(true, false);
}
