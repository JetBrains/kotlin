/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "KAssert.h"

#include "gtest/gtest.h"

TEST(AssertDeathTest, EmptyTODO) {
    EXPECT_DEATH({
        TODO();
    }, "KAssertTest.cpp:12: runtime assert: Unimplemented");
}

TEST(AssertDeathTest, TODOWithMessage) {
    EXPECT_DEATH({
        TODO("Nope");
    }, "KAssertTest.cpp:18: runtime assert: Nope");
}

TEST(AssertDeathTest, StackTraceInAssert) {
    EXPECT_DEATH({
        RuntimeAssert(false, "Crash with a stacktrace");
    }, testing::ContainsRegex(
            "KAssertTest.cpp:24: runtime assert: Crash with a stacktrace\n"
            ".*RuntimeAssertFailed.*\n"
            ".*StackTraceInAssert.*"
    ));
}

TEST(AssertDeathTest, AssertWithFormattedMessage) {
    EXPECT_DEATH({
        RuntimeAssert(false, "Message: %d, %3.1f, %x, %s", 5, 0.5, 0xFF, "foo");
    }, "runtime assert: Message: 5, 0.5, ff, foo");
}

TEST(AssertDeathTest, TODOWithFormattedMessage) {
    EXPECT_DEATH({
        TODO("Message: %d, %3.1f, %x, %s", 5, 0.5, 0xFF, "foo");
    }, "runtime assert: Message: 5, 0.5, ff, foo");
}