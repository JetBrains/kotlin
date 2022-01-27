/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Saturating.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

// What follows are some sanity tests on saturating usage, then an extensive test of allowed conversions
// and resulting types of sanity<T> operations, then an extensive test of saturating_* operations using
// values MIN, MIN + 1, -1, 0, 1, MAX - 1, MAX.

TEST(SaturatingSanityTest, SaturatingCast) {
    EXPECT_THAT(saturating_cast<int8_t>(129), 127);
    EXPECT_THAT(saturating_cast<int8_t>(-129), -128);
    EXPECT_THAT(saturating_cast<uint32_t>(-17), 0);
}

TEST(SaturatingSanityTest, SaturatingAdd) {
    EXPECT_THAT(saturating_add(4, 1), 5);
    EXPECT_THAT(saturating_add(1, -2), -1);
    EXPECT_THAT(saturating_add(uint8_t(127), uint16_t(5)), 132);
    EXPECT_THAT(saturating_add(int8_t(-127), int16_t(-5)), -132);
}

TEST(SaturatingSanityTest, SaturatingSub) {
    EXPECT_THAT(saturating_sub(4, 1), 3);
    EXPECT_THAT(saturating_sub(1, -2), 3);
    EXPECT_THAT(saturating_sub(uint16_t(5), uint8_t(127)), 0);
    EXPECT_THAT(saturating_sub(uint16_t(129), uint8_t(127)), 2);
    EXPECT_THAT(saturating_sub(int8_t(-127), int16_t(5)), -132);
}

TEST(SaturatingSanityTest, SaturatingMul) {
    EXPECT_THAT(saturating_mul(3, 5), 15);
    EXPECT_THAT(saturating_mul(-3, 5), -15);
    EXPECT_THAT(saturating_mul(uint8_t(127), uint16_t(2)), 254);
    EXPECT_THAT(saturating_mul(uint8_t(127), uint16_t(1000)), 65535);
    EXPECT_THAT(saturating_mul(int8_t(-127), int16_t(2)), -254);
    EXPECT_THAT(saturating_mul(int8_t(-127), int16_t(1000)), -32768);
    EXPECT_THAT(saturating_mul(int8_t(-127), int16_t(-2)), 254);
    EXPECT_THAT(saturating_mul(int8_t(-127), int16_t(-1000)), 32767);
}

TEST(SaturatingSanityTest, SaturatingComparison) {
    EXPECT_TRUE(saturating(1) == saturating(1));
    EXPECT_FALSE(saturating(1) == saturating(2));
    EXPECT_FALSE(saturating(2) == saturating(1));

    EXPECT_FALSE(saturating(1) != saturating(1));
    EXPECT_TRUE(saturating(1) != saturating(2));
    EXPECT_TRUE(saturating(2) != saturating(1));

    EXPECT_FALSE(saturating(1) < saturating(1));
    EXPECT_TRUE(saturating(1) < saturating(2));
    EXPECT_FALSE(saturating(2) < saturating(1));

    EXPECT_TRUE(saturating(1) <= saturating(1));
    EXPECT_TRUE(saturating(1) <= saturating(2));
    EXPECT_FALSE(saturating(2) <= saturating(1));

    EXPECT_FALSE(saturating(1) > saturating(1));
    EXPECT_FALSE(saturating(1) > saturating(2));
    EXPECT_TRUE(saturating(2) > saturating(1));

    EXPECT_TRUE(saturating(1) >= saturating(1));
    EXPECT_FALSE(saturating(1) >= saturating(2));
    EXPECT_TRUE(saturating(2) >= saturating(1));
}

TEST(SaturatingSanityTest, SaturatingOutOfPlace) {
    EXPECT_THAT(saturating(int8_t(2)) + saturating(int8_t(3)), saturating(int8_t(5)));
    EXPECT_THAT(saturating(int8_t(2)) + saturating(int16_t(3)), saturating(int16_t(5)));
    EXPECT_THAT(saturating(int8_t(2)) + saturating(int16_t(1000)), saturating(int16_t(1002)));
    EXPECT_THAT(saturating(int8_t(2)) + saturating(int16_t(32767)), saturating(int16_t(32767)));

    EXPECT_THAT(saturating(int8_t(2)) - saturating(int8_t(3)), saturating(int8_t(-1)));
    EXPECT_THAT(saturating(int8_t(2)) - saturating(int16_t(3)), saturating(int16_t(-1)));
    EXPECT_THAT(saturating(int8_t(2)) - saturating(int16_t(1000)), saturating(int16_t(-998)));
    EXPECT_THAT(saturating(int8_t(2)) - saturating(int16_t(-32768)), saturating(int16_t(32767)));

    EXPECT_THAT(saturating(int8_t(2)) * saturating(int8_t(3)), saturating(int8_t(6)));
    EXPECT_THAT(saturating(int8_t(2)) * saturating(int16_t(3)), saturating(int16_t(6)));
    EXPECT_THAT(saturating(int8_t(2)) * saturating(int16_t(1000)), saturating(int16_t(2000)));
    EXPECT_THAT(saturating(int8_t(2)) * saturating(int16_t(20000)), saturating(int16_t(32767)));
}

TEST(SaturatingSanityTest, SaturatingInPlace) {
    saturating value(int8_t(13));
    EXPECT_THAT(int8_t(value), 13);
    value += saturating(int8_t(15));
    EXPECT_THAT(int8_t(value), 28);
    value += saturating(int16_t(1000));
    EXPECT_THAT(int8_t(value), 127);
    value -= saturating(int16_t(200));
    EXPECT_THAT(int8_t(value), -73);
    value -= saturating(int8_t(100));
    EXPECT_THAT(int8_t(value), -128);
    value *= saturating(0);
    EXPECT_THAT(int8_t(value), 0);
    value += saturating(2);
    EXPECT_THAT(int8_t(value), 2);
    value *= saturating(int8_t(10));
    EXPECT_THAT(int8_t(value), 20);
    value *= saturating(int16_t(1000));
    EXPECT_THAT(int8_t(value), 127);
    value *= saturating(int8_t(-1));
    EXPECT_THAT(int8_t(value), -127);
    value *= saturating(int16_t(1000));
    EXPECT_THAT(int8_t(value), -128);
}

namespace {

template <typename From, typename Into>
inline constexpr bool is_explicitly_convertible_v = !std::is_convertible_v<From, Into> && std::is_constructible_v<Into, From>;

} // namespace

TEST(SaturatingTest, ValueConstructor) {
    static_assert(is_explicitly_convertible_v<int8_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int8_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<int8_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<int8_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int8_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int8_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int8_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int8_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int16_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int16_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<int16_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<int16_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int16_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int16_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int16_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int16_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int32_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int32_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<int32_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<int32_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int32_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int32_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int32_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int32_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int64_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int64_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<int64_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<int64_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int64_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int64_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int64_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int64_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint8_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint16_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint32_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint64_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, uint_sat64_t>);
}

TEST(SaturatingTest, ValueCast) {
    static_assert(is_explicitly_convertible_v<int_sat8_t, int8_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, int16_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, int32_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, int64_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint8_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint16_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint32_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint64_t>);

    static_assert(is_explicitly_convertible_v<int_sat16_t, int8_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, int16_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, int32_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, int64_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint8_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint16_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint32_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint64_t>);

    static_assert(is_explicitly_convertible_v<int_sat32_t, int8_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, int16_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, int32_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, int64_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, uint8_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, uint16_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, uint32_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, uint64_t>);

    static_assert(is_explicitly_convertible_v<int_sat64_t, int8_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, int16_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, int32_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, int64_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, uint8_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, uint16_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, uint32_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, uint64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat8_t, int8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, int16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, int32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, int64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, uint8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, uint16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, uint32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, uint64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat16_t, int8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, uint8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, uint16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, uint32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, uint64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat32_t, int8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat64_t, int8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, int16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, int32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, int64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, uint8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, uint16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, uint32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, uint64_t>);
}

TEST(SaturatingTest, SaturatingConstructor) {
    static_assert(std::is_convertible_v<int_sat8_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int_sat16_t, int_sat8_t>);
    static_assert(std::is_convertible_v<int_sat16_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int_sat32_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, int_sat16_t>);
    static_assert(std::is_convertible_v<int_sat32_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int_sat64_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, int_sat32_t>);
    static_assert(std::is_convertible_v<int_sat64_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat64_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat8_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, int_sat64_t>);
    static_assert(std::is_convertible_v<uint_sat8_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat8_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat16_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, uint_sat8_t>);
    static_assert(std::is_convertible_v<uint_sat16_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat32_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint_sat16_t>);
    static_assert(std::is_convertible_v<uint_sat32_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat64_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat64_t, uint_sat32_t>);
    static_assert(std::is_convertible_v<uint_sat64_t, uint_sat64_t>);
}

TEST(SaturatingTest, Addition) {
    static_assert(std::is_same_v<decltype(int_sat8_t(0) + int_sat8_t(0)), int_sat8_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) + int_sat16_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) + int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) + int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat16_t(0) + int_sat8_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) + int_sat16_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) + int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) + int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat32_t(0) + int_sat8_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) + int_sat16_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) + int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) + int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat64_t(0) + int_sat8_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) + int_sat16_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) + int_sat32_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) + int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat8_t(0) + uint_sat8_t(0)), uint_sat8_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) + uint_sat16_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) + uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) + uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat16_t(0) + uint_sat8_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) + uint_sat16_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) + uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) + uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat32_t(0) + uint_sat8_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) + uint_sat16_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) + uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) + uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat64_t(0) + uint_sat8_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) + uint_sat16_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) + uint_sat32_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) + uint_sat64_t(0)), uint_sat64_t>);
}

TEST(SaturatingTest, Subtraction) {
    static_assert(std::is_same_v<decltype(int_sat8_t(0) - int_sat8_t(0)), int_sat8_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) - int_sat16_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) - int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) - int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat16_t(0) - int_sat8_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) - int_sat16_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) - int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) - int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat32_t(0) - int_sat8_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) - int_sat16_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) - int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) - int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat64_t(0) - int_sat8_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) - int_sat16_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) - int_sat32_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) - int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat8_t(0) - uint_sat8_t(0)), uint_sat8_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) - uint_sat16_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) - uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) - uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat16_t(0) - uint_sat8_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) - uint_sat16_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) - uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) - uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat32_t(0) - uint_sat8_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) - uint_sat16_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) - uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) - uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat64_t(0) - uint_sat8_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) - uint_sat16_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) - uint_sat32_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) - uint_sat64_t(0)), uint_sat64_t>);
}

TEST(SaturatingTest, Multiplication) {
    static_assert(std::is_same_v<decltype(int_sat8_t(0) * int_sat8_t(0)), int_sat8_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) * int_sat16_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) * int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) * int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat16_t(0) * int_sat8_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) * int_sat16_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) * int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) * int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat32_t(0) * int_sat8_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) * int_sat16_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) * int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) * int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat64_t(0) * int_sat8_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) * int_sat16_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) * int_sat32_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) * int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat8_t(0) * uint_sat8_t(0)), uint_sat8_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) * uint_sat16_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) * uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) * uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat16_t(0) * uint_sat8_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) * uint_sat16_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) * uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) * uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat32_t(0) * uint_sat8_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) * uint_sat16_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) * uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) * uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat64_t(0) * uint_sat8_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) * uint_sat16_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) * uint_sat32_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) * uint_sat64_t(0)), uint_sat64_t>);
}

TEST(SaturatingTest, CommonType) {
    static_assert(std::is_same_v<std::common_type_t<int_sat8_t, int_sat8_t>, int_sat8_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat8_t, int_sat16_t>, int_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat8_t, int_sat32_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat8_t, int_sat64_t>, int_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<int_sat16_t, int_sat8_t>, int_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat16_t, int_sat16_t>, int_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat16_t, int_sat32_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat16_t, int_sat64_t>, int_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<int_sat32_t, int_sat8_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat32_t, int_sat16_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat32_t, int_sat32_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat32_t, int_sat64_t>, int_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<int_sat64_t, int_sat8_t>, int_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat64_t, int_sat16_t>, int_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat64_t, int_sat32_t>, int_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat64_t, int_sat64_t>, int_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<uint_sat8_t, uint_sat8_t>, uint_sat8_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat8_t, uint_sat16_t>, uint_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat8_t, uint_sat32_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat8_t, uint_sat64_t>, uint_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<uint_sat16_t, uint_sat8_t>, uint_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat16_t, uint_sat16_t>, uint_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat16_t, uint_sat32_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat16_t, uint_sat64_t>, uint_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<uint_sat32_t, uint_sat8_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat32_t, uint_sat16_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat32_t, uint_sat32_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat32_t, uint_sat64_t>, uint_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<uint_sat64_t, uint_sat8_t>, uint_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat64_t, uint_sat16_t>, uint_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat64_t, uint_sat32_t>, uint_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat64_t, uint_sat64_t>, uint_sat64_t>);
}

TEST(SaturatingTest, CommonTypeWithIntegral) {
    static_assert(std::is_same_v<std::common_type_t<int_sat8_t, int8_t>, int_sat8_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat8_t, int16_t>, int_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat8_t, int32_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat8_t, int64_t>, int_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<int_sat16_t, int8_t>, int_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat16_t, int16_t>, int_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat16_t, int32_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat16_t, int64_t>, int_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<int_sat32_t, int8_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat32_t, int16_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat32_t, int32_t>, int_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat32_t, int64_t>, int_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<int_sat64_t, int8_t>, int_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat64_t, int16_t>, int_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat64_t, int32_t>, int_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<int_sat64_t, int64_t>, int_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<uint_sat8_t, uint8_t>, uint_sat8_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat8_t, uint16_t>, uint_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat8_t, uint32_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat8_t, uint64_t>, uint_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<uint_sat16_t, uint8_t>, uint_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat16_t, uint16_t>, uint_sat16_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat16_t, uint32_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat16_t, uint64_t>, uint_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<uint_sat32_t, uint8_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat32_t, uint16_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat32_t, uint32_t>, uint_sat32_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat32_t, uint64_t>, uint_sat64_t>);

    static_assert(std::is_same_v<std::common_type_t<uint_sat64_t, uint8_t>, uint_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat64_t, uint16_t>, uint_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat64_t, uint32_t>, uint_sat64_t>);
    static_assert(std::is_same_v<std::common_type_t<uint_sat64_t, uint64_t>, uint_sat64_t>);
}

namespace {

template <typename T>
constexpr inline T min_v = std::numeric_limits<T>::min();

template <typename T>
constexpr inline T minr_v = min_v<T> + T(1);

template <typename T>
constexpr inline T minrr_v = min_v<T> + T(2);

template <typename T>
constexpr inline T max_v = std::numeric_limits<T>::max();

template <typename T>
constexpr inline T maxl_v = max_v<T> - T(1);

template <typename T>
constexpr inline T maxll_v = max_v<T> - T(2);

template <typename T>
constexpr inline T zero_v = T(0);

template <typename T>
constexpr inline T zeror_v = T(1);

template <typename T>
constexpr inline T zerorr_v = T(2);

template <typename T>
constexpr inline T zerol_v = [] {
    static_assert(std::is_signed_v<T>, "Only available for signed types");
    return T(-1);
}();

template <typename T>
constexpr inline T zeroll_v = [] {
    static_assert(std::is_signed_v<T>, "Only available for signed types");
    return T(-2);
}();

#define EXPECT_SATURATING_CAST(IntoType, FromValue, IntoValue) EXPECT_THAT(saturating_cast<IntoType>(FromValue), IntoType(IntoValue))

} // namespace

TEST(SaturatingCastTest, Into_8) {
    EXPECT_SATURATING_CAST(int8_t, min_v<int8_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, minr_v<int8_t>, minr_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, max_v<int8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, maxl_v<int8_t>, maxl_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zero_v<int8_t>, zero_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zeror_v<int8_t>, zeror_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zerol_v<int8_t>, zerol_v<int8_t>);

    EXPECT_SATURATING_CAST(int8_t, min_v<int16_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, minr_v<int16_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, max_v<int16_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, maxl_v<int16_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zero_v<int16_t>, zero_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zeror_v<int16_t>, zeror_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zerol_v<int16_t>, zerol_v<int8_t>);

    EXPECT_SATURATING_CAST(int8_t, min_v<int32_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, minr_v<int32_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, max_v<int32_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, maxl_v<int32_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zero_v<int32_t>, zero_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zeror_v<int32_t>, zeror_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zerol_v<int32_t>, zerol_v<int8_t>);

    EXPECT_SATURATING_CAST(int8_t, min_v<int64_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, minr_v<int64_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, max_v<int64_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, maxl_v<int64_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zero_v<int64_t>, zero_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zeror_v<int64_t>, zeror_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, zerol_v<int64_t>, zerol_v<int8_t>);

    EXPECT_SATURATING_CAST(int8_t, min_v<uint8_t>, zero_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, minr_v<uint8_t>, zeror_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, max_v<uint8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, maxl_v<uint8_t>, max_v<int8_t>);

    EXPECT_SATURATING_CAST(int8_t, min_v<uint16_t>, zero_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, minr_v<uint16_t>, zeror_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, max_v<uint16_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, maxl_v<uint16_t>, max_v<int8_t>);

    EXPECT_SATURATING_CAST(int8_t, min_v<uint32_t>, zero_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, minr_v<uint32_t>, zeror_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, max_v<uint32_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, maxl_v<uint32_t>, max_v<int8_t>);

    EXPECT_SATURATING_CAST(int8_t, min_v<uint64_t>, zero_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, minr_v<uint64_t>, zeror_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, max_v<uint64_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int8_t, maxl_v<uint64_t>, max_v<int8_t>);

    EXPECT_SATURATING_CAST(uint8_t, min_v<int8_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, minr_v<int8_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, max_v<int8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(uint8_t, maxl_v<int8_t>, maxl_v<int8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zero_v<int8_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zeror_v<int8_t>, zeror_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zerol_v<int8_t>, zero_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint8_t, min_v<int16_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, minr_v<int16_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, max_v<int16_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, maxl_v<int16_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zero_v<int16_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zeror_v<int16_t>, zeror_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zerol_v<int16_t>, zero_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint8_t, min_v<int32_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, minr_v<int32_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, max_v<int32_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, maxl_v<int32_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zero_v<int32_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zeror_v<int32_t>, zeror_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zerol_v<int32_t>, zero_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint8_t, min_v<int64_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, minr_v<int64_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, max_v<int64_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, maxl_v<int64_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zero_v<int64_t>, zero_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zeror_v<int64_t>, zeror_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, zerol_v<int64_t>, zero_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint8_t, min_v<uint8_t>, min_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, minr_v<uint8_t>, minr_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, max_v<uint8_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, maxl_v<uint8_t>, maxl_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint8_t, min_v<uint16_t>, min_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, minr_v<uint16_t>, minr_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, max_v<uint16_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, maxl_v<uint16_t>, max_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint8_t, min_v<uint32_t>, min_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, minr_v<uint32_t>, minr_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, max_v<uint32_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, maxl_v<uint32_t>, max_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint8_t, min_v<uint64_t>, min_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, minr_v<uint64_t>, minr_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, max_v<uint64_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint8_t, maxl_v<uint64_t>, max_v<uint8_t>);
}

TEST(SaturatingCastTest, Into_16) {
    EXPECT_SATURATING_CAST(int16_t, min_v<int8_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int16_t, minr_v<int8_t>, minr_v<int8_t>);
    EXPECT_SATURATING_CAST(int16_t, max_v<int8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int16_t, maxl_v<int8_t>, maxl_v<int8_t>);
    EXPECT_SATURATING_CAST(int16_t, zero_v<int8_t>, zero_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zeror_v<int8_t>, zeror_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zerol_v<int8_t>, zerol_v<int16_t>);

    EXPECT_SATURATING_CAST(int16_t, min_v<int16_t>, min_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, minr_v<int16_t>, minr_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, max_v<int16_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, maxl_v<int16_t>, maxl_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zero_v<int16_t>, zero_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zeror_v<int16_t>, zeror_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zerol_v<int16_t>, zerol_v<int16_t>);

    EXPECT_SATURATING_CAST(int16_t, min_v<int32_t>, min_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, minr_v<int32_t>, min_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, max_v<int32_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, maxl_v<int32_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zero_v<int32_t>, zero_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zeror_v<int32_t>, zeror_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zerol_v<int32_t>, zerol_v<int16_t>);

    EXPECT_SATURATING_CAST(int16_t, min_v<int64_t>, min_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, minr_v<int64_t>, min_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, max_v<int64_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, maxl_v<int64_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zero_v<int64_t>, zero_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zeror_v<int64_t>, zeror_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, zerol_v<int64_t>, zerol_v<int16_t>);

    EXPECT_SATURATING_CAST(int16_t, min_v<uint8_t>, zero_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, minr_v<uint8_t>, zeror_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, max_v<uint8_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(int16_t, maxl_v<uint8_t>, maxl_v<uint8_t>);

    EXPECT_SATURATING_CAST(int16_t, min_v<uint16_t>, zero_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, minr_v<uint16_t>, zeror_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, max_v<uint16_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, maxl_v<uint16_t>, max_v<int16_t>);

    EXPECT_SATURATING_CAST(int16_t, min_v<uint32_t>, zero_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, minr_v<uint32_t>, zeror_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, max_v<uint32_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, maxl_v<uint32_t>, max_v<int16_t>);

    EXPECT_SATURATING_CAST(int16_t, min_v<uint64_t>, zero_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, minr_v<uint64_t>, zeror_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, max_v<uint64_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int16_t, maxl_v<uint64_t>, max_v<int16_t>);

    EXPECT_SATURATING_CAST(uint16_t, min_v<int8_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, minr_v<int8_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, max_v<int8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(uint16_t, maxl_v<int8_t>, maxl_v<int8_t>);
    EXPECT_SATURATING_CAST(uint16_t, zero_v<int8_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zeror_v<int8_t>, zeror_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zerol_v<int8_t>, zero_v<uint16_t>);

    EXPECT_SATURATING_CAST(uint16_t, min_v<int16_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, minr_v<int16_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, max_v<int16_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(uint16_t, maxl_v<int16_t>, maxl_v<int16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zero_v<int16_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zeror_v<int16_t>, zeror_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zerol_v<int16_t>, zero_v<uint16_t>);

    EXPECT_SATURATING_CAST(uint16_t, min_v<int32_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, minr_v<int32_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, max_v<int32_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, maxl_v<int32_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zero_v<int32_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zeror_v<int32_t>, zeror_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zerol_v<int32_t>, zero_v<uint16_t>);

    EXPECT_SATURATING_CAST(uint16_t, min_v<int64_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, minr_v<int64_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, max_v<int64_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, maxl_v<int64_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zero_v<int64_t>, zero_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zeror_v<int64_t>, zeror_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, zerol_v<int64_t>, zero_v<uint16_t>);

    EXPECT_SATURATING_CAST(uint16_t, min_v<uint8_t>, min_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, minr_v<uint8_t>, minr_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, max_v<uint8_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint16_t, maxl_v<uint8_t>, maxl_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint16_t, min_v<uint16_t>, min_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, minr_v<uint16_t>, minr_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, max_v<uint16_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, maxl_v<uint16_t>, maxl_v<uint16_t>);

    EXPECT_SATURATING_CAST(uint16_t, min_v<uint32_t>, min_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, minr_v<uint32_t>, minr_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, max_v<uint32_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, maxl_v<uint32_t>, max_v<uint16_t>);

    EXPECT_SATURATING_CAST(uint16_t, min_v<uint64_t>, min_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, minr_v<uint64_t>, minr_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, max_v<uint64_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint16_t, maxl_v<uint64_t>, max_v<uint16_t>);
}

TEST(SaturatingCastTest, Into_32) {
    EXPECT_SATURATING_CAST(int32_t, min_v<int8_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int32_t, minr_v<int8_t>, minr_v<int8_t>);
    EXPECT_SATURATING_CAST(int32_t, max_v<int8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int32_t, maxl_v<int8_t>, maxl_v<int8_t>);
    EXPECT_SATURATING_CAST(int32_t, zero_v<int8_t>, zero_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zeror_v<int8_t>, zeror_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zerol_v<int8_t>, zerol_v<int32_t>);

    EXPECT_SATURATING_CAST(int32_t, min_v<int16_t>, min_v<int16_t>);
    EXPECT_SATURATING_CAST(int32_t, minr_v<int16_t>, minr_v<int16_t>);
    EXPECT_SATURATING_CAST(int32_t, max_v<int16_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int32_t, maxl_v<int16_t>, maxl_v<int16_t>);
    EXPECT_SATURATING_CAST(int32_t, zero_v<int16_t>, zero_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zeror_v<int16_t>, zeror_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zerol_v<int16_t>, zerol_v<int32_t>);

    EXPECT_SATURATING_CAST(int32_t, min_v<int32_t>, min_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, minr_v<int32_t>, minr_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, max_v<int32_t>, max_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, maxl_v<int32_t>, maxl_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zero_v<int32_t>, zero_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zeror_v<int32_t>, zeror_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zerol_v<int32_t>, zerol_v<int32_t>);

    EXPECT_SATURATING_CAST(int32_t, min_v<int64_t>, min_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, minr_v<int64_t>, min_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, max_v<int64_t>, max_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, maxl_v<int64_t>, max_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zero_v<int64_t>, zero_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zeror_v<int64_t>, zeror_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, zerol_v<int64_t>, zerol_v<int32_t>);

    EXPECT_SATURATING_CAST(int32_t, min_v<uint8_t>, zero_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, minr_v<uint8_t>, zeror_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, max_v<uint8_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(int32_t, maxl_v<uint8_t>, maxl_v<uint8_t>);

    EXPECT_SATURATING_CAST(int32_t, min_v<uint16_t>, zero_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, minr_v<uint16_t>, zeror_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, max_v<uint16_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(int32_t, maxl_v<uint16_t>, maxl_v<uint16_t>);

    EXPECT_SATURATING_CAST(int32_t, min_v<uint32_t>, zero_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, minr_v<uint32_t>, zeror_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, max_v<uint32_t>, max_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, maxl_v<uint32_t>, max_v<int32_t>);

    EXPECT_SATURATING_CAST(int32_t, min_v<uint64_t>, zero_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, minr_v<uint64_t>, zeror_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, max_v<uint64_t>, max_v<int32_t>);
    EXPECT_SATURATING_CAST(int32_t, maxl_v<uint64_t>, max_v<int32_t>);

    EXPECT_SATURATING_CAST(uint32_t, min_v<int8_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, minr_v<int8_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, max_v<int8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(uint32_t, maxl_v<int8_t>, maxl_v<int8_t>);
    EXPECT_SATURATING_CAST(uint32_t, zero_v<int8_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zeror_v<int8_t>, zeror_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zerol_v<int8_t>, zero_v<uint32_t>);

    EXPECT_SATURATING_CAST(uint32_t, min_v<int16_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, minr_v<int16_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, max_v<int16_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(uint32_t, maxl_v<int16_t>, maxl_v<int16_t>);
    EXPECT_SATURATING_CAST(uint32_t, zero_v<int16_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zeror_v<int16_t>, zeror_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zerol_v<int16_t>, zero_v<uint32_t>);

    EXPECT_SATURATING_CAST(uint32_t, min_v<int32_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, minr_v<int32_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, max_v<int32_t>, max_v<int32_t>);
    EXPECT_SATURATING_CAST(uint32_t, maxl_v<int32_t>, maxl_v<int32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zero_v<int32_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zeror_v<int32_t>, zeror_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zerol_v<int32_t>, zero_v<uint32_t>);

    EXPECT_SATURATING_CAST(uint32_t, min_v<int64_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, minr_v<int64_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, max_v<int64_t>, max_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, maxl_v<int64_t>, max_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zero_v<int64_t>, zero_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zeror_v<int64_t>, zeror_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, zerol_v<int64_t>, zero_v<uint32_t>);

    EXPECT_SATURATING_CAST(uint32_t, min_v<uint8_t>, min_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, minr_v<uint8_t>, minr_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, max_v<uint8_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint32_t, maxl_v<uint8_t>, maxl_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint32_t, min_v<uint16_t>, min_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, minr_v<uint16_t>, minr_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, max_v<uint16_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint32_t, maxl_v<uint16_t>, maxl_v<uint16_t>);

    EXPECT_SATURATING_CAST(uint32_t, min_v<uint32_t>, min_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, minr_v<uint32_t>, minr_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, max_v<uint32_t>, max_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, maxl_v<uint32_t>, maxl_v<uint32_t>);

    EXPECT_SATURATING_CAST(uint32_t, min_v<uint64_t>, min_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, minr_v<uint64_t>, minr_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, max_v<uint64_t>, max_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint32_t, maxl_v<uint64_t>, max_v<uint32_t>);
}

TEST(SaturatingCastTest, Into_64) {
    EXPECT_SATURATING_CAST(int64_t, min_v<int8_t>, min_v<int8_t>);
    EXPECT_SATURATING_CAST(int64_t, minr_v<int8_t>, minr_v<int8_t>);
    EXPECT_SATURATING_CAST(int64_t, max_v<int8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(int64_t, maxl_v<int8_t>, maxl_v<int8_t>);
    EXPECT_SATURATING_CAST(int64_t, zero_v<int8_t>, zero_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zeror_v<int8_t>, zeror_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zerol_v<int8_t>, zerol_v<int64_t>);

    EXPECT_SATURATING_CAST(int64_t, min_v<int16_t>, min_v<int16_t>);
    EXPECT_SATURATING_CAST(int64_t, minr_v<int16_t>, minr_v<int16_t>);
    EXPECT_SATURATING_CAST(int64_t, max_v<int16_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(int64_t, maxl_v<int16_t>, maxl_v<int16_t>);
    EXPECT_SATURATING_CAST(int64_t, zero_v<int16_t>, zero_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zeror_v<int16_t>, zeror_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zerol_v<int16_t>, zerol_v<int64_t>);

    EXPECT_SATURATING_CAST(int64_t, min_v<int32_t>, min_v<int32_t>);
    EXPECT_SATURATING_CAST(int64_t, minr_v<int32_t>, minr_v<int32_t>);
    EXPECT_SATURATING_CAST(int64_t, max_v<int32_t>, max_v<int32_t>);
    EXPECT_SATURATING_CAST(int64_t, maxl_v<int32_t>, maxl_v<int32_t>);
    EXPECT_SATURATING_CAST(int64_t, zero_v<int32_t>, zero_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zeror_v<int32_t>, zeror_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zerol_v<int32_t>, zerol_v<int64_t>);

    EXPECT_SATURATING_CAST(int64_t, min_v<int64_t>, min_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, minr_v<int64_t>, minr_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, max_v<int64_t>, max_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, maxl_v<int64_t>, maxl_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zero_v<int64_t>, zero_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zeror_v<int64_t>, zeror_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, zerol_v<int64_t>, zerol_v<int64_t>);

    EXPECT_SATURATING_CAST(int64_t, min_v<uint8_t>, zero_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, minr_v<uint8_t>, zeror_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, max_v<uint8_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(int64_t, maxl_v<uint8_t>, maxl_v<uint8_t>);

    EXPECT_SATURATING_CAST(int64_t, min_v<uint16_t>, zero_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, minr_v<uint16_t>, zeror_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, max_v<uint16_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(int64_t, maxl_v<uint16_t>, maxl_v<uint16_t>);

    EXPECT_SATURATING_CAST(int64_t, min_v<uint32_t>, zero_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, minr_v<uint32_t>, zeror_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, max_v<uint32_t>, max_v<uint32_t>);
    EXPECT_SATURATING_CAST(int64_t, maxl_v<uint32_t>, maxl_v<uint32_t>);

    EXPECT_SATURATING_CAST(int64_t, min_v<uint64_t>, zero_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, minr_v<uint64_t>, zeror_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, max_v<uint64_t>, max_v<int64_t>);
    EXPECT_SATURATING_CAST(int64_t, maxl_v<uint64_t>, max_v<int64_t>);

    EXPECT_SATURATING_CAST(uint64_t, min_v<int8_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, minr_v<int8_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, max_v<int8_t>, max_v<int8_t>);
    EXPECT_SATURATING_CAST(uint64_t, maxl_v<int8_t>, maxl_v<int8_t>);
    EXPECT_SATURATING_CAST(uint64_t, zero_v<int8_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zeror_v<int8_t>, zeror_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zerol_v<int8_t>, zero_v<uint64_t>);

    EXPECT_SATURATING_CAST(uint64_t, min_v<int16_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, minr_v<int16_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, max_v<int16_t>, max_v<int16_t>);
    EXPECT_SATURATING_CAST(uint64_t, maxl_v<int16_t>, maxl_v<int16_t>);
    EXPECT_SATURATING_CAST(uint64_t, zero_v<int16_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zeror_v<int16_t>, zeror_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zerol_v<int16_t>, zero_v<uint64_t>);

    EXPECT_SATURATING_CAST(uint64_t, min_v<int32_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, minr_v<int32_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, max_v<int32_t>, max_v<int32_t>);
    EXPECT_SATURATING_CAST(uint64_t, maxl_v<int32_t>, maxl_v<int32_t>);
    EXPECT_SATURATING_CAST(uint64_t, zero_v<int32_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zeror_v<int32_t>, zeror_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zerol_v<int32_t>, zero_v<uint64_t>);

    EXPECT_SATURATING_CAST(uint64_t, min_v<int64_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, minr_v<int64_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, max_v<int64_t>, max_v<int64_t>);
    EXPECT_SATURATING_CAST(uint64_t, maxl_v<int64_t>, maxl_v<int64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zero_v<int64_t>, zero_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zeror_v<int64_t>, zeror_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, zerol_v<int64_t>, zero_v<uint64_t>);

    EXPECT_SATURATING_CAST(uint64_t, min_v<uint8_t>, min_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, minr_v<uint8_t>, minr_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, max_v<uint8_t>, max_v<uint8_t>);
    EXPECT_SATURATING_CAST(uint64_t, maxl_v<uint8_t>, maxl_v<uint8_t>);

    EXPECT_SATURATING_CAST(uint64_t, min_v<uint16_t>, min_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, minr_v<uint16_t>, minr_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, max_v<uint16_t>, max_v<uint16_t>);
    EXPECT_SATURATING_CAST(uint64_t, maxl_v<uint16_t>, maxl_v<uint16_t>);

    EXPECT_SATURATING_CAST(uint64_t, min_v<uint32_t>, min_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, minr_v<uint32_t>, minr_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, max_v<uint32_t>, max_v<uint32_t>);
    EXPECT_SATURATING_CAST(uint64_t, maxl_v<uint32_t>, maxl_v<uint32_t>);

    EXPECT_SATURATING_CAST(uint64_t, min_v<uint64_t>, min_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, minr_v<uint64_t>, minr_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, max_v<uint64_t>, max_v<uint64_t>);
    EXPECT_SATURATING_CAST(uint64_t, maxl_v<uint64_t>, maxl_v<uint64_t>);
}

TEST(SaturatingAddTest, Test_8_8) {
    EXPECT_THAT(saturating_add(min_v<int8_t>, min_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, minr_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, max_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, maxl_v<int8_t>), int8_t(zeroll_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zero_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zeror_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zerol_v<int8_t>), int8_t(min_v<int8_t>));

    EXPECT_THAT(saturating_add(minr_v<int8_t>, min_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, minr_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, max_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, maxl_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zero_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zeror_v<int8_t>), int8_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zerol_v<int8_t>), int8_t(min_v<int8_t>));

    EXPECT_THAT(saturating_add(max_v<int8_t>, min_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, minr_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, max_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, maxl_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zero_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zeror_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zerol_v<int8_t>), int8_t(maxl_v<int8_t>));

    EXPECT_THAT(saturating_add(maxl_v<int8_t>, min_v<int8_t>), int8_t(zeroll_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, minr_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, max_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, maxl_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zero_v<int8_t>), int8_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zeror_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zerol_v<int8_t>), int8_t(maxll_v<int8_t>));

    EXPECT_THAT(saturating_add(zero_v<int8_t>, min_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, minr_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, max_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, maxl_v<int8_t>), int8_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zeror_v<int8_t>), int8_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zerol_v<int8_t>), int8_t(zerol_v<int8_t>));

    EXPECT_THAT(saturating_add(zeror_v<int8_t>, min_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, minr_v<int8_t>), int8_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, max_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, maxl_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zero_v<int8_t>), int8_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zeror_v<int8_t>), int8_t(zerorr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zerol_v<int8_t>), int8_t(zero_v<int8_t>));

    EXPECT_THAT(saturating_add(zerol_v<int8_t>, min_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, minr_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, max_v<int8_t>), int8_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, maxl_v<int8_t>), int8_t(maxll_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zero_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zeror_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zerol_v<int8_t>), int8_t(zeroll_v<int8_t>));

    EXPECT_THAT(saturating_add(min_v<uint8_t>, min_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, minr_v<uint8_t>), uint8_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, max_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, maxl_v<uint8_t>), uint8_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_add(minr_v<uint8_t>, min_v<uint8_t>), uint8_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, minr_v<uint8_t>), uint8_t(minrr_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, max_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, maxl_v<uint8_t>), uint8_t(max_v<uint8_t>));

    EXPECT_THAT(saturating_add(max_v<uint8_t>, min_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, minr_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, max_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, maxl_v<uint8_t>), uint8_t(max_v<uint8_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, min_v<uint8_t>), uint8_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, minr_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, max_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, maxl_v<uint8_t>), uint8_t(max_v<uint8_t>));
}

TEST(SaturatingAddTest, Test_8_16) {
    EXPECT_THAT(saturating_add(min_v<int8_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, max_v<int16_t>), int16_t(min_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_add(min_v<int8_t>, maxl_v<int16_t>), int16_t(min_v<int8_t>) + maxl_v<int16_t>);
    EXPECT_THAT(saturating_add(min_v<int8_t>, zero_v<int16_t>), int16_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zeror_v<int16_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zerol_v<int16_t>), int16_t(min_v<int8_t>) + zerol_v<int16_t>);

    EXPECT_THAT(saturating_add(minr_v<int8_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, max_v<int16_t>), int16_t(minr_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_add(minr_v<int8_t>, maxl_v<int16_t>), int16_t(minr_v<int8_t>) + maxl_v<int16_t>);
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zero_v<int16_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zeror_v<int16_t>), int16_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zerol_v<int16_t>), int16_t(min_v<int8_t>));

    EXPECT_THAT(saturating_add(max_v<int8_t>, min_v<int16_t>), int16_t(max_v<int8_t>) + min_v<int16_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, minr_v<int16_t>), int16_t(max_v<int8_t>) + minr_v<int16_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zero_v<int16_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zeror_v<int16_t>), int16_t(max_v<int8_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, zerol_v<int16_t>), int16_t(maxl_v<int8_t>));

    EXPECT_THAT(saturating_add(maxl_v<int8_t>, min_v<int16_t>), int16_t(maxl_v<int8_t>) + min_v<int16_t>);
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, minr_v<int16_t>), int16_t(maxl_v<int8_t>) + minr_v<int16_t>);
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zero_v<int16_t>), int16_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zeror_v<int16_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zerol_v<int16_t>), int16_t(maxll_v<int8_t>));

    EXPECT_THAT(saturating_add(zero_v<int8_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, minr_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, maxl_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zeror_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zerol_v<int16_t>), int16_t(zerol_v<int16_t>));

    EXPECT_THAT(saturating_add(zeror_v<int8_t>, min_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, minr_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zero_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zeror_v<int16_t>), int16_t(zerorr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zerol_v<int16_t>), int16_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_add(zerol_v<int8_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, max_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, maxl_v<int16_t>), int16_t(maxll_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zero_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zeror_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zerol_v<int16_t>), int16_t(zeroll_v<int16_t>));

    EXPECT_THAT(saturating_add(min_v<uint8_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, minr_v<uint16_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, maxl_v<uint16_t>), uint16_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_add(minr_v<uint8_t>, min_v<uint16_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, minr_v<uint16_t>), uint16_t(minrr_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_add(max_v<uint8_t>, min_v<uint16_t>), uint16_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, minr_v<uint16_t>), uint16_t(max_v<uint8_t>) + zeror_v<uint16_t>);
    EXPECT_THAT(saturating_add(max_v<uint8_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, min_v<uint16_t>), uint16_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, minr_v<uint16_t>), uint16_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));
}

TEST(SaturatingAddTest, Test_8_32) {
    EXPECT_THAT(saturating_add(min_v<int8_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, max_v<int32_t>), int32_t(min_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_add(min_v<int8_t>, maxl_v<int32_t>), int32_t(min_v<int8_t>) + maxl_v<int32_t>);
    EXPECT_THAT(saturating_add(min_v<int8_t>, zero_v<int32_t>), int32_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zeror_v<int32_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zerol_v<int32_t>), int32_t(min_v<int8_t>) + zerol_v<int32_t>);

    EXPECT_THAT(saturating_add(minr_v<int8_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, max_v<int32_t>), int32_t(minr_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_add(minr_v<int8_t>, maxl_v<int32_t>), int32_t(minr_v<int8_t>) + maxl_v<int32_t>);
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zero_v<int32_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zeror_v<int32_t>), int32_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zerol_v<int32_t>), int32_t(min_v<int8_t>));

    EXPECT_THAT(saturating_add(max_v<int8_t>, min_v<int32_t>), int32_t(max_v<int8_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, minr_v<int32_t>), int32_t(max_v<int8_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zero_v<int32_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zeror_v<int32_t>), int32_t(max_v<int8_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, zerol_v<int32_t>), int32_t(maxl_v<int8_t>));

    EXPECT_THAT(saturating_add(maxl_v<int8_t>, min_v<int32_t>), int32_t(maxl_v<int8_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, minr_v<int32_t>), int32_t(maxl_v<int8_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zero_v<int32_t>), int32_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zeror_v<int32_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zerol_v<int32_t>), int32_t(maxll_v<int8_t>));

    EXPECT_THAT(saturating_add(zero_v<int8_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, minr_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, maxl_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zeror_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zerol_v<int32_t>), int32_t(zerol_v<int32_t>));

    EXPECT_THAT(saturating_add(zeror_v<int8_t>, min_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, minr_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zero_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zeror_v<int32_t>), int32_t(zerorr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_add(zerol_v<int8_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, max_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, maxl_v<int32_t>), int32_t(maxll_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zero_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zerol_v<int32_t>), int32_t(zeroll_v<int32_t>));

    EXPECT_THAT(saturating_add(min_v<uint8_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, minr_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, maxl_v<uint32_t>), uint32_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_add(minr_v<uint8_t>, min_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, minr_v<uint32_t>), uint32_t(minrr_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(max_v<uint8_t>, min_v<uint32_t>), uint32_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, minr_v<uint32_t>), uint32_t(max_v<uint8_t>) + zeror_v<uint32_t>);
    EXPECT_THAT(saturating_add(max_v<uint8_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, min_v<uint32_t>), uint32_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, minr_v<uint32_t>), uint32_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingAddTest, Test_8_64) {
    EXPECT_THAT(saturating_add(min_v<int8_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, max_v<int64_t>), int64_t(min_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int8_t>, maxl_v<int64_t>), int64_t(min_v<int8_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int8_t>, zero_v<int64_t>), int64_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zeror_v<int64_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(min_v<int8_t>, zerol_v<int64_t>), int64_t(min_v<int8_t>) + zerol_v<int64_t>);

    EXPECT_THAT(saturating_add(minr_v<int8_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, max_v<int64_t>), int64_t(minr_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int8_t>, maxl_v<int64_t>), int64_t(minr_v<int8_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zero_v<int64_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zeror_v<int64_t>), int64_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_add(minr_v<int8_t>, zerol_v<int64_t>), int64_t(min_v<int8_t>));

    EXPECT_THAT(saturating_add(max_v<int8_t>, min_v<int64_t>), int64_t(max_v<int8_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, minr_v<int64_t>), int64_t(max_v<int8_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zero_v<int64_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(max_v<int8_t>, zeror_v<int64_t>), int64_t(max_v<int8_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int8_t>, zerol_v<int64_t>), int64_t(maxl_v<int8_t>));

    EXPECT_THAT(saturating_add(maxl_v<int8_t>, min_v<int64_t>), int64_t(maxl_v<int8_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, minr_v<int64_t>), int64_t(maxl_v<int8_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zero_v<int64_t>), int64_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zeror_v<int64_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(maxl_v<int8_t>, zerol_v<int64_t>), int64_t(maxll_v<int8_t>));

    EXPECT_THAT(saturating_add(zero_v<int8_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, minr_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, maxl_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zeror_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int8_t>, zerol_v<int64_t>), int64_t(zerol_v<int64_t>));

    EXPECT_THAT(saturating_add(zeror_v<int8_t>, min_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, minr_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zero_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zeror_v<int64_t>), int64_t(zerorr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int8_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_add(zerol_v<int8_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, max_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, maxl_v<int64_t>), int64_t(maxll_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zero_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int8_t>, zerol_v<int64_t>), int64_t(zeroll_v<int64_t>));

    EXPECT_THAT(saturating_add(min_v<uint8_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, minr_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint8_t>, maxl_v<uint64_t>), uint64_t(maxl_v<uint64_t>));

    EXPECT_THAT(saturating_add(minr_v<uint8_t>, min_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, minr_v<uint64_t>), uint64_t(minrr_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint8_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(max_v<uint8_t>, min_v<uint64_t>), uint64_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, minr_v<uint64_t>), uint64_t(max_v<uint8_t>) + zeror_v<uint64_t>);
    EXPECT_THAT(saturating_add(max_v<uint8_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint8_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, min_v<uint64_t>), uint64_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, minr_v<uint64_t>), uint64_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint8_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingAddTest, Test_16_8) {
    EXPECT_THAT(saturating_add(min_v<int16_t>, min_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, minr_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, max_v<int8_t>), int16_t(max_v<int8_t>) + min_v<int16_t>);
    EXPECT_THAT(saturating_add(min_v<int16_t>, maxl_v<int8_t>), int16_t(maxl_v<int8_t>) + min_v<int16_t>);
    EXPECT_THAT(saturating_add(min_v<int16_t>, zero_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zeror_v<int8_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zerol_v<int8_t>), int16_t(min_v<int16_t>));

    EXPECT_THAT(saturating_add(minr_v<int16_t>, min_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, minr_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, max_v<int8_t>), int16_t(max_v<int8_t>) + minr_v<int16_t>);
    EXPECT_THAT(saturating_add(minr_v<int16_t>, maxl_v<int8_t>), int16_t(maxl_v<int8_t>) + minr_v<int16_t>);
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zero_v<int8_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zeror_v<int8_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zerol_v<int8_t>), int16_t(min_v<int16_t>));

    EXPECT_THAT(saturating_add(max_v<int16_t>, min_v<int8_t>), int16_t(min_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_add(max_v<int16_t>, minr_v<int8_t>), int16_t(minr_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_add(max_v<int16_t>, max_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, maxl_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zero_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zeror_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zerol_v<int8_t>), int16_t(maxl_v<int16_t>));

    EXPECT_THAT(saturating_add(maxl_v<int16_t>, min_v<int8_t>), int16_t(min_v<int8_t>) + maxl_v<int16_t>);
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, minr_v<int8_t>), int16_t(minr_v<int8_t>) + maxl_v<int16_t>);
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, max_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, maxl_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zero_v<int8_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zeror_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zerol_v<int8_t>), int16_t(maxll_v<int16_t>));

    EXPECT_THAT(saturating_add(zero_v<int16_t>, min_v<int8_t>), int16_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, minr_v<int8_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, max_v<int8_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, maxl_v<int8_t>), int16_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zeror_v<int8_t>), int16_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zerol_v<int8_t>), int16_t(zerol_v<int8_t>));

    EXPECT_THAT(saturating_add(zeror_v<int16_t>, min_v<int8_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, minr_v<int8_t>), int16_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, max_v<int8_t>), int16_t(max_v<int8_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, maxl_v<int8_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zero_v<int8_t>), int16_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zeror_v<int8_t>), int16_t(zerorr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zerol_v<int8_t>), int16_t(zero_v<int8_t>));

    EXPECT_THAT(saturating_add(zerol_v<int16_t>, min_v<int8_t>), int16_t(min_v<int8_t>) + zerol_v<int16_t>);
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, minr_v<int8_t>), int16_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, max_v<int8_t>), int16_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, maxl_v<int8_t>), int16_t(maxll_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zero_v<int8_t>), int16_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zeror_v<int8_t>), int16_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zerol_v<int8_t>), int16_t(zeroll_v<int8_t>));

    EXPECT_THAT(saturating_add(min_v<uint16_t>, min_v<uint8_t>), uint16_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, minr_v<uint8_t>), uint16_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, max_v<uint8_t>), uint16_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, maxl_v<uint8_t>), uint16_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_add(minr_v<uint16_t>, min_v<uint8_t>), uint16_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, minr_v<uint8_t>), uint16_t(minrr_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, max_v<uint8_t>), uint16_t(max_v<uint8_t>) + zeror_v<uint16_t>);
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, maxl_v<uint8_t>), uint16_t(max_v<uint8_t>));

    EXPECT_THAT(saturating_add(max_v<uint16_t>, min_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, minr_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, max_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, maxl_v<uint8_t>), uint16_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, min_v<uint8_t>), uint16_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, minr_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, max_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, maxl_v<uint8_t>), uint16_t(max_v<uint16_t>));
}

TEST(SaturatingAddTest, Test_16_16) {
    EXPECT_THAT(saturating_add(min_v<int16_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, max_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, maxl_v<int16_t>), int16_t(zeroll_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zero_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zeror_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zerol_v<int16_t>), int16_t(min_v<int16_t>));

    EXPECT_THAT(saturating_add(minr_v<int16_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, max_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, maxl_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zero_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zeror_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zerol_v<int16_t>), int16_t(min_v<int16_t>));

    EXPECT_THAT(saturating_add(max_v<int16_t>, min_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, minr_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zero_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zeror_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zerol_v<int16_t>), int16_t(maxl_v<int16_t>));

    EXPECT_THAT(saturating_add(maxl_v<int16_t>, min_v<int16_t>), int16_t(zeroll_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, minr_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zero_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zeror_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zerol_v<int16_t>), int16_t(maxll_v<int16_t>));

    EXPECT_THAT(saturating_add(zero_v<int16_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, minr_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, maxl_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zeror_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zerol_v<int16_t>), int16_t(zerol_v<int16_t>));

    EXPECT_THAT(saturating_add(zeror_v<int16_t>, min_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, minr_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zero_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zeror_v<int16_t>), int16_t(zerorr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zerol_v<int16_t>), int16_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_add(zerol_v<int16_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, max_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, maxl_v<int16_t>), int16_t(maxll_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zero_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zeror_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zerol_v<int16_t>), int16_t(zeroll_v<int16_t>));

    EXPECT_THAT(saturating_add(min_v<uint16_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, minr_v<uint16_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, maxl_v<uint16_t>), uint16_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_add(minr_v<uint16_t>, min_v<uint16_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, minr_v<uint16_t>), uint16_t(minrr_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_add(max_v<uint16_t>, min_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, minr_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, min_v<uint16_t>), uint16_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, minr_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));
}

TEST(SaturatingAddTest, Test_16_32) {
    EXPECT_THAT(saturating_add(min_v<int16_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, max_v<int32_t>), int32_t(min_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_add(min_v<int16_t>, maxl_v<int32_t>), int32_t(min_v<int16_t>) + maxl_v<int32_t>);
    EXPECT_THAT(saturating_add(min_v<int16_t>, zero_v<int32_t>), int32_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zeror_v<int32_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zerol_v<int32_t>), int32_t(min_v<int16_t>) + zerol_v<int32_t>);

    EXPECT_THAT(saturating_add(minr_v<int16_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, max_v<int32_t>), int32_t(minr_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_add(minr_v<int16_t>, maxl_v<int32_t>), int32_t(minr_v<int16_t>) + maxl_v<int32_t>);
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zero_v<int32_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zeror_v<int32_t>), int32_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zerol_v<int32_t>), int32_t(min_v<int16_t>));

    EXPECT_THAT(saturating_add(max_v<int16_t>, min_v<int32_t>), int32_t(max_v<int16_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int16_t>, minr_v<int32_t>), int32_t(max_v<int16_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int16_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zero_v<int32_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zeror_v<int32_t>), int32_t(max_v<int16_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int16_t>, zerol_v<int32_t>), int32_t(maxl_v<int16_t>));

    EXPECT_THAT(saturating_add(maxl_v<int16_t>, min_v<int32_t>), int32_t(maxl_v<int16_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, minr_v<int32_t>), int32_t(maxl_v<int16_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zero_v<int32_t>), int32_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zeror_v<int32_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zerol_v<int32_t>), int32_t(maxll_v<int16_t>));

    EXPECT_THAT(saturating_add(zero_v<int16_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, minr_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, maxl_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zeror_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zerol_v<int32_t>), int32_t(zerol_v<int32_t>));

    EXPECT_THAT(saturating_add(zeror_v<int16_t>, min_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, minr_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zero_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zeror_v<int32_t>), int32_t(zerorr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_add(zerol_v<int16_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, max_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, maxl_v<int32_t>), int32_t(maxll_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zero_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zerol_v<int32_t>), int32_t(zeroll_v<int32_t>));

    EXPECT_THAT(saturating_add(min_v<uint16_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, minr_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, maxl_v<uint32_t>), uint32_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_add(minr_v<uint16_t>, min_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, minr_v<uint32_t>), uint32_t(minrr_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(max_v<uint16_t>, min_v<uint32_t>), uint32_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, minr_v<uint32_t>), uint32_t(max_v<uint16_t>) + zeror_v<uint32_t>);
    EXPECT_THAT(saturating_add(max_v<uint16_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, min_v<uint32_t>), uint32_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, minr_v<uint32_t>), uint32_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingAddTest, Test_16_64) {
    EXPECT_THAT(saturating_add(min_v<int16_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, max_v<int64_t>), int64_t(min_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int16_t>, maxl_v<int64_t>), int64_t(min_v<int16_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int16_t>, zero_v<int64_t>), int64_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zeror_v<int64_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(min_v<int16_t>, zerol_v<int64_t>), int64_t(min_v<int16_t>) + zerol_v<int64_t>);

    EXPECT_THAT(saturating_add(minr_v<int16_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, max_v<int64_t>), int64_t(minr_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int16_t>, maxl_v<int64_t>), int64_t(minr_v<int16_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zero_v<int64_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zeror_v<int64_t>), int64_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_add(minr_v<int16_t>, zerol_v<int64_t>), int64_t(min_v<int16_t>));

    EXPECT_THAT(saturating_add(max_v<int16_t>, min_v<int64_t>), int64_t(max_v<int16_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int16_t>, minr_v<int64_t>), int64_t(max_v<int16_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int16_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zero_v<int64_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(max_v<int16_t>, zeror_v<int64_t>), int64_t(max_v<int16_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int16_t>, zerol_v<int64_t>), int64_t(maxl_v<int16_t>));

    EXPECT_THAT(saturating_add(maxl_v<int16_t>, min_v<int64_t>), int64_t(maxl_v<int16_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, minr_v<int64_t>), int64_t(maxl_v<int16_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zero_v<int64_t>), int64_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zeror_v<int64_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(maxl_v<int16_t>, zerol_v<int64_t>), int64_t(maxll_v<int16_t>));

    EXPECT_THAT(saturating_add(zero_v<int16_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, minr_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, maxl_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zeror_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int16_t>, zerol_v<int64_t>), int64_t(zerol_v<int64_t>));

    EXPECT_THAT(saturating_add(zeror_v<int16_t>, min_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, minr_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zero_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zeror_v<int64_t>), int64_t(zerorr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int16_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_add(zerol_v<int16_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, max_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, maxl_v<int64_t>), int64_t(maxll_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zero_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int16_t>, zerol_v<int64_t>), int64_t(zeroll_v<int64_t>));

    EXPECT_THAT(saturating_add(min_v<uint16_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, minr_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint16_t>, maxl_v<uint64_t>), uint64_t(maxl_v<uint64_t>));

    EXPECT_THAT(saturating_add(minr_v<uint16_t>, min_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, minr_v<uint64_t>), uint64_t(minrr_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint16_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(max_v<uint16_t>, min_v<uint64_t>), uint64_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, minr_v<uint64_t>), uint64_t(max_v<uint16_t>) + zeror_v<uint64_t>);
    EXPECT_THAT(saturating_add(max_v<uint16_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint16_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, min_v<uint64_t>), uint64_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, minr_v<uint64_t>), uint64_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint16_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingAddTest, Test_32_8) {
    EXPECT_THAT(saturating_add(min_v<int32_t>, min_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, minr_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, max_v<int8_t>), int32_t(max_v<int8_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_add(min_v<int32_t>, maxl_v<int8_t>), int32_t(maxl_v<int8_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_add(min_v<int32_t>, zero_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zeror_v<int8_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zerol_v<int8_t>), int32_t(min_v<int32_t>));

    EXPECT_THAT(saturating_add(minr_v<int32_t>, min_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, minr_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, max_v<int8_t>), int32_t(max_v<int8_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_add(minr_v<int32_t>, maxl_v<int8_t>), int32_t(maxl_v<int8_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zero_v<int8_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zeror_v<int8_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zerol_v<int8_t>), int32_t(min_v<int32_t>));

    EXPECT_THAT(saturating_add(max_v<int32_t>, min_v<int8_t>), int32_t(min_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int32_t>, minr_v<int8_t>), int32_t(minr_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int32_t>, max_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, maxl_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zero_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zeror_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zerol_v<int8_t>), int32_t(maxl_v<int32_t>));

    EXPECT_THAT(saturating_add(maxl_v<int32_t>, min_v<int8_t>), int32_t(min_v<int8_t>) + maxl_v<int32_t>);
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, minr_v<int8_t>), int32_t(minr_v<int8_t>) + maxl_v<int32_t>);
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, max_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, maxl_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zero_v<int8_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zeror_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zerol_v<int8_t>), int32_t(maxll_v<int32_t>));

    EXPECT_THAT(saturating_add(zero_v<int32_t>, min_v<int8_t>), int32_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, minr_v<int8_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, max_v<int8_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, maxl_v<int8_t>), int32_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zeror_v<int8_t>), int32_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zerol_v<int8_t>), int32_t(zerol_v<int8_t>));

    EXPECT_THAT(saturating_add(zeror_v<int32_t>, min_v<int8_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, minr_v<int8_t>), int32_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, max_v<int8_t>), int32_t(max_v<int8_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, maxl_v<int8_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zero_v<int8_t>), int32_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zeror_v<int8_t>), int32_t(zerorr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zerol_v<int8_t>), int32_t(zero_v<int8_t>));

    EXPECT_THAT(saturating_add(zerol_v<int32_t>, min_v<int8_t>), int32_t(min_v<int8_t>) + zerol_v<int32_t>);
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, minr_v<int8_t>), int32_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, max_v<int8_t>), int32_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, maxl_v<int8_t>), int32_t(maxll_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zero_v<int8_t>), int32_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zeror_v<int8_t>), int32_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zerol_v<int8_t>), int32_t(zeroll_v<int8_t>));

    EXPECT_THAT(saturating_add(min_v<uint32_t>, min_v<uint8_t>), uint32_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, minr_v<uint8_t>), uint32_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, max_v<uint8_t>), uint32_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, maxl_v<uint8_t>), uint32_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_add(minr_v<uint32_t>, min_v<uint8_t>), uint32_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, minr_v<uint8_t>), uint32_t(minrr_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, max_v<uint8_t>), uint32_t(max_v<uint8_t>) + zeror_v<uint32_t>);
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, maxl_v<uint8_t>), uint32_t(max_v<uint8_t>));

    EXPECT_THAT(saturating_add(max_v<uint32_t>, min_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, minr_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, max_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, maxl_v<uint8_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, min_v<uint8_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, minr_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, max_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, maxl_v<uint8_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingAddTest, Test_32_16) {
    EXPECT_THAT(saturating_add(min_v<int32_t>, min_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, minr_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, max_v<int16_t>), int32_t(max_v<int16_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_add(min_v<int32_t>, maxl_v<int16_t>), int32_t(maxl_v<int16_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_add(min_v<int32_t>, zero_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zeror_v<int16_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zerol_v<int16_t>), int32_t(min_v<int32_t>));

    EXPECT_THAT(saturating_add(minr_v<int32_t>, min_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, minr_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, max_v<int16_t>), int32_t(max_v<int16_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_add(minr_v<int32_t>, maxl_v<int16_t>), int32_t(maxl_v<int16_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zero_v<int16_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zeror_v<int16_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zerol_v<int16_t>), int32_t(min_v<int32_t>));

    EXPECT_THAT(saturating_add(max_v<int32_t>, min_v<int16_t>), int32_t(min_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int32_t>, minr_v<int16_t>), int32_t(minr_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_add(max_v<int32_t>, max_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, maxl_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zero_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zeror_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zerol_v<int16_t>), int32_t(maxl_v<int32_t>));

    EXPECT_THAT(saturating_add(maxl_v<int32_t>, min_v<int16_t>), int32_t(min_v<int16_t>) + maxl_v<int32_t>);
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, minr_v<int16_t>), int32_t(minr_v<int16_t>) + maxl_v<int32_t>);
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, max_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, maxl_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zero_v<int16_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zeror_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zerol_v<int16_t>), int32_t(maxll_v<int32_t>));

    EXPECT_THAT(saturating_add(zero_v<int32_t>, min_v<int16_t>), int32_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, minr_v<int16_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, max_v<int16_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, maxl_v<int16_t>), int32_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zeror_v<int16_t>), int32_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zerol_v<int16_t>), int32_t(zerol_v<int16_t>));

    EXPECT_THAT(saturating_add(zeror_v<int32_t>, min_v<int16_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, minr_v<int16_t>), int32_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, max_v<int16_t>), int32_t(max_v<int16_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, maxl_v<int16_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zero_v<int16_t>), int32_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zeror_v<int16_t>), int32_t(zerorr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zerol_v<int16_t>), int32_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_add(zerol_v<int32_t>, min_v<int16_t>), int32_t(min_v<int16_t>) + zerol_v<int32_t>);
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, minr_v<int16_t>), int32_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, max_v<int16_t>), int32_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, maxl_v<int16_t>), int32_t(maxll_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zero_v<int16_t>), int32_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zeror_v<int16_t>), int32_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zerol_v<int16_t>), int32_t(zeroll_v<int16_t>));

    EXPECT_THAT(saturating_add(min_v<uint32_t>, min_v<uint16_t>), uint32_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, minr_v<uint16_t>), uint32_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, max_v<uint16_t>), uint32_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, maxl_v<uint16_t>), uint32_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_add(minr_v<uint32_t>, min_v<uint16_t>), uint32_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, minr_v<uint16_t>), uint32_t(minrr_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, max_v<uint16_t>), uint32_t(max_v<uint16_t>) + zeror_v<uint32_t>);
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, maxl_v<uint16_t>), uint32_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_add(max_v<uint32_t>, min_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, minr_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, max_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, maxl_v<uint16_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, min_v<uint16_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, minr_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, max_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, maxl_v<uint16_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingAddTest, Test_32_32) {
    EXPECT_THAT(saturating_add(min_v<int32_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, max_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, maxl_v<int32_t>), int32_t(zeroll_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zero_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zeror_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zerol_v<int32_t>), int32_t(min_v<int32_t>));

    EXPECT_THAT(saturating_add(minr_v<int32_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, max_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, maxl_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zero_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zeror_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zerol_v<int32_t>), int32_t(min_v<int32_t>));

    EXPECT_THAT(saturating_add(max_v<int32_t>, min_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, minr_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zero_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zeror_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zerol_v<int32_t>), int32_t(maxl_v<int32_t>));

    EXPECT_THAT(saturating_add(maxl_v<int32_t>, min_v<int32_t>), int32_t(zeroll_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, minr_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zero_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zeror_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zerol_v<int32_t>), int32_t(maxll_v<int32_t>));

    EXPECT_THAT(saturating_add(zero_v<int32_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, minr_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, maxl_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zeror_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zerol_v<int32_t>), int32_t(zerol_v<int32_t>));

    EXPECT_THAT(saturating_add(zeror_v<int32_t>, min_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, minr_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zero_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zeror_v<int32_t>), int32_t(zerorr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_add(zerol_v<int32_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, max_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, maxl_v<int32_t>), int32_t(maxll_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zero_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zerol_v<int32_t>), int32_t(zeroll_v<int32_t>));

    EXPECT_THAT(saturating_add(min_v<uint32_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, minr_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, maxl_v<uint32_t>), uint32_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_add(minr_v<uint32_t>, min_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, minr_v<uint32_t>), uint32_t(minrr_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(max_v<uint32_t>, min_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, minr_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, min_v<uint32_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, minr_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingAddTest, Test_32_64) {
    EXPECT_THAT(saturating_add(min_v<int32_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, max_v<int64_t>), int64_t(min_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int32_t>, maxl_v<int64_t>), int64_t(min_v<int32_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int32_t>, zero_v<int64_t>), int64_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zeror_v<int64_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(min_v<int32_t>, zerol_v<int64_t>), int64_t(min_v<int32_t>) + zerol_v<int64_t>);

    EXPECT_THAT(saturating_add(minr_v<int32_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, max_v<int64_t>), int64_t(minr_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int32_t>, maxl_v<int64_t>), int64_t(minr_v<int32_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zero_v<int64_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zeror_v<int64_t>), int64_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_add(minr_v<int32_t>, zerol_v<int64_t>), int64_t(min_v<int32_t>));

    EXPECT_THAT(saturating_add(max_v<int32_t>, min_v<int64_t>), int64_t(max_v<int32_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int32_t>, minr_v<int64_t>), int64_t(max_v<int32_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int32_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zero_v<int64_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(max_v<int32_t>, zeror_v<int64_t>), int64_t(max_v<int32_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int32_t>, zerol_v<int64_t>), int64_t(maxl_v<int32_t>));

    EXPECT_THAT(saturating_add(maxl_v<int32_t>, min_v<int64_t>), int64_t(maxl_v<int32_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, minr_v<int64_t>), int64_t(maxl_v<int32_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zero_v<int64_t>), int64_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zeror_v<int64_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(maxl_v<int32_t>, zerol_v<int64_t>), int64_t(maxll_v<int32_t>));

    EXPECT_THAT(saturating_add(zero_v<int32_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, minr_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, maxl_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zeror_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int32_t>, zerol_v<int64_t>), int64_t(zerol_v<int64_t>));

    EXPECT_THAT(saturating_add(zeror_v<int32_t>, min_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, minr_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zero_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zeror_v<int64_t>), int64_t(zerorr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int32_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_add(zerol_v<int32_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, max_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, maxl_v<int64_t>), int64_t(maxll_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zero_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int32_t>, zerol_v<int64_t>), int64_t(zeroll_v<int64_t>));

    EXPECT_THAT(saturating_add(min_v<uint32_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, minr_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint32_t>, maxl_v<uint64_t>), uint64_t(maxl_v<uint64_t>));

    EXPECT_THAT(saturating_add(minr_v<uint32_t>, min_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, minr_v<uint64_t>), uint64_t(minrr_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint32_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(max_v<uint32_t>, min_v<uint64_t>), uint64_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, minr_v<uint64_t>), uint64_t(max_v<uint32_t>) + zeror_v<uint64_t>);
    EXPECT_THAT(saturating_add(max_v<uint32_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint32_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, min_v<uint64_t>), uint64_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, minr_v<uint64_t>), uint64_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint32_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingAddTest, Test_64_8) {
    EXPECT_THAT(saturating_add(min_v<int64_t>, min_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, minr_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, max_v<int8_t>), int64_t(max_v<int8_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int64_t>, maxl_v<int8_t>), int64_t(maxl_v<int8_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int64_t>, zero_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zeror_v<int8_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zerol_v<int8_t>), int64_t(min_v<int64_t>));

    EXPECT_THAT(saturating_add(minr_v<int64_t>, min_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, minr_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, max_v<int8_t>), int64_t(max_v<int8_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int64_t>, maxl_v<int8_t>), int64_t(maxl_v<int8_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zero_v<int8_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zeror_v<int8_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zerol_v<int8_t>), int64_t(min_v<int64_t>));

    EXPECT_THAT(saturating_add(max_v<int64_t>, min_v<int8_t>), int64_t(min_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int64_t>, minr_v<int8_t>), int64_t(minr_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int64_t>, max_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, maxl_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zero_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zeror_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zerol_v<int8_t>), int64_t(maxl_v<int64_t>));

    EXPECT_THAT(saturating_add(maxl_v<int64_t>, min_v<int8_t>), int64_t(min_v<int8_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, minr_v<int8_t>), int64_t(minr_v<int8_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, max_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, maxl_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zero_v<int8_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zeror_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zerol_v<int8_t>), int64_t(maxll_v<int64_t>));

    EXPECT_THAT(saturating_add(zero_v<int64_t>, min_v<int8_t>), int64_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, minr_v<int8_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, max_v<int8_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, maxl_v<int8_t>), int64_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zeror_v<int8_t>), int64_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zerol_v<int8_t>), int64_t(zerol_v<int8_t>));

    EXPECT_THAT(saturating_add(zeror_v<int64_t>, min_v<int8_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, minr_v<int8_t>), int64_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, max_v<int8_t>), int64_t(max_v<int8_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, maxl_v<int8_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zero_v<int8_t>), int64_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zeror_v<int8_t>), int64_t(zerorr_v<int8_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zerol_v<int8_t>), int64_t(zero_v<int8_t>));

    EXPECT_THAT(saturating_add(zerol_v<int64_t>, min_v<int8_t>), int64_t(min_v<int8_t>) + zerol_v<int64_t>);
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, minr_v<int8_t>), int64_t(min_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, max_v<int8_t>), int64_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, maxl_v<int8_t>), int64_t(maxll_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zero_v<int8_t>), int64_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zeror_v<int8_t>), int64_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zerol_v<int8_t>), int64_t(zeroll_v<int8_t>));

    EXPECT_THAT(saturating_add(min_v<uint64_t>, min_v<uint8_t>), uint64_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, minr_v<uint8_t>), uint64_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, max_v<uint8_t>), uint64_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, maxl_v<uint8_t>), uint64_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_add(minr_v<uint64_t>, min_v<uint8_t>), uint64_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, minr_v<uint8_t>), uint64_t(minrr_v<uint8_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, max_v<uint8_t>), uint64_t(max_v<uint8_t>) + zeror_v<uint64_t>);
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, maxl_v<uint8_t>), uint64_t(max_v<uint8_t>));

    EXPECT_THAT(saturating_add(max_v<uint64_t>, min_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, minr_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, max_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, maxl_v<uint8_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, min_v<uint8_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, minr_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, max_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, maxl_v<uint8_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingAddTest, Test_64_16) {
    EXPECT_THAT(saturating_add(min_v<int64_t>, min_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, minr_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, max_v<int16_t>), int64_t(max_v<int16_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int64_t>, maxl_v<int16_t>), int64_t(maxl_v<int16_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int64_t>, zero_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zeror_v<int16_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zerol_v<int16_t>), int64_t(min_v<int64_t>));

    EXPECT_THAT(saturating_add(minr_v<int64_t>, min_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, minr_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, max_v<int16_t>), int64_t(max_v<int16_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int64_t>, maxl_v<int16_t>), int64_t(maxl_v<int16_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zero_v<int16_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zeror_v<int16_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zerol_v<int16_t>), int64_t(min_v<int64_t>));

    EXPECT_THAT(saturating_add(max_v<int64_t>, min_v<int16_t>), int64_t(min_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int64_t>, minr_v<int16_t>), int64_t(minr_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int64_t>, max_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, maxl_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zero_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zeror_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zerol_v<int16_t>), int64_t(maxl_v<int64_t>));

    EXPECT_THAT(saturating_add(maxl_v<int64_t>, min_v<int16_t>), int64_t(min_v<int16_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, minr_v<int16_t>), int64_t(minr_v<int16_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, max_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, maxl_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zero_v<int16_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zeror_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zerol_v<int16_t>), int64_t(maxll_v<int64_t>));

    EXPECT_THAT(saturating_add(zero_v<int64_t>, min_v<int16_t>), int64_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, minr_v<int16_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, max_v<int16_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, maxl_v<int16_t>), int64_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zeror_v<int16_t>), int64_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zerol_v<int16_t>), int64_t(zerol_v<int16_t>));

    EXPECT_THAT(saturating_add(zeror_v<int64_t>, min_v<int16_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, minr_v<int16_t>), int64_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, max_v<int16_t>), int64_t(max_v<int16_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, maxl_v<int16_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zero_v<int16_t>), int64_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zeror_v<int16_t>), int64_t(zerorr_v<int16_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zerol_v<int16_t>), int64_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_add(zerol_v<int64_t>, min_v<int16_t>), int64_t(min_v<int16_t>) + zerol_v<int64_t>);
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, minr_v<int16_t>), int64_t(min_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, max_v<int16_t>), int64_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, maxl_v<int16_t>), int64_t(maxll_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zero_v<int16_t>), int64_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zeror_v<int16_t>), int64_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zerol_v<int16_t>), int64_t(zeroll_v<int16_t>));

    EXPECT_THAT(saturating_add(min_v<uint64_t>, min_v<uint16_t>), uint64_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, minr_v<uint16_t>), uint64_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, max_v<uint16_t>), uint64_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, maxl_v<uint16_t>), uint64_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_add(minr_v<uint64_t>, min_v<uint16_t>), uint64_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, minr_v<uint16_t>), uint64_t(minrr_v<uint16_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, max_v<uint16_t>), uint64_t(max_v<uint16_t>) + zeror_v<uint64_t>);
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, maxl_v<uint16_t>), uint64_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_add(max_v<uint64_t>, min_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, minr_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, max_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, maxl_v<uint16_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, min_v<uint16_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, minr_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, max_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, maxl_v<uint16_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingAddTest, Test_64_32) {
    EXPECT_THAT(saturating_add(min_v<int64_t>, min_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, minr_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, max_v<int32_t>), int64_t(max_v<int32_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int64_t>, maxl_v<int32_t>), int64_t(maxl_v<int32_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_add(min_v<int64_t>, zero_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zeror_v<int32_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zerol_v<int32_t>), int64_t(min_v<int64_t>));

    EXPECT_THAT(saturating_add(minr_v<int64_t>, min_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, minr_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, max_v<int32_t>), int64_t(max_v<int32_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int64_t>, maxl_v<int32_t>), int64_t(maxl_v<int32_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zero_v<int32_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zeror_v<int32_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zerol_v<int32_t>), int64_t(min_v<int64_t>));

    EXPECT_THAT(saturating_add(max_v<int64_t>, min_v<int32_t>), int64_t(min_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int64_t>, minr_v<int32_t>), int64_t(minr_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_add(max_v<int64_t>, max_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, maxl_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zero_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zeror_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zerol_v<int32_t>), int64_t(maxl_v<int64_t>));

    EXPECT_THAT(saturating_add(maxl_v<int64_t>, min_v<int32_t>), int64_t(min_v<int32_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, minr_v<int32_t>), int64_t(minr_v<int32_t>) + maxl_v<int64_t>);
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, max_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, maxl_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zero_v<int32_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zeror_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zerol_v<int32_t>), int64_t(maxll_v<int64_t>));

    EXPECT_THAT(saturating_add(zero_v<int64_t>, min_v<int32_t>), int64_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, minr_v<int32_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, max_v<int32_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, maxl_v<int32_t>), int64_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zeror_v<int32_t>), int64_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zerol_v<int32_t>), int64_t(zerol_v<int32_t>));

    EXPECT_THAT(saturating_add(zeror_v<int64_t>, min_v<int32_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, minr_v<int32_t>), int64_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, max_v<int32_t>), int64_t(max_v<int32_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, maxl_v<int32_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zero_v<int32_t>), int64_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zeror_v<int32_t>), int64_t(zerorr_v<int32_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zerol_v<int32_t>), int64_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_add(zerol_v<int64_t>, min_v<int32_t>), int64_t(min_v<int32_t>) + zerol_v<int64_t>);
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, minr_v<int32_t>), int64_t(min_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, max_v<int32_t>), int64_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, maxl_v<int32_t>), int64_t(maxll_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zero_v<int32_t>), int64_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zeror_v<int32_t>), int64_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zerol_v<int32_t>), int64_t(zeroll_v<int32_t>));

    EXPECT_THAT(saturating_add(min_v<uint64_t>, min_v<uint32_t>), uint64_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, minr_v<uint32_t>), uint64_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, max_v<uint32_t>), uint64_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, maxl_v<uint32_t>), uint64_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_add(minr_v<uint64_t>, min_v<uint32_t>), uint64_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, minr_v<uint32_t>), uint64_t(minrr_v<uint32_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, max_v<uint32_t>), uint64_t(max_v<uint32_t>) + zeror_v<uint64_t>);
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, maxl_v<uint32_t>), uint64_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_add(max_v<uint64_t>, min_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, minr_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, max_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, maxl_v<uint32_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, min_v<uint32_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, minr_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, max_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, maxl_v<uint32_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingAddTest, Test_64_64) {
    EXPECT_THAT(saturating_add(min_v<int64_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, max_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, maxl_v<int64_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zero_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zeror_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(min_v<int64_t>, zerol_v<int64_t>), int64_t(min_v<int64_t>));

    EXPECT_THAT(saturating_add(minr_v<int64_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, max_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, maxl_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zero_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zeror_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_add(minr_v<int64_t>, zerol_v<int64_t>), int64_t(min_v<int64_t>));

    EXPECT_THAT(saturating_add(max_v<int64_t>, min_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, minr_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zero_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zeror_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(max_v<int64_t>, zerol_v<int64_t>), int64_t(maxl_v<int64_t>));

    EXPECT_THAT(saturating_add(maxl_v<int64_t>, min_v<int64_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, minr_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zero_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zeror_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(maxl_v<int64_t>, zerol_v<int64_t>), int64_t(maxll_v<int64_t>));

    EXPECT_THAT(saturating_add(zero_v<int64_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, minr_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, maxl_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zeror_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_add(zero_v<int64_t>, zerol_v<int64_t>), int64_t(zerol_v<int64_t>));

    EXPECT_THAT(saturating_add(zeror_v<int64_t>, min_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, minr_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zero_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zeror_v<int64_t>), int64_t(zerorr_v<int64_t>));
    EXPECT_THAT(saturating_add(zeror_v<int64_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_add(zerol_v<int64_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, max_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, maxl_v<int64_t>), int64_t(maxll_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zero_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_add(zerol_v<int64_t>, zerol_v<int64_t>), int64_t(zeroll_v<int64_t>));

    EXPECT_THAT(saturating_add(min_v<uint64_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, minr_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(min_v<uint64_t>, maxl_v<uint64_t>), uint64_t(maxl_v<uint64_t>));

    EXPECT_THAT(saturating_add(minr_v<uint64_t>, min_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, minr_v<uint64_t>), uint64_t(minrr_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(minr_v<uint64_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(max_v<uint64_t>, min_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, minr_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(max_v<uint64_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, min_v<uint64_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, minr_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_add(maxl_v<uint64_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingSubTest, Test_8_8) {
    EXPECT_THAT(saturating_sub(min_v<int8_t>, min_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, minr_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, max_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, maxl_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zero_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zeror_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zerol_v<int8_t>), int8_t(minr_v<int8_t>));

    EXPECT_THAT(saturating_sub(minr_v<int8_t>, min_v<int8_t>), int8_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, minr_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, max_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, maxl_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zero_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zeror_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zerol_v<int8_t>), int8_t(minrr_v<int8_t>));

    EXPECT_THAT(saturating_sub(max_v<int8_t>, min_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, minr_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, max_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, maxl_v<int8_t>), int8_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zero_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zeror_v<int8_t>), int8_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zerol_v<int8_t>), int8_t(max_v<int8_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, min_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, minr_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, max_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, maxl_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zero_v<int8_t>), int8_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zeror_v<int8_t>), int8_t(maxll_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zerol_v<int8_t>), int8_t(max_v<int8_t>));

    EXPECT_THAT(saturating_sub(zero_v<int8_t>, min_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, minr_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, max_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, maxl_v<int8_t>), int8_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zeror_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zerol_v<int8_t>), int8_t(zeror_v<int8_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, min_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, minr_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, max_v<int8_t>), int8_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, maxl_v<int8_t>), int8_t(minrr_v<int8_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zero_v<int8_t>), int8_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zeror_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zerol_v<int8_t>), int8_t(zerorr_v<int8_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, min_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, minr_v<int8_t>), int8_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, max_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, maxl_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zero_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zeror_v<int8_t>), int8_t(zeroll_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zerol_v<int8_t>), int8_t(zero_v<int8_t>));

    EXPECT_THAT(saturating_sub(min_v<uint8_t>, min_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, minr_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, max_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, maxl_v<uint8_t>), uint8_t(min_v<uint8_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, min_v<uint8_t>), uint8_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, minr_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, max_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, maxl_v<uint8_t>), uint8_t(min_v<uint8_t>));

    EXPECT_THAT(saturating_sub(max_v<uint8_t>, min_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, minr_v<uint8_t>), uint8_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, max_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, maxl_v<uint8_t>), uint8_t(minr_v<uint8_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, min_v<uint8_t>), uint8_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, minr_v<uint8_t>), uint8_t(maxll_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, max_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, maxl_v<uint8_t>), uint8_t(min_v<uint8_t>));
}

TEST(SaturatingSubTest, Test_8_16) {
    EXPECT_THAT(saturating_sub(min_v<int8_t>, min_v<int16_t>), int16_t(minr_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, minr_v<int16_t>), int16_t(min_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, maxl_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zero_v<int16_t>), int16_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zeror_v<int16_t>), int16_t(min_v<int8_t>) - zeror_v<int16_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zerol_v<int16_t>), int16_t(minr_v<int8_t>));

    EXPECT_THAT(saturating_sub(minr_v<int8_t>, min_v<int16_t>), int16_t(minrr_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, minr_v<int16_t>), int16_t(minr_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, maxl_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zero_v<int16_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zeror_v<int16_t>), int16_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zerol_v<int16_t>), int16_t(minrr_v<int8_t>));

    EXPECT_THAT(saturating_sub(max_v<int8_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, max_v<int16_t>), int16_t(max_v<int8_t>) + minr_v<int16_t>);
    EXPECT_THAT(saturating_sub(max_v<int8_t>, maxl_v<int16_t>), int16_t(max_v<int8_t>) + minrr_v<int16_t>);
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zero_v<int16_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zeror_v<int16_t>), int16_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zerol_v<int16_t>), int16_t(max_v<int8_t>) + zeror_v<int16_t>);

    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, max_v<int16_t>), int16_t(max_v<int8_t>) + min_v<int16_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, maxl_v<int16_t>), int16_t(max_v<int8_t>) + minr_v<int16_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zero_v<int16_t>), int16_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zeror_v<int16_t>), int16_t(maxll_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zerol_v<int16_t>), int16_t(max_v<int8_t>));

    EXPECT_THAT(saturating_sub(zero_v<int8_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, max_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, maxl_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zeror_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zerol_v<int16_t>), int16_t(zeror_v<int16_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, max_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, maxl_v<int16_t>), int16_t(minrr_v<int16_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zero_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zeror_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zerol_v<int16_t>), int16_t(zerorr_v<int16_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, minr_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, maxl_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zero_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zeror_v<int16_t>), int16_t(zeroll_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zerol_v<int16_t>), int16_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_sub(min_v<uint8_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, minr_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, min_v<uint16_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, minr_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_sub(max_v<uint8_t>, min_v<uint16_t>), uint16_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, minr_v<uint16_t>), uint16_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, min_v<uint16_t>), uint16_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, minr_v<uint16_t>), uint16_t(maxll_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));
}

TEST(SaturatingSubTest, Test_8_32) {
    EXPECT_THAT(saturating_sub(min_v<int8_t>, min_v<int32_t>), int32_t(minr_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, minr_v<int32_t>), int32_t(min_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zero_v<int32_t>), int32_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zeror_v<int32_t>), int32_t(min_v<int8_t>) - zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zerol_v<int32_t>), int32_t(minr_v<int8_t>));

    EXPECT_THAT(saturating_sub(minr_v<int8_t>, min_v<int32_t>), int32_t(minrr_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, minr_v<int32_t>), int32_t(minr_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zero_v<int32_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zeror_v<int32_t>), int32_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zerol_v<int32_t>), int32_t(minrr_v<int8_t>));

    EXPECT_THAT(saturating_sub(max_v<int8_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, max_v<int32_t>), int32_t(max_v<int8_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_sub(max_v<int8_t>, maxl_v<int32_t>), int32_t(max_v<int8_t>) + minrr_v<int32_t>);
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zero_v<int32_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zeror_v<int32_t>), int32_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zerol_v<int32_t>), int32_t(max_v<int8_t>) + zeror_v<int32_t>);

    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, max_v<int32_t>), int32_t(max_v<int8_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, maxl_v<int32_t>), int32_t(max_v<int8_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zero_v<int32_t>), int32_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zeror_v<int32_t>), int32_t(maxll_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zerol_v<int32_t>), int32_t(max_v<int8_t>));

    EXPECT_THAT(saturating_sub(zero_v<int8_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, max_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zeror_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zerol_v<int32_t>), int32_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, max_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zero_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zerol_v<int32_t>), int32_t(zerorr_v<int32_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, minr_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, maxl_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zero_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zeror_v<int32_t>), int32_t(zeroll_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_sub(min_v<uint8_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, min_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(max_v<uint8_t>, min_v<uint32_t>), uint32_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, minr_v<uint32_t>), uint32_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, min_v<uint32_t>), uint32_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, minr_v<uint32_t>), uint32_t(maxll_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));
}

TEST(SaturatingSubTest, Test_8_64) {
    EXPECT_THAT(saturating_sub(min_v<int8_t>, min_v<int64_t>), int64_t(minr_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, minr_v<int64_t>), int64_t(min_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zero_v<int64_t>), int64_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zeror_v<int64_t>), int64_t(min_v<int8_t>) - zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int8_t>, zerol_v<int64_t>), int64_t(minr_v<int8_t>));

    EXPECT_THAT(saturating_sub(minr_v<int8_t>, min_v<int64_t>), int64_t(minrr_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, minr_v<int64_t>), int64_t(minr_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zero_v<int64_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zeror_v<int64_t>), int64_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(minr_v<int8_t>, zerol_v<int64_t>), int64_t(minrr_v<int8_t>));

    EXPECT_THAT(saturating_sub(max_v<int8_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, max_v<int64_t>), int64_t(max_v<int8_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int8_t>, maxl_v<int64_t>), int64_t(max_v<int8_t>) + minrr_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zero_v<int64_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zeror_v<int64_t>), int64_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(max_v<int8_t>, zerol_v<int64_t>), int64_t(max_v<int8_t>) + zeror_v<int64_t>);

    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, max_v<int64_t>), int64_t(max_v<int8_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, maxl_v<int64_t>), int64_t(max_v<int8_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zero_v<int64_t>), int64_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zeror_v<int64_t>), int64_t(maxll_v<int8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int8_t>, zerol_v<int64_t>), int64_t(max_v<int8_t>));

    EXPECT_THAT(saturating_sub(zero_v<int8_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, max_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zeror_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int8_t>, zerol_v<int64_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, max_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zero_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int8_t>, zerol_v<int64_t>), int64_t(zerorr_v<int64_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, minr_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, maxl_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zero_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zeror_v<int64_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int8_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_sub(min_v<uint8_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint8_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, min_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint8_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(max_v<uint8_t>, min_v<uint64_t>), uint64_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, minr_v<uint64_t>), uint64_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint8_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, min_v<uint64_t>), uint64_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, minr_v<uint64_t>), uint64_t(maxll_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint8_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));
}

TEST(SaturatingSubTest, Test_16_8) {
    EXPECT_THAT(saturating_sub(min_v<int16_t>, min_v<int8_t>), int16_t(max_v<int8_t>) + minr_v<int16_t>);
    EXPECT_THAT(saturating_sub(min_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int8_t>) + min_v<int16_t>);
    EXPECT_THAT(saturating_sub(min_v<int16_t>, max_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, maxl_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zero_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zeror_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zerol_v<int8_t>), int16_t(minr_v<int16_t>));

    EXPECT_THAT(saturating_sub(minr_v<int16_t>, min_v<int8_t>), int16_t(max_v<int8_t>) + minrr_v<int16_t>);
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int8_t>) + minr_v<int16_t>);
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, max_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, maxl_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zero_v<int8_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zeror_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zerol_v<int8_t>), int16_t(minrr_v<int16_t>));

    EXPECT_THAT(saturating_sub(max_v<int16_t>, min_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, max_v<int8_t>), int16_t(minr_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_sub(max_v<int16_t>, maxl_v<int8_t>), int16_t(minrr_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zero_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zeror_v<int8_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zerol_v<int8_t>), int16_t(max_v<int16_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, min_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, max_v<int8_t>), int16_t(min_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, maxl_v<int8_t>), int16_t(minr_v<int8_t>) + max_v<int16_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zero_v<int8_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zeror_v<int8_t>), int16_t(maxll_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zerol_v<int8_t>), int16_t(max_v<int16_t>));

    EXPECT_THAT(saturating_sub(zero_v<int16_t>, min_v<int8_t>), int16_t(max_v<int8_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, max_v<int8_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, maxl_v<int8_t>), int16_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zeror_v<int8_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zerol_v<int8_t>), int16_t(zeror_v<int16_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, min_v<int8_t>), int16_t(max_v<int8_t>) + zerorr_v<int16_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int8_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, max_v<int8_t>), int16_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, maxl_v<int8_t>), int16_t(minrr_v<int8_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zero_v<int8_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zeror_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zerol_v<int8_t>), int16_t(zerorr_v<int16_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, min_v<int8_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, minr_v<int8_t>), int16_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, max_v<int8_t>), int16_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, maxl_v<int8_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zero_v<int8_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zeror_v<int8_t>), int16_t(zeroll_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zerol_v<int8_t>), int16_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_sub(min_v<uint16_t>, min_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, minr_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, max_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, maxl_v<uint8_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, min_v<uint8_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, minr_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, max_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, maxl_v<uint8_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_sub(max_v<uint16_t>, min_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, minr_v<uint8_t>), uint16_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, max_v<uint8_t>), max_v<uint16_t> - uint16_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, maxl_v<uint8_t>), max_v<uint16_t> - uint16_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, min_v<uint8_t>), uint16_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, minr_v<uint8_t>), uint16_t(maxll_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, max_v<uint8_t>), maxl_v<uint16_t> - uint16_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, maxl_v<uint8_t>), max_v<uint16_t> - uint16_t(max_v<uint8_t>));
}

TEST(SaturatingSubTest, Test_16_16) {
    EXPECT_THAT(saturating_sub(min_v<int16_t>, min_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, minr_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, maxl_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zero_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zeror_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zerol_v<int16_t>), int16_t(minr_v<int16_t>));

    EXPECT_THAT(saturating_sub(minr_v<int16_t>, min_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, minr_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, maxl_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zero_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zeror_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zerol_v<int16_t>), int16_t(minrr_v<int16_t>));

    EXPECT_THAT(saturating_sub(max_v<int16_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, max_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, maxl_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zero_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zeror_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zerol_v<int16_t>), int16_t(max_v<int16_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, max_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, maxl_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zero_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zeror_v<int16_t>), int16_t(maxll_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zerol_v<int16_t>), int16_t(max_v<int16_t>));

    EXPECT_THAT(saturating_sub(zero_v<int16_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, max_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, maxl_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zeror_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zerol_v<int16_t>), int16_t(zeror_v<int16_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, max_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, maxl_v<int16_t>), int16_t(minrr_v<int16_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zero_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zeror_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zerol_v<int16_t>), int16_t(zerorr_v<int16_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, minr_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, maxl_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zero_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zeror_v<int16_t>), int16_t(zeroll_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zerol_v<int16_t>), int16_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_sub(min_v<uint16_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, minr_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, min_v<uint16_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, minr_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_sub(max_v<uint16_t>, min_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, minr_v<uint16_t>), uint16_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, maxl_v<uint16_t>), uint16_t(minr_v<uint16_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, min_v<uint16_t>), uint16_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, minr_v<uint16_t>), uint16_t(maxll_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));
}

TEST(SaturatingSubTest, Test_16_32) {
    EXPECT_THAT(saturating_sub(min_v<int16_t>, min_v<int32_t>), int32_t(minr_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int16_t>, minr_v<int32_t>), int32_t(min_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int16_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zero_v<int32_t>), int32_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zeror_v<int32_t>), int32_t(min_v<int16_t>) - zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zerol_v<int32_t>), int32_t(minr_v<int16_t>));

    EXPECT_THAT(saturating_sub(minr_v<int16_t>, min_v<int32_t>), int32_t(minrr_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, minr_v<int32_t>), int32_t(minr_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zero_v<int32_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zeror_v<int32_t>), int32_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zerol_v<int32_t>), int32_t(minrr_v<int16_t>));

    EXPECT_THAT(saturating_sub(max_v<int16_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, max_v<int32_t>), int32_t(max_v<int16_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_sub(max_v<int16_t>, maxl_v<int32_t>), int32_t(max_v<int16_t>) + minrr_v<int32_t>);
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zero_v<int32_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zeror_v<int32_t>), int32_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zerol_v<int32_t>), int32_t(max_v<int16_t>) + zeror_v<int32_t>);

    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, max_v<int32_t>), int32_t(max_v<int16_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, maxl_v<int32_t>), int32_t(max_v<int16_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zero_v<int32_t>), int32_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zeror_v<int32_t>), int32_t(maxll_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zerol_v<int32_t>), int32_t(max_v<int16_t>));

    EXPECT_THAT(saturating_sub(zero_v<int16_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, max_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zeror_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zerol_v<int32_t>), int32_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, max_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zero_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zerol_v<int32_t>), int32_t(zerorr_v<int32_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, minr_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, maxl_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zero_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zeror_v<int32_t>), int32_t(zeroll_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_sub(min_v<uint16_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, min_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(max_v<uint16_t>, min_v<uint32_t>), uint32_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, minr_v<uint32_t>), uint32_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, min_v<uint32_t>), uint32_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, minr_v<uint32_t>), uint32_t(maxll_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));
}

TEST(SaturatingSubTest, Test_16_64) {
    EXPECT_THAT(saturating_sub(min_v<int16_t>, min_v<int64_t>), int64_t(minr_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int16_t>, minr_v<int64_t>), int64_t(min_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int16_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zero_v<int64_t>), int64_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zeror_v<int64_t>), int64_t(min_v<int16_t>) - zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int16_t>, zerol_v<int64_t>), int64_t(minr_v<int16_t>));

    EXPECT_THAT(saturating_sub(minr_v<int16_t>, min_v<int64_t>), int64_t(minrr_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, minr_v<int64_t>), int64_t(minr_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zero_v<int64_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zeror_v<int64_t>), int64_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(minr_v<int16_t>, zerol_v<int64_t>), int64_t(minrr_v<int16_t>));

    EXPECT_THAT(saturating_sub(max_v<int16_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, max_v<int64_t>), int64_t(max_v<int16_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int16_t>, maxl_v<int64_t>), int64_t(max_v<int16_t>) + minrr_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zero_v<int64_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zeror_v<int64_t>), int64_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(max_v<int16_t>, zerol_v<int64_t>), int64_t(max_v<int16_t>) + zeror_v<int64_t>);

    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, max_v<int64_t>), int64_t(max_v<int16_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, maxl_v<int64_t>), int64_t(max_v<int16_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zero_v<int64_t>), int64_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zeror_v<int64_t>), int64_t(maxll_v<int16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int16_t>, zerol_v<int64_t>), int64_t(max_v<int16_t>));

    EXPECT_THAT(saturating_sub(zero_v<int16_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, max_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zeror_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int16_t>, zerol_v<int64_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, max_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zero_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int16_t>, zerol_v<int64_t>), int64_t(zerorr_v<int64_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, minr_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, maxl_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zero_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zeror_v<int64_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int16_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_sub(min_v<uint16_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint16_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, min_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint16_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(max_v<uint16_t>, min_v<uint64_t>), uint64_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, minr_v<uint64_t>), uint64_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint16_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, min_v<uint64_t>), uint64_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, minr_v<uint64_t>), uint64_t(maxll_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint16_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));
}

TEST(SaturatingSubTest, Test_32_8) {
    EXPECT_THAT(saturating_sub(min_v<int32_t>, min_v<int8_t>), int32_t(max_v<int8_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int8_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int32_t>, max_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, maxl_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zero_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zeror_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zerol_v<int8_t>), int32_t(minr_v<int32_t>));

    EXPECT_THAT(saturating_sub(minr_v<int32_t>, min_v<int8_t>), int32_t(max_v<int8_t>) + minrr_v<int32_t>);
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int8_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, max_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, maxl_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zero_v<int8_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zeror_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zerol_v<int8_t>), int32_t(minrr_v<int32_t>));

    EXPECT_THAT(saturating_sub(max_v<int32_t>, min_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, max_v<int8_t>), int32_t(minr_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(max_v<int32_t>, maxl_v<int8_t>), int32_t(minrr_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zero_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zeror_v<int8_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zerol_v<int8_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, min_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, max_v<int8_t>), int32_t(min_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, maxl_v<int8_t>), int32_t(minr_v<int8_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zero_v<int8_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zeror_v<int8_t>), int32_t(maxll_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zerol_v<int8_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_sub(zero_v<int32_t>, min_v<int8_t>), int32_t(max_v<int8_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, max_v<int8_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, maxl_v<int8_t>), int32_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zeror_v<int8_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zerol_v<int8_t>), int32_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, min_v<int8_t>), int32_t(max_v<int8_t>) + zerorr_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int8_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, max_v<int8_t>), int32_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, maxl_v<int8_t>), int32_t(minrr_v<int8_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zero_v<int8_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zeror_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zerol_v<int8_t>), int32_t(zerorr_v<int32_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, min_v<int8_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, minr_v<int8_t>), int32_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, max_v<int8_t>), int32_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, maxl_v<int8_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zero_v<int8_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zeror_v<int8_t>), int32_t(zeroll_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zerol_v<int8_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_sub(min_v<uint32_t>, min_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, minr_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, max_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, maxl_v<uint8_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, min_v<uint8_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, minr_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, max_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, maxl_v<uint8_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(max_v<uint32_t>, min_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, minr_v<uint8_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, max_v<uint8_t>), max_v<uint32_t> - uint32_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, maxl_v<uint8_t>), max_v<uint32_t> - uint32_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, min_v<uint8_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, minr_v<uint8_t>), uint32_t(maxll_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, max_v<uint8_t>), maxl_v<uint32_t> - uint32_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, maxl_v<uint8_t>), max_v<uint32_t> - uint32_t(max_v<uint8_t>));
}

TEST(SaturatingSubTest, Test_32_16) {
    EXPECT_THAT(saturating_sub(min_v<int32_t>, min_v<int16_t>), int32_t(max_v<int16_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int16_t>) + min_v<int32_t>);
    EXPECT_THAT(saturating_sub(min_v<int32_t>, max_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, maxl_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zero_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zeror_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zerol_v<int16_t>), int32_t(minr_v<int32_t>));

    EXPECT_THAT(saturating_sub(minr_v<int32_t>, min_v<int16_t>), int32_t(max_v<int16_t>) + minrr_v<int32_t>);
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int16_t>) + minr_v<int32_t>);
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, max_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, maxl_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zero_v<int16_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zeror_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zerol_v<int16_t>), int32_t(minrr_v<int32_t>));

    EXPECT_THAT(saturating_sub(max_v<int32_t>, min_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, max_v<int16_t>), int32_t(minr_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(max_v<int32_t>, maxl_v<int16_t>), int32_t(minrr_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zero_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zeror_v<int16_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zerol_v<int16_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, min_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, max_v<int16_t>), int32_t(min_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, maxl_v<int16_t>), int32_t(minr_v<int16_t>) + max_v<int32_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zero_v<int16_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zeror_v<int16_t>), int32_t(maxll_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zerol_v<int16_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_sub(zero_v<int32_t>, min_v<int16_t>), int32_t(max_v<int16_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, max_v<int16_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, maxl_v<int16_t>), int32_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zeror_v<int16_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zerol_v<int16_t>), int32_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, min_v<int16_t>), int32_t(max_v<int16_t>) + zerorr_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int16_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, max_v<int16_t>), int32_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, maxl_v<int16_t>), int32_t(minrr_v<int16_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zero_v<int16_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zeror_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zerol_v<int16_t>), int32_t(zerorr_v<int32_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, min_v<int16_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, minr_v<int16_t>), int32_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, max_v<int16_t>), int32_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, maxl_v<int16_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zero_v<int16_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zeror_v<int16_t>), int32_t(zeroll_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zerol_v<int16_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_sub(min_v<uint32_t>, min_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, minr_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, max_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, maxl_v<uint16_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, min_v<uint16_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, minr_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, max_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, maxl_v<uint16_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(max_v<uint32_t>, min_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, minr_v<uint16_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, max_v<uint16_t>), max_v<uint32_t> - uint32_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, maxl_v<uint16_t>), max_v<uint32_t> - uint32_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, min_v<uint16_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, minr_v<uint16_t>), uint32_t(maxll_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, max_v<uint16_t>), maxl_v<uint32_t> - uint32_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, maxl_v<uint16_t>), max_v<uint32_t> - uint32_t(max_v<uint16_t>));
}

TEST(SaturatingSubTest, Test_32_32) {
    EXPECT_THAT(saturating_sub(min_v<int32_t>, min_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, minr_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zero_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zeror_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zerol_v<int32_t>), int32_t(minr_v<int32_t>));

    EXPECT_THAT(saturating_sub(minr_v<int32_t>, min_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, minr_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zero_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zeror_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zerol_v<int32_t>), int32_t(minrr_v<int32_t>));

    EXPECT_THAT(saturating_sub(max_v<int32_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, max_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, maxl_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zero_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zeror_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zerol_v<int32_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, max_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, maxl_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zero_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zeror_v<int32_t>), int32_t(maxll_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zerol_v<int32_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_sub(zero_v<int32_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, max_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zeror_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zerol_v<int32_t>), int32_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, max_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zero_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zerol_v<int32_t>), int32_t(zerorr_v<int32_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, minr_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, maxl_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zero_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zeror_v<int32_t>), int32_t(zeroll_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_sub(min_v<uint32_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, min_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_sub(max_v<uint32_t>, min_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, minr_v<uint32_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, maxl_v<uint32_t>), uint32_t(minr_v<uint32_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, min_v<uint32_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, minr_v<uint32_t>), uint32_t(maxll_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));
}

TEST(SaturatingSubTest, Test_32_64) {
    EXPECT_THAT(saturating_sub(min_v<int32_t>, min_v<int64_t>), int64_t(minr_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int32_t>, minr_v<int64_t>), int64_t(min_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int32_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zero_v<int64_t>), int64_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zeror_v<int64_t>), int64_t(min_v<int32_t>) - zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int32_t>, zerol_v<int64_t>), int64_t(minr_v<int32_t>));

    EXPECT_THAT(saturating_sub(minr_v<int32_t>, min_v<int64_t>), int64_t(minrr_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, minr_v<int64_t>), int64_t(minr_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zero_v<int64_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zeror_v<int64_t>), int64_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(minr_v<int32_t>, zerol_v<int64_t>), int64_t(minrr_v<int32_t>));

    EXPECT_THAT(saturating_sub(max_v<int32_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, max_v<int64_t>), int64_t(max_v<int32_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int32_t>, maxl_v<int64_t>), int64_t(max_v<int32_t>) + minrr_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zero_v<int64_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zeror_v<int64_t>), int64_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(max_v<int32_t>, zerol_v<int64_t>), int64_t(max_v<int32_t>) + zeror_v<int64_t>);

    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, max_v<int64_t>), int64_t(max_v<int32_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, maxl_v<int64_t>), int64_t(max_v<int32_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zero_v<int64_t>), int64_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zeror_v<int64_t>), int64_t(maxll_v<int32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int32_t>, zerol_v<int64_t>), int64_t(max_v<int32_t>));

    EXPECT_THAT(saturating_sub(zero_v<int32_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, max_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zeror_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int32_t>, zerol_v<int64_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, max_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zero_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int32_t>, zerol_v<int64_t>), int64_t(zerorr_v<int64_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, minr_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, maxl_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zero_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zeror_v<int64_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int32_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_sub(min_v<uint32_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint32_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, min_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint32_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(max_v<uint32_t>, min_v<uint64_t>), uint64_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, minr_v<uint64_t>), uint64_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint32_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, min_v<uint64_t>), uint64_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, minr_v<uint64_t>), uint64_t(maxll_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint32_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));
}

TEST(SaturatingSubTest, Test_64_8) {
    EXPECT_THAT(saturating_sub(min_v<int64_t>, min_v<int8_t>), int64_t(max_v<int8_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int8_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int64_t>, max_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, maxl_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zero_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zeror_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zerol_v<int8_t>), int64_t(minr_v<int64_t>));

    EXPECT_THAT(saturating_sub(minr_v<int64_t>, min_v<int8_t>), int64_t(max_v<int8_t>) + minrr_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int8_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, max_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, maxl_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zero_v<int8_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zeror_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zerol_v<int8_t>), int64_t(minrr_v<int64_t>));

    EXPECT_THAT(saturating_sub(max_v<int64_t>, min_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, max_v<int8_t>), int64_t(minr_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int64_t>, maxl_v<int8_t>), int64_t(minrr_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zero_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zeror_v<int8_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zerol_v<int8_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, min_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, max_v<int8_t>), int64_t(min_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, maxl_v<int8_t>), int64_t(minr_v<int8_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zero_v<int8_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zeror_v<int8_t>), int64_t(maxll_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zerol_v<int8_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_sub(zero_v<int64_t>, min_v<int8_t>), int64_t(max_v<int8_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, max_v<int8_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, maxl_v<int8_t>), int64_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zeror_v<int8_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zerol_v<int8_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, min_v<int8_t>), int64_t(max_v<int8_t>) + zerorr_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int8_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, max_v<int8_t>), int64_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, maxl_v<int8_t>), int64_t(minrr_v<int8_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zero_v<int8_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zeror_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zerol_v<int8_t>), int64_t(zerorr_v<int64_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, min_v<int8_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, minr_v<int8_t>), int64_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, max_v<int8_t>), int64_t(min_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, maxl_v<int8_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zero_v<int8_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zeror_v<int8_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zerol_v<int8_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_sub(min_v<uint64_t>, min_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, minr_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, max_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, maxl_v<uint8_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, min_v<uint8_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, minr_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, max_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, maxl_v<uint8_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(max_v<uint64_t>, min_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, minr_v<uint8_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, max_v<uint8_t>), max_v<uint64_t> - uint64_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, maxl_v<uint8_t>), max_v<uint64_t> - uint64_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, min_v<uint8_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, minr_v<uint8_t>), uint64_t(maxll_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, max_v<uint8_t>), maxl_v<uint64_t> - uint64_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, maxl_v<uint8_t>), max_v<uint64_t> - uint64_t(max_v<uint8_t>));
}

TEST(SaturatingSubTest, Test_64_16) {
    EXPECT_THAT(saturating_sub(min_v<int64_t>, min_v<int16_t>), int64_t(max_v<int16_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int16_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int64_t>, max_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, maxl_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zero_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zeror_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zerol_v<int16_t>), int64_t(minr_v<int64_t>));

    EXPECT_THAT(saturating_sub(minr_v<int64_t>, min_v<int16_t>), int64_t(max_v<int16_t>) + minrr_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int16_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, max_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, maxl_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zero_v<int16_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zeror_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zerol_v<int16_t>), int64_t(minrr_v<int64_t>));

    EXPECT_THAT(saturating_sub(max_v<int64_t>, min_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, max_v<int16_t>), int64_t(minr_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int64_t>, maxl_v<int16_t>), int64_t(minrr_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zero_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zeror_v<int16_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zerol_v<int16_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, min_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, max_v<int16_t>), int64_t(min_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, maxl_v<int16_t>), int64_t(minr_v<int16_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zero_v<int16_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zeror_v<int16_t>), int64_t(maxll_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zerol_v<int16_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_sub(zero_v<int64_t>, min_v<int16_t>), int64_t(max_v<int16_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, max_v<int16_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, maxl_v<int16_t>), int64_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zeror_v<int16_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zerol_v<int16_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, min_v<int16_t>), int64_t(max_v<int16_t>) + zerorr_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int16_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, max_v<int16_t>), int64_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, maxl_v<int16_t>), int64_t(minrr_v<int16_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zero_v<int16_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zeror_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zerol_v<int16_t>), int64_t(zerorr_v<int64_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, min_v<int16_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, minr_v<int16_t>), int64_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, max_v<int16_t>), int64_t(min_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, maxl_v<int16_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zero_v<int16_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zeror_v<int16_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zerol_v<int16_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_sub(min_v<uint64_t>, min_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, minr_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, max_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, maxl_v<uint16_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, min_v<uint16_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, minr_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, max_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, maxl_v<uint16_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(max_v<uint64_t>, min_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, minr_v<uint16_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, max_v<uint16_t>), max_v<uint64_t> - uint64_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, maxl_v<uint16_t>), max_v<uint64_t> - uint64_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, min_v<uint16_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, minr_v<uint16_t>), uint64_t(maxll_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, max_v<uint16_t>), maxl_v<uint64_t> - uint64_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, maxl_v<uint16_t>), max_v<uint64_t> - uint64_t(max_v<uint16_t>));
}

TEST(SaturatingSubTest, Test_64_32) {
    EXPECT_THAT(saturating_sub(min_v<int64_t>, min_v<int32_t>), int64_t(max_v<int32_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int32_t>) + min_v<int64_t>);
    EXPECT_THAT(saturating_sub(min_v<int64_t>, max_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, maxl_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zero_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zeror_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zerol_v<int32_t>), int64_t(minr_v<int64_t>));

    EXPECT_THAT(saturating_sub(minr_v<int64_t>, min_v<int32_t>), int64_t(max_v<int32_t>) + minrr_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int32_t>) + minr_v<int64_t>);
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, max_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, maxl_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zero_v<int32_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zeror_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zerol_v<int32_t>), int64_t(minrr_v<int64_t>));

    EXPECT_THAT(saturating_sub(max_v<int64_t>, min_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, max_v<int32_t>), int64_t(minr_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int64_t>, maxl_v<int32_t>), int64_t(minrr_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zero_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zeror_v<int32_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zerol_v<int32_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, min_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, max_v<int32_t>), int64_t(min_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, maxl_v<int32_t>), int64_t(minr_v<int32_t>) + max_v<int64_t>);
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zero_v<int32_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zeror_v<int32_t>), int64_t(maxll_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zerol_v<int32_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_sub(zero_v<int64_t>, min_v<int32_t>), int64_t(max_v<int32_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, max_v<int32_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, maxl_v<int32_t>), int64_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zeror_v<int32_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zerol_v<int32_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, min_v<int32_t>), int64_t(max_v<int32_t>) + zerorr_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int32_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, max_v<int32_t>), int64_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, maxl_v<int32_t>), int64_t(minrr_v<int32_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zero_v<int32_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zeror_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zerol_v<int32_t>), int64_t(zerorr_v<int64_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, min_v<int32_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, minr_v<int32_t>), int64_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, max_v<int32_t>), int64_t(min_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, maxl_v<int32_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zero_v<int32_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zeror_v<int32_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zerol_v<int32_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_sub(min_v<uint64_t>, min_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, minr_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, max_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, maxl_v<uint32_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, min_v<uint32_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, minr_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, max_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, maxl_v<uint32_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(max_v<uint64_t>, min_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, minr_v<uint32_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, max_v<uint32_t>), max_v<uint64_t> - uint64_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, maxl_v<uint32_t>), max_v<uint64_t> - uint64_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, min_v<uint32_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, minr_v<uint32_t>), uint64_t(maxll_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, max_v<uint32_t>), maxl_v<uint64_t> - uint64_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, maxl_v<uint32_t>), max_v<uint64_t> - uint64_t(max_v<uint32_t>));
}

TEST(SaturatingSubTest, Test_64_64) {
    EXPECT_THAT(saturating_sub(min_v<int64_t>, min_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, minr_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zero_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zeror_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(min_v<int64_t>, zerol_v<int64_t>), int64_t(minr_v<int64_t>));

    EXPECT_THAT(saturating_sub(minr_v<int64_t>, min_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, minr_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zero_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zeror_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(minr_v<int64_t>, zerol_v<int64_t>), int64_t(minrr_v<int64_t>));

    EXPECT_THAT(saturating_sub(max_v<int64_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, max_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, maxl_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zero_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zeror_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(max_v<int64_t>, zerol_v<int64_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, max_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, maxl_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zero_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zeror_v<int64_t>), int64_t(maxll_v<int64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<int64_t>, zerol_v<int64_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_sub(zero_v<int64_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, max_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zeror_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zero_v<int64_t>, zerol_v<int64_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, max_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zero_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_sub(zeror_v<int64_t>, zerol_v<int64_t>), int64_t(zerorr_v<int64_t>));

    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, minr_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, maxl_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zero_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zeror_v<int64_t>), int64_t(zeroll_v<int64_t>));
    EXPECT_THAT(saturating_sub(zerol_v<int64_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_sub(min_v<uint64_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(min_v<uint64_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, min_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(minr_v<uint64_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_sub(max_v<uint64_t>, min_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, minr_v<uint64_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(max_v<uint64_t>, maxl_v<uint64_t>), uint64_t(minr_v<uint64_t>));

    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, min_v<uint64_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, minr_v<uint64_t>), uint64_t(maxll_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_sub(maxl_v<uint64_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));
}

TEST(SaturatingMulTest, Test_8_8) {
    EXPECT_THAT(saturating_mul(min_v<int8_t>, min_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, minr_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, max_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, maxl_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zeror_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zerol_v<int8_t>), int8_t(max_v<int8_t>));

    EXPECT_THAT(saturating_mul(minr_v<int8_t>, min_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, minr_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, max_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, maxl_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zeror_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zerol_v<int8_t>), int8_t(max_v<int8_t>));

    EXPECT_THAT(saturating_mul(max_v<int8_t>, min_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, minr_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, max_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, maxl_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zeror_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zerol_v<int8_t>), int8_t(minr_v<int8_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, min_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, minr_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, max_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, maxl_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zeror_v<int8_t>), int8_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zerol_v<int8_t>), int8_t(minrr_v<int8_t>));

    EXPECT_THAT(saturating_mul(zero_v<int8_t>, min_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, minr_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, max_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, maxl_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zeror_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zerol_v<int8_t>), int8_t(zero_v<int8_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, min_v<int8_t>), int8_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, minr_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, max_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, maxl_v<int8_t>), int8_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zeror_v<int8_t>), int8_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zerol_v<int8_t>), int8_t(zerol_v<int8_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, min_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, minr_v<int8_t>), int8_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, max_v<int8_t>), int8_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, maxl_v<int8_t>), int8_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zero_v<int8_t>), int8_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zeror_v<int8_t>), int8_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zerol_v<int8_t>), int8_t(zeror_v<int8_t>));

    EXPECT_THAT(saturating_mul(min_v<uint8_t>, min_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, minr_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, max_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, maxl_v<uint8_t>), uint8_t(min_v<uint8_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, min_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, minr_v<uint8_t>), uint8_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, max_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, maxl_v<uint8_t>), uint8_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_mul(max_v<uint8_t>, min_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, minr_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, max_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, maxl_v<uint8_t>), uint8_t(max_v<uint8_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, min_v<uint8_t>), uint8_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, minr_v<uint8_t>), uint8_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, max_v<uint8_t>), uint8_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, maxl_v<uint8_t>), uint8_t(max_v<uint8_t>));
}

TEST(SaturatingMulTest, Test_8_16) {
    EXPECT_THAT(saturating_mul(min_v<int8_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, maxl_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zeror_v<int16_t>), int16_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zerol_v<int16_t>), int16_t(max_v<int8_t>) + zeror_v<int16_t>);

    EXPECT_THAT(saturating_mul(minr_v<int8_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, maxl_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zeror_v<int16_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zerol_v<int16_t>), int16_t(max_v<int8_t>));

    EXPECT_THAT(saturating_mul(max_v<int8_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zeror_v<int16_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zerol_v<int16_t>), int16_t(minr_v<int8_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zeror_v<int16_t>), int16_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zerol_v<int16_t>), int16_t(minrr_v<int8_t>));

    EXPECT_THAT(saturating_mul(zero_v<int8_t>, min_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, minr_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, max_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, maxl_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zeror_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zerol_v<int16_t>), int16_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, minr_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, maxl_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zeror_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zerol_v<int16_t>), int16_t(zerol_v<int16_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, max_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, maxl_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zeror_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zerol_v<int16_t>), int16_t(zeror_v<int16_t>));

    EXPECT_THAT(saturating_mul(min_v<uint8_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, minr_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, minr_v<uint16_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, maxl_v<uint16_t>), uint16_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_mul(max_v<uint8_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, minr_v<uint16_t>), uint16_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, minr_v<uint16_t>), uint16_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));
}

TEST(SaturatingMulTest, Test_8_32) {
    EXPECT_THAT(saturating_mul(min_v<int8_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zeror_v<int32_t>), int32_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zerol_v<int32_t>), int32_t(max_v<int8_t>) + zeror_v<int32_t>);

    EXPECT_THAT(saturating_mul(minr_v<int8_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zeror_v<int32_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zerol_v<int32_t>), int32_t(max_v<int8_t>));

    EXPECT_THAT(saturating_mul(max_v<int8_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zeror_v<int32_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zerol_v<int32_t>), int32_t(minr_v<int8_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zeror_v<int32_t>), int32_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zerol_v<int32_t>), int32_t(minrr_v<int8_t>));

    EXPECT_THAT(saturating_mul(zero_v<int8_t>, min_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, minr_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, max_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, maxl_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, minr_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, maxl_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zeror_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zerol_v<int32_t>), int32_t(zerol_v<int32_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, max_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zeror_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zerol_v<int32_t>), int32_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_mul(min_v<uint8_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, minr_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, maxl_v<uint32_t>), uint32_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_mul(max_v<uint8_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, minr_v<uint32_t>), uint32_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, minr_v<uint32_t>), uint32_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingMulTest, Test_8_64) {
    EXPECT_THAT(saturating_mul(min_v<int8_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zeror_v<int64_t>), int64_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(min_v<int8_t>, zerol_v<int64_t>), int64_t(max_v<int8_t>) + zeror_v<int64_t>);

    EXPECT_THAT(saturating_mul(minr_v<int8_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zeror_v<int64_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(minr_v<int8_t>, zerol_v<int64_t>), int64_t(max_v<int8_t>));

    EXPECT_THAT(saturating_mul(max_v<int8_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zeror_v<int64_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(max_v<int8_t>, zerol_v<int64_t>), int64_t(minr_v<int8_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zeror_v<int64_t>), int64_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int8_t>, zerol_v<int64_t>), int64_t(minrr_v<int8_t>));

    EXPECT_THAT(saturating_mul(zero_v<int8_t>, min_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, minr_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, max_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, maxl_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int8_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, minr_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, maxl_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zeror_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int8_t>, zerol_v<int64_t>), int64_t(zerol_v<int64_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, max_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zeror_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int8_t>, zerol_v<int64_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_mul(min_v<uint8_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint8_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, minr_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint8_t>, maxl_v<uint64_t>), uint64_t(maxl_v<uint64_t>));

    EXPECT_THAT(saturating_mul(max_v<uint8_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, minr_v<uint64_t>), uint64_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint8_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, minr_v<uint64_t>), uint64_t(maxl_v<uint8_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint8_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingMulTest, Test_16_8) {
    EXPECT_THAT(saturating_mul(min_v<int16_t>, min_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, max_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, maxl_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zeror_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zerol_v<int8_t>), int16_t(max_v<int16_t>));

    EXPECT_THAT(saturating_mul(minr_v<int16_t>, min_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, max_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, maxl_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zeror_v<int8_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zerol_v<int8_t>), int16_t(max_v<int16_t>));

    EXPECT_THAT(saturating_mul(max_v<int16_t>, min_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, minr_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, max_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, maxl_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zeror_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zerol_v<int8_t>), int16_t(minr_v<int16_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, min_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, minr_v<int8_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, max_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, maxl_v<int8_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zeror_v<int8_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zerol_v<int8_t>), int16_t(minrr_v<int16_t>));

    EXPECT_THAT(saturating_mul(zero_v<int16_t>, min_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, minr_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, max_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, maxl_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zeror_v<int8_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zerol_v<int8_t>), int16_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, min_v<int8_t>), int16_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, minr_v<int8_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, max_v<int8_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, maxl_v<int8_t>), int16_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zeror_v<int8_t>), int16_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zerol_v<int8_t>), int16_t(zerol_v<int8_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, min_v<int8_t>), int16_t(max_v<int8_t>) + zeror_v<int16_t>);
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, minr_v<int8_t>), int16_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, max_v<int8_t>), int16_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, maxl_v<int8_t>), int16_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zero_v<int8_t>), int16_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zeror_v<int8_t>), int16_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zerol_v<int8_t>), int16_t(zeror_v<int8_t>));

    EXPECT_THAT(saturating_mul(min_v<uint16_t>, min_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, minr_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, max_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, maxl_v<uint8_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, min_v<uint8_t>), uint16_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, minr_v<uint8_t>), uint16_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, max_v<uint8_t>), uint16_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, maxl_v<uint8_t>), uint16_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_mul(max_v<uint16_t>, min_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, minr_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, max_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, maxl_v<uint8_t>), uint16_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, min_v<uint8_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, minr_v<uint8_t>), uint16_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, max_v<uint8_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, maxl_v<uint8_t>), uint16_t(max_v<uint16_t>));
}

TEST(SaturatingMulTest, Test_16_16) {
    EXPECT_THAT(saturating_mul(min_v<int16_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, maxl_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zeror_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zerol_v<int16_t>), int16_t(max_v<int16_t>));

    EXPECT_THAT(saturating_mul(minr_v<int16_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, max_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, maxl_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zeror_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zerol_v<int16_t>), int16_t(max_v<int16_t>));

    EXPECT_THAT(saturating_mul(max_v<int16_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zeror_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zerol_v<int16_t>), int16_t(minr_v<int16_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, minr_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, maxl_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zeror_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zerol_v<int16_t>), int16_t(minrr_v<int16_t>));

    EXPECT_THAT(saturating_mul(zero_v<int16_t>, min_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, minr_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, max_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, maxl_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zeror_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zerol_v<int16_t>), int16_t(zero_v<int16_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, min_v<int16_t>), int16_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, minr_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, max_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, maxl_v<int16_t>), int16_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zeror_v<int16_t>), int16_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zerol_v<int16_t>), int16_t(zerol_v<int16_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, min_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, minr_v<int16_t>), int16_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, max_v<int16_t>), int16_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, maxl_v<int16_t>), int16_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zero_v<int16_t>), int16_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zeror_v<int16_t>), int16_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zerol_v<int16_t>), int16_t(zeror_v<int16_t>));

    EXPECT_THAT(saturating_mul(min_v<uint16_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, minr_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, max_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, maxl_v<uint16_t>), uint16_t(min_v<uint16_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, minr_v<uint16_t>), uint16_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, maxl_v<uint16_t>), uint16_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_mul(max_v<uint16_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, minr_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, min_v<uint16_t>), uint16_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, minr_v<uint16_t>), uint16_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, max_v<uint16_t>), uint16_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, maxl_v<uint16_t>), uint16_t(max_v<uint16_t>));
}

TEST(SaturatingMulTest, Test_16_32) {
    EXPECT_THAT(saturating_mul(min_v<int16_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zeror_v<int32_t>), int32_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zerol_v<int32_t>), int32_t(max_v<int16_t>) + zeror_v<int32_t>);

    EXPECT_THAT(saturating_mul(minr_v<int16_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zeror_v<int32_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zerol_v<int32_t>), int32_t(max_v<int16_t>));

    EXPECT_THAT(saturating_mul(max_v<int16_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zeror_v<int32_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zerol_v<int32_t>), int32_t(minr_v<int16_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zeror_v<int32_t>), int32_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zerol_v<int32_t>), int32_t(minrr_v<int16_t>));

    EXPECT_THAT(saturating_mul(zero_v<int16_t>, min_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, minr_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, max_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, maxl_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, minr_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, maxl_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zeror_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zerol_v<int32_t>), int32_t(zerol_v<int32_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, max_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zeror_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zerol_v<int32_t>), int32_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_mul(min_v<uint16_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, minr_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, maxl_v<uint32_t>), uint32_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_mul(max_v<uint16_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, minr_v<uint32_t>), uint32_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, minr_v<uint32_t>), uint32_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingMulTest, Test_16_64) {
    EXPECT_THAT(saturating_mul(min_v<int16_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zeror_v<int64_t>), int64_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(min_v<int16_t>, zerol_v<int64_t>), int64_t(max_v<int16_t>) + zeror_v<int64_t>);

    EXPECT_THAT(saturating_mul(minr_v<int16_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zeror_v<int64_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(minr_v<int16_t>, zerol_v<int64_t>), int64_t(max_v<int16_t>));

    EXPECT_THAT(saturating_mul(max_v<int16_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zeror_v<int64_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(max_v<int16_t>, zerol_v<int64_t>), int64_t(minr_v<int16_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zeror_v<int64_t>), int64_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int16_t>, zerol_v<int64_t>), int64_t(minrr_v<int16_t>));

    EXPECT_THAT(saturating_mul(zero_v<int16_t>, min_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, minr_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, max_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, maxl_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int16_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, minr_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, maxl_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zeror_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int16_t>, zerol_v<int64_t>), int64_t(zerol_v<int64_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, max_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zeror_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int16_t>, zerol_v<int64_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_mul(min_v<uint16_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint16_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, minr_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint16_t>, maxl_v<uint64_t>), uint64_t(maxl_v<uint64_t>));

    EXPECT_THAT(saturating_mul(max_v<uint16_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, minr_v<uint64_t>), uint64_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint16_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, minr_v<uint64_t>), uint64_t(maxl_v<uint16_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint16_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingMulTest, Test_32_8) {
    EXPECT_THAT(saturating_mul(min_v<int32_t>, min_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, max_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, maxl_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zeror_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zerol_v<int8_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_mul(minr_v<int32_t>, min_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, max_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, maxl_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zeror_v<int8_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zerol_v<int8_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_mul(max_v<int32_t>, min_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, minr_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, max_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, maxl_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zeror_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zerol_v<int8_t>), int32_t(minr_v<int32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, min_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, minr_v<int8_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, max_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, maxl_v<int8_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zeror_v<int8_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zerol_v<int8_t>), int32_t(minrr_v<int32_t>));

    EXPECT_THAT(saturating_mul(zero_v<int32_t>, min_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, minr_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, max_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, maxl_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zeror_v<int8_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zerol_v<int8_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, min_v<int8_t>), int32_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, minr_v<int8_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, max_v<int8_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, maxl_v<int8_t>), int32_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zeror_v<int8_t>), int32_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zerol_v<int8_t>), int32_t(zerol_v<int8_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, min_v<int8_t>), int32_t(max_v<int8_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, minr_v<int8_t>), int32_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, max_v<int8_t>), int32_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, maxl_v<int8_t>), int32_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zero_v<int8_t>), int32_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zeror_v<int8_t>), int32_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zerol_v<int8_t>), int32_t(zeror_v<int8_t>));

    EXPECT_THAT(saturating_mul(min_v<uint32_t>, min_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, minr_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, max_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, maxl_v<uint8_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, min_v<uint8_t>), uint32_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, minr_v<uint8_t>), uint32_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, max_v<uint8_t>), uint32_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, maxl_v<uint8_t>), uint32_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_mul(max_v<uint32_t>, min_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, minr_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, max_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, maxl_v<uint8_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, min_v<uint8_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, minr_v<uint8_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, max_v<uint8_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, maxl_v<uint8_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingMulTest, Test_32_16) {
    EXPECT_THAT(saturating_mul(min_v<int32_t>, min_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, max_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, maxl_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zeror_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zerol_v<int16_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_mul(minr_v<int32_t>, min_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, max_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, maxl_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zeror_v<int16_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zerol_v<int16_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_mul(max_v<int32_t>, min_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, minr_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, max_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, maxl_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zeror_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zerol_v<int16_t>), int32_t(minr_v<int32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, min_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, minr_v<int16_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, max_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, maxl_v<int16_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zeror_v<int16_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zerol_v<int16_t>), int32_t(minrr_v<int32_t>));

    EXPECT_THAT(saturating_mul(zero_v<int32_t>, min_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, minr_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, max_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, maxl_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zeror_v<int16_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zerol_v<int16_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, min_v<int16_t>), int32_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, minr_v<int16_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, max_v<int16_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, maxl_v<int16_t>), int32_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zeror_v<int16_t>), int32_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zerol_v<int16_t>), int32_t(zerol_v<int16_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, min_v<int16_t>), int32_t(max_v<int16_t>) + zeror_v<int32_t>);
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, minr_v<int16_t>), int32_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, max_v<int16_t>), int32_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, maxl_v<int16_t>), int32_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zero_v<int16_t>), int32_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zeror_v<int16_t>), int32_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zerol_v<int16_t>), int32_t(zeror_v<int16_t>));

    EXPECT_THAT(saturating_mul(min_v<uint32_t>, min_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, minr_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, max_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, maxl_v<uint16_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, min_v<uint16_t>), uint32_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, minr_v<uint16_t>), uint32_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, max_v<uint16_t>), uint32_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, maxl_v<uint16_t>), uint32_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_mul(max_v<uint32_t>, min_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, minr_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, max_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, maxl_v<uint16_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, min_v<uint16_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, minr_v<uint16_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, max_v<uint16_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, maxl_v<uint16_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingMulTest, Test_32_32) {
    EXPECT_THAT(saturating_mul(min_v<int32_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zeror_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zerol_v<int32_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_mul(minr_v<int32_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, max_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, maxl_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zeror_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zerol_v<int32_t>), int32_t(max_v<int32_t>));

    EXPECT_THAT(saturating_mul(max_v<int32_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zeror_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zerol_v<int32_t>), int32_t(minr_v<int32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, minr_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, maxl_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zeror_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zerol_v<int32_t>), int32_t(minrr_v<int32_t>));

    EXPECT_THAT(saturating_mul(zero_v<int32_t>, min_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, minr_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, max_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, maxl_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zeror_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zerol_v<int32_t>), int32_t(zero_v<int32_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, min_v<int32_t>), int32_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, minr_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, max_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, maxl_v<int32_t>), int32_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zeror_v<int32_t>), int32_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zerol_v<int32_t>), int32_t(zerol_v<int32_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, min_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, minr_v<int32_t>), int32_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, max_v<int32_t>), int32_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, maxl_v<int32_t>), int32_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zero_v<int32_t>), int32_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zeror_v<int32_t>), int32_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zerol_v<int32_t>), int32_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_mul(min_v<uint32_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, minr_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, max_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, maxl_v<uint32_t>), uint32_t(min_v<uint32_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, minr_v<uint32_t>), uint32_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, maxl_v<uint32_t>), uint32_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_mul(max_v<uint32_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, minr_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, min_v<uint32_t>), uint32_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, minr_v<uint32_t>), uint32_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, max_v<uint32_t>), uint32_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, maxl_v<uint32_t>), uint32_t(max_v<uint32_t>));
}

TEST(SaturatingMulTest, Test_32_64) {
    EXPECT_THAT(saturating_mul(min_v<int32_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zeror_v<int64_t>), int64_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(min_v<int32_t>, zerol_v<int64_t>), int64_t(max_v<int32_t>) + zeror_v<int64_t>);

    EXPECT_THAT(saturating_mul(minr_v<int32_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zeror_v<int64_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(minr_v<int32_t>, zerol_v<int64_t>), int64_t(max_v<int32_t>));

    EXPECT_THAT(saturating_mul(max_v<int32_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zeror_v<int64_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(max_v<int32_t>, zerol_v<int64_t>), int64_t(minr_v<int32_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zeror_v<int64_t>), int64_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int32_t>, zerol_v<int64_t>), int64_t(minrr_v<int32_t>));

    EXPECT_THAT(saturating_mul(zero_v<int32_t>, min_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, minr_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, max_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, maxl_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int32_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, minr_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, maxl_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zeror_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int32_t>, zerol_v<int64_t>), int64_t(zerol_v<int64_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, max_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zeror_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int32_t>, zerol_v<int64_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_mul(min_v<uint32_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint32_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, minr_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint32_t>, maxl_v<uint64_t>), uint64_t(maxl_v<uint64_t>));

    EXPECT_THAT(saturating_mul(max_v<uint32_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, minr_v<uint64_t>), uint64_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint32_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, minr_v<uint64_t>), uint64_t(maxl_v<uint32_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint32_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingMulTest, Test_64_8) {
    EXPECT_THAT(saturating_mul(min_v<int64_t>, min_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, max_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, maxl_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zeror_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zerol_v<int8_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_mul(minr_v<int64_t>, min_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, max_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, maxl_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zeror_v<int8_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zerol_v<int8_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_mul(max_v<int64_t>, min_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, minr_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, max_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, maxl_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zeror_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zerol_v<int8_t>), int64_t(minr_v<int64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, min_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, minr_v<int8_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, max_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, maxl_v<int8_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zeror_v<int8_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zerol_v<int8_t>), int64_t(minrr_v<int64_t>));

    EXPECT_THAT(saturating_mul(zero_v<int64_t>, min_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, minr_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, max_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, maxl_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zeror_v<int8_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zerol_v<int8_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, min_v<int8_t>), int64_t(min_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, minr_v<int8_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, max_v<int8_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, maxl_v<int8_t>), int64_t(maxl_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zeror_v<int8_t>), int64_t(zeror_v<int8_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zerol_v<int8_t>), int64_t(zerol_v<int8_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, min_v<int8_t>), int64_t(max_v<int8_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, minr_v<int8_t>), int64_t(max_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, max_v<int8_t>), int64_t(minr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, maxl_v<int8_t>), int64_t(minrr_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zero_v<int8_t>), int64_t(zero_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zeror_v<int8_t>), int64_t(zerol_v<int8_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zerol_v<int8_t>), int64_t(zeror_v<int8_t>));

    EXPECT_THAT(saturating_mul(min_v<uint64_t>, min_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, minr_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, max_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, maxl_v<uint8_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, min_v<uint8_t>), uint64_t(min_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, minr_v<uint8_t>), uint64_t(minr_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, max_v<uint8_t>), uint64_t(max_v<uint8_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, maxl_v<uint8_t>), uint64_t(maxl_v<uint8_t>));

    EXPECT_THAT(saturating_mul(max_v<uint64_t>, min_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, minr_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, max_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, maxl_v<uint8_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, min_v<uint8_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, minr_v<uint8_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, max_v<uint8_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, maxl_v<uint8_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingMulTest, Test_64_16) {
    EXPECT_THAT(saturating_mul(min_v<int64_t>, min_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, max_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, maxl_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zeror_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zerol_v<int16_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_mul(minr_v<int64_t>, min_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, max_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, maxl_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zeror_v<int16_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zerol_v<int16_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_mul(max_v<int64_t>, min_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, minr_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, max_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, maxl_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zeror_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zerol_v<int16_t>), int64_t(minr_v<int64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, min_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, minr_v<int16_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, max_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, maxl_v<int16_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zeror_v<int16_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zerol_v<int16_t>), int64_t(minrr_v<int64_t>));

    EXPECT_THAT(saturating_mul(zero_v<int64_t>, min_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, minr_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, max_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, maxl_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zeror_v<int16_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zerol_v<int16_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, min_v<int16_t>), int64_t(min_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, minr_v<int16_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, max_v<int16_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, maxl_v<int16_t>), int64_t(maxl_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zeror_v<int16_t>), int64_t(zeror_v<int16_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zerol_v<int16_t>), int64_t(zerol_v<int16_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, min_v<int16_t>), int64_t(max_v<int16_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, minr_v<int16_t>), int64_t(max_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, max_v<int16_t>), int64_t(minr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, maxl_v<int16_t>), int64_t(minrr_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zero_v<int16_t>), int64_t(zero_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zeror_v<int16_t>), int64_t(zerol_v<int16_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zerol_v<int16_t>), int64_t(zeror_v<int16_t>));

    EXPECT_THAT(saturating_mul(min_v<uint64_t>, min_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, minr_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, max_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, maxl_v<uint16_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, min_v<uint16_t>), uint64_t(min_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, minr_v<uint16_t>), uint64_t(minr_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, max_v<uint16_t>), uint64_t(max_v<uint16_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, maxl_v<uint16_t>), uint64_t(maxl_v<uint16_t>));

    EXPECT_THAT(saturating_mul(max_v<uint64_t>, min_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, minr_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, max_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, maxl_v<uint16_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, min_v<uint16_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, minr_v<uint16_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, max_v<uint16_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, maxl_v<uint16_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingMulTest, Test_64_32) {
    EXPECT_THAT(saturating_mul(min_v<int64_t>, min_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, max_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, maxl_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zeror_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zerol_v<int32_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_mul(minr_v<int64_t>, min_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, max_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, maxl_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zeror_v<int32_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zerol_v<int32_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_mul(max_v<int64_t>, min_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, minr_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, max_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, maxl_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zeror_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zerol_v<int32_t>), int64_t(minr_v<int64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, min_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, minr_v<int32_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, max_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, maxl_v<int32_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zeror_v<int32_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zerol_v<int32_t>), int64_t(minrr_v<int64_t>));

    EXPECT_THAT(saturating_mul(zero_v<int64_t>, min_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, minr_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, max_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, maxl_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zeror_v<int32_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zerol_v<int32_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, min_v<int32_t>), int64_t(min_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, minr_v<int32_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, max_v<int32_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, maxl_v<int32_t>), int64_t(maxl_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zeror_v<int32_t>), int64_t(zeror_v<int32_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zerol_v<int32_t>), int64_t(zerol_v<int32_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, min_v<int32_t>), int64_t(max_v<int32_t>) + zeror_v<int64_t>);
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, minr_v<int32_t>), int64_t(max_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, max_v<int32_t>), int64_t(minr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, maxl_v<int32_t>), int64_t(minrr_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zero_v<int32_t>), int64_t(zero_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zeror_v<int32_t>), int64_t(zerol_v<int32_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zerol_v<int32_t>), int64_t(zeror_v<int32_t>));

    EXPECT_THAT(saturating_mul(min_v<uint64_t>, min_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, minr_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, max_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, maxl_v<uint32_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, min_v<uint32_t>), uint64_t(min_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, minr_v<uint32_t>), uint64_t(minr_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, max_v<uint32_t>), uint64_t(max_v<uint32_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, maxl_v<uint32_t>), uint64_t(maxl_v<uint32_t>));

    EXPECT_THAT(saturating_mul(max_v<uint64_t>, min_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, minr_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, max_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, maxl_v<uint32_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, min_v<uint32_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, minr_v<uint32_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, max_v<uint32_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, maxl_v<uint32_t>), uint64_t(max_v<uint64_t>));
}

TEST(SaturatingMulTest, Test_64_64) {
    EXPECT_THAT(saturating_mul(min_v<int64_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zeror_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(min_v<int64_t>, zerol_v<int64_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_mul(minr_v<int64_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, max_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, maxl_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zeror_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(minr_v<int64_t>, zerol_v<int64_t>), int64_t(max_v<int64_t>));

    EXPECT_THAT(saturating_mul(max_v<int64_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zeror_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(max_v<int64_t>, zerol_v<int64_t>), int64_t(minr_v<int64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, minr_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, maxl_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zeror_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<int64_t>, zerol_v<int64_t>), int64_t(minrr_v<int64_t>));

    EXPECT_THAT(saturating_mul(zero_v<int64_t>, min_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, minr_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, max_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, maxl_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zeror_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zero_v<int64_t>, zerol_v<int64_t>), int64_t(zero_v<int64_t>));

    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, min_v<int64_t>), int64_t(min_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, minr_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, max_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, maxl_v<int64_t>), int64_t(maxl_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zeror_v<int64_t>), int64_t(zeror_v<int64_t>));
    EXPECT_THAT(saturating_mul(zeror_v<int64_t>, zerol_v<int64_t>), int64_t(zerol_v<int64_t>));

    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, min_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, minr_v<int64_t>), int64_t(max_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, max_v<int64_t>), int64_t(minr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, maxl_v<int64_t>), int64_t(minrr_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zero_v<int64_t>), int64_t(zero_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zeror_v<int64_t>), int64_t(zerol_v<int64_t>));
    EXPECT_THAT(saturating_mul(zerol_v<int64_t>, zerol_v<int64_t>), int64_t(zeror_v<int64_t>));

    EXPECT_THAT(saturating_mul(min_v<uint64_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, minr_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, max_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(min_v<uint64_t>, maxl_v<uint64_t>), uint64_t(min_v<uint64_t>));

    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, minr_v<uint64_t>), uint64_t(minr_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(minr_v<uint64_t>, maxl_v<uint64_t>), uint64_t(maxl_v<uint64_t>));

    EXPECT_THAT(saturating_mul(max_v<uint64_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, minr_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(max_v<uint64_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));

    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, min_v<uint64_t>), uint64_t(min_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, minr_v<uint64_t>), uint64_t(maxl_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, max_v<uint64_t>), uint64_t(max_v<uint64_t>));
    EXPECT_THAT(saturating_mul(maxl_v<uint64_t>, maxl_v<uint64_t>), uint64_t(max_v<uint64_t>));
}
