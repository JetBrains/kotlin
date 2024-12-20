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

void checkContentsEquality(const char* input, const char16_t* expected) {
    auto actual = CreatePermanentStringFromCString(input);
    auto header = StringHeader::of(actual);
    size_t size = header->size() / sizeof(char16_t);
    EXPECT_THAT(size, std::char_traits<char16_t>::length(expected));
    const char16_t* data = reinterpret_cast<const char16_t*>(header->data());
    for (size_t i=0; i<size; i++) {
        EXPECT_THAT(data[i], expected[i]);
    }
    EXPECT_THAT(actual->permanent(), true);
    FreePermanentStringForTests(actual);  // to prevent Address Sanitizer test failures, permanently allocated strings must be deallocated before test end
}

TEST(KStringTest, CreatePermanentStringFromCString_ascii) {
    const char* ascii = "Ascii";
    EXPECT_THAT(strlen(ascii), 5);
    const char16_t* expected = u"Ascii";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 5);
    checkContentsEquality(ascii, expected);
}

TEST(KStringTest, CreatePermanentStringFromCString_misc) {
    const char* non_ascii = "-£öü²ソニーΣℜ∫♣‰€";
    EXPECT_THAT(strlen(non_ascii), 35);
    const char16_t* expected = u"-£öü²ソニーΣℜ∫♣‰€";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 14);
    checkContentsEquality(non_ascii, expected);
}

TEST(KStringTest, CreatePermanentStringFromCString_surrogates) {
    const char* surrogates = "😃𓄀🌀🐀𝜀";
    EXPECT_THAT(strlen(surrogates), 20);
    const char16_t* expected = u"😃𓄀🌀🐀𝜀";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 10);
    checkContentsEquality(surrogates, expected);
}

TEST(KStringTest, CreatePermanentStringFromCString_empty) {
    const char* empty = "";
    EXPECT_THAT(strlen(empty), 0);
    const char16_t* expected = u"";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 0);
    checkContentsEquality(empty, expected);
}

TEST(KStringTest, CreatePermanentStringFromCString_impossible) {
    const char* impossible = "\xff";
    EXPECT_THAT(strlen(impossible), 1);
    const char16_t* expected = u"\xfffd";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 1);
    checkContentsEquality(impossible, expected);
}

TEST(KStringTest, CreatePermanentStringFromCString_overlong) {
    const char* overlong = "\xc0\xaf";
    EXPECT_THAT(strlen(overlong), 2);
    const char16_t* expected = u"\xfffd\xfffd";
    EXPECT_THAT(std::char_traits<char16_t>::length(expected), 2);
    checkContentsEquality(overlong, expected);
}
