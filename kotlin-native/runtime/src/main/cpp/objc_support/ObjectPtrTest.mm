/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_HAS_FOUNDATION_FRAMEWORK

#include "ObjectPtr.hpp"

#import <CoreFoundation/CFArray.h>
#import <Foundation/NSArray.h>
#import <Foundation/NSObject.h>
#include <functional>
#include <memory>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Utils.hpp"

using namespace kotlin;

namespace {

class WithDestructorHook;

using DestructorHook = void(WithDestructorHook*);

class WithDestructorHook : private Pinned {
public:
    explicit WithDestructorHook(std::function<DestructorHook> hook) : hook_(std::move(hook)) {}

    ~WithDestructorHook() { hook_(this); }

private:
    std::function<DestructorHook> hook_;
};

} // namespace

@interface WithDestructorHookObjC : NSObject {
    std::unique_ptr<WithDestructorHook> impl_;
}

@property(readonly) WithDestructorHook* impl;

- (instancetype)initWithDestructorHook:(std::function<DestructorHook>)hook;

@end

@implementation WithDestructorHookObjC

- (instancetype)initWithDestructorHook:(std::function<DestructorHook>)hook {
    if ((self = [super init])) {
        impl_ = std::make_unique<WithDestructorHook>(hook);
    }
    return self;
}

- (WithDestructorHook*)impl {
    return impl_.get();
}

@end

template <typename T>
class ObjectPtrTest : public testing::Test {};

struct ObjectPtrTestCF {
    using TestedType = objc_support::object_ptr<CFArrayRef>;
    using OtherType = objc_support::object_ptr<CFTypeRef>;

    static CFArrayRef withDestructorHook(std::function<DestructorHook> hook) noexcept {
        auto* contents = [[WithDestructorHookObjC alloc] initWithDestructorHook:std::move(hook)];
        auto* array = @[contents];
        [contents release];
        return (__bridge CFArrayRef)array;
    }

    static WithDestructorHook* getHook(CFArrayRef ptr) noexcept {
        auto* array = (__bridge NSArray<WithDestructorHookObjC*>*)ptr;
        return array[0].impl;
    }

    static void release(CFArrayRef ptr) noexcept { CFRelease(ptr); }

    static CFArrayRef retain(CFArrayRef ptr) noexcept {
        CFRetain(ptr);
        return ptr;
    }
};

struct ObjectPtrTestNS {
    using TestedType = objc_support::object_ptr<WithDestructorHookObjC>;
    using OtherType = objc_support::object_ptr<NSObject>;

    static WithDestructorHookObjC* withDestructorHook(std::function<DestructorHook> hook) noexcept {
        return [[WithDestructorHookObjC alloc] initWithDestructorHook:std::move(hook)];
    }

    static WithDestructorHook* getHook(WithDestructorHookObjC* ptr) noexcept { return ptr.impl; }

    static void release(WithDestructorHookObjC* ptr) noexcept { [ptr release]; }

    static WithDestructorHookObjC* retain(WithDestructorHookObjC* ptr) noexcept { return [ptr retain]; }
};

using ObjectPtrTestTypes = testing::Types<ObjectPtrTestCF, ObjectPtrTestNS>;
class ObjectPtrTestNames {
public:
    template <typename T>
    static std::string GetName(int) {
        if constexpr (std::is_same_v<T, ObjectPtrTestCF>) {
            return "CF";
        } else if constexpr (std::is_same_v<T, ObjectPtrTestNS>) {
            return "NS";
        }
    }
};
TYPED_TEST_SUITE(ObjectPtrTest, ObjectPtrTestTypes, ObjectPtrTestNames);

#define EXPECT_CONTAINS(smartPtr, ptr) \
    do { \
        EXPECT_THAT(static_cast<bool>(smartPtr), (ptr) != nullptr); \
        EXPECT_THAT(*(smartPtr), (ptr)); \
        EXPECT_THAT((smartPtr).get(), (ptr)); \
    } while (false)

TYPED_TEST(ObjectPtrTest, DefaultCtor) {
    typename TypeParam::TestedType obj;
    EXPECT_CONTAINS(obj, nil);
}

TYPED_TEST(ObjectPtrTest, ObjectCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj(ptr);
        EXPECT_CONTAINS(obj, ptr);
        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TYPED_TEST(ObjectPtrTest, ObjectCtorWithRetain) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj(objc_support::object_ptr_retain, ptr);
        EXPECT_CONTAINS(obj, ptr);
        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr))).Times(0);
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    TypeParam::release(ptr);
}

TYPED_TEST(ObjectPtrTest, CopyCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj1(ptr);
        typename TypeParam::TestedType obj2(obj1);
        EXPECT_CONTAINS(obj1, ptr);
        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr))).Times(0);
        obj1.reset();
        testing::Mock::VerifyAndClearExpectations(&destructorHook);
        EXPECT_CONTAINS(obj1, nil);
        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TYPED_TEST(ObjectPtrTest, MoveCtor) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj1(ptr);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr))).Times(0);
        typename TypeParam::TestedType obj2(std::move(obj1));
        testing::Mock::VerifyAndClearExpectations(&destructorHook);
        EXPECT_CONTAINS(obj1, nil);
        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TYPED_TEST(ObjectPtrTest, CopyAssignmentIntoEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj2;
        {
            typename TypeParam::TestedType obj1(ptr);
            obj2 = obj1;
            EXPECT_CONTAINS(obj1, ptr);
            EXPECT_CONTAINS(obj2, ptr);

            EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr))).Times(0);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TYPED_TEST(ObjectPtrTest, CopyAssignmentIntoSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj2(ptr2);
        {
            typename TypeParam::TestedType obj1(ptr1);

            EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
            obj2 = obj1;
            testing::Mock::VerifyAndClearExpectations(&destructorHook);

            EXPECT_CONTAINS(obj1, ptr1);
            EXPECT_CONTAINS(obj2, ptr1);

            EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1))).Times(0);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_CONTAINS(obj2, ptr1);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TYPED_TEST(ObjectPtrTest, MoveAssignmentIntoEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj2;
        {
            EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr))).Times(0);

            typename TypeParam::TestedType obj1(ptr);
            obj2 = std::move(obj1);
            EXPECT_CONTAINS(obj1, nil);
            EXPECT_CONTAINS(obj2, ptr);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_CONTAINS(obj2, ptr);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TYPED_TEST(ObjectPtrTest, MoveAssignmentIntoSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj2(ptr2);
        {
            typename TypeParam::TestedType obj1(ptr1);

            EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
            EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1))).Times(0);
            obj2 = std::move(obj1);
            testing::Mock::VerifyAndClearExpectations(&destructorHook);

            EXPECT_CONTAINS(obj1, nil);
            EXPECT_CONTAINS(obj2, ptr1);
        }
        testing::Mock::VerifyAndClearExpectations(&destructorHook);

        EXPECT_CONTAINS(obj2, ptr1);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
}

TYPED_TEST(ObjectPtrTest, SwapEmptyEmpty) {
    typename TypeParam::TestedType obj1;
    typename TypeParam::TestedType obj2;
    obj1.swap(obj2);
    EXPECT_CONTAINS(obj1, nil);
    EXPECT_CONTAINS(obj2, nil);
}

TYPED_TEST(ObjectPtrTest, SwapEmptySet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType obj1;
    typename TypeParam::TestedType obj2(ptr2);
    obj1.swap(obj2);
    EXPECT_CONTAINS(obj1, ptr2);
    EXPECT_CONTAINS(obj2, nil);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
}

TYPED_TEST(ObjectPtrTest, SwapSetEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType obj1(ptr1);
    typename TypeParam::TestedType obj2;
    obj1.swap(obj2);
    EXPECT_CONTAINS(obj1, nil);
    EXPECT_CONTAINS(obj2, ptr1);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
}

TYPED_TEST(ObjectPtrTest, SwapSetSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType obj1(ptr1);
    typename TypeParam::TestedType obj2(ptr2);
    obj1.swap(obj2);
    EXPECT_CONTAINS(obj1, ptr2);
    EXPECT_CONTAINS(obj2, ptr1);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
}

TYPED_TEST(ObjectPtrTest, ResetEmptyFromEmpty) {
    typename TypeParam::TestedType obj;
    obj.reset();
    EXPECT_CONTAINS(obj, nil);
}

TYPED_TEST(ObjectPtrTest, ResetEmptyFromSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType obj(ptr);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    obj.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    EXPECT_CONTAINS(obj, nil);
}

TYPED_TEST(ObjectPtrTest, ResetObjectFromEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType obj;

    obj.reset(ptr);
    EXPECT_CONTAINS(obj, ptr);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
}

TYPED_TEST(ObjectPtrTest, ResetObjectFromEmptyWithRetain) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj;

        obj.reset(objc_support::object_ptr_retain, ptr);
        EXPECT_CONTAINS(obj, ptr);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr))).Times(0);
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    TypeParam::release(ptr);
}

TYPED_TEST(ObjectPtrTest, ResetObjectFromSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType obj(ptr1);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
    obj.reset(ptr2);
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    EXPECT_CONTAINS(obj, ptr2);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
}

TYPED_TEST(ObjectPtrTest, ResetObjectFromSetWithRetain) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    {
        typename TypeParam::TestedType obj(ptr1);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
        obj.reset(objc_support::object_ptr_retain, ptr2);
        testing::Mock::VerifyAndClearExpectations(&destructorHook);
        EXPECT_CONTAINS(obj, ptr2);

        EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2))).Times(0);
    }
    testing::Mock::VerifyAndClearExpectations(&destructorHook);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
    TypeParam::release(ptr2);
}

#define EXPECT_EQUAL(expr1, expr2) \
    do { \
        EXPECT_TRUE((expr1) == (expr2)); \
        EXPECT_FALSE((expr1) != (expr2)); \
        EXPECT_FALSE((expr1) < (expr2)); \
        EXPECT_TRUE((expr1) <= (expr2)); \
        EXPECT_FALSE((expr1) > (expr2)); \
        EXPECT_TRUE((expr1) >= (expr2)); \
    } while (false)

#define EXPECT_LESS(expr1, expr2) \
    do { \
        EXPECT_FALSE((expr1) == (expr2)); \
        EXPECT_TRUE((expr1) != (expr2)); \
        EXPECT_TRUE((expr1) < (expr2)); \
        EXPECT_TRUE((expr1) <= (expr2)); \
        EXPECT_FALSE((expr1) > (expr2)); \
        EXPECT_FALSE((expr1) >= (expr2)); \
    } while (false)

#define EXPECT_GREATER(expr1, expr2) \
    do { \
        EXPECT_FALSE((expr1) == (expr2)); \
        EXPECT_TRUE((expr1) != (expr2)); \
        EXPECT_FALSE((expr1) < (expr2)); \
        EXPECT_FALSE((expr1) <= (expr2)); \
        EXPECT_TRUE((expr1) > (expr2)); \
        EXPECT_TRUE((expr1) >= (expr2)); \
    } while (false)

TYPED_TEST(ObjectPtrTest, ComparisonsEmpty) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType empty;
    typename TypeParam::TestedType reset(ptr);
    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr)));
    reset.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    typename TypeParam::OtherType emptyOtherType;

    EXPECT_EQUAL(empty, empty);
    EXPECT_EQUAL(empty, typename TypeParam::TestedType());
    EXPECT_EQUAL(empty, reset);
    EXPECT_EQUAL(empty, emptyOtherType);
    EXPECT_EQUAL(reset, empty);
    EXPECT_EQUAL(reset, reset);
    EXPECT_EQUAL(reset, emptyOtherType);
}

TYPED_TEST(ObjectPtrTest, ComparisonsEmptySet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr3 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType obj(ptr1);
    typename TypeParam::TestedType empty;
    typename TypeParam::TestedType reset(ptr2);
    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
    reset.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);
    typename TypeParam::OtherType emptyOtherType;
    typename TypeParam::TestedType objOtherType(ptr3);

    EXPECT_GREATER(obj, empty);
    EXPECT_GREATER(obj, reset);
    EXPECT_GREATER(obj, emptyOtherType);
    EXPECT_LESS(empty, obj);
    EXPECT_LESS(empty, objOtherType);
    EXPECT_LESS(reset, obj);
    EXPECT_LESS(reset, objOtherType);
    EXPECT_GREATER(objOtherType, empty);
    EXPECT_GREATER(objOtherType, reset);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr3)));
}

TYPED_TEST(ObjectPtrTest, ComparisonsSetSet) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    if (ptr2 < ptr1) {
        std::swap(ptr1, ptr2);
    }
    ASSERT_LT(ptr1, ptr2);

    typename TypeParam::TestedType obj1(ptr1);
    typename TypeParam::TestedType obj2(ptr2);
    typename TypeParam::TestedType obj3(obj1);
    typename TypeParam::OtherType objOtherType(TypeParam::retain(ptr1));

    EXPECT_EQUAL(obj1, obj1);
    EXPECT_LESS(obj1, obj2);
    EXPECT_EQUAL(obj1, obj3);
    EXPECT_EQUAL(obj1, objOtherType);
    EXPECT_GREATER(obj2, obj1);
    EXPECT_EQUAL(obj2, obj2);
    EXPECT_GREATER(obj2, obj3);
    EXPECT_GREATER(obj2, objOtherType);
    EXPECT_EQUAL(obj3, obj1);
    EXPECT_LESS(obj3, obj2);
    EXPECT_EQUAL(obj3, obj3);
    EXPECT_EQUAL(obj3, objOtherType);
    EXPECT_EQUAL(objOtherType, obj1);
    EXPECT_LESS(objOtherType, obj2);
    EXPECT_EQUAL(objOtherType, obj3);
    EXPECT_EQUAL(objOtherType, objOtherType);

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
}

TYPED_TEST(ObjectPtrTest, Hashing) {
    testing::StrictMock<testing::MockFunction<DestructorHook>> destructorHook;

    auto* ptr1 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    auto* ptr2 = TypeParam::withDestructorHook(destructorHook.AsStdFunction());
    typename TypeParam::TestedType obj(ptr1);
    typename TypeParam::TestedType empty;
    typename TypeParam::TestedType reset(ptr2);
    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr2)));
    reset.reset();
    testing::Mock::VerifyAndClearExpectations(&destructorHook);

    using Hash = std::hash<typename TypeParam::TestedType>;
    using HashImpl = std::hash<const void*>;

    EXPECT_THAT(Hash()(obj), HashImpl()(ptr1));
    EXPECT_THAT(Hash()(empty), HashImpl()(nullptr));
    EXPECT_THAT(Hash()(reset), HashImpl()(nullptr));

    EXPECT_CALL(destructorHook, Call(TypeParam::getHook(ptr1)));
}

#endif
