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

TEST(SaturatingSanityTest, SaturatingDiv) {
    EXPECT_THAT(saturating_div(14, 4), 3);
    EXPECT_THAT(saturating_div(-14, 4), -3);
    EXPECT_THAT(saturating_div(int8_t(-128), int8_t(-1)), 127);
    EXPECT_THAT(saturating_div(int8_t(-128), int16_t(-1)), 128);
    EXPECT_THAT(saturating_div(int16_t(-128), int8_t(-1)), 128);
    EXPECT_THAT(saturating_div(int16_t(-128), int16_t(-1)), 128);
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

TEST(SaturatingSanityTest, CustomBuiltinMullOverflow) {
    int32_t result;
    EXPECT_TRUE(custom_builtin_mul_overflow(1 << 16, 1 << 15, &result));
    EXPECT_FALSE(custom_builtin_mul_overflow(1 << 15, 1 << 15, &result));
    EXPECT_THAT(result, 1 << 30);
    EXPECT_TRUE(custom_builtin_mul_overflow(std::numeric_limits<int32_t>::min(), 2, &result));
    EXPECT_TRUE(custom_builtin_mul_overflow(std::numeric_limits<int32_t>::min(), -1, &result));
    EXPECT_TRUE(custom_builtin_mul_overflow(std::numeric_limits<int32_t>::min(), -2, &result));
    EXPECT_FALSE(custom_builtin_mul_overflow(std::numeric_limits<int32_t>::min(), 1, &result));
    EXPECT_THAT(result, std::numeric_limits<int32_t>::min());
    EXPECT_FALSE(custom_builtin_mul_overflow(std::numeric_limits<int32_t>::min(), 0, &result));
    EXPECT_THAT(result, 0);

    EXPECT_TRUE(custom_builtin_mul_overflow(2, std::numeric_limits<int32_t>::min(), &result));
    EXPECT_TRUE(custom_builtin_mul_overflow(-1, std::numeric_limits<int32_t>::min(), &result));
    EXPECT_TRUE(custom_builtin_mul_overflow(-2, std::numeric_limits<int32_t>::min(), &result));
    EXPECT_FALSE(custom_builtin_mul_overflow(1, std::numeric_limits<int32_t>::min(), &result));
    EXPECT_THAT(result, std::numeric_limits<int32_t>::min());
    EXPECT_FALSE(custom_builtin_mul_overflow(0, std::numeric_limits<int32_t>::min(), &result));
    EXPECT_THAT(result, 0);

    EXPECT_FALSE(custom_builtin_mul_overflow(0, 0, &result));
    EXPECT_THAT(result, 0);

    EXPECT_FALSE(custom_builtin_mul_overflow(0, 1, &result));
    EXPECT_THAT(result, 0);
    EXPECT_FALSE(custom_builtin_mul_overflow(1, 0, &result));
    EXPECT_THAT(result, 0);

    EXPECT_TRUE(custom_builtin_mul_overflow(1 << 16, 1 << 15, &result));

    EXPECT_FALSE(custom_builtin_mul_overflow(95, 22605091, &result));
    EXPECT_THAT(result, 2147483645);

    uint32_t uresult;
    EXPECT_FALSE(custom_builtin_mul_overflow(65535u, 65537u, &uresult));
    EXPECT_THAT(uresult, 4294967295u);
    EXPECT_TRUE(custom_builtin_mul_overflow(65537u, 65537u, &uresult));
    EXPECT_FALSE(custom_builtin_mul_overflow(0u, 4294967295u, &uresult));
    EXPECT_THAT(uresult, 0);
    EXPECT_FALSE(custom_builtin_mul_overflow(4294967295u, 0u, &uresult));
    EXPECT_THAT(uresult, 0);
    EXPECT_FALSE(custom_builtin_mul_overflow(0u, 0u, &uresult));
    EXPECT_THAT(uresult, 0);
}

namespace {

template <typename From, typename Into>
inline constexpr bool is_explicitly_convertible_v = !std::is_convertible_v<From, Into> && std::is_constructible_v<Into, From>;

} // namespace

TEST(SaturatingTest, ValueConstructor) {
    static_assert(std::is_convertible_v<int8_t, int_sat8_t>);
    static_assert(std::is_convertible_v<int8_t, int_sat16_t>);
    static_assert(std::is_convertible_v<int8_t, int_sat32_t>);
    static_assert(std::is_convertible_v<int8_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int8_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int8_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int8_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int8_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int16_t, int_sat8_t>);
    static_assert(std::is_convertible_v<int16_t, int_sat16_t>);
    static_assert(std::is_convertible_v<int16_t, int_sat32_t>);
    static_assert(std::is_convertible_v<int16_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int16_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int16_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int16_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int16_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int32_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int32_t, int_sat16_t>);
    static_assert(std::is_convertible_v<int32_t, int_sat32_t>);
    static_assert(std::is_convertible_v<int32_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int32_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int32_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int32_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int32_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int64_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int64_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<int64_t, int_sat32_t>);
    static_assert(std::is_convertible_v<int64_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int64_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int64_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int64_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int64_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint8_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint8_t, int_sat64_t>);
    static_assert(std::is_convertible_v<uint8_t, uint_sat8_t>);
    static_assert(std::is_convertible_v<uint8_t, uint_sat16_t>);
    static_assert(std::is_convertible_v<uint8_t, uint_sat32_t>);
    static_assert(std::is_convertible_v<uint8_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint16_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint16_t, uint_sat8_t>);
    static_assert(std::is_convertible_v<uint16_t, uint_sat16_t>);
    static_assert(std::is_convertible_v<uint16_t, uint_sat32_t>);
    static_assert(std::is_convertible_v<uint16_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint32_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint32_t, uint_sat16_t>);
    static_assert(std::is_convertible_v<uint32_t, uint_sat32_t>);
    static_assert(std::is_convertible_v<uint32_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint64_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint64_t, uint_sat32_t>);
    static_assert(std::is_convertible_v<uint64_t, uint_sat64_t>);
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
    static_assert(std::is_convertible_v<int_sat8_t, int_sat16_t>);
    static_assert(std::is_convertible_v<int_sat8_t, int_sat32_t>);
    static_assert(std::is_convertible_v<int_sat8_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat8_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int_sat16_t, int_sat8_t>);
    static_assert(std::is_convertible_v<int_sat16_t, int_sat16_t>);
    static_assert(std::is_convertible_v<int_sat16_t, int_sat32_t>);
    static_assert(std::is_convertible_v<int_sat16_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint_sat16_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint_sat32_t>);
    static_assert(is_explicitly_convertible_v<int_sat16_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<int_sat32_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<int_sat32_t, int_sat16_t>);
    static_assert(std::is_convertible_v<int_sat32_t, int_sat32_t>);
    static_assert(std::is_convertible_v<int_sat32_t, int_sat64_t>);
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
    static_assert(std::is_convertible_v<uint_sat8_t, uint_sat16_t>);
    static_assert(std::is_convertible_v<uint_sat8_t, uint_sat32_t>);
    static_assert(std::is_convertible_v<uint_sat8_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat16_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat16_t, uint_sat8_t>);
    static_assert(std::is_convertible_v<uint_sat16_t, uint_sat16_t>);
    static_assert(std::is_convertible_v<uint_sat16_t, uint_sat32_t>);
    static_assert(std::is_convertible_v<uint_sat16_t, uint_sat64_t>);

    static_assert(is_explicitly_convertible_v<uint_sat32_t, int_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int_sat16_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int_sat32_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, int_sat64_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint_sat8_t>);
    static_assert(is_explicitly_convertible_v<uint_sat32_t, uint_sat16_t>);
    static_assert(std::is_convertible_v<uint_sat32_t, uint_sat32_t>);
    static_assert(std::is_convertible_v<uint_sat32_t, uint_sat64_t>);

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

TEST(SaturatingTest, Division) {
    static_assert(std::is_same_v<decltype(int_sat8_t(0) / int_sat8_t(0)), int_sat8_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) / int_sat16_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) / int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat8_t(0) / int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat16_t(0) / int_sat8_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) / int_sat16_t(0)), int_sat16_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) / int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat16_t(0) / int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat32_t(0) / int_sat8_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) / int_sat16_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) / int_sat32_t(0)), int_sat32_t>);
    static_assert(std::is_same_v<decltype(int_sat32_t(0) / int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(int_sat64_t(0) / int_sat8_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) / int_sat16_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) / int_sat32_t(0)), int_sat64_t>);
    static_assert(std::is_same_v<decltype(int_sat64_t(0) / int_sat64_t(0)), int_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat8_t(0) / uint_sat8_t(0)), uint_sat8_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) / uint_sat16_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) / uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat8_t(0) / uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat16_t(0) / uint_sat8_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) / uint_sat16_t(0)), uint_sat16_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) / uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat16_t(0) / uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat32_t(0) / uint_sat8_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) / uint_sat16_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) / uint_sat32_t(0)), uint_sat32_t>);
    static_assert(std::is_same_v<decltype(uint_sat32_t(0) / uint_sat64_t(0)), uint_sat64_t>);

    static_assert(std::is_same_v<decltype(uint_sat64_t(0) / uint_sat8_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) / uint_sat16_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) / uint_sat32_t(0)), uint_sat64_t>);
    static_assert(std::is_same_v<decltype(uint_sat64_t(0) / uint_sat64_t(0)), uint_sat64_t>);
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

TEST(SaturatingTest, IsSaturating) {
    static_assert(!is_saturating_v<int8_t>);
    static_assert(!is_saturating_v<int16_t>);
    static_assert(!is_saturating_v<int32_t>);
    static_assert(!is_saturating_v<int64_t>);
    static_assert(!is_saturating_v<uint8_t>);
    static_assert(!is_saturating_v<uint16_t>);
    static_assert(!is_saturating_v<uint32_t>);
    static_assert(!is_saturating_v<uint64_t>);
    static_assert(is_saturating_v<int_sat8_t>);
    static_assert(is_saturating_v<int_sat16_t>);
    static_assert(is_saturating_v<int_sat32_t>);
    static_assert(is_saturating_v<int_sat64_t>);
    static_assert(is_saturating_v<uint_sat8_t>);
    static_assert(is_saturating_v<uint_sat16_t>);
    static_assert(is_saturating_v<uint_sat32_t>);
    static_assert(is_saturating_v<uint_sat64_t>);
}

namespace {

template <typename T>
constexpr inline T min_v = std::numeric_limits<T>::min();

template <typename T>
constexpr inline T minr_v = min_v<T> + T(1);

template <typename T>
constexpr inline T max_v = std::numeric_limits<T>::max();

template <typename T>
constexpr inline T maxl_v = max_v<T> - T(1);

template <typename T>
constexpr inline T zero_v = T(0);

template <typename T>
constexpr inline T zeror_v = T(1);

template <typename T>
constexpr inline T zerol_v = [] {
    static_assert(std::is_signed_v<T>, "Only available for signed types");
    return T(-1);
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

namespace {

class SaturatingBinOpTestNames {
public:
    template <typename T>
    static std::string GetName(int) noexcept {
        std::string result;
        result += typeName<std::tuple_element_t<0, T>>();
        result += "_";
        result += typeName<std::tuple_element_t<1, T>>();
        return result;
    }

private:
    template <typename T>
    static const char* typeName() noexcept {
        if constexpr (std::is_same_v<T, int8_t>) {
            return "i8";
        } else if constexpr (std::is_same_v<T, int16_t>) {
            return "i16";
        } else if constexpr (std::is_same_v<T, int32_t>) {
            return "i32";
        } else if constexpr (std::is_same_v<T, int64_t>) {
            return "i64";
        } else if constexpr (std::is_same_v<T, uint8_t>) {
            return "u8";
        } else if constexpr (std::is_same_v<T, uint16_t>) {
            return "u16";
        } else if constexpr (std::is_same_v<T, uint32_t>) {
            return "u32";
        } else if constexpr (std::is_same_v<T, uint64_t>) {
            return "u64";
        } else {
            return "unknown";
        }
    }
};

using SaturatingBinOpSignedTestTypes = testing::Types<
        std::tuple<int8_t, int8_t>,
        std::tuple<int8_t, int16_t>,
        std::tuple<int8_t, int32_t>,
        std::tuple<int8_t, int64_t>,
        std::tuple<int16_t, int8_t>,
        std::tuple<int16_t, int16_t>,
        std::tuple<int16_t, int32_t>,
        std::tuple<int16_t, int64_t>,
        std::tuple<int32_t, int8_t>,
        std::tuple<int32_t, int16_t>,
        std::tuple<int32_t, int32_t>,
        std::tuple<int32_t, int64_t>,
        std::tuple<int64_t, int8_t>,
        std::tuple<int64_t, int16_t>,
        std::tuple<int64_t, int32_t>,
        std::tuple<int64_t, int64_t>>;

using SaturatingBinOpUnsignedTestTypes = testing::Types<
        std::tuple<uint8_t, uint8_t>,
        std::tuple<uint8_t, uint16_t>,
        std::tuple<uint8_t, uint32_t>,
        std::tuple<uint8_t, uint64_t>,
        std::tuple<uint16_t, uint8_t>,
        std::tuple<uint16_t, uint16_t>,
        std::tuple<uint16_t, uint32_t>,
        std::tuple<uint16_t, uint64_t>,
        std::tuple<uint32_t, uint8_t>,
        std::tuple<uint32_t, uint16_t>,
        std::tuple<uint32_t, uint32_t>,
        std::tuple<uint32_t, uint64_t>,
        std::tuple<uint64_t, uint8_t>,
        std::tuple<uint64_t, uint16_t>,
        std::tuple<uint64_t, uint32_t>,
        std::tuple<uint64_t, uint64_t>>;

} // namespace

template <typename T>
class SaturatingBinOpTest : public testing::Test {
public:
    using Lhs = std::tuple_element_t<0, T>;
    using Rhs = std::tuple_element_t<1, T>;
    using Result = internal::wider_t<Lhs, Rhs>;
};

template <typename T>
class SaturatingBinOpSignedTest : public SaturatingBinOpTest<T> {};

TYPED_TEST_SUITE(SaturatingBinOpSignedTest, SaturatingBinOpSignedTestTypes, SaturatingBinOpTestNames);

template <typename T>
class SaturatingBinOpUnsignedTest : public SaturatingBinOpTest<T> {};

TYPED_TEST_SUITE(SaturatingBinOpUnsignedTest, SaturatingBinOpUnsignedTestTypes, SaturatingBinOpTestNames);

#define EXPECT_REG_BIN_OP(name, op, lhs, rhs, Result) \
    EXPECT_THAT(saturating_##name((lhs), (rhs)), static_cast<Result>((lhs)) op static_cast<Result>((rhs)))

#define EXPECT_SAT_BIN_OP(name, lhs, rhs, actual) EXPECT_THAT(saturating_##name((lhs), (rhs)), (actual))

TYPED_TEST(SaturatingBinOpSignedTest, SaturatingAdd) {
    using Lhs = typename TestFixture::Lhs;
    using Rhs = typename TestFixture::Rhs;
    using Result = typename TestFixture::Result;

#define EXPECT_REG(lhs, rhs) EXPECT_REG_BIN_OP(add, +, lhs, rhs, Result)
#define EXPECT_MAX(lhs, rhs) EXPECT_SAT_BIN_OP(add, lhs, rhs, max_v<Result>)
#define EXPECT_MIN(lhs, rhs) EXPECT_SAT_BIN_OP(add, lhs, rhs, min_v<Result>)

    EXPECT_MIN(min_v<Lhs>, min_v<Rhs>);
    EXPECT_MIN(min_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, zeror_v<Rhs>);
    if constexpr (sizeof(Rhs) > sizeof(Lhs)) {
        EXPECT_REG(min_v<Lhs>, zerol_v<Rhs>);
    } else {
        EXPECT_MIN(min_v<Lhs>, zerol_v<Rhs>);
    }

    EXPECT_MIN(minr_v<Lhs>, min_v<Rhs>);
    EXPECT_MIN(minr_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(max_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, minr_v<Rhs>);
    EXPECT_MAX(max_v<Lhs>, max_v<Rhs>);
    EXPECT_MAX(max_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, zero_v<Rhs>);
    if constexpr (sizeof(Rhs) > sizeof(Lhs)) {
        EXPECT_REG(max_v<Lhs>, zeror_v<Rhs>);
    } else {
        EXPECT_MAX(max_v<Lhs>, zeror_v<Rhs>);
    }
    EXPECT_REG(max_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(maxl_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, minr_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, max_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(zero_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(zeror_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, minr_v<Rhs>);
    if constexpr (sizeof(Lhs) > sizeof(Rhs)) {
        EXPECT_REG(zeror_v<Lhs>, max_v<Rhs>);
    } else {
        EXPECT_MAX(zeror_v<Lhs>, max_v<Rhs>);
    }
    EXPECT_REG(zeror_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zerol_v<Rhs>);

    if constexpr (sizeof(Lhs) > sizeof(Rhs)) {
        EXPECT_REG(zerol_v<Lhs>, min_v<Rhs>);
    } else {
        EXPECT_MIN(zerol_v<Lhs>, min_v<Rhs>);
    }
    EXPECT_REG(zerol_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zerol_v<Rhs>);

#undef EXPECT_REG
#undef EXPECT_MAX
#undef EXPECT_MIN
}

TYPED_TEST(SaturatingBinOpUnsignedTest, SaturatingAdd) {
    using Lhs = typename TestFixture::Lhs;
    using Rhs = typename TestFixture::Rhs;
    using Result = typename TestFixture::Result;

#define EXPECT_REG(lhs, rhs) EXPECT_REG_BIN_OP(add, +, lhs, rhs, Result)
#define EXPECT_MAX(lhs, rhs) EXPECT_SAT_BIN_OP(add, lhs, rhs, max_v<Result>)
#define EXPECT_MIN(lhs, rhs) EXPECT_SAT_BIN_OP(add, lhs, rhs, min_v<Result>)

    EXPECT_REG(min_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(minr_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, minr_v<Rhs>);
    if constexpr (sizeof(Lhs) > sizeof(Rhs)) {
        EXPECT_REG(minr_v<Lhs>, max_v<Rhs>);
    } else {
        EXPECT_MAX(minr_v<Lhs>, max_v<Rhs>);
    }
    EXPECT_REG(minr_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(max_v<Lhs>, min_v<Rhs>);
    if constexpr (sizeof(Rhs) > sizeof(Lhs)) {
        EXPECT_REG(max_v<Lhs>, minr_v<Rhs>);
    } else {
        EXPECT_MAX(max_v<Lhs>, minr_v<Rhs>);
    }
    EXPECT_MAX(max_v<Lhs>, max_v<Rhs>);
    EXPECT_MAX(max_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(maxl_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, minr_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, max_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, maxl_v<Rhs>);

#undef EXPECT_REG
#undef EXPECT_MAX
#undef EXPECT_MIN
}

TYPED_TEST(SaturatingBinOpSignedTest, SaturatingSub) {
    using Lhs = typename TestFixture::Lhs;
    using Rhs = typename TestFixture::Rhs;
    using Result = typename TestFixture::Result;

#define EXPECT_REG(lhs, rhs) EXPECT_REG_BIN_OP(sub, -, lhs, rhs, Result)
#define EXPECT_MAX(lhs, rhs) EXPECT_SAT_BIN_OP(sub, lhs, rhs, max_v<Result>)
#define EXPECT_MIN(lhs, rhs) EXPECT_SAT_BIN_OP(sub, lhs, rhs, min_v<Result>)

    EXPECT_REG(min_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, minr_v<Rhs>);
    EXPECT_MIN(min_v<Lhs>, max_v<Rhs>);
    EXPECT_MIN(min_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, zero_v<Rhs>);
    if constexpr (sizeof(Rhs) > sizeof(Lhs)) {
        EXPECT_REG(min_v<Lhs>, zeror_v<Rhs>);
    } else {
        EXPECT_MIN(min_v<Lhs>, zeror_v<Rhs>);
    }
    EXPECT_REG(min_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(minr_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, minr_v<Rhs>);
    EXPECT_MIN(minr_v<Lhs>, max_v<Rhs>);
    EXPECT_MIN(minr_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zerol_v<Rhs>);

    EXPECT_MAX(max_v<Lhs>, min_v<Rhs>);
    EXPECT_MAX(max_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, zeror_v<Rhs>);
    if constexpr (sizeof(Rhs) > sizeof(Lhs)) {
        EXPECT_REG(max_v<Lhs>, zerol_v<Rhs>);
    } else {
        EXPECT_MAX(max_v<Lhs>, zerol_v<Rhs>);
    }

    EXPECT_MAX(maxl_v<Lhs>, min_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zerol_v<Rhs>);

    if constexpr (sizeof(Lhs) > sizeof(Rhs)) {
        EXPECT_REG(zero_v<Lhs>, min_v<Rhs>);
    } else {
        EXPECT_MAX(zero_v<Lhs>, min_v<Rhs>);
    }
    EXPECT_REG(zero_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zerol_v<Rhs>);

    if constexpr (sizeof(Lhs) > sizeof(Rhs)) {
        EXPECT_REG(zeror_v<Lhs>, min_v<Rhs>);
        EXPECT_REG(zeror_v<Lhs>, minr_v<Rhs>);
    } else {
        EXPECT_MAX(zeror_v<Lhs>, min_v<Rhs>);
        EXPECT_MAX(zeror_v<Lhs>, minr_v<Rhs>);
    }
    EXPECT_REG(zeror_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(zerol_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zerol_v<Rhs>);

#undef EXPECT_REG
#undef EXPECT_MAX
#undef EXPECT_MIN
}

TYPED_TEST(SaturatingBinOpUnsignedTest, SaturatingSub) {
    using Lhs = typename TestFixture::Lhs;
    using Rhs = typename TestFixture::Rhs;
    using Result = typename TestFixture::Result;

#define EXPECT_REG(lhs, rhs) EXPECT_REG_BIN_OP(sub, -, lhs, rhs, Result)
#define EXPECT_MAX(lhs, rhs) EXPECT_SAT_BIN_OP(sub, lhs, rhs, max_v<Result>)
#define EXPECT_MIN(lhs, rhs) EXPECT_SAT_BIN_OP(sub, lhs, rhs, min_v<Result>)

    EXPECT_REG(min_v<Lhs>, min_v<Rhs>);
    EXPECT_MIN(min_v<Lhs>, minr_v<Rhs>);
    EXPECT_MIN(min_v<Lhs>, max_v<Rhs>);
    EXPECT_MIN(min_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(minr_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, minr_v<Rhs>);
    EXPECT_MIN(minr_v<Lhs>, max_v<Rhs>);
    EXPECT_MIN(minr_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(max_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, minr_v<Rhs>);
    if constexpr (sizeof(Lhs) >= sizeof(Rhs)) {
        EXPECT_REG(max_v<Lhs>, max_v<Rhs>);
        EXPECT_REG(max_v<Lhs>, maxl_v<Rhs>);
    } else {
        EXPECT_MIN(max_v<Lhs>, max_v<Rhs>);
        EXPECT_MIN(max_v<Lhs>, maxl_v<Rhs>);
    }

    EXPECT_REG(maxl_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, minr_v<Rhs>);
    if constexpr (sizeof(Lhs) > sizeof(Rhs)) {
        EXPECT_REG(maxl_v<Lhs>, max_v<Rhs>);
        EXPECT_REG(maxl_v<Lhs>, maxl_v<Rhs>);
    } else {
        EXPECT_MIN(maxl_v<Lhs>, max_v<Rhs>);
        EXPECT_MIN(maxl_v<Lhs>, maxl_v<Rhs>);
    }

#undef EXPECT_REG
#undef EXPECT_MAX
#undef EXPECT_MIN
}

TYPED_TEST(SaturatingBinOpSignedTest, SaturatingMul) {
    using Lhs = typename TestFixture::Lhs;
    using Rhs = typename TestFixture::Rhs;
    using Result = typename TestFixture::Result;

#define EXPECT_REG(lhs, rhs) EXPECT_REG_BIN_OP(mul, *, lhs, rhs, Result)
#define EXPECT_MAX(lhs, rhs) EXPECT_SAT_BIN_OP(mul, lhs, rhs, max_v<Result>)
#define EXPECT_MIN(lhs, rhs) EXPECT_SAT_BIN_OP(mul, lhs, rhs, min_v<Result>)

    EXPECT_MAX(min_v<Lhs>, min_v<Rhs>);
    EXPECT_MAX(min_v<Lhs>, minr_v<Rhs>);
    EXPECT_MIN(min_v<Lhs>, max_v<Rhs>);
    EXPECT_MIN(min_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, zeror_v<Rhs>);
    if constexpr (sizeof(Rhs) > sizeof(Lhs)) {
        EXPECT_REG(min_v<Lhs>, zerol_v<Rhs>);
    } else {
        EXPECT_MAX(min_v<Lhs>, zerol_v<Rhs>);
    }

    EXPECT_MAX(minr_v<Lhs>, min_v<Rhs>);
    EXPECT_MAX(minr_v<Lhs>, minr_v<Rhs>);
    EXPECT_MIN(minr_v<Lhs>, max_v<Rhs>);
    EXPECT_MIN(minr_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zerol_v<Rhs>);

    EXPECT_MIN(max_v<Lhs>, min_v<Rhs>);
    EXPECT_MIN(max_v<Lhs>, minr_v<Rhs>);
    EXPECT_MAX(max_v<Lhs>, max_v<Rhs>);
    EXPECT_MAX(max_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, zerol_v<Rhs>);

    EXPECT_MIN(maxl_v<Lhs>, min_v<Rhs>);
    EXPECT_MIN(maxl_v<Lhs>, minr_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, max_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(zero_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(zeror_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zerol_v<Rhs>);

    if constexpr (sizeof(Lhs) > sizeof(Rhs)) {
        EXPECT_REG(zerol_v<Lhs>, min_v<Rhs>);
    } else {
        EXPECT_MAX(zerol_v<Lhs>, min_v<Rhs>);
    }
    EXPECT_REG(zerol_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zero_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zerol_v<Rhs>);

#undef EXPECT_REG
#undef EXPECT_MAX
#undef EXPECT_MIN
}

TYPED_TEST(SaturatingBinOpUnsignedTest, SaturatingMul) {
    using Lhs = typename TestFixture::Lhs;
    using Rhs = typename TestFixture::Rhs;
    using Result = typename TestFixture::Result;

#define EXPECT_REG(lhs, rhs) EXPECT_REG_BIN_OP(mul, *, lhs, rhs, Result)
#define EXPECT_MAX(lhs, rhs) EXPECT_SAT_BIN_OP(mul, lhs, rhs, max_v<Result>)
#define EXPECT_MIN(lhs, rhs) EXPECT_SAT_BIN_OP(mul, lhs, rhs, min_v<Result>)

    EXPECT_REG(min_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(minr_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(max_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, minr_v<Rhs>);
    EXPECT_MAX(max_v<Lhs>, max_v<Rhs>);
    EXPECT_MAX(max_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(maxl_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, minr_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, max_v<Rhs>);
    EXPECT_MAX(maxl_v<Lhs>, maxl_v<Rhs>);

#undef EXPECT_REG
#undef EXPECT_MAX
#undef EXPECT_MIN
}

TYPED_TEST(SaturatingBinOpSignedTest, SaturatingDiv) {
    using Lhs = typename TestFixture::Lhs;
    using Rhs = typename TestFixture::Rhs;
    using Result = typename TestFixture::Result;

#define EXPECT_REG(lhs, rhs) EXPECT_REG_BIN_OP(div, /, lhs, rhs, Result)
#define EXPECT_MAX(lhs, rhs) EXPECT_SAT_BIN_OP(div, lhs, rhs, max_v<Result>)
#define EXPECT_MIN(lhs, rhs) EXPECT_SAT_BIN_OP(div, lhs, rhs, min_v<Result>)

    EXPECT_REG(min_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, zeror_v<Rhs>);
    if constexpr (sizeof(Rhs) > sizeof(Lhs)) {
        EXPECT_REG(min_v<Lhs>, zerol_v<Rhs>);
    } else {
        EXPECT_MAX(min_v<Lhs>, zerol_v<Rhs>);
    }

    EXPECT_REG(minr_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(max_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(maxl_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(zero_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zero_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(zeror_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zeror_v<Lhs>, zerol_v<Rhs>);

    EXPECT_REG(zerol_v<Lhs>, min_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, maxl_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zeror_v<Rhs>);
    EXPECT_REG(zerol_v<Lhs>, zerol_v<Rhs>);

#undef EXPECT_REG
#undef EXPECT_MAX
#undef EXPECT_MIN
}

TYPED_TEST(SaturatingBinOpUnsignedTest, SaturatingDiv) {
    using Lhs = typename TestFixture::Lhs;
    using Rhs = typename TestFixture::Rhs;
    using Result = typename TestFixture::Result;

#define EXPECT_REG(lhs, rhs) EXPECT_REG_BIN_OP(div, /, lhs, rhs, Result)
#define EXPECT_MAX(lhs, rhs) EXPECT_SAT_BIN_OP(div, lhs, rhs, max_v<Result>)
#define EXPECT_MIN(lhs, rhs) EXPECT_SAT_BIN_OP(div, lhs, rhs, min_v<Result>)

    EXPECT_REG(min_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(min_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(minr_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(minr_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(max_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(max_v<Lhs>, maxl_v<Rhs>);

    EXPECT_REG(maxl_v<Lhs>, minr_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, max_v<Rhs>);
    EXPECT_REG(maxl_v<Lhs>, maxl_v<Rhs>);

#undef EXPECT_REG
#undef EXPECT_MAX
#undef EXPECT_MIN
}
