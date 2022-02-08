/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Alignment.hpp"

#include <cstddef>
#include <tuple>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Types.h"

using namespace kotlin;

namespace {

template <typename... Args>
class NamedTestWithParam : public testing::TestWithParam<std::tuple<const char*, Args...>> {
public:
    using Param = std::tuple<const char*, Args...>;

    static std::string Print(const testing::TestParamInfo<Param>& info) { return std::string(std::get<0>(info.param)); }

    template <size_t I>
    static const typename std::tuple_element<I + 1, Param>::type& Get() {
        const auto& param = testing::TestWithParam<Param>::GetParam();
        return std::get<I + 1>(param);
    }
};

#define INSTANTIATE_NAMED_TEST(testName, ...) INSTANTIATE_TEST_SUITE_P(, testName, testing::Values(__VA_ARGS__), &testName::Print)

} // namespace

using IsValidAlignmentTest = NamedTestWithParam<size_t, bool>;

TEST_P(IsValidAlignmentTest, Test) {
    const auto& alignment = Get<0>();
    const auto& expected = Get<1>();
    EXPECT_THAT(IsValidAlignment(alignment), expected);
}

INSTANTIATE_NAMED_TEST(
        IsValidAlignmentTest,
        std::make_tuple("0", 0, false),
        std::make_tuple("1", 1, true),
        std::make_tuple("2", 2, true),
        std::make_tuple("3", 3, false),
        std::make_tuple("4", 4, true),
        std::make_tuple("5", 5, false),
        std::make_tuple("6", 6, false),
        std::make_tuple("7", 7, false),
        std::make_tuple("8", 8, true),
        std::make_tuple("9", 9, false),
        std::make_tuple("10", 10, false),
        std::make_tuple("11", 11, false),
        std::make_tuple("12", 12, false),
        std::make_tuple("13", 13, false),
        std::make_tuple("14", 14, false),
        std::make_tuple("15", 15, false),
        std::make_tuple("16", 16, true),
        std::make_tuple("int", alignof(int), true),
        std::make_tuple("ptr", alignof(void*), true),
        std::make_tuple("max", alignof(std::max_align_t), true));

using IsAlignedSizeTest = NamedTestWithParam<size_t, size_t, bool>;

TEST_P(IsAlignedSizeTest, Test) {
    const auto& size = Get<0>();
    const auto& alignment = Get<1>();
    const auto& expected = Get<2>();
    EXPECT_THAT(IsAligned(size, alignment), expected);
}

INSTANTIATE_NAMED_TEST(
        IsAlignedSizeTest,
        std::make_tuple("1_1", 1, 1, true),
        std::make_tuple("2_1", 2, 1, true),
        std::make_tuple("3_1", 3, 1, true),
        std::make_tuple("4_1", 4, 1, true),
        std::make_tuple("1_2", 1, 2, false),
        std::make_tuple("2_2", 2, 2, true),
        std::make_tuple("3_2", 3, 2, false),
        std::make_tuple("4_2", 4, 2, true));

using IsAlignedPointerTest = NamedTestWithParam<uintptr_t, size_t, bool>;

TEST_P(IsAlignedPointerTest, Test) {
    const auto& ptr = Get<0>();
    const auto& alignment = Get<1>();
    const auto& expected = Get<2>();
    EXPECT_THAT(IsAligned(reinterpret_cast<void*>(ptr), alignment), expected);
}

INSTANTIATE_NAMED_TEST(
        IsAlignedPointerTest,
        std::make_tuple("0_1", 0, 1, true),
        std::make_tuple("1_1", 1, 1, true),
        std::make_tuple("2_1", 2, 1, true),
        std::make_tuple("3_1", 3, 1, true),
        std::make_tuple("4_1", 4, 1, true),
        std::make_tuple("0_2", 0, 2, true),
        std::make_tuple("1_2", 1, 2, false),
        std::make_tuple("2_2", 2, 2, true),
        std::make_tuple("3_2", 3, 2, false),
        std::make_tuple("4_2", 4, 2, true));

using AlignUpSizeTest = NamedTestWithParam<size_t, size_t, size_t>;

TEST_P(AlignUpSizeTest, Test) {
    const auto& size = Get<0>();
    const auto& alignment = Get<1>();
    const auto& expected = Get<2>();
    EXPECT_THAT(AlignUp(size, alignment), expected);
}

INSTANTIATE_NAMED_TEST(
        AlignUpSizeTest,
        std::make_tuple("1_1", 1, 1, 1),
        std::make_tuple("2_1", 2, 1, 2),
        std::make_tuple("3_1", 3, 1, 3),
        std::make_tuple("4_1", 4, 1, 4),
        std::make_tuple("1_2", 1, 2, 2),
        std::make_tuple("2_2", 2, 2, 2),
        std::make_tuple("3_2", 3, 2, 4),
        std::make_tuple("4_2", 4, 2, 4));

using AlignUpPointerTest = NamedTestWithParam<uintptr_t, size_t, uintptr_t>;

TEST_P(AlignUpPointerTest, Test) {
    const auto& ptr = Get<0>();
    const auto& alignment = Get<1>();
    const auto& expected = Get<2>();
    EXPECT_THAT(AlignUp(reinterpret_cast<void*>(ptr), alignment), reinterpret_cast<void*>(expected));
}

INSTANTIATE_NAMED_TEST(
        AlignUpPointerTest,
        std::make_tuple("0_1", 0, 1, 0),
        std::make_tuple("1_1", 1, 1, 1),
        std::make_tuple("2_1", 2, 1, 2),
        std::make_tuple("3_1", 3, 1, 3),
        std::make_tuple("4_1", 4, 1, 4),
        std::make_tuple("0_2", 0, 2, 0),
        std::make_tuple("1_2", 1, 2, 2),
        std::make_tuple("2_2", 2, 2, 2),
        std::make_tuple("3_2", 3, 2, 4),
        std::make_tuple("4_2", 4, 2, 4));

TEST(AlignmentTest, ObjectAlignment) {
    static_assert(IsValidAlignment(kObjectAlignment), "kObjectAlignment must be a valid alignment");
    static_assert(kObjectAlignment % alignof(KLong) == 0, "");
    static_assert(kObjectAlignment % alignof(KDouble) == 0, "");
}
