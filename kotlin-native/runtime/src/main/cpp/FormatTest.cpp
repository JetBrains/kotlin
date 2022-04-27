/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Format.h"

#include <array>

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "std_support/Span.hpp"

using namespace kotlin;

TEST(FormatTest, FormatToSpan_String) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(buffer, "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('a', 'b', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), buffer.size() - 2);
}

TEST(FormatTest, FormatToSpan_StringFormat) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(buffer, "%s", "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('a', 'b', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), buffer.size() - 2);
}

TEST(FormatTest, FormatToSpan_IntFormat) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(buffer, "%d", 42);
    EXPECT_THAT(buffer, testing::ElementsAre('4', '2', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), buffer.size() - 2);
}

TEST(FormatTest, FormatToSpan_String_Size0) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(0), "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('\1', '\1', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data());
    EXPECT_THAT(result.size(), 0);
}

TEST(FormatTest, FormatToSpan_StringFormat_Size0) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(0), "%s", "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('\1', '\1', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data());
    EXPECT_THAT(result.size(), 0);
}

TEST(FormatTest, FormatToSpan_IntFormat_Size0) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(0), "%d", 42);
    EXPECT_THAT(buffer, testing::ElementsAre('\1', '\1', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data());
    EXPECT_THAT(result.size(), 0);
}

TEST(FormatTest, FormatToSpan_String_Size1) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(1), "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('\0', '\1', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data());
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_StringFormat_Size1) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(1), "%s", "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('\0', '\1', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data());
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_IntFormat_Size1) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(1), "%d", 42);
    EXPECT_THAT(buffer, testing::ElementsAre('\0', '\1', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data());
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_String_Size2) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(2), "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('a', '\0', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 1);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_StringFormat_Size2) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(2), "%s", "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('a', '\0', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 1);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_IntFormat_Size2) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(2), "%d", 42);
    EXPECT_THAT(buffer, testing::ElementsAre('4', '\0', '\1', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 1);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_String_Size3) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(3), "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('a', 'b', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_StringFormat_Size3) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(3), "%s", "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('a', 'b', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_IntFormat_Size3) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(3), "%d", 42);
    EXPECT_THAT(buffer, testing::ElementsAre('4', '2', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_String_Size4) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(4), "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('a', 'b', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), 2);
}

TEST(FormatTest, FormatToSpan_StringFormat_Size4) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(4), "%s", "ab");
    EXPECT_THAT(buffer, testing::ElementsAre('a', 'b', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), 2);
}

TEST(FormatTest, FormatToSpan_IntFormat_Size4) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatToSpan(std_support::span(buffer).first(4), "%d", 42);
    EXPECT_THAT(buffer, testing::ElementsAre('4', '2', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), 2);
}

TEST(FormatTest, FormatToSpan_Sequence) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    std_support::span<char> result(buffer);
    result = FormatToSpan(result, "a");
    result = FormatToSpan(result, "%s", "b");
    result = FormatToSpan(result, "%d", 4);
    EXPECT_THAT(buffer, testing::ElementsAre('a', 'b', '4', '\0', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 3);
    EXPECT_THAT(result.size(), 2);
}

namespace {

std_support::span<char> FormatNesting(std_support::span<char> buffer) {
    buffer = FormatToSpan(buffer, "(");
    auto nested = buffer.first(buffer.size() - 1); // Leave a space for `)`
    nested = FormatToSpan(nested, "a ");
    nested = FormatToSpan(nested, "b ");
    buffer = buffer.subspan(nested.data() - buffer.data());
    return FormatToSpan(buffer, ")");
}

} // namespace

TEST(FormatTest, FormatToSpan_Nesting_Size1) {
    std::array buffer{'\1'};
    auto result = FormatNesting(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('\0'));
    EXPECT_THAT(result.data(), buffer.data());
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_Nesting_Size2) {
    std::array buffer{'\1', '\1'};
    auto result = FormatNesting(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 1);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_Nesting_Size3) {
    std::array buffer{'\1', '\1', '\1'};
    auto result = FormatNesting(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_Nesting_Size4) {
    std::array buffer{'\1', '\1', '\1', '\1'};
    auto result = FormatNesting(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 3);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_Nesting_Size5) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatNesting(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ' ', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 4);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_Nesting_Size6) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1', '\1'};
    auto result = FormatNesting(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ' ', 'b', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 5);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_Nesting_Size7) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1', '\1', '\1'};
    auto result = FormatNesting(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ' ', 'b', ' ', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 6);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_Nesting_Size8) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1', '\1', '\1', '\1'};
    auto result = FormatNesting(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ' ', 'b', ' ', ')', '\0', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 6);
    EXPECT_THAT(result.size(), 2);
}

namespace {

std_support::span<char> FormatNestingWithBacktrack(std_support::span<char> buffer) {
    buffer = FormatToSpan(buffer, "(");
    auto nested = buffer;
    nested = FormatToSpan(nested, "a ");
    nested = FormatToSpan(nested, "b ");
    if (nested.data() != buffer.data()) {
        // If we managed to format something in nested, replace the last formatted character with ')'
        buffer = buffer.subspan(nested.data() - buffer.data() - 1);
    }
    return FormatToSpan(buffer, ")");
}

} // namespace

TEST(FormatTest, FormatToSpan_NestingWithBacktrack_Size1) {
    std::array buffer{'\1'};
    auto result = FormatNestingWithBacktrack(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('\0'));
    EXPECT_THAT(result.data(), buffer.data());
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_NestingWithBacktrack_Size2) {
    std::array buffer{'\1', '\1'};
    auto result = FormatNestingWithBacktrack(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 1);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_NestingWithBacktrack_Size3) {
    std::array buffer{'\1', '\1', '\1'};
    auto result = FormatNestingWithBacktrack(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 2);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_NestingWithBacktrack_Size4) {
    std::array buffer{'\1', '\1', '\1', '\1'};
    auto result = FormatNestingWithBacktrack(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 3);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_NestingWithBacktrack_Size5) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1'};
    auto result = FormatNestingWithBacktrack(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ' ', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 4);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_NestingWithBacktrack_Size6) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1', '\1'};
    auto result = FormatNestingWithBacktrack(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ' ', 'b', ')', '\0'));
    EXPECT_THAT(result.data(), buffer.data() + 5);
    EXPECT_THAT(result.size(), 1);
}

TEST(FormatTest, FormatToSpan_NestingWithBacktrack_Size7) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1', '\1', '\1'};
    auto result = FormatNestingWithBacktrack(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ' ', 'b', ')', '\0', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 5);
    EXPECT_THAT(result.size(), 2);
}

TEST(FormatTest, FormatToSpan_NestingWithBacktrack_Size8) {
    std::array buffer{'\1', '\1', '\1', '\1', '\1', '\1', '\1', '\1'};
    auto result = FormatNestingWithBacktrack(buffer);
    EXPECT_THAT(buffer, testing::ElementsAre('(', 'a', ' ', 'b', ')', '\0', '\1', '\1'));
    EXPECT_THAT(result.data(), buffer.data() + 5);
    EXPECT_THAT(result.size(), 3);
}
