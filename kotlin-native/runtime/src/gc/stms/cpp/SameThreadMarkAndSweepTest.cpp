/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SameThreadMarkAndSweep.hpp"

#include <condition_variable>
#include <future>
#include <mutex>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ExtraObjectData.hpp"
#include "FinalizerHooksTestSupport.hpp"
#include "GCImpl.hpp"
#include "GlobalData.hpp"
#include "ObjectOps.hpp"
#include "ObjectTestSupport.hpp"
#include "SingleThreadExecutor.hpp"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "WeakRef.hpp"
#include "std_support/Memory.hpp"
#include "std_support/Vector.hpp"

using namespace kotlin;

// These tests can only work if `GC` is `SameThreadMarkAndSweep`.

namespace {

struct Payload {
    ObjHeader* field1;
    ObjHeader* field2;
    ObjHeader* field3;

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

    ObjHeader*& operator[](size_t index) noexcept { return (**this).elements()[index]; }

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

    ObjHeader*& operator[](size_t index) noexcept { return (**this).elements()[index]; }

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

std_support::vector<ObjHeader*> Alive(mm::ThreadData& threadData) {
    std_support::vector<ObjHeader*> objects;
    for (auto node : threadData.gc().impl().objectFactoryThreadQueue()) {
        objects.push_back(node.GetObjHeader());
    }
    for (auto node : mm::GlobalData::Instance().gc().impl().objectFactory().LockForIter()) {
        objects.push_back(node.GetObjHeader());
    }
    return objects;
}

bool IsMarked(ObjHeader* objHeader) {
    auto nodeRef = mm::ObjectFactory<gc::SameThreadMarkAndSweep>::NodeRef::From(objHeader);
    return nodeRef.ObjectData().marked();
}

test_support::RegularWeakReferenceImpl& InstallWeakReference(mm::ThreadData& threadData, ObjHeader* objHeader, ObjHeader** location) {
    mm::AllocateObject(&threadData, theRegularWeakReferenceImplTypeInfo, location);
    auto& weakReference = test_support::RegularWeakReferenceImpl::FromObjHeader(*location);
    auto& extraObjectData = mm::ExtraObjectData::GetOrInstall(objHeader);
    weakReference->weakRef = static_cast<mm::RawSpecialRef*>(mm::WeakRef::create(objHeader));
    weakReference->referred = objHeader;
    auto* setWeakRef = extraObjectData.GetOrSetRegularWeakReferenceImpl(objHeader, weakReference.header());
    EXPECT_EQ(setWeakRef, weakReference.header());
    return weakReference;
}

class SameThreadMarkAndSweepTest : public testing::Test {
public:
    ~SameThreadMarkAndSweepTest() {
        mm::GlobalsRegistry::Instance().ClearForTests();
        mm::SpecialRefRegistry::instance().clearForTests();
        mm::GlobalData::Instance().extraObjectDataFactory().ClearForTests();
        mm::GlobalData::Instance().gc().impl().objectFactory().ClearForTests();
    }

    testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHooks_.finalizerHook(); }

private:
    FinalizerHooksTestSupport finalizerHooks_;
};

} // namespace

TEST_F(SameThreadMarkAndSweepTest, RootSet) {
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
        ASSERT_THAT(IsMarked(global1.header()), false);
        ASSERT_THAT(IsMarked(global2.header()), false);
        ASSERT_THAT(IsMarked(global3.header()), false);
        ASSERT_THAT(IsMarked(stack1.header()), false);
        ASSERT_THAT(IsMarked(stack2.header()), false);
        ASSERT_THAT(IsMarked(stack3.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        EXPECT_THAT(IsMarked(global1.header()), false);
        EXPECT_THAT(IsMarked(global2.header()), false);
        EXPECT_THAT(IsMarked(global3.header()), false);
        EXPECT_THAT(IsMarked(stack1.header()), false);
        EXPECT_THAT(IsMarked(stack2.header()), false);
        EXPECT_THAT(IsMarked(stack3.header()), false);
    });
}

TEST_F(SameThreadMarkAndSweepTest, InterconnectedRootSet) {
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
        ASSERT_THAT(IsMarked(global1.header()), false);
        ASSERT_THAT(IsMarked(global2.header()), false);
        ASSERT_THAT(IsMarked(global3.header()), false);
        ASSERT_THAT(IsMarked(stack1.header()), false);
        ASSERT_THAT(IsMarked(stack2.header()), false);
        ASSERT_THAT(IsMarked(stack3.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global1.header(), global2.header(), global3.header(), stack1.header(), stack2.header(), stack3.header()));
        EXPECT_THAT(IsMarked(global1.header()), false);
        EXPECT_THAT(IsMarked(global2.header()), false);
        EXPECT_THAT(IsMarked(global3.header()), false);
        EXPECT_THAT(IsMarked(stack1.header()), false);
        EXPECT_THAT(IsMarked(stack2.header()), false);
        EXPECT_THAT(IsMarked(stack3.header()), false);
    });
}

TEST_F(SameThreadMarkAndSweepTest, FreeObjects) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        auto& object2 = AllocateObject(threadData);

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), object2.header()));
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(object2.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TEST_F(SameThreadMarkAndSweepTest, FreeObjectsWithFinalizers) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        auto& object1 = AllocateObjectWithFinalizer(threadData);
        auto& object2 = AllocateObjectWithFinalizer(threadData);

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), object2.header()));
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(object2.header()), false);

        EXPECT_CALL(finalizerHook(), Call(object1.header()));
        EXPECT_CALL(finalizerHook(), Call(object2.header()));
        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TEST_F(SameThreadMarkAndSweepTest, FreeObjectWithFreeWeak) {
    RunInNewThread([this](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        auto& weak1 = ([&threadData, &object1]() -> test_support::RegularWeakReferenceImpl& {
            ObjHolder holder;
            return InstallWeakReference(threadData, object1.header(), holder.slot());
        })();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), weak1.header()));
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(weak1.header()), false);
        ASSERT_THAT(weak1.get(), object1.header());

        EXPECT_CALL(finalizerHook(), Call(weak1.header()));
        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre());
    });
}

TEST_F(SameThreadMarkAndSweepTest, FreeObjectWithHoldedWeak) {
    RunInNewThread([](mm::ThreadData& threadData) {
        auto& object1 = AllocateObject(threadData);
        StackObjectHolder stack{threadData};
        auto& weak1 = InstallWeakReference(threadData, object1.header(), &stack->field1);

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1.header(), weak1.header(), stack.header()));
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(weak1.header()), false);
        ASSERT_THAT(weak1.get(), object1.header());

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(weak1.header(), stack.header()));
        EXPECT_THAT(IsMarked(weak1.header()), false);
        EXPECT_THAT(weak1.get(), nullptr);
    });
}

TEST_F(SameThreadMarkAndSweepTest, ObjectReferencedFromRootSet) {
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
        ASSERT_THAT(IsMarked(global.header()), false);
        ASSERT_THAT(IsMarked(stack.header()), false);
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(object2.header()), false);
        ASSERT_THAT(IsMarked(object3.header()), false);
        ASSERT_THAT(IsMarked(object4.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(IsMarked(global.header()), false);
        EXPECT_THAT(IsMarked(stack.header()), false);
        EXPECT_THAT(IsMarked(object1.header()), false);
        EXPECT_THAT(IsMarked(object2.header()), false);
        EXPECT_THAT(IsMarked(object3.header()), false);
        EXPECT_THAT(IsMarked(object4.header()), false);
    });
}

TEST_F(SameThreadMarkAndSweepTest, ObjectsWithCycles) {
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
        ASSERT_THAT(IsMarked(global.header()), false);
        ASSERT_THAT(IsMarked(stack.header()), false);
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(object2.header()), false);
        ASSERT_THAT(IsMarked(object3.header()), false);
        ASSERT_THAT(IsMarked(object4.header()), false);
        ASSERT_THAT(IsMarked(object5.header()), false);
        ASSERT_THAT(IsMarked(object6.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(IsMarked(global.header()), false);
        EXPECT_THAT(IsMarked(stack.header()), false);
        EXPECT_THAT(IsMarked(object1.header()), false);
        EXPECT_THAT(IsMarked(object2.header()), false);
        EXPECT_THAT(IsMarked(object3.header()), false);
        EXPECT_THAT(IsMarked(object4.header()), false);
    });
}

TEST_F(SameThreadMarkAndSweepTest, ObjectsWithCyclesAndFinalizers) {
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
        ASSERT_THAT(IsMarked(global.header()), false);
        ASSERT_THAT(IsMarked(stack.header()), false);
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(object2.header()), false);
        ASSERT_THAT(IsMarked(object3.header()), false);
        ASSERT_THAT(IsMarked(object4.header()), false);
        ASSERT_THAT(IsMarked(object5.header()), false);
        ASSERT_THAT(IsMarked(object6.header()), false);

        EXPECT_CALL(finalizerHook(), Call(object5.header()));
        EXPECT_CALL(finalizerHook(), Call(object6.header()));
        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(IsMarked(global.header()), false);
        EXPECT_THAT(IsMarked(stack.header()), false);
        EXPECT_THAT(IsMarked(object1.header()), false);
        EXPECT_THAT(IsMarked(object2.header()), false);
        EXPECT_THAT(IsMarked(object3.header()), false);
        EXPECT_THAT(IsMarked(object4.header()), false);
    });
}

TEST_F(SameThreadMarkAndSweepTest, ObjectsWithCyclesIntoRootSet) {
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
        ASSERT_THAT(IsMarked(global.header()), false);
        ASSERT_THAT(IsMarked(stack.header()), false);
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(object2.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), stack.header(), object1.header(), object2.header()));
        EXPECT_THAT(IsMarked(global.header()), false);
        EXPECT_THAT(IsMarked(stack.header()), false);
        EXPECT_THAT(IsMarked(object1.header()), false);
        EXPECT_THAT(IsMarked(object2.header()), false);
    });
}

TEST_F(SameThreadMarkAndSweepTest, RunGCTwice) {
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
        ASSERT_THAT(IsMarked(global.header()), false);
        ASSERT_THAT(IsMarked(stack.header()), false);
        ASSERT_THAT(IsMarked(object1.header()), false);
        ASSERT_THAT(IsMarked(object2.header()), false);
        ASSERT_THAT(IsMarked(object3.header()), false);
        ASSERT_THAT(IsMarked(object4.header()), false);
        ASSERT_THAT(IsMarked(object5.header()), false);
        ASSERT_THAT(IsMarked(object6.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();
        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(
                Alive(threadData),
                testing::UnorderedElementsAre(
                        global.header(), stack.header(), object1.header(), object2.header(), object3.header(), object4.header()));
        EXPECT_THAT(IsMarked(global.header()), false);
        EXPECT_THAT(IsMarked(stack.header()), false);
        EXPECT_THAT(IsMarked(object1.header()), false);
        EXPECT_THAT(IsMarked(object2.header()), false);
        EXPECT_THAT(IsMarked(object3.header()), false);
        EXPECT_THAT(IsMarked(object4.header()), false);
    });
}

TEST_F(SameThreadMarkAndSweepTest, PermanentObjects) {
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
        EXPECT_THAT(IsMarked(global2.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global2.header()));
        EXPECT_THAT(IsMarked(global2.header()), false);
    });
}

TEST_F(SameThreadMarkAndSweepTest, SameObjectInRootSet) {
    RunInNewThread([](mm::ThreadData& threadData) {
        GlobalObjectHolder global{threadData};
        StackObjectHolder stack(*global);
        auto& object = AllocateObject(threadData);

        global->field1 = object.header();

        ASSERT_THAT(global.header(), stack.header());
        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), object.header()));
        EXPECT_THAT(IsMarked(global.header()), false);
        EXPECT_THAT(IsMarked(object.header()), false);

        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global.header(), object.header()));
        EXPECT_THAT(IsMarked(global.header()), false);
        EXPECT_THAT(IsMarked(object.header()), false);
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
        auto holder = std_support::make_unique<StackObjectHolder>(*context.memory_->memoryState()->GetThreadData());
        auto& holderRef = *holder;
        context.stackRoots_.push_back(std::move(holder));
        return holderRef;
    }

    StackObjectHolder& AddStackRoot(ObjHeader* object) {
        RuntimeAssert(std::this_thread::get_id() == executor_.threadId(), "AddStackRoot can only be called in the mutator thread");
        auto& context = executor_.context();
        auto holder = std_support::make_unique<StackObjectHolder>(object);
        auto& holderRef = *holder;
        context.stackRoots_.push_back(std::move(holder));
        return holderRef;
    }

    GlobalObjectHolder& AddGlobalRoot() {
        RuntimeAssert(std::this_thread::get_id() == executor_.threadId(), "AddGlobalRoot can only be called in the mutator thread");
        auto& context = executor_.context();
        auto holder = std_support::make_unique<GlobalObjectHolder>(*context.memory_->memoryState()->GetThreadData());
        auto& holderRef = *holder;
        context.globalRoots_.push_back(std::move(holder));
        return holderRef;
    }

    GlobalObjectHolder& AddGlobalRoot(ObjHeader* object) {
        RuntimeAssert(std::this_thread::get_id() == executor_.threadId(), "AddGlobalRoot can only be called in the mutator thread");
        auto& context = executor_.context();
        auto holder = std_support::make_unique<GlobalObjectHolder>(*context.memory_->memoryState()->GetThreadData(), object);
        auto& holderRef = *holder;
        context.globalRoots_.push_back(std::move(holder));
        return holderRef;
    }

    std_support::vector<ObjHeader*> Alive() { return ::Alive(*executor_.context().memory_->memoryState()->GetThreadData()); }

private:
    struct Context {
        std_support::unique_ptr<ScopedMemoryInit> memory_;
        std_support::vector<std_support::unique_ptr<StackObjectHolder>> stackRoots_;
        std_support::vector<std_support::unique_ptr<GlobalObjectHolder>> globalRoots_;

        Context() : memory_(std_support::make_unique<ScopedMemoryInit>()) {
            // SingleThreadExecutor must work in the runnable state, so that GC does not collect between tasks.
            AssertThreadState(memory_->memoryState(), ThreadState::kRunnable);
        }
    };

    SingleThreadExecutor<Context> executor_;
};

} // namespace

TEST_F(SameThreadMarkAndSweepTest, MultipleMutatorsCollect) {
    std_support::vector<Mutator> mutators(kDefaultThreadCount);
    std_support::vector<ObjHeader*> globals(kDefaultThreadCount);
    std_support::vector<ObjHeader*> locals(kDefaultThreadCount);
    std_support::vector<ObjHeader*> reachables(kDefaultThreadCount);

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

    std_support::vector<std::future<void>> gcFutures(kDefaultThreadCount);

    gcFutures[0] = mutators[0].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().ScheduleAndWaitFullGC(); });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] =
                mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().SafePointFunctionPrologue(); });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    std_support::vector<ObjHeader*> expectedAlive;
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

TEST_F(SameThreadMarkAndSweepTest, MultipleMutatorsAllCollect) {
    std_support::vector<Mutator> mutators(kDefaultThreadCount);
    std_support::vector<ObjHeader*> globals(kDefaultThreadCount);
    std_support::vector<ObjHeader*> locals(kDefaultThreadCount);
    std_support::vector<ObjHeader*> reachables(kDefaultThreadCount);

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

    std_support::vector<std::future<void>> gcFutures(kDefaultThreadCount);

    // TODO: Maybe check that only one GC is performed.
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        gcFutures[i] = mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().ScheduleAndWaitFullGC(); });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    std_support::vector<ObjHeader*> expectedAlive;
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

TEST_F(SameThreadMarkAndSweepTest, MultipleMutatorsAddToRootSetAfterCollectionRequested) {
    std_support::vector<Mutator> mutators(kDefaultThreadCount);
    std_support::vector<ObjHeader*> globals(kDefaultThreadCount);
    std_support::vector<ObjHeader*> locals(kDefaultThreadCount);
    std_support::vector<ObjHeader*> reachables(kDefaultThreadCount);

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

    mutators[0]
            .Execute([expandRootSet, allocateInHeap](mm::ThreadData& threadData, Mutator& mutator) {
                allocateInHeap(threadData, mutator, 0);
                expandRootSet(threadData, mutator, 0);
            })
            .wait();

    // Allocate everything in heap before scheduling the GC.
    for (int i = 1; i < kDefaultThreadCount; ++i) {
        mutators[i]
                .Execute([allocateInHeap, i](mm::ThreadData& threadData, Mutator& mutator) { allocateInHeap(threadData, mutator, i); })
                .wait();
    }

    std_support::vector<std::future<void>> gcFutures(kDefaultThreadCount);
    gcFutures[0] = mutators[0].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().ScheduleAndWaitFullGC(); });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] = mutators[i].Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) {
            expandRootSet(threadData, mutator, i);
            threadData.gc().SafePointFunctionPrologue();
        });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    std_support::vector<ObjHeader*> expectedAlive;
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

TEST_F(SameThreadMarkAndSweepTest, CrossThreadReference) {
    std_support::vector<Mutator> mutators(kDefaultThreadCount);
    std_support::vector<ObjHeader*> globals(kDefaultThreadCount);
    std_support::vector<ObjHeader*> locals(kDefaultThreadCount);
    std_support::vector<ObjHeader*> reachables(2 * kDefaultThreadCount);

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

    std_support::vector<std::future<void>> gcFutures(kDefaultThreadCount);

    gcFutures[0] = mutators[0].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().ScheduleAndWaitFullGC(); });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] =
                mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().SafePointFunctionPrologue(); });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    std_support::vector<ObjHeader*> expectedAlive;
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

TEST_F(SameThreadMarkAndSweepTest, MultipleMutatorsWeaks) {
    std_support::vector<Mutator> mutators(kDefaultThreadCount);
    ObjHeader* globalRoot = nullptr;
    test_support::RegularWeakReferenceImpl* weak = nullptr;

    mutators[0]
            .Execute([&weak, &globalRoot](mm::ThreadData& threadData, Mutator& mutator) {
                auto& global = mutator.AddGlobalRoot();

                auto& object = AllocateObject(threadData);
                auto& objectWeak = ([&threadData, &object]() -> test_support::RegularWeakReferenceImpl& {
                    ObjHolder holder;
                    return InstallWeakReference(threadData, object.header(), holder.slot());
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

    std_support::vector<std::future<void>> gcFutures(kDefaultThreadCount);

    gcFutures[0] = mutators[0].Execute([weak](mm::ThreadData& threadData, Mutator& mutator) {
        threadData.gc().ScheduleAndWaitFullGC();
        EXPECT_THAT(weak->get(), nullptr);
    });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] = mutators[i].Execute([weak](mm::ThreadData& threadData, Mutator& mutator) {
            threadData.gc().SafePointFunctionPrologue();
            EXPECT_THAT(weak->get(), nullptr);
        });
    }

    for (auto& future : gcFutures) {
        future.wait();
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAre(globalRoot, weak->header()));
    }
}

TEST_F(SameThreadMarkAndSweepTest, NewThreadsWhileRequestingCollection) {
    std_support::vector<Mutator> mutators(kDefaultThreadCount);
    std_support::vector<ObjHeader*> globals(2 * kDefaultThreadCount);
    std_support::vector<ObjHeader*> locals(2 * kDefaultThreadCount);
    std_support::vector<ObjHeader*> reachables(2 * kDefaultThreadCount);
    std_support::vector<ObjHeader*> unreachables(2 * kDefaultThreadCount);

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

    std_support::vector<std::future<void>> gcFutures(kDefaultThreadCount);

    gcFutures[0] = mutators[0].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().ScheduleAndWaitFullGC(); });

    // Spin until thread suspension is requested.
    while (!mm::IsThreadSuspensionRequested()) {
    }

    // Now start attaching new threads.
    std_support::vector<Mutator> newMutators(kDefaultThreadCount);
    std_support::vector<std::future<void>> attachFutures(kDefaultThreadCount);

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        attachFutures[i] = newMutators[i].Execute([i, expandRootSet](mm::ThreadData& threadData, Mutator& mutator) { expandRootSet(threadData, mutator, i + kDefaultThreadCount); });
    }

    // All the other threads are stopping at safe points.
    for (int i = 1; i < kDefaultThreadCount; ++i) {
        gcFutures[i] =
                mutators[i].Execute([](mm::ThreadData& threadData, Mutator& mutator) { threadData.gc().SafePointFunctionPrologue(); });
    }

    // GC will be completed first
    for (auto& future : gcFutures) {
        future.wait();
    }

    // Only then will the new threads be allowed to attach.
    for (auto& future : attachFutures) {
        future.wait();
    }

    // Old mutators don't even see alive objects from the new threads yet (as the latter ones have not published anything).

    std_support::vector<ObjHeader*> expectedAlive;
    for (int i = 0; i < kDefaultThreadCount; ++i) {
        expectedAlive.push_back(globals[i]);
        expectedAlive.push_back(locals[i]);
        expectedAlive.push_back(reachables[i]);
    }

    for (auto& mutator : mutators) {
        EXPECT_THAT(mutator.Alive(), testing::UnorderedElementsAreArray(expectedAlive));
    }

    for (int i = 0; i < kDefaultThreadCount; ++i) {
        std_support::vector<ObjHeader*> aliveForThisThread(expectedAlive.begin(), expectedAlive.end());
        aliveForThisThread.push_back(globals[kDefaultThreadCount + i]);
        aliveForThisThread.push_back(locals[kDefaultThreadCount + i]);
        aliveForThisThread.push_back(reachables[kDefaultThreadCount + i]);
        // Unreachables for new threads were not collected.
        aliveForThisThread.push_back(unreachables[kDefaultThreadCount + i]);
        EXPECT_THAT(newMutators[i].Alive(), testing::UnorderedElementsAreArray(aliveForThisThread));
    }
}


TEST_F(SameThreadMarkAndSweepTest, FreeObjectWithFreeWeakReversedOrder) {
    std_support::vector<Mutator> mutators(2);
    std::atomic<test_support::Object<Payload>*> object1 = nullptr;
    std::atomic<test_support::RegularWeakReferenceImpl*> weak = nullptr;
    std::atomic<bool> done = false;
    auto f0 = mutators[0].Execute([&](mm::ThreadData& threadData, Mutator &) {
        GlobalObjectHolder global1{threadData};
        auto& object1_local = AllocateObject(threadData);
        object1 = &object1_local;
        global1->field1 = object1_local.header();
        while (weak.load() == nullptr)
            ;
        threadData.gc().ScheduleAndWaitFullGC();

        ASSERT_THAT(Alive(threadData), testing::UnorderedElementsAre(object1_local.header(), weak.load()->header(), global1.header()));
        ASSERT_THAT(IsMarked(global1.header()), false);
        ASSERT_THAT(IsMarked(object1_local.header()), false);
        ASSERT_THAT(IsMarked(weak.load()->header()), false);
        ASSERT_THAT(weak.load()->get(), object1_local.header());

        global1->field1 = nullptr;

        EXPECT_CALL(finalizerHook(), Call(weak.load()->header()));
        threadData.gc().ScheduleAndWaitFullGC();

        EXPECT_THAT(Alive(threadData), testing::UnorderedElementsAre(global1.header()));
        done = true;
    });
    
    auto f1 = mutators[1].Execute([&](mm::ThreadData& threadData, Mutator &) {
        while (object1.load() == nullptr) {}
        ObjHolder holder;
        auto& weak_local = InstallWeakReference(threadData, object1.load()->header(), holder.slot());
        weak = &weak_local;
        *holder.slot() = nullptr;
        while (!done) threadData.gc().SafePointLoopBody();
    });

    f0.wait();
    f1.wait();
}
