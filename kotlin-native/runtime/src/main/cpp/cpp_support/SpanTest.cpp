/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Span.hpp"

#include <iterator>
#include <type_traits>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

namespace {

struct BaseType {
    int field1;

    bool operator==(const BaseType& rhs) const { return field1 == rhs.field1; }
};

struct DerivedType : public BaseType {
    int field2;
};

template <typename Span, typename T>
void ExpectEmpty(const Span& s, T data) {
    EXPECT_TRUE(s.empty());
    EXPECT_THAT(s.data(), data);
    EXPECT_THAT(s.size(), 0);
    EXPECT_THAT(s.size_bytes(), 0);
    EXPECT_THAT(std::distance(s.begin(), s.end()), 0);
    EXPECT_THAT(std::distance(s.rbegin(), s.rend()), 0);
}

template <typename Span, typename Source>
void ExpectContains(const Span& s, const Source& source, size_t offset, size_t size) {
    EXPECT_FALSE(s.empty());
    EXPECT_THAT(s.data(), std::data(source) + offset);
    EXPECT_THAT(s.size(), size);
    EXPECT_THAT(s.size_bytes(), size * sizeof(BaseType));
    EXPECT_THAT(std::distance(s.begin(), s.end()), size);
    EXPECT_THAT(*s.begin(), source[offset]);
    EXPECT_THAT(*(s.end() - 1), source[offset + size - 1]);
    EXPECT_THAT(std::distance(s.rbegin(), s.rend()), size);
    EXPECT_THAT(*s.rbegin(), source[offset + size - 1]);
    EXPECT_THAT(*(s.rend() - 1), source[offset]);
    EXPECT_THAT(s.front(), source[offset]);
    EXPECT_THAT(s.back(), source[offset + size - 1]);
    for (size_t i = 0; i < size; ++i) {
        EXPECT_THAT(s[i], source[offset + i]);
    }
}

} // namespace

TEST(SpanTest, span_ctorDefault) {
    std_support::span<BaseType> s;
    ExpectEmpty(s, nullptr);
}

TEST(SpanTest, span0_ctorDefault) {
    std_support::span<BaseType, 0> s;
    ExpectEmpty(s, nullptr);
}

TEST(SpanTest, span3_ctorDefault) {
    static_assert(!std::is_default_constructible_v<std_support::span<BaseType, 3>>);
}

TEST(SpanTest, span_ctorFirstCount) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType> s(arr.data(), size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanConst_ctorFirstCount) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType> s(arr.data(), size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanBad_ctorFirstCount) {
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, DerivedType*, size_t>);
}

TEST(SpanTest, span0_ctorFirstCount) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> s(arr.data(), size);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Const_ctorFirstCount) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<const BaseType, size> s(arr.data(), size);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Bad_ctorFirstCount) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, DerivedType*, size_t>);
}

TEST(SpanTest, span3_ctorFirstCount) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType, size> s(arr.data(), size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Const_ctorFirstCount) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType, size> s(arr.data(), size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Bad_ctorFirstCount) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, DerivedType*, size_t>);
}

TEST(SpanTest, span_ctorFirstLast) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType> s(arr.data(), arr.data() + size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanConst_ctorFirstLast) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType> s(arr.data(), arr.data() + size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanBad_ctorFirstLast) {
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, DerivedType*, DerivedType*>);
}

TEST(SpanTest, span0_ctorFirstLast) {
    constexpr size_t size = 0;
    std::array<BaseType, 3> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType, size> s(arr.data(), arr.data() + size);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Const_ctorFirstLast) {
    constexpr size_t size = 0;
    std::array<BaseType, 3> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType, size> s(arr.data(), arr.data() + size);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Bad_ctorFirstLast) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, DerivedType*, DerivedType*>);
}

TEST(SpanTest, span3_ctorFirstLast) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType, size> s(arr.data(), arr.data() + size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Const_ctorFirstLast) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType, size> s(arr.data(), arr.data() + size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Bad_ctorFirstLast) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, DerivedType*, DerivedType*>);
}

TEST(SpanTest, spanCTAD_ctorPrimArray3) {
    constexpr size_t size = 3;
    BaseType arr[size] = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span s(arr);
    static_assert(std::is_same_v<decltype(s)::element_type, BaseType>);
    static_assert(decltype(s)::extent == size);
}

TEST(SpanTest, span_ctorPrimArray3) {
    constexpr size_t size = 3;
    BaseType arr[size] = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanConst_ctorPrimArray3) {
    constexpr size_t size = 3;
    BaseType arr[size] = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanBad_ctorPrimArray3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, DerivedType[size]>);
}

TEST(SpanTest, span0_ctorPrimArray0) {
    constexpr size_t size = 0;
    BaseType arr[size] = {};
    std_support::span<BaseType, size> s(arr);
    ExpectEmpty(s, arr);
}

TEST(SpanTest, span0Const_ctorPrimArray0) {
    constexpr size_t size = 0;
    BaseType arr[size] = {};
    std_support::span<const BaseType, size> s(arr);
    ExpectEmpty(s, arr);
}

TEST(SpanTest, span0Bad_ctorPrimArray0) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, DerivedType[size]>);
}

TEST(SpanTest, span0_ctorPrimArray5) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, DerivedType[sourceSize]>);
}

TEST(SpanTest, span3_ctorPrimArray3) {
    constexpr size_t size = 3;
    BaseType arr[size] = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType, size> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Const_ctorPrimArray3) {
    constexpr size_t size = 3;
    BaseType arr[size] = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType, size> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Bad_ctorPrimArray3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, DerivedType[size]>);
}

TEST(SpanTest, span3_ctorPrimArray0) {
    constexpr size_t sourceSize = 0;
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, DerivedType[sourceSize]>);
}

TEST(SpanTest, spanCTAD_ctorArray3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span s(arr);
    static_assert(std::is_same_v<decltype(s)::element_type, BaseType>);
    static_assert(decltype(s)::extent == size);
}

TEST(SpanTest, span_ctorArray3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanConst_ctorArray3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanBad_ctorArray3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, std::array<DerivedType, size>>);
}

TEST(SpanTest, span_ctorArray0) {
    constexpr size_t sourceSize = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, std::array<DerivedType, sourceSize>>);
}

TEST(SpanTest, span0_ctorArray0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> s(arr);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Const_ctorArray0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<const BaseType, size> s(arr);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Bad_ctorArray0) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, std::array<DerivedType, size>>);
}

TEST(SpanTest, span0_ctorArray5) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, std::array<DerivedType, sourceSize>>);
}

TEST(SpanTest, span3_ctorArray3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType, size> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Const_ctorArray3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType, size> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Bad_ctorArray3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, std::array<DerivedType, size>>);
}

TEST(SpanTest, span3_ctorArray0) {
    constexpr size_t sourceSize = 0;
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, std::array<DerivedType, sourceSize>>);
}

TEST(SpanTest, spanCTAD_ctorConstArray) {
    constexpr size_t size = 3;
    std::array<const BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span s(arr);
    static_assert(std::is_same_v<decltype(s)::element_type, const BaseType>);
    static_assert(decltype(s)::extent == size);
}

TEST(SpanTest, spanConst_ctorConstArray3) {
    constexpr size_t size = 3;
    const std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanBad_ctorConstArray3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, const std::array<DerivedType, size>>);
}

TEST(SpanTest, spanConst_ctorConstArray0) {
    constexpr size_t size = 0;
    const std::array<BaseType, size> arr = {};
    std_support::span<const BaseType> s(arr);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0_ctorConstArray0) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std::array<BaseType, size>>);
}

TEST(SpanTest, span0Const_ctorConstArray0) {
    constexpr size_t size = 0;
    const std::array<BaseType, size> arr = {};
    std_support::span<const BaseType, size> s(arr);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Bad_ctorConstArray0) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std::array<DerivedType, size>>);
}

TEST(SpanTest, span0_ctorConstArray5) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std::array<DerivedType, sourceSize>>);
}

TEST(SpanTest, span3_ctorConstArray3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std::array<BaseType, size>>);
}

TEST(SpanTest, span3Const_ctorConstArray3) {
    constexpr size_t size = 3;
    const std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<const BaseType, size> s(arr);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Bad_ctorConstArray3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std::array<DerivedType, size>>);
}

TEST(SpanTest, span3_ctorConstArray5) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std::array<DerivedType, sourceSize>>);
}

TEST(SpanTest, span_ctorSpan) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    const std_support::span<BaseType> source(arr);
    std_support::span<BaseType> s(source);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanConst_ctorSpan) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    const std_support::span<BaseType> source(arr);
    std_support::span<const BaseType> s(source);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanBad_ctorSpan) {
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, const std_support::span<DerivedType>>);
}

TEST(SpanTest, span0_ctorSpan) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    const std_support::span<BaseType> source(arr);
    std_support::span<BaseType, size> s(source);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Const_ctorSpan) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    const std_support::span<BaseType> source(arr);
    std_support::span<const BaseType, size> s(source);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Bad_ctorSpan) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std_support::span<DerivedType>>);
}

TEST(SpanTest, span3_ctorSpan) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    const std_support::span<BaseType> source(arr);
    std_support::span<BaseType, size> s(source);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Const_ctorSpan) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    const std_support::span<BaseType> source(arr);
    std_support::span<const BaseType, size> s(source);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Bad_ctorSpan) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std_support::span<DerivedType>>);
}

TEST(SpanTest, span_ctorSpan3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    const std_support::span<BaseType, size> source(arr);
    std_support::span<BaseType> s(source);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanConst_ctorSpan3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    const std_support::span<BaseType, size> source(arr);
    std_support::span<const BaseType> s(source);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, spanBad_ctorSpan3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, const std_support::span<DerivedType, size>>);
}

TEST(SpanTest, span_ctorSpan0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    const std_support::span<BaseType, size> source(arr);
    std_support::span<BaseType> s(source);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, spanConst_ctorSpan0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    const std_support::span<BaseType, size> source(arr);
    std_support::span<const BaseType> s(source);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, spanBad_ctorSpan0) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType>, const std_support::span<DerivedType, size>>);
}

TEST(SpanTest, span0_ctorSpan0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    const std_support::span<BaseType, size> source(arr);
    std_support::span<BaseType, size> s(source);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Const_ctorSpan0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    const std_support::span<BaseType, size> source(arr);
    std_support::span<const BaseType, size> s(source);
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span0Bad_ctorSpan0) {
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std_support::span<DerivedType, size>>);
}

TEST(SpanTest, span3_ctorSpan0) {
    constexpr size_t sourceSize = 0;
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std_support::span<BaseType, sourceSize>>);
}

TEST(SpanTest, span0_ctorSpan3) {
    constexpr size_t sourceSize = 3;
    constexpr size_t size = 0;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std_support::span<BaseType, sourceSize>>);
}

TEST(SpanTest, span3_ctorSpan3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    const std_support::span<BaseType, size> source(arr);
    std_support::span<BaseType, size> s(source);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Const_ctorSpan3) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    const std_support::span<BaseType, size> source(arr);
    std_support::span<const BaseType, size> s(source);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span3Bad_ctorSpan3) {
    constexpr size_t size = 3;
    static_assert(!std::is_constructible_v<std_support::span<BaseType, size>, const std_support::span<DerivedType, size>>);
}

TEST(SpanTest, span_firstStatic3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    auto s = source.first<size>();
    ExpectContains(s, arr, 0, size);
}

TEST(SpanDeathTest, span_firstStatic7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH({ source.first<size>(); }, "Count 7 must be smaller than size 5");
}

TEST(SpanTest, span0_firstStatic0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    auto s = source.first<size>();
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span3_firstStatic3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    auto s = source.first<size>();
    ExpectContains(s, arr, 0, size);
}

TEST(SpanTest, span_firstDynamic3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    auto s = source.first(size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanDeathTest, span_firstDynamic7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH({ source.first(size); }, "count 7 must be smaller than size 5");
}

TEST(SpanTest, span0_firstDynamic0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    auto s = source.first(size);
    ExpectEmpty(s, arr.data());
}

TEST(SpanDeathTest, span0_firstDynamic7) {
    constexpr size_t sourceSize = 0;
    constexpr size_t size = 7;
    std::array<BaseType, sourceSize> arr = {};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.first(size); }, "count 7 must be smaller than size 0");
}

TEST(SpanTest, span5_firstDynamic3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    auto s = source.first(size);
    ExpectContains(s, arr, 0, size);
}

TEST(SpanDeathTest, span5_firstDynamic7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.first(size); }, "count 7 must be smaller than size 5");
}

TEST(SpanTest, span_lastStatic3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    constexpr size_t offset = sourceSize - size;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    auto s = source.last<size>();
    ExpectContains(s, arr, offset, size);
}

TEST(SpanTest, span0_lastStatic0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    auto s = source.last<size>();
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span5_lastStatic3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    constexpr size_t offset = sourceSize - size;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    auto s = source.last<size>();
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span_lastStatic7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH({ source.last<size>(); }, "Count 7 must be smaller than size 5");
}

TEST(SpanTest, span_lastDynamic3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    constexpr size_t offset = sourceSize - size;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    auto s = source.last(size);
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span_lastDynamic7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH({ source.last(size); }, "count 7 must be smaller than size 5");
}

TEST(SpanTest, span0_lastDynamic0) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    auto s = source.last(size);
    ExpectEmpty(s, arr.data());
}

TEST(SpanDeathTest, span0_lastDynamic7) {
    constexpr size_t sourceSize = 0;
    constexpr size_t size = 7;
    std::array<BaseType, sourceSize> arr = {};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.last(size); }, "count 7 must be smaller than size 0");
}

TEST(SpanTest, span5_lastDynamic3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 3;
    constexpr size_t offset = sourceSize - size;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    auto s = source.last(size);
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span5_lastDynamic7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t size = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.last(size); }, "count 7 must be smaller than size 5");
}

TEST(SpanTest, span_subspanStatic_1) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 1;
    constexpr size_t size = sourceSize - offset;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    auto s = source.subspan<offset>();
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span_subspanStatic_7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH({ source.subspan<offset>(); }, "Offset 7 must be smaller than size 5");
}

TEST(SpanTest, span0_subspanStatic_0) {
    constexpr size_t size = 0;
    constexpr size_t offset = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    auto s = source.subspan<offset>();
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span5_subspanStatic_1) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 1;
    constexpr size_t size = sourceSize - offset;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    auto s = source.subspan<offset>();
    ExpectContains(s, arr, offset, size);
}

TEST(SpanTest, span_subspanStatic_1_3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 1;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    auto s = source.subspan<offset, size>();
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span_subspanStatic_7_3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 7;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH(({ source.subspan<offset, size>(); }), "Offset 7 must be smaller than size 5");
}

TEST(SpanDeathTest, span_subspanStatic_2_4) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 2;
    constexpr size_t size = 4;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH(({ source.subspan<offset, size>(); }), "Count 4 must be smaller than size 5 - Offset 2");
}

TEST(SpanTest, span0_subspanStatic_0_0) {
    constexpr size_t size = 0;
    constexpr size_t offset = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    auto s = source.subspan<offset, size>();
    ExpectEmpty(s, arr.data());
}

TEST(SpanTest, span3_subspanStatic_1_3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 1;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    auto s = source.subspan<offset, size>();
    ExpectContains(s, arr, offset, size);
}

TEST(SpanTest, span_subspanDynamic_1) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 1;
    constexpr size_t size = sourceSize - offset;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    auto s = source.subspan(offset);
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span_subspanDynamic_7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH({ source.subspan(offset); }, "offset 7 must be smaller than size 5");
}

TEST(SpanTest, span0_subspanDynamic_0) {
    constexpr size_t size = 0;
    constexpr size_t offset = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    auto s = source.subspan(offset);
    ExpectEmpty(s, arr.data());
}

TEST(SpanDeathTest, span0_subspanDynamic_7) {
    constexpr size_t size = 0;
    constexpr size_t offset = 7;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    EXPECT_DEATH({ source.subspan(offset); }, "offset 7 must be smaller than size 0");
}

TEST(SpanTest, span5_subspanDynamic_1) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 1;
    constexpr size_t size = sourceSize - offset;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    auto s = source.subspan(offset);
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span5_subspanDynamic_7) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 7;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.subspan(offset); }, "offset 7 must be smaller than size 5");
}

TEST(SpanTest, span_subspanDynamic_1_3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 1;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    auto s = source.subspan(offset, size);
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span_subspanDynamic_7_3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 7;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH({ source.subspan(offset, size); }, "offset 7 must be smaller than size 5");
}

TEST(SpanDeathTest, span_subspanDynamic_2_4) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 2;
    constexpr size_t size = 4;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType> source(arr);
    EXPECT_DEATH({ source.subspan(offset, size); }, "count 4 must be smaller than size 5 - offset 2");
}

TEST(SpanTest, span0_subspanDynamic_0_0) {
    constexpr size_t size = 0;
    constexpr size_t offset = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, size> source(arr);
    auto s = source.subspan(offset, size);
    ExpectEmpty(s, arr.data());
}

TEST(SpanDeathTest, span0_subspanDynamic_7_0) {
    constexpr size_t sourceSize = 0;
    constexpr size_t offset = 7;
    constexpr size_t size = 0;
    std::array<BaseType, sourceSize> arr = {};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.subspan(offset, size); }, "offset 7 must be smaller than size 0");
}

TEST(SpanDeathTest, span0_subspanDynamic_0_4) {
    constexpr size_t sourceSize = 0;
    constexpr size_t offset = 0;
    constexpr size_t size = 4;
    std::array<BaseType, sourceSize> arr = {};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.subspan(offset, size); }, "count 4 must be smaller than size 0 - offset 0");
}

TEST(SpanTest, span5_subspanDynamic_1_3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 1;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    auto s = source.subspan(offset, size);
    ExpectContains(s, arr, offset, size);
}

TEST(SpanDeathTest, span5_subspanDynamic_7_3) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 7;
    constexpr size_t size = 3;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.subspan(offset, size); }, "offset 7 must be smaller than size 5");
}

TEST(SpanDeathTest, span5_subspanDynamic_2_4) {
    constexpr size_t sourceSize = 5;
    constexpr size_t offset = 2;
    constexpr size_t size = 4;
    std::array<BaseType, sourceSize> arr = {BaseType{0}, BaseType{1}, BaseType{2}, BaseType{3}, BaseType{4}};
    std_support::span<BaseType, sourceSize> source(arr);
    EXPECT_DEATH({ source.subspan(offset, size); }, "count 4 must be smaller than size 5 - offset 2");
}

TEST(SpanDeathTest, span_front) {
    std_support::span<BaseType> s;
    EXPECT_DEATH({ s.front(); }, "Calling front on an empty span");
}

TEST(SpanDeathTest, span0_front) {
    std_support::span<BaseType, 0> s;
    EXPECT_DEATH({ s.front(); }, "Calling front on an empty span");
}

TEST(SpanDeathTest, span_back) {
    std_support::span<BaseType> s;
    EXPECT_DEATH({ s.back(); }, "Calling back on an empty span");
}

TEST(SpanDeathTest, span0_back) {
    std_support::span<BaseType, 0> s;
    EXPECT_DEATH({ s.back(); }, "Calling back on an empty span");
}

TEST(SpanDeathTest, span_index_5) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType> s(arr);
    EXPECT_DEATH({ s[5]; }, "Indexing at 5 on a span of size 3");
}

TEST(SpanDeathTest, span0_index_5) {
    constexpr size_t size = 0;
    std::array<BaseType, size> arr = {};
    std_support::span<BaseType, 0> s(arr);
    EXPECT_DEATH({ s[5]; }, "Indexing at 5 on a span of size 0");
}

TEST(SpanDeathTest, span3_index_5) {
    constexpr size_t size = 3;
    std::array<BaseType, size> arr = {BaseType{0}, BaseType{1}, BaseType{2}};
    std_support::span<BaseType, size> s(arr);
    EXPECT_DEATH({ s[5]; }, "Indexing at 5 on a span of size 3");
}
