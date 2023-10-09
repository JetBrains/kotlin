/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectTraversal.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ObjectTestSupport.hpp"
#include "ReferenceOps.hpp"
#include "Types.h"
#include "Utils.hpp"
#include "ObjectOps.hpp"

using namespace kotlin;

using ::testing::_;

namespace {

struct EmptyPayload {
    static constexpr test_support::NoRefFields<EmptyPayload> kFields{};
};

struct Payload {
    mm::RefField field1;
    mm::RefField field2;
    mm::RefField field3;

    static constexpr std::array kFields{
            &Payload::field1,
            &Payload::field2,
            &Payload::field3,
    };
};

using ProcessFunMock = testing::StrictMock<testing::MockFunction<void(mm::RefFieldAccessor)>>;

MATCHER_P(SameAccessor, accessor, "") {
    return arg.direct().location() == accessor.direct().location();
}

} // namespace

TEST(ObjectTraversalTest, TraverseFieldsExceptions) {
    struct CallableWithExceptions {
        void operator()(mm::RefFieldAccessor) noexcept(false) {}
    };
    struct CallableWithoutExceptions {
        void operator()(mm::RefFieldAccessor) noexcept {}
    };

    static_assert(
            noexcept(traverseObjectFields(std::declval<ObjHeader*>(), std::declval<CallableWithoutExceptions>())),
            "Callable is noexcept, so traverse is noexcept");
    static_assert(
            !noexcept(traverseObjectFields(std::declval<ObjHeader*>(), std::declval<CallableWithExceptions>())),
            "Callable is noexcept(false), so traverse is noexcept(false)");
}

TEST(ObjectTraversalTest, TraverseEmptyObjectFields) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    ProcessFunMock process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseObjectFields(object.header(), process.AsStdFunction());
}

TEST(ObjectTraversalTest, TraverseObjectFields) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjHeader field1;
    ObjHeader field3;
    test_support::Object<Payload> object(type.typeInfo());
    object->field1.direct() = &field1;
    object->field3.direct() = &field3;
    ProcessFunMock process;

    EXPECT_CALL(process, Call(SameAccessor(object->field1.accessor())));
    EXPECT_CALL(process, Call(SameAccessor(object->field2.accessor())));
    EXPECT_CALL(process, Call(SameAccessor(object->field3.accessor())));
    traverseObjectFields(object.header(), process.AsStdFunction());
}

TEST(ObjectTraversalTest, TraverseObjectFieldsWithException) {
    constexpr int kException = 1;

    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjHeader field1;
    ObjHeader field2;
    ObjHeader field3;
    test_support::Object<Payload> object(type.typeInfo());
    object->field1.direct() = &field1;
    object->field2.direct() = &field2;
    object->field3.direct() = &field3;
    ProcessFunMock process;

    EXPECT_CALL(process, Call(SameAccessor(object->field1.accessor())));
    EXPECT_CALL(process, Call(SameAccessor(object->field2.accessor()))).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(SameAccessor(object->field3.accessor()))).Times(0);
    try {
        traverseObjectFields(object.header(), process.AsStdFunction());
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}

TEST(ObjectTraversalTest, TraverseEmptyArrayFields) {
    test_support::ObjectArray<0> array;
    ProcessFunMock process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseObjectFields(array.header(), process.AsStdFunction());
}

TEST(ObjectTraversalTest, TraverseArrayFields) {
    ObjHeader element1;
    ObjHeader element3;
    test_support::ObjectArray<3> array;
    array.elements()[0].direct() = &element1;
    array.elements()[2].direct() = &element3;
    ProcessFunMock process;

    EXPECT_CALL(process, Call(SameAccessor(array.elements()[0].accessor())));
    EXPECT_CALL(process, Call(SameAccessor(array.elements()[1].accessor())));
    EXPECT_CALL(process, Call(SameAccessor(array.elements()[2].accessor())));
    traverseObjectFields(array.header(), process.AsStdFunction());
}

TEST(ObjectTraversalTest, TraverseArrayFieldsWithException) {
    constexpr int kException = 1;

    ObjHeader element1;
    ObjHeader element2;
    ObjHeader element3;
    test_support::ObjectArray<3> array;
    array.elements()[0].direct() = &element1;
    array.elements()[1].direct() = &element2;
    array.elements()[2].direct() = &element3;
    ProcessFunMock process;

    EXPECT_CALL(process, Call(SameAccessor(array.elements()[0].accessor())));
    EXPECT_CALL(process, Call(SameAccessor(array.elements()[1].accessor()))).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(SameAccessor(array.elements()[2].accessor()))).Times(0);
    try {
        traverseObjectFields(array.header(), process.AsStdFunction());
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}

TEST(ObjectTraversalTest, TraverseRefsExceptions) {
    struct CallableWithExceptions {
        void operator()(ObjHeader*) noexcept(false) {}
    };
    struct CallableWithoutExceptions {
        void operator()(ObjHeader*) noexcept {}
    };

    static_assert(
            noexcept(traverseReferredObjects(std::declval<ObjHeader*>(), std::declval<CallableWithoutExceptions>())),
            "Callable is noexcept, so traverse is noexcept");
    static_assert(
            !noexcept(traverseReferredObjects(std::declval<ObjHeader*>(), std::declval<CallableWithExceptions>())),
            "Callable is noexcept(false), so traverse is noexcept(false)");
}

TEST(ObjectTraversalTest, TraverseEmptyObjectRefs) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
    test_support::Object<EmptyPayload> object(type.typeInfo());
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseReferredObjects(object.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectRefs) {
    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjHeader field1;
    ObjHeader field3;
    test_support::Object<Payload> object(type.typeInfo());
    object->field1.direct() = &field1;
    object->field3.direct() = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&field1));
    EXPECT_CALL(process, Call(&field3));
    traverseReferredObjects(object.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseObjectRefsWithException) {
    constexpr int kException = 1;

    test_support::TypeInfoHolder type{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
    ObjHeader field1;
    ObjHeader field2;
    ObjHeader field3;
    test_support::Object<Payload> object(type.typeInfo());
    object->field1.direct() = &field1;
    object->field2.direct() = &field2;
    object->field3.direct() = &field3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&field1));
    EXPECT_CALL(process, Call(&field2)).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(&field3)).Times(0);
    try {
        traverseReferredObjects(object.header(), [&process](ObjHeader* field) { process.Call(field); });
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}

TEST(ObjectTraversalTest, TraverseEmptyArrayRefs) {
    test_support::ObjectArray<0> array;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(_)).Times(0);
    traverseReferredObjects(array.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayRefs) {
    ObjHeader element1;
    ObjHeader element3;
    test_support::ObjectArray<3> array;
    array.elements()[0].direct() = &element1;
    array.elements()[2].direct() = &element3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&element1));
    EXPECT_CALL(process, Call(&element3));
    traverseReferredObjects(array.header(), [&process](ObjHeader* field) { process.Call(field); });
}

TEST(ObjectTraversalTest, TraverseArrayRefsWithException) {
    constexpr int kException = 1;

    ObjHeader element1;
    ObjHeader element2;
    ObjHeader element3;
    test_support::ObjectArray<3> array;
    array.elements()[0].direct() = &element1;
    array.elements()[1].direct() = &element2;
    array.elements()[2].direct() = &element3;
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> process;

    EXPECT_CALL(process, Call(&element1));
    EXPECT_CALL(process, Call(&element2)).WillOnce([]() { throw kException; });
    EXPECT_CALL(process, Call(&element3)).Times(0);
    try {
        traverseReferredObjects(array.header(), [&process](ObjHeader* field) { process.Call(field); });
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    } catch (...) {
        EXPECT_TRUE(false);
    }
}
