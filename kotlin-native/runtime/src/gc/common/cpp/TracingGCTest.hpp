/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <condition_variable>
#include <future>
#include <mutex>
#include <thread>
#include <vector>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "AllocatorTestSupport.hpp"
#include "ExtraObjectData.hpp"
#include "FinalizerHooksTestSupport.hpp"
#include "GlobalData.hpp"
#include "ObjectOps.hpp"
#include "ObjectTestSupport.hpp"
#include "SafePoint.hpp"
#include "SingleThreadExecutor.hpp"
#include "StableRef.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "WeakRef.hpp"

using namespace kotlin;

struct Payload {
    mm::RefField field1;
    mm::RefField field2;
    mm::RefField field3;

    static constexpr std::array kFields = {
            &Payload::field1,
            &Payload::field2,
            &Payload::field3,
    };
};

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
test_support::TypeInfoHolder typeHolderWithFinalizer{test_support::TypeInfoHolder::ObjectBuilder<Payload>().addFlag(TF_HAS_FINALIZER)};

// TODO: Clean GlobalObjectHolder after it's gone.
class GlobalObjectHolder : private Pinned {
public:
    explicit GlobalObjectHolder(mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &location_);
        mm::AllocateObject(&threadData, typeHolder.typeInfo(), &location_);
    }

    GlobalObjectHolder(mm::ThreadData& threadData, ObjHeader* object) : location_(object) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &location_);
    }

    ObjHeader* header() { return location_; }

    test_support::Object<Payload>& operator*() { return test_support::Object<Payload>::FromObjHeader(location_); }
    test_support::Object<Payload>& operator->() { return test_support::Object<Payload>::FromObjHeader(location_); }

private:
    ObjHeader* location_ = nullptr;
};

// TODO: Clean GlobalPermanentObjectHolder after it's gone.
class GlobalPermanentObjectHolder : private Pinned {
public:
    explicit GlobalPermanentObjectHolder(mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &global_);
        global_->typeInfoOrMeta_ = setPointerBits(global_->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        RuntimeAssert(global_->permanent(), "Must be permanent");
    }

    ObjHeader* header() { return global_; }

    test_support::Object<Payload>& operator*() { return object_; }
    test_support::Object<Payload>& operator->() { return object_; }

private:
    test_support::Object<Payload> object_{typeHolder.typeInfo()};
    ObjHeader* global_{object_.header()};
};

// TODO: Clean GlobalObjectArrayHolder after it's gone.
class GlobalObjectArrayHolder : private Pinned {
public:
    explicit GlobalObjectArrayHolder(mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &location_);
        mm::AllocateArray(&threadData, theArrayTypeInfo, 3, &location_);
    }

    ObjHeader* header() { return location_; }

    test_support::ObjectArray<3>& operator*() { return test_support::ObjectArray<3>::FromArrayHeader(location_->array()); }
    test_support::ObjectArray<3>& operator->() { return test_support::ObjectArray<3>::FromArrayHeader(location_->array()); }

    mm::RefField& operator[](size_t index) noexcept { return (**this).elements()[index]; }

private:
    ObjHeader* location_ = nullptr;
};

// TODO: Clean GlobalCharArrayHolder after it's gone.
class GlobalCharArrayHolder : private Pinned {
public:
    explicit GlobalCharArrayHolder(mm::ThreadData& threadData) {
        mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(&threadData, &location_);
        mm::AllocateArray(&threadData, theCharArrayTypeInfo, 3, &location_);
    }

    ObjHeader* header() { return location_; }

    test_support::CharArray<3>& operator*() { return test_support::CharArray<3>::FromArrayHeader(location_->array()); }
    test_support::CharArray<3>& operator->() { return test_support::CharArray<3>::FromArrayHeader(location_->array()); }

private:
    ObjHeader* location_ = nullptr;
};

class StackObjectHolder : private Pinned {
public:
    explicit StackObjectHolder(mm::ThreadData& threadData) { mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder_.slot()); }
    explicit StackObjectHolder(test_support::Object<Payload>& object) : holder_(object.header()) {}
    explicit StackObjectHolder(ObjHeader* object) : holder_(object) {}

    ObjHeader* header() { return holder_.obj(); }

    test_support::Object<Payload>& operator*() { return test_support::Object<Payload>::FromObjHeader(holder_.obj()); }
    test_support::Object<Payload>& operator->() { return test_support::Object<Payload>::FromObjHeader(holder_.obj()); }

private:
    ObjHolder holder_;
};

class StackObjectArrayHolder : private Pinned {
public:
    explicit StackObjectArrayHolder(mm::ThreadData& threadData) { mm::AllocateArray(&threadData, theArrayTypeInfo, 3, holder_.slot()); }

    ObjHeader* header() { return holder_.obj(); }

    test_support::ObjectArray<3>& operator*() { return test_support::ObjectArray<3>::FromArrayHeader(holder_.obj()->array()); }
    test_support::ObjectArray<3>& operator->() { return test_support::ObjectArray<3>::FromArrayHeader(holder_.obj()->array()); }

    mm::RefField& operator[](size_t index) noexcept { return (**this).elements()[index]; }

private:
    ObjHolder holder_;
};

class StackCharArrayHolder : private Pinned {
public:
    explicit StackCharArrayHolder(mm::ThreadData& threadData) { mm::AllocateArray(&threadData, theCharArrayTypeInfo, 3, holder_.slot()); }

    ObjHeader* header() { return holder_.obj(); }

    test_support::CharArray<3>& operator*() { return test_support::CharArray<3>::FromArrayHeader(holder_.obj()->array()); }
    test_support::CharArray<3>& operator->() { return test_support::CharArray<3>::FromArrayHeader(holder_.obj()->array()); }

private:
    ObjHolder holder_;
};

test_support::Object<Payload>& AllocateObject(mm::ThreadData& threadData) {
    ObjHolder holder;
    mm::AllocateObject(&threadData, typeHolder.typeInfo(), holder.slot());
    return test_support::Object<Payload>::FromObjHeader(holder.obj());
}

test_support::Object<Payload>& AllocateObjectWithFinalizer(mm::ThreadData& threadData) {
    ObjHolder holder;
    mm::AllocateObject(&threadData, typeHolderWithFinalizer.typeInfo(), holder.slot());
    return test_support::Object<Payload>::FromObjHeader(holder.obj());
}

std::vector<ObjHeader*> Alive(mm::ThreadData& threadData) {
    return alloc::test_support::allocatedObjects(threadData);
}


template <typename FixtureImpl>
class TracingGCTest : public testing::Test {
public:
    void SetUp() override {
        impl_.SetUp();
    }

    testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHooks_.finalizerHook(); }
private:
    FixtureImpl impl_{};
    FinalizerHooksTestSupport finalizerHooks_;
};

TYPED_TEST_SUITE_P(TracingGCTest);

TYPED_TEST_P(TracingGCTest, RootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global1{threadData};
        GlobalObjectArrayHolder global2{threadData};
        GlobalCharArrayHolder global3{threadData};
        StackObjectHolder stack1{threadData};
        StackObjectArrayHolder stack2{threadData};
        StackCharArrayHolder stack3{threadData};

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        ASSERT_THAT(gc::isMarked(global1.header()), false);
        ASSERT_THAT(gc::isMarked(global2.header()), false);
        ASSERT_THAT(gc::isMarked(global3.header()), false);
        ASSERT_THAT(gc::isMarked(stack1.header()), false);
        ASSERT_THAT(gc::isMarked(stack2.header()), false);
        ASSERT_THAT(gc::isMarked(stack3.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        EXPECT_THAT(gc::isMarked(global1.header()), false);
        EXPECT_THAT(gc::isMarked(global2.header()), false);
        EXPECT_THAT(gc::isMarked(global3.header()), false);
        EXPECT_THAT(gc::isMarked(stack1.header()), false);
        EXPECT_THAT(gc::isMarked(stack2.header()), false);
        EXPECT_THAT(gc::isMarked(stack3.header()), false);
    });
}

TYPED_TEST_P(TracingGCTest, InterconnectedRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global1{threadData};
        GlobalObjectArrayHolder global2{threadData};
        GlobalCharArrayHolder global3{threadData};
        StackObjectHolder stack1{threadData};
        StackObjectArrayHolder stack2{threadData};
        StackCharArrayHolder stack3{threadData};

        global1->field1 = stack1.header();
        global1->field2 = global1.header();
        global1->field3 = global2.header();
        global2[0] = global1.header();
        global2[1] = global3.header();
        stack1->field1 = global1.header();
        stack1->field2 = stack1.header();
        stack1->field3 = stack2.header();
        stack2[0] = stack1.header();
        stack2[1] = stack3.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        ASSERT_THAT(gc::isMarked(global1.header()), false);
        ASSERT_THAT(gc::isMarked(global2.header()), false);
        ASSERT_THAT(gc::isMarked(global3.header()), false);
        ASSERT_THAT(gc::isMarked(stack1.header()), false);
        ASSERT_THAT(gc::isMarked(stack2.header()), false);
        ASSERT_THAT(gc::isMarked(stack3.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        EXPECT_THAT(gc::isMarked(global1.header()), false);
        EXPECT_THAT(gc::isMarked(global2.header()), false);
        EXPECT_THAT(gc::isMarked(global3.header()), false);
        EXPECT_THAT(gc::isMarked(stack1.header()), false);
        EXPECT_THAT(gc::isMarked(stack2.header()), false);
        EXPECT_THAT(gc::isMarked(stack3.header()), false);
    });
}

TYPED_TEST_P(TracingGCTest, FreeObjects) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), object2.header()));
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(object2.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TYPED_TEST_P(TracingGCTest, FreeObjectsWithFinalizers) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        auto& object1 = AllocateObjectWithFinalizer(threadData);
        auto& object2 = AllocateObjectWithFinalizer(threadData);

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), object2.header()));
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(object2.header()), false);

        EXPECT_CALL(this->finalizerHook(), Call(object1.header()));
        EXPECT_CALL(this->finalizerHook(), Call(object2.header()));
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TYPED_TEST_P(TracingGCTest, FreeObjectWithFreeWeak) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        auto& weak1 = ([&threadData, &object1]() -> test_support::RegularWeakReferenceImpl& {
            ObjHolder holder;
            return test_support::InstallWeakReference(threadData, object1.header(), holder.slot());
        })();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), weak1.header()));
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(weak1.header()), false);
        ASSERT_THAT(weak1.get(), object1.header());

        EXPECT_CALL(this->finalizerHook(), Call(weak1.header()));
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TYPED_TEST_P(TracingGCTest, FreeObjectWithHoldedWeak) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        StackObjectHolder stack{threadData};
        auto& weak1 = test_support::InstallWeakReference(threadData, object1.header(), stack->field1.ptr());

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), weak1.header(), stack.header()));
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(weak1.header()), false);
        ASSERT_THAT(weak1.get(), object1.header());

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(weak1.header(), stack.header()));
        EXPECT_THAT(gc::isMarked(weak1.header()), false);
        EXPECT_THAT(weak1.get(), nullptr);
    });
}

TYPED_TEST_P(TracingGCTest, ObjectReferencedFromRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);
        auto& object3 = AllocateObject(threadData);
        auto& object4 = AllocateObject(threadData);

        global->field1 = object1.header();
        object1->field1 = object2.header();
        stack->field1 = object3.header();
        object3->field1 = object4.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        ASSERT_THAT(gc::isMarked(global.header()), false);
        ASSERT_THAT(gc::isMarked(stack.header()), false);
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(object2.header()), false);
        ASSERT_THAT(gc::isMarked(object3.header()), false);
        ASSERT_THAT(gc::isMarked(object4.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(gc::isMarked(global.header()), false);
        EXPECT_THAT(gc::isMarked(stack.header()), false);
        EXPECT_THAT(gc::isMarked(object1.header()), false);
        EXPECT_THAT(gc::isMarked(object2.header()), false);
        EXPECT_THAT(gc::isMarked(object3.header()), false);
        EXPECT_THAT(gc::isMarked(object4.header()), false);
    });
}

TYPED_TEST_P(TracingGCTest, ObjectsWithCycles) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);
        auto& object3 = AllocateObject(threadData);
        auto& object4 = AllocateObject(threadData);
        auto& object5 = AllocateObject(threadData);
        auto& object6 = AllocateObject(threadData);

        global->field1 = object1.header();
        object1->field1 = object2.header();
        object2->field1 = object1.header();
        stack->field1 = object3.header();
        object3->field1 = object4.header();
        object4->field1 = object3.header();
        object5->field1 = object6.header();
        object6->field1 = object5.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header(),
                        object5.header(), object6.header()));
        ASSERT_THAT(gc::isMarked(global.header()), false);
        ASSERT_THAT(gc::isMarked(stack.header()), false);
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(object2.header()), false);
        ASSERT_THAT(gc::isMarked(object3.header()), false);
        ASSERT_THAT(gc::isMarked(object4.header()), false);
        ASSERT_THAT(gc::isMarked(object5.header()), false);
        ASSERT_THAT(gc::isMarked(object6.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(gc::isMarked(global.header()), false);
        EXPECT_THAT(gc::isMarked(stack.header()), false);
        EXPECT_THAT(gc::isMarked(object1.header()), false);
        EXPECT_THAT(gc::isMarked(object2.header()), false);
        EXPECT_THAT(gc::isMarked(object3.header()), false);
        EXPECT_THAT(gc::isMarked(object4.header()), false);
    });
}

TYPED_TEST_P(TracingGCTest, ObjectsWithCyclesAndFinalizers) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObjectWithFinalizer(threadData);
        auto& object2 = AllocateObjectWithFinalizer(threadData);
        auto& object3 = AllocateObjectWithFinalizer(threadData);
        auto& object4 = AllocateObjectWithFinalizer(threadData);
        auto& object5 = AllocateObjectWithFinalizer(threadData);
        auto& object6 = AllocateObjectWithFinalizer(threadData);

        global->field1 = object1.header();
        object1->field1 = object2.header();
        object2->field1 = object1.header();
        stack->field1 = object3.header();
        object3->field1 = object4.header();
        object4->field1 = object3.header();
        object5->field1 = object6.header();
        object6->field1 = object5.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header(),
                        object5.header(), object6.header()));
        ASSERT_THAT(gc::isMarked(global.header()), false);
        ASSERT_THAT(gc::isMarked(stack.header()), false);
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(object2.header()), false);
        ASSERT_THAT(gc::isMarked(object3.header()), false);
        ASSERT_THAT(gc::isMarked(object4.header()), false);
        ASSERT_THAT(gc::isMarked(object5.header()), false);
        ASSERT_THAT(gc::isMarked(object6.header()), false);

        EXPECT_CALL(this->finalizerHook(), Call(object5.header()));
        EXPECT_CALL(this->finalizerHook(), Call(object6.header()));
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(gc::isMarked(global.header()), false);
        EXPECT_THAT(gc::isMarked(stack.header()), false);
        EXPECT_THAT(gc::isMarked(object1.header()), false);
        EXPECT_THAT(gc::isMarked(object2.header()), false);
        EXPECT_THAT(gc::isMarked(object3.header()), false);
        EXPECT_THAT(gc::isMarked(object4.header()), false);
    });
}

TYPED_TEST_P(TracingGCTest, ObjectsWithCyclesIntoRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);

        global->field1 = object1.header();
        object1->field1 = global.header();
        stack->field1 = object2.header();
        object2->field1 = stack.header();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), stack.header(), object1.header(), object2.header()));
        ASSERT_THAT(gc::isMarked(global.header()), false);
        ASSERT_THAT(gc::isMarked(stack.header()), false);
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(object2.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), stack.header(), object1.header(), object2.header()));
        EXPECT_THAT(gc::isMarked(global.header()), false);
        EXPECT_THAT(gc::isMarked(stack.header()), false);
        EXPECT_THAT(gc::isMarked(object1.header()), false);
        EXPECT_THAT(gc::isMarked(object2.header()), false);
    });
}

TYPED_TEST_P(TracingGCTest, RunGCTwice) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack{threadData};
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);
        auto& object3 = AllocateObject(threadData);
        auto& object4 = AllocateObject(threadData);
        auto& object5 = AllocateObject(threadData);
        auto& object6 = AllocateObject(threadData);

        global->field1 = object1.header();
        object1->field1 = object2.header();
        object2->field1 = object1.header();
        stack->field1 = object3.header();
        object3->field1 = object4.header();
        object4->field1 = object3.header();
        object5->field1 = object6.header();
        object6->field1 = object5.header();

        ASSERT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header(),
                        object5.header(), object6.header()));
        ASSERT_THAT(gc::isMarked(global.header()), false);
        ASSERT_THAT(gc::isMarked(stack.header()), false);
        ASSERT_THAT(gc::isMarked(object1.header()), false);
        ASSERT_THAT(gc::isMarked(object2.header()), false);
        ASSERT_THAT(gc::isMarked(object3.header()), false);
        ASSERT_THAT(gc::isMarked(object4.header()), false);
        ASSERT_THAT(gc::isMarked(object5.header()), false);
        ASSERT_THAT(gc::isMarked(object6.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(gc::isMarked(global.header()), false);
        EXPECT_THAT(gc::isMarked(stack.header()), false);
        EXPECT_THAT(gc::isMarked(object1.header()), false);
        EXPECT_THAT(gc::isMarked(object2.header()), false);
        EXPECT_THAT(gc::isMarked(object3.header()), false);
        EXPECT_THAT(gc::isMarked(object4.header()), false);
    });
}

TYPED_TEST_P(TracingGCTest, PermanentObjects) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalPermanentObjectHolder global1{threadData};
        GlobalObjectHolder global2{threadData};
        test_support::Object<Payload> permanentObject{typeHolder.typeInfo()};
        permanentObject.header()->typeInfoOrMeta_ =
                setPointerBits(permanentObject.header()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
        RuntimeAssert(permanentObject.header()->permanent(), "Must be permanent");

        global1->field1 = permanentObject.header();
        global2->field1 = global1.header();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(global2.header()));
        EXPECT_THAT(gc::isMarked(global2.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global2.header()));
        EXPECT_THAT(gc::isMarked(global2.header()), false);
    });
}

TYPED_TEST_P(TracingGCTest, SameObjectInRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack(*global);
        auto& object = AllocateObject(threadData);

        global->field1 = object.header();

        ASSERT_THAT(global.header(), stack.header());
        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), object.header()));
        EXPECT_THAT(gc::isMarked(global.header()), false);
        EXPECT_THAT(gc::isMarked(object.header()), false);

        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), object.header()));
        EXPECT_THAT(gc::isMarked(global.header()), false);
        EXPECT_THAT(gc::isMarked(object.header()), false);
    });
}

namespace {

class Mutator : private Pinned {
public:
    template <typename F>
    [[nodiscard]] std::future<void> Execute(F&& f) {
        return executor_.execute(
                [this, f = std::forward<F>(f)] { f(*executor_.context().memory_->memoryState()->GetThreadData(), *this); });
    }

    StackObjectHolder& AddStackRoot() {
        RuntimeAssert(std::this_thread::get_id() == executor_.threadId(), "AddStackRoot can only be called in the mutator thread");
        auto& context = executor_.context();
        auto holder = std::make_unique<StackObjectHolder>(*context.memory_->memoryState()->GetThreadData());
        auto& holderRef = *holder;
        context.stackRoots_.push_back(std::move(holder));
        return holderRef;
    }

    StackObjectHolder& AddStackRoot(ObjHeader* object) {
        RuntimeAssert(std::this_thread::get_id() == executor_.threadId(), "AddStackRoot can only be called in the mutator thread");
        auto& context = executor_.context();
        auto holder = std::make_unique<StackObjectHolder>(object);
        auto& holderRef = *holder;
        context.stackRoots_.push_back(std::move(holder));
        return holderRef;
    }

    GlobalObjectHolder& AddGlobalRoot() {
        RuntimeAssert(std::this_thread::get_id() == executor_.threadId(), "AddGlobalRoot can only be called in the mutator thread");
        auto& context = executor_.context();
        auto holder = std::make_unique<GlobalObjectHolder>(*context.memory_->memoryState()->GetThreadData());
        auto& holderRef = *holder;
        context.globalRoots_.push_back(std::move(holder));
        return holderRef;
    }

    GlobalObjectHolder& AddGlobalRoot(ObjHeader* object) {
        RuntimeAssert(std::this_thread::get_id() == executor_.threadId(), "AddGlobalRoot can only be called in the mutator thread");
        auto& context = executor_.context();
        auto holder = std::make_unique<GlobalObjectHolder>(*context.memory_->memoryState()->GetThreadData(), object);
        auto& holderRef = *holder;
        context.globalRoots_.push_back(std::move(holder));
        return holderRef;
    }

    std::vector<ObjHeader*> Alive() { return ::Alive(*executor_.context().memory_->memoryState()->GetThreadData()); }

private:
    struct Context {
        std::unique_ptr<ScopedMemoryInit> memory_;
        std::vector<std::unique_ptr<StackObjectHolder>> stackRoots_;
        std::vector<std::unique_ptr<GlobalObjectHolder>> globalRoots_;

        Context() : memory_(std::make_unique<ScopedMemoryInit>()) {
            // SingleThreadExecutor must work in the runnable state, so that GC does not collect between tasks.
            AssertThreadState(memory_->memoryState(), ThreadState::kRunnable);
        }
    };

    SingleThreadExecutor<Context> executor_;
};

} // namespace

TYPED_TEST_P(TracingGCTest, MultipleMutatorsCollect) {
    std::vector<Mutator> mutators(kDefaultThreadCount);
    std::vector<ObjHeader*> globals(kDefaultThreadCount);
    std::vector<ObjHeader*> locals(kDefaultThreadCount);
    std::vector<ObjHeader*> reachables(kDefaultThreadCount);

    auto expandRootSet = [&globals, &locals, &reachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = mutator.AddGlobalRoot();
        auto& local = mutator.AddStackRoot();
        auto& reachable = AllocateObject(threadData);
        AllocateObject(threadData);
        local->field1 = reachable.header();
        globals[i] = global.header();
        locals[i] = local.header();
        reachables[i] = reachable.header();
    };

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i); })
                .wait();
    }

    std::vector<std::future<void>> gcFutures;

    std::atomic<bool> gcDone = false;
    for (auto& mutator : mutators) {
        gcFutures.emplace_back(mutator.Execute([&](mm::ThreadData& threadData, Mutator& mutator) noexcept {
            while (!gcDone.load(std::memory_order_relaxed)) {
                mm::safePoint(threadData);
            }
        }));
    }

    mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
    gcDone.store(true, std::memory_order_relaxed);

    for (auto& future : gcFutures) {
        future.wait();
    }

    std::vector<ObjHeader*> expectedAlive;
    for (auto& global : globals) {
        expectedAlive.push_back(global);
    }
    for (auto& local : locals) {
        expectedAlive.push_back(local);
    }
    for (auto& reachable : reachables) {
        expectedAlive.push_back(reachable);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }
}

TYPED_TEST_P(TracingGCTest, MultipleMutatorsAllCollect) {
    std::vector<Mutator> mutators(kDefaultThreadCount);
    std::vector<ObjHeader*> globals(kDefaultThreadCount);
    std::vector<ObjHeader*> locals(kDefaultThreadCount);
    std::vector<ObjHeader*> reachables(kDefaultThreadCount);

    auto expandRootSet = [&globals, &locals, &reachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = mutator.AddGlobalRoot();
        auto& local = mutator.AddStackRoot();
        auto& reachable = AllocateObject(threadData);
        AllocateObject(threadData);
        local->field1 = reachable.header();
        globals[i] = global.header();
        locals[i] = local.header();
        reachables[i] = reachable.header();
    };

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i); })
                .wait();
    }

    std::vector<std::future<void>> gcFutures;

    for (auto& mutator : mutators) {
        gcFutures.emplace_back(mutator.Execute([](mm::ThreadData& threadData, Mutator& mutator) {
            mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
            // If GC starts before all thread executed line above, two gc will be run
            // So we temporarily switch threads to native state and then return them back after all GC runs are done
            SwitchThreadState(mm::GetMemoryState(), kotlin::ThreadState::kNative);
        }));
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([](mm::ThreadData& threadData, Mutator& mutator) {
                    SwitchThreadState(mm::GetMemoryState(), kotlin::ThreadState::kRunnable);
                })
                .wait();
    }

    std::vector<ObjHeader*> expectedAlive;
    for (auto& global : globals) {
        expectedAlive.push_back(global);
    }
    for (auto& local : locals) {
        expectedAlive.push_back(local);
    }
    for (auto& reachable : reachables) {
        expectedAlive.push_back(reachable);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }
}

TYPED_TEST_P(TracingGCTest, MultipleMutatorsAddToRootSetAfterCollectionRequested) {
    std::vector<Mutator> mutators(kDefaultThreadCount);
    std::vector<ObjHeader*> globals(kDefaultThreadCount);
    std::vector<ObjHeader*> locals(kDefaultThreadCount);
    std::vector<ObjHeader*> reachables(kDefaultThreadCount);

    auto allocateInHeap = [&globals, &locals, &reachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = AllocateObject(threadData);
        auto& local = AllocateObject(threadData);
        auto& reachable = AllocateObject(threadData);
        AllocateObject(threadData);

        local->field1 = reachable.header();

        globals[i] = global.header();
        locals[i] = local.header();
        reachables[i] = reachable.header();
    };

    auto expandRootSet = [&globals, &locals](mm::ThreadData& threadData, Mutator& mutator, int i) {
        mutator.AddGlobalRoot(globals[i]);
        mutator.AddStackRoot(locals[i]);
    };

    // Allocate everything in heap before scheduling the GC.
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([allocateInHeap, i](mm::ThreadData& threadData, Mutator& mutator) { allocateInHeap(threadData, mutator, i); })
                .wait();
    }

    std::vector<std::future<void>> gcFutures;
    auto epoch = mm::GlobalData::Instance().gc().Schedule();
    std::atomic<bool> gcDone = false;

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        gcFutures.emplace_back(mutators[i].Execute([&gcDone, i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) {
            expandRootSet(threadData, mutator, i);
            while (!gcDone.load(std::memory_order_relaxed)) {
                mm::safePoint(threadData);
            }
        }));
    }

    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
    gcDone.store(true, std::memory_order_relaxed);

    for (auto& future : gcFutures) {
        future.wait();
    }

    std::vector<ObjHeader*> expectedAlive;
    for (auto& global : globals) {
        expectedAlive.push_back(global);
    }
    for (auto& local : locals) {
        expectedAlive.push_back(local);
    }
    for (auto& reachable : reachables) {
        expectedAlive.push_back(reachable);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }
}

TYPED_TEST_P(TracingGCTest, CrossThreadReference) {
    std::vector<Mutator> mutators(kDefaultThreadCount);
    std::vector<ObjHeader*> globals(kDefaultThreadCount);
    std::vector<ObjHeader*> locals(kDefaultThreadCount);
    std::vector<ObjHeader*> reachables(2 * kDefaultThreadCount);

    auto expandRootSet = [&globals, &locals, &reachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = mutator.AddGlobalRoot();
        auto& local = mutator.AddStackRoot();
        auto& reachable1 = AllocateObject(threadData);
        auto& reachable2 = AllocateObject(threadData);
        globals[i] = global.header();
        locals[i] = local.header();
        reachables[2 * i] = reachable1.header();
        reachables[2 * i + 1] = reachable2.header();

        // Expected to be run consequtively, so `reachables` for `j < i` are set.
        if (i != 0) {
            global->field1 = reachables[2 * (i - 1)];
            local->field1 = reachables[2 * (i - 1) + 1];
        }
    };

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        // `expandRootSet` is expected to be run consequtively for each thread, so `.wait()` is required below.
        mutators[i]
                .Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i); })
                .wait();
    }

    std::vector<std::future<void>> gcFutures;

    std::atomic<bool> gcDone = false;
    for (auto& mutator : mutators) {
        gcFutures.emplace_back(mutator.Execute([&](mm::ThreadData& threadData, Mutator& mutator) noexcept {
            while (!gcDone.load(std::memory_order_relaxed)) {
                mm::safePoint(threadData);
            }
        }));
    }

    mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
    gcDone.store(true, std::memory_order_relaxed);

    for (auto& future : gcFutures) {
        future.wait();
    }

    std::vector<ObjHeader*> expectedAlive;
    for (auto& global : globals) {
        expectedAlive.push_back(global);
    }
    for (auto& local : locals) {
        expectedAlive.push_back(local);
    }
    // The last two are in fact unreachable. Their absence allows us to check that GC was in fact performed.
    reachables.pop_back();
    reachables.pop_back();
    for (auto& reachable : reachables) {
        expectedAlive.push_back(reachable);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }
}

TYPED_TEST_P(TracingGCTest, NewThreadsWhileRequestingCollection) {
    std::vector<Mutator> mutators(kDefaultThreadCount);
    std::vector<ObjHeader*> globals(2 * kDefaultThreadCount);
    std::vector<ObjHeader*> locals(2 * kDefaultThreadCount);
    std::vector<ObjHeader*> reachables(2 * kDefaultThreadCount);
    std::vector<ObjHeader*> unreachables(2 * kDefaultThreadCount);

    auto expandRootSet = [&globals, &locals, &reachables, &unreachables](mm::ThreadData& threadData, Mutator& mutator, int i) {
        auto& global = mutator.AddGlobalRoot();
        auto& local = mutator.AddStackRoot();
        auto& reachable = AllocateObject(threadData);
        auto& unreachable = AllocateObject(threadData);
        local->field1 = reachable.header();
        globals[i] = global.header();
        locals[i] = local.header();
        reachables[i] = reachable.header();
        unreachables[i] = unreachable.header();
    };

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i); })
                .wait();
    }

    std::vector<std::future<void>> gcFutures;
    auto epoch = mm::GlobalData::Instance().gc().Schedule();
    std::atomic<bool> gcDone = false;

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    // Now start attaching new threads.
    std::vector<Mutator> newMutators(kDefaultThreadCount);
    std::vector<std::future<void>> attachFutures(kDefaultThreadCount);

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        attachFutures[i] = newMutators[i].Execute([&gcDone, i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) {
            expandRootSet(threadData, mutator, i + kDefaultThreadCount);
            while (!gcDone.load(std::memory_order_relaxed)) {
                mm::safePoint(threadData);
            }
        });
    }

    // All the other threads are stopping at safe points.
    for (auto& mutator : mutators) {
        gcFutures.emplace_back(mutator.Execute([&gcDone](mm::ThreadData& threadData, Mutator& mutator) {
            while (!gcDone.load(std::memory_order_relaxed)) {
                mm::safePoint(threadData);
            }
        }));
    }

    // Wait for the GC to be done.
    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
    gcDone.store(true, std::memory_order_relaxed);

    // GC will be completed first
    for (auto& future : gcFutures) {
        future.wait();
    }

    // Only then will the new threads be allowed to attach.
    for (auto& future : attachFutures) {
        future.wait();
    }

    std::vector<ObjHeader*> expectedAlive;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        expectedAlive.push_back(globals[i]);
        expectedAlive.push_back(locals[i]);
        expectedAlive.push_back(reachables[i]);
        expectedAlive.push_back(globals[kDefaultThreadCount + i]);
        expectedAlive.push_back(locals[kDefaultThreadCount + i]);
        expectedAlive.push_back(reachables[kDefaultThreadCount + i]);
        // Unreachables for new threads were not collected.
        expectedAlive.push_back(unreachables[kDefaultThreadCount + i]);
    }

    // Force mutators to publish their internal heaps
    // Really only needed for legacy allocators.
    std::vector<std::future<void>> publishFutures;
    for (auto& mutator : mutators) {
        publishFutures.emplace_back(
                mutator.Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.allocator().prepareForGC(); }));
    }
    for (auto& mutator : newMutators) {
        publishFutures.emplace_back(
                mutator.Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.allocator().prepareForGC(); }));
    }
    for (auto& future : publishFutures) {
        future.wait();
    }

    // All threads see the same alive objects, enough to check a single mutator.
    EXPECT_THAT(mutators[0].Alive(), testing::UnorderedElementsAreArray(expectedAlive));
}

TYPED_TEST_P(TracingGCTest, FreeObjectWithFreeWeakReversedOrder) {
    std::vector<Mutator> mutators(2);
    std::atomic<test_support::Object<Payload>*> object1 = nullptr;
    std::atomic<test_support::RegularWeakReferenceImpl*> weak = nullptr;
    std::atomic<bool> done = false;
    auto f0 = mutators[0].Execute([&](mm::ThreadData& threadData, Mutator&) {
        GlobalObjectHolder global1{threadData};
        auto& object1_local = AllocateObject(threadData);
        object1 = &object1_local;
        global1->field1 = object1_local.header();
        while (weak.load() == nullptr)
            ;
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1_local.header(), weak.load()->header(), global1.header()));
        ASSERT_THAT(gc::isMarked(global1.header()), false);
        ASSERT_THAT(gc::isMarked(object1_local.header()), false);
        ASSERT_THAT(gc::isMarked(weak.load()->header()), false);
        ASSERT_THAT(weak.load()->get(), object1_local.header());

        global1->field1 = nullptr;

        EXPECT_CALL(this->finalizerHook(), Call(weak.load()->header()));
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global1.header()));
        done = true;
    });

    auto f1 = mutators[1].Execute([&](mm::ThreadData& threadData, Mutator&) {
        while (object1.load() == nullptr) {
        }
        ObjHolder holder;
        auto& weak_local = test_support::InstallWeakReference(threadData, object1.load()->header(), holder.slot());
        weak = &weak_local;
        *holder.slot() = nullptr;
        while (!done) mm::safePoint(threadData);
    });

    f0.wait();
    f1.wait();
}

TYPED_TEST_P(TracingGCTest, MutateBetweenSafePoints) {
    constexpr auto kQuant = 5;
    constexpr auto kGcNumber = 5;
    constexpr auto kMaxItersPerGC = 10;

    std::atomic<bool> stopMutation = false;
    std::atomic<bool> startMutation = false;
    std::atomic<std::size_t> initializedMutators = 0;
    std::atomic<int> gcEpoch = 0;

    Mutator scheduler;
    std::future<void> schedulerFuture = scheduler.Execute([&](mm::ThreadData& threadData, Mutator& mutator) {
        while (initializedMutators < kDefaultThreadCount) { /* wait */ }
        startMutation = true;
        for (int i = 0; i < kGcNumber; ++i) {
            gcEpoch = i;
            mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
        }
        stopMutation = true;
    });

    std::vector<Mutator> mutators(kDefaultThreadCount);
    std::vector<std::future<void>> mutatorFutures;

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutatorFutures.emplace_back(mutators[i].Execute([&](mm::ThreadData& threadData, Mutator& mutator) {
            StackObjectHolder head{threadData};
            ObjHeader* tail = head.header();

            auto append = [&](int count) {
                for (int i = 0; i < count; ++i) {
                    auto& next = AllocateObject(threadData);
                    auto& tailObj = test_support::Object<Payload>::FromObjHeader(tail);
                    tailObj->field1 = next.header();
                    tail = next.header();
                }
            };

            auto cut = [&](ObjHeader* first, int count) {
                ObjHeader* last = first;
                for (int i = 0; i < count; ++i) {
                    auto& lastObj = test_support::Object<Payload>::FromObjHeader(last);
                    last = lastObj->field1.accessor().load();
                }
                auto& firstObj = test_support::Object<Payload>::FromObjHeader(first);
                auto& lastObj = test_support::Object<Payload>::FromObjHeader(last);
                firstObj->field1.accessor() = lastObj->field1.accessor().load();
            };

            // Initialize
            append(kQuant * 2);
            ++initializedMutators;
            while (!startMutation.load()) { /* wait */ };

            int iter = 0;
            while (!stopMutation.load()) {
                // do not let mutator outpace gc to much
                while (iter > (gcEpoch.load() * kMaxItersPerGC) && !stopMutation.load()) {
                    mm::safePoint(threadData);
                }

                auto oldTail = tail;
                append(kQuant);
                // [head]->(a)->...->(b)->...->(oldTail)->(c)->...->(d)
                mm::safePoint(threadData);

                append(kQuant);
                // [head]->(a)->...->(b)->...->(oldTail)->(c)->...->(d)->...->(tail)
                mm::safePoint(threadData);

                cut(head.header(), kQuant);
                // (a)->...-+
                //          v
                // [head]->(b)->...->(oldTail)->(c)->...->(d)->...->(tail)
                mm::safePoint(threadData);

                cut(oldTail, kQuant);
                // (a)->...-+           (c)->...-+
                //          v                    v
                // [head]->(b)->...->(oldTail)->(d)->...->(tail)
                mm::safePoint(threadData);
                ++iter;
            }

            std::size_t length = 0;
            auto* node = &test_support::Object<Payload>::FromObjHeader(head.header());
            while ((*node)->field1.accessor().load() != nullptr) {
                node = &test_support::Object<Payload>::FromObjHeader((*node)->field1.accessor().load());
                ++length;
            }

            EXPECT_THAT(length, kQuant * 2);
        }));
    }

    schedulerFuture.wait();
    for (auto& future : mutatorFutures) {
        future.wait();
    }
}

TYPED_TEST_P(TracingGCTest, WeakResuractionInMark) {
    constexpr auto kObjectsPerThread = 100;
    std::vector<Mutator> mutators(kDefaultThreadCount);
    std::vector<test_support::RegularWeakReferenceImpl*> weaks(kDefaultThreadCount);
    std::vector<test_support::Object<Payload>*> roots(kDefaultThreadCount);

    // initialize threads
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i].Execute([&, i](mm::ThreadData& threadData, Mutator& mutator) {
            // make sure GC will have somthing to do whil we play with weak references
            for (int j = 0; j < kObjectsPerThread; ++j) {
                auto& object = AllocateObject(threadData);
                object->field1 = roots[i]->header();
                roots[i] = &object;
            }
            mutator.AddGlobalRoot(roots[i]->header());

            auto& weakReferee = AllocateObject(threadData);
            auto& weakRef = [&threadData, &weakReferee]() -> test_support::RegularWeakReferenceImpl& {
                ObjHolder holder;
                return test_support::InstallWeakReference(threadData, weakReferee.header(), holder.slot());
            }();
            EXPECT_NE(weakRef.get(), nullptr);
            weaks[i] = &weakRef;
            mutator.AddGlobalRoot(weakRef.header());
        }).wait();
    }

    auto epoch = mm::GlobalData::Instance().gc().Schedule();
    std::atomic gcDone = false;

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {}

    std::vector<std::future<void>> mutatorFutures;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutatorFutures.emplace_back(mutators[i].Execute([&, i](mm::ThreadData& threadData, Mutator& mutator) noexcept {
            safePoint(threadData);

            auto weakReferee = weaks[i]->get();
            (*roots[i])->field2 = weakReferee;
            bool resurected = weakReferee != nullptr;

            while (!gcDone.load(std::memory_order_relaxed)) {
                safePoint(threadData);
            }
            if (resurected) {
                EXPECT_NE(weaks[i]->get(), nullptr);
            } else {
                EXPECT_EQ(weaks[i]->get(), nullptr);
            }
        }));
    }

    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
    gcDone = true;

    for (auto& future : mutatorFutures) {
        future.wait();
    }
}

#define TRACING_GC_TEST_LIST \
    RootSet, \
    InterconnectedRootSet, \
    FreeObjects, \
    FreeObjectsWithFinalizers, \
    FreeObjectWithFreeWeak, \
    FreeObjectWithHoldedWeak, \
    ObjectReferencedFromRootSet, \
    ObjectsWithCycles, \
    ObjectsWithCyclesAndFinalizers, \
    ObjectsWithCyclesIntoRootSet, \
    RunGCTwice, \
    PermanentObjects, \
    SameObjectInRootSet, \
    MultipleMutatorsCollect, \
    MultipleMutatorsAllCollect, \
    MultipleMutatorsAddToRootSetAfterCollectionRequested, \
    CrossThreadReference, \
    NewThreadsWhileRequestingCollection, \
    FreeObjectWithFreeWeakReversedOrder, \
    MutateBetweenSafePoints, \
    WeakResuractionInMark

template <typename FixtureImpl>
class STWMarkGCTest : public TracingGCTest<FixtureImpl> {};

TYPED_TEST_SUITE_P(STWMarkGCTest);

TYPED_TEST_P(STWMarkGCTest, MultipleMutatorsWeaks) {
    std::vector<Mutator> mutators(kDefaultThreadCount);
    ObjHeader* globalRoot = nullptr;
    test_support::RegularWeakReferenceImpl* weak = nullptr;

    mutators[0]
            .Execute([&weak, &globalRoot](mm::ThreadData& threadData, Mutator& mutator) {
                auto& global = mutator.AddGlobalRoot();

                auto& object = AllocateObject(threadData);
                auto& objectWeak = ([&threadData, &object]() -> test_support::RegularWeakReferenceImpl& {
                    ObjHolder holder;
                    return test_support::InstallWeakReference(threadData, object.header(), holder.slot());
                })();
                global->field1 = objectWeak.header();
                weak = &objectWeak;
                globalRoot = global.header();
            })
            .wait();

    // Make sure all mutators are initialized.
    for (int i = 1; i < kDefaultThreadCount; ++i) {
        mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) {}).wait();
    }

    std::vector<std::future<void>> gcFutures;
    auto epoch = mm::GlobalData::Instance().gc().Schedule();
    std::atomic<bool> gcDone = false;

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (auto& mutator : mutators) {
        gcFutures.emplace_back(mutator.Execute([&](mm::ThreadData& threadData, Mutator& mutator) noexcept {
            while (!gcDone.load(std::memory_order_relaxed)) {
                mm::safePoint(threadData);
                EXPECT_THAT(weak->get(), nullptr);
            }
        }));
    }

    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
    gcDone.store(true, std::memory_order_relaxed);

    for (auto& future : gcFutures) {
        future.wait();
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAre(globalRoot, weak->header()));
    }
}

TYPED_TEST_P(STWMarkGCTest, MultipleMutatorsWeakNewObj) {
    std::vector<Mutator> mutators(kDefaultThreadCount);

    // Make sure all mutators are initialized.
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) {}).wait();
    }

    std::vector<std::future<void>> gcFutures;
    auto epoch = mm::GlobalData::Instance().gc().Schedule();
    std::atomic<bool> gcDone = false;

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (auto& mutator : mutators) {
        gcFutures.emplace_back(mutator.Execute([&](mm::ThreadData& threadData, Mutator& mutator) noexcept {
            mm::safePoint(threadData);

            auto& object = AllocateObject(threadData);
            auto& objectWeak = ([&threadData, &object]() -> test_support::RegularWeakReferenceImpl& {
                ObjHolder holder;
                return test_support::InstallWeakReference(threadData, object.header(), holder.slot());
            })();
            EXPECT_NE(objectWeak.get(), nullptr);

            while (!gcDone.load(std::memory_order_relaxed)) {
                mm::safePoint(threadData);
            }
        }));
    }

    mm::GlobalData::Instance().gc().WaitFinalizers(epoch);
    gcDone.store(true, std::memory_order_relaxed);

    for (auto& future : gcFutures) {
        future.wait();
    }
}

#define STW_MARK_GC_TEST_LIST \
    MultipleMutatorsWeaks, \
    MultipleMutatorsWeakNewObj

// expand lists before stringization in REGISTER_TYPED_TEST_SUITE_P
#define REGISTER_TYPED_TEST_SUITE_WITH_LISTS(SuiteName, ...) REGISTER_TYPED_TEST_SUITE_P(SuiteName, __VA_ARGS__)
