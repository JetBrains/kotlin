/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectTestSupport.hpp"

#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Natives.h"
#include "TestSupport.hpp"

using namespace kotlin;

namespace {

struct RegularPayload {
    ObjHeader* field1;
    ObjHeader* field2;
    ObjHeader* field3;

    static constexpr std::array kFields{
            &RegularPayload::field1,
            &RegularPayload::field2,
            &RegularPayload::field3,
    };
};

struct IrregularPayload {
    int skipBefore;
    ObjHeader* field1;
    int skip;
    ObjHeader* field2;
    std::array<int, 10> skipALot;
    ObjHeader* field3;

    static constexpr std::array kFields{
            &IrregularPayload::field1,
            &IrregularPayload::field2,
            &IrregularPayload::field3,
    };
};

struct RegularObjectTestCase {
    using Payload = RegularPayload;

    static constexpr const char* name = "RegularPayload";
};

struct IrregularObjectTestCase {
    using Payload = IrregularPayload;

    static constexpr const char* name = "IrregularPayload";
};

class ObjectTestCaseNames {
public:
    template <typename T>
    static std::string GetName(int i) {
        return T::name;
    }
};

template <typename TestCase>
class ObjectTestSupportObjectTest : public testing::Test {};
using ObjectTestCases = testing::Types<RegularObjectTestCase, IrregularObjectTestCase>;
TYPED_TEST_SUITE(ObjectTestSupportObjectTest, ObjectTestCases, ObjectTestCaseNames);

template <typename Payload>
std::vector<ObjHeader**> Collect(test_support::Object<Payload>& object) {
    std::vector<ObjHeader**> result;
    for (auto& field : object.fields()) {
        result.push_back(&field);
    }
    return result;
}

} // namespace

TYPED_TEST(ObjectTestSupportObjectTest, Local) {
    using Payload = typename TypeParam::Payload;

    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};

    test_support::Object<Payload> object(type.typeInfo());
    EXPECT_THAT(object.header()->type_info(), type.typeInfo());

    EXPECT_THAT(object.header()->type_info()->objOffsetsCount_, 3);

    EXPECT_THAT(
            reinterpret_cast<uintptr_t>(&object->field1),
            reinterpret_cast<uintptr_t>(object.header()) + object.header()->type_info()->objOffsets_[0]);
    EXPECT_THAT(
            reinterpret_cast<uintptr_t>(&object->field2),
            reinterpret_cast<uintptr_t>(object.header()) + object.header()->type_info()->objOffsets_[1]);
    EXPECT_THAT(
            reinterpret_cast<uintptr_t>(&object->field3),
            reinterpret_cast<uintptr_t>(object.header()) + object.header()->type_info()->objOffsets_[2]);

    EXPECT_THAT(object.fields().size(), 3);

    EXPECT_THAT(&object.fields()[0], &object->field1);
    EXPECT_THAT(&object.fields()[1], &object->field2);
    EXPECT_THAT(&object.fields()[2], &object->field3);

    EXPECT_THAT(Collect(object), testing::ElementsAre(&object->field1, &object->field2, &object->field3));

    EXPECT_THAT(object.fields()[0], nullptr);
    EXPECT_THAT(object.fields()[1], nullptr);
    EXPECT_THAT(object.fields()[2], nullptr);

    auto& recoveredObject = test_support::Object<Payload>::FromObjHeader(object.header());
    EXPECT_THAT(&recoveredObject, &object);
}

TYPED_TEST(ObjectTestSupportObjectTest, Heap) {
    using Payload = typename TypeParam::Payload;
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};

    RunInNewThread([&type]() {
        ObjHolder resultHolder;
        ObjHeader* result = AllocInstance(type.typeInfo(), resultHolder.slot());
        ASSERT_THAT(result, testing::Ne(nullptr));

        auto& object = test_support::Object<Payload>::FromObjHeader(result);
        EXPECT_THAT(object.header(), result);
        EXPECT_THAT(object.header()->type_info(), type.typeInfo());

        EXPECT_THAT(object.header()->type_info()->objOffsetsCount_, 3);

        EXPECT_THAT(
                reinterpret_cast<uintptr_t>(&object->field1),
                reinterpret_cast<uintptr_t>(object.header()) + object.header()->type_info()->objOffsets_[0]);
        EXPECT_THAT(
                reinterpret_cast<uintptr_t>(&object->field2),
                reinterpret_cast<uintptr_t>(object.header()) + object.header()->type_info()->objOffsets_[1]);
        EXPECT_THAT(
                reinterpret_cast<uintptr_t>(&object->field3),
                reinterpret_cast<uintptr_t>(object.header()) + object.header()->type_info()->objOffsets_[2]);

        EXPECT_THAT(object.fields().size(), 3);

        EXPECT_THAT(&object.fields()[0], &object->field1);
        EXPECT_THAT(&object.fields()[1], &object->field2);
        EXPECT_THAT(&object.fields()[2], &object->field3);

        EXPECT_THAT(Collect(object), testing::ElementsAre(&object->field1, &object->field2, &object->field3));

        EXPECT_THAT(object.fields()[0], nullptr);
        EXPECT_THAT(object.fields()[1], nullptr);
        EXPECT_THAT(object.fields()[2], nullptr);
    });
}

namespace {

template <typename Payload>
struct PayloadTraits;

template <>
struct PayloadTraits<ObjHeader*> {
    template <size_t Size>
    using Array = test_support::ObjectArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theArrayTypeInfo; }
    static constexpr const char* name = "Array";
};

template <>
struct PayloadTraits<KBoolean> {
    template <size_t Size>
    using Array = test_support::BooleanArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theBooleanArrayTypeInfo; }
    static constexpr const char* name = "BooleanArray";
};

template <>
struct PayloadTraits<KByte> {
    template <size_t Size>
    using Array = test_support::ByteArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theByteArrayTypeInfo; }
    static constexpr const char* name = "ByteArray";
};

template <>
struct PayloadTraits<KChar> {
    template <size_t Size>
    using Array = test_support::CharArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theCharArrayTypeInfo; }
    static constexpr const char* name = "CharArray";
};

template <>
struct PayloadTraits<KDouble> {
    template <size_t Size>
    using Array = test_support::DoubleArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theDoubleArrayTypeInfo; }
    static constexpr const char* name = "DoubleArray";
};

template <>
struct PayloadTraits<KFloat> {
    template <size_t Size>
    using Array = test_support::FloatArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theFloatArrayTypeInfo; }
    static constexpr const char* name = "FloatArray";
};

template <>
struct PayloadTraits<KInt> {
    template <size_t Size>
    using Array = test_support::IntArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theIntArrayTypeInfo; }
    static constexpr const char* name = "IntArray";
};

template <>
struct PayloadTraits<KLong> {
    template <size_t Size>
    using Array = test_support::LongArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theLongArrayTypeInfo; }
    static constexpr const char* name = "LongArray";
};

template <>
struct PayloadTraits<KNativePtr> {
    template <size_t Size>
    using Array = test_support::NativePtrArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theNativePtrArrayTypeInfo; }
    static constexpr const char* name = "NativePtrArray";
};

template <>
struct PayloadTraits<KShort> {
    template <size_t Size>
    using Array = test_support::ShortArray<Size>;
    static const TypeInfo* GetTypeInfo() { return theShortArrayTypeInfo; }
    static constexpr const char* name = "ShortArray";
};

template <size_t Size>
struct SizeTraits;

template <>
struct SizeTraits<0> {
    static constexpr const char* name = "Empty";
};

template <>
struct SizeTraits<3> {
    static constexpr const char* name = "";
};

template <typename T, size_t Size>
struct ArrayTestCase {
    using Payload = T;
    using Array = typename PayloadTraits<Payload>::template Array<Size>;

    static constexpr size_t size = Size;
    static const TypeInfo* GetTypeInfo() { return PayloadTraits<Payload>::GetTypeInfo(); }
    static std::string GetName() { return std::string(SizeTraits<Size>::name) + std::string(PayloadTraits<Payload>::name); }
};

template <size_t Size>
struct StringTestCase {
    using Payload = KChar;
    using Array = test_support::String<Size>;

    static constexpr size_t size = Size;
    static const TypeInfo* GetTypeInfo() { return theStringTypeInfo; }
    static std::string GetName() { return std::string(SizeTraits<Size>::name) + std::string("String"); }
};

class ArrayTestCaseNames {
public:
    template <typename T>
    static std::string GetName(int i) {
        return T::GetName();
    }
};

template <typename TestCase>
class ObjectTestSupportArrayTest : public testing::Test {};
using ArrayTestCases = testing::Types<
        ArrayTestCase<ObjHeader*, 0>,
        ArrayTestCase<ObjHeader*, 3>,
        ArrayTestCase<KBoolean, 0>,
        ArrayTestCase<KBoolean, 3>,
        ArrayTestCase<KByte, 0>,
        ArrayTestCase<KByte, 3>,
        ArrayTestCase<KChar, 0>,
        ArrayTestCase<KChar, 3>,
        ArrayTestCase<KDouble, 0>,
        ArrayTestCase<KDouble, 3>,
        ArrayTestCase<KFloat, 0>,
        ArrayTestCase<KFloat, 3>,
        ArrayTestCase<KInt, 0>,
        ArrayTestCase<KInt, 3>,
        ArrayTestCase<KLong, 0>,
        ArrayTestCase<KLong, 3>,
        ArrayTestCase<KNativePtr, 0>,
        ArrayTestCase<KNativePtr, 3>,
        ArrayTestCase<KShort, 0>,
        ArrayTestCase<KShort, 3>,
        StringTestCase<0>,
        StringTestCase<3>>;
TYPED_TEST_SUITE(ObjectTestSupportArrayTest, ArrayTestCases, ArrayTestCaseNames);

template <typename Payload, size_t ElementCount>
std::vector<Payload*> Collect(test_support::internal::Array<Payload, ElementCount>& array) {
    std::vector<Payload*> result;
    for (auto& element : array.elements()) {
        result.push_back(&element);
    }
    return result;
}

} // namespace

TYPED_TEST(ObjectTestSupportArrayTest, Local) {
    using Payload = typename TypeParam::Payload;
    using Array = typename TypeParam::Array;
    const auto typeInfo = TypeParam::GetTypeInfo();
    constexpr auto size = TypeParam::size;

    Array array;

    EXPECT_THAT(array.header()->type_info(), typeInfo);
    EXPECT_THAT(array.arrayHeader()->count_, size);
    EXPECT_THAT(array.elements().size(), size);

    std::vector<Payload*> expected;
    for (size_t i = 0; i < size; ++i) {
        auto* element = AddressOfElementAt<Payload>(array.arrayHeader(), i);
        EXPECT_THAT(&array.elements()[i], element);
        EXPECT_THAT(array.elements()[i], Payload{});
        expected.push_back(element);
    }

    EXPECT_THAT(Collect(array), testing::ElementsAreArray(expected));

    auto& recoveredArray = Array::FromArrayHeader(array.arrayHeader());
    EXPECT_THAT(&recoveredArray, &array);
}

TYPED_TEST(ObjectTestSupportArrayTest, Heap) {
    using Payload = typename TypeParam::Payload;
    using Array = typename TypeParam::Array;
    const auto typeInfo = TypeParam::GetTypeInfo();
    constexpr auto size = TypeParam::size;

    RunInNewThread([typeInfo]() {
        ObjHolder resultHolder;
        ObjHeader* result = AllocArrayInstance(typeInfo, size, resultHolder.slot());
        ASSERT_THAT(result, testing::Ne(nullptr));

        auto& array = Array::FromArrayHeader(result->array());
        EXPECT_THAT(array.header(), result);
        EXPECT_THAT(array.header()->type_info(), typeInfo);
        EXPECT_THAT(array.arrayHeader()->count_, size);
        EXPECT_THAT(array.elements().size(), size);

        std::vector<Payload*> expected;
        for (size_t i = 0; i < size; ++i) {
            auto* element = AddressOfElementAt<Payload>(array.arrayHeader(), i);
            EXPECT_THAT(&array.elements()[i], element);
            EXPECT_THAT(array.elements()[i], Payload{});
            expected.push_back(element);
        }

        EXPECT_THAT(Collect(array), testing::ElementsAreArray(expected));
    });
}
