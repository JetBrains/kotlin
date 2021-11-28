/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "TypeInfo.h"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ObjectTestSupport.hpp"
#include "Types.h"

using namespace kotlin;

namespace {

struct EmptyPayload {
    using Field = ObjHeader* EmptyPayload::*;
    static constexpr std::array<Field, 0> kFields{};
};

struct Payload1 {
    ObjHeader* field1;
    ObjHeader* field2;

    static constexpr std::array kFields{
            &Payload1::field1,
            &Payload1::field2,
    };
};

struct Payload2 {
    ObjHeader* field1;
    ObjHeader* field2;

    static constexpr std::array kFields{
            &Payload2::field1,
            &Payload2::field2,
    };
};

test_support::TypeInfoHolder emptyObjectTypeHolder{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
test_support::TypeInfoHolder object1TypeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload1>()};
test_support::TypeInfoHolder object2TypeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload2>()};

const TypeInfo* emptyObjectType = emptyObjectTypeHolder.typeInfo();
const TypeInfo* object1Type = object1TypeHolder.typeInfo();
const TypeInfo* object2Type = object2TypeHolder.typeInfo();

using LayoutCompatibleTestParam = std::tuple<const TypeInfo*, const TypeInfo*, bool, const char*>;

class LayoutCompatibleTest : public testing::TestWithParam<LayoutCompatibleTestParam> {
public:
    static std::string Print(const testing::TestParamInfo<LayoutCompatibleTestParam>& param) { return std::get<3>(param.param); }

    const TypeInfo* lhsType() { return std::get<0>(GetParam()); }
    const TypeInfo* rhsType() { return std::get<1>(GetParam()); }
    bool expectCompatible() { return std::get<2>(GetParam()); }
};

INSTANTIATE_TEST_SUITE_P(
        ,
        LayoutCompatibleTest,
        testing::Values(
                std::make_tuple(emptyObjectType, emptyObjectType, true, "empty_empty"),
                std::make_tuple(emptyObjectType, object1Type, false, "empty_obj1"),
                std::make_tuple(emptyObjectType, object2Type, false, "empty_obj2"),
                std::make_tuple(emptyObjectType, theArrayTypeInfo, false, "empty_arr"),
                std::make_tuple(emptyObjectType, theCharArrayTypeInfo, false, "empty_charArr"),

                std::make_tuple(object1Type, emptyObjectType, false, "obj1_empty"),
                std::make_tuple(object1Type, object1Type, true, "obj1_obj1"),
                std::make_tuple(object1Type, object2Type, true, "obj1_obj2"),
                std::make_tuple(object1Type, theArrayTypeInfo, false, "obj1_arr"),
                std::make_tuple(object1Type, theCharArrayTypeInfo, false, "obj1_charArr"),

                std::make_tuple(object2Type, emptyObjectType, false, "obj2_empty"),
                std::make_tuple(object2Type, object1Type, true, "obj2_obj1"),
                std::make_tuple(object2Type, object2Type, true, "obj2_obj2"),
                std::make_tuple(object2Type, theArrayTypeInfo, false, "obj2_arr"),
                std::make_tuple(object2Type, theCharArrayTypeInfo, false, "obj2_charArr"),

                std::make_tuple(theArrayTypeInfo, emptyObjectType, false, "arr_empty"),
                std::make_tuple(theArrayTypeInfo, object1Type, false, "arr_obj1"),
                std::make_tuple(theArrayTypeInfo, object2Type, false, "arr_obj2"),
                std::make_tuple(theArrayTypeInfo, theArrayTypeInfo, true, "arr_arr"),
                std::make_tuple(theArrayTypeInfo, theCharArrayTypeInfo, false, "arr_charArr"),

                std::make_tuple(theCharArrayTypeInfo, emptyObjectType, false, "charArr_empty"),
                std::make_tuple(theCharArrayTypeInfo, object1Type, false, "charArr_obj1"),
                std::make_tuple(theCharArrayTypeInfo, object2Type, false, "charArr_obj2"),
                std::make_tuple(theCharArrayTypeInfo, theArrayTypeInfo, false, "charArr_arr"),
                std::make_tuple(theCharArrayTypeInfo, theCharArrayTypeInfo, true, "charArr_charArr")),
        &LayoutCompatibleTest::Print);

} // namespace

TEST_P(LayoutCompatibleTest, IsLayoutCompatible) {
    EXPECT_THAT(lhsType()->IsLayoutCompatible(rhsType()), expectCompatible());
}
