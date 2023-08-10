/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "KString.h"

#include <cstdlib>

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "Natives.h"

using namespace kotlin;

void checkContentsEquality(ArrayHeader* actual, const char16_t* expected) {
    EXPECT_THAT(actual->count_, std::char_traits<char16_t>::length(expected));
    for (size_t i=0; i<actual->count_; i++) {
        EXPECT_THAT(*CharArrayAddressOfElementAt(actual, i), expected[i]);
    }
    EXPECT_THAT(actual->obj()->permanent(), true);
}

TEST(KStringTest, CreatePermanentStringFromCString_ascii) {
    const char* ascii = "Ascii";
    EXPECT_THAT(strlen(ascii), 5);
    const char16_t* expected = u"Ascii";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 5);

    auto actual = CreatePermanentStringFromCString(ascii)->array();
    checkContentsEquality(actual, expected);
    FreePermanentStringForTests(actual);  // to prevent Address Sanitizer test failures, permanently allocated strings must be deallocated before test end
}

TEST(KStringTest, CreatePermanentStringFromCString_misc) {
    const char* non_ascii = "-£öü²ソニーΣℜ∫♣‰€";
    EXPECT_THAT(strlen(non_ascii), 35);
    const char16_t* expected = u"-£öü²ソニーΣℜ∫♣‰€";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 14);

    auto actual = CreatePermanentStringFromCString(non_ascii)->array();
    checkContentsEquality(actual, expected);
    FreePermanentStringForTests(actual);  // to prevent Address Sanitizer test failures, permanently allocated strings must be deallocated before test end
}

TEST(KStringTest, CreatePermanentStringFromCString_surrogates) {
    const char* surrogates = "😃𓄀🌀🐀𝜀";
    EXPECT_THAT(strlen(surrogates), 20);
    const char16_t* expected = u"😃𓄀🌀🐀𝜀";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 10);

    auto actual = CreatePermanentStringFromCString(surrogates)->array();
    checkContentsEquality(actual, expected);
    FreePermanentStringForTests(actual);  // to prevent Address Sanitizer test failures, permanently allocated strings must be deallocated before test end
}

TEST(KStringTest, CreatePermanentStringFromCString_empty) {
    const char* empty = "";
    EXPECT_THAT(strlen(empty), 0);
    const char16_t* expected = u"";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 0);

    auto actual = CreatePermanentStringFromCString(empty)->array();
    checkContentsEquality(actual, expected);
    FreePermanentStringForTests(actual);  // to prevent Address Sanitizer test failures, permanently allocated strings must be deallocated before test end
}

TEST(KStringTest, CreatePermanentStringFromCString_impossible) {
    const char* impossible = "\xff";
    EXPECT_THAT(strlen(impossible), 1);
    const char16_t* expected = u"\xfffd";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 1);

    auto actual = CreatePermanentStringFromCString(impossible)->array();
    checkContentsEquality(actual, expected);
    FreePermanentStringForTests(actual);  // to prevent Address Sanitizer test failures, permanently allocated strings must be deallocated before test end
}

TEST(KStringTest, CreatePermanentStringFromCString_overlong) {
    const char* overlong = "\xc0\xaf";
    EXPECT_THAT(strlen(overlong), 2);
    const char16_t* expected = u"\xfffd\xfffd";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 2);

    auto actual = CreatePermanentStringFromCString(overlong)->array();
    checkContentsEquality(actual, expected);
    FreePermanentStringForTests(actual);  // to prevent Address Sanitizer test failures, permanently allocated strings must be deallocated before test end
}
