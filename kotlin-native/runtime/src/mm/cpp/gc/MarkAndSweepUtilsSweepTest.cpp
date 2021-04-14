/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <TestSupport.hpp>
#include "MarkAndSweepUtils.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "../ObjectFactory.hpp"
#include "FinalizerHooksTestSupport.hpp"
#include "ObjectTestSupport.hpp"

using namespace kotlin;

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

// TODO: This should go into test support for weak references.
struct WeakCounterPayload {
    void* referred;
    KInt lock;
    KInt cookie;

    static constexpr std::array<ObjHeader * WeakCounterPayload::*, 0> kFields{};
};

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
test_support::TypeInfoHolder typeHolderWithFinalizer{test_support::TypeInfoHolder::ObjectBuilder<Payload>().addFlag(TF_HAS_FINALIZER)};
test_support::TypeInfoHolder typeHolderWeakCounter{test_support::TypeInfoHolder::ObjectBuilder<WeakCounterPayload>()};

struct GC {
    struct ObjectData {
        enum class State {
            kUnmarked,
            kMarked,
            kMarkReset,
        };
        State state = State::kUnmarked;
    };

    struct ThreadData {
        void SafePointAllocation(size_t) {}
        void OnOOM(size_t) {}
    };
};

using ObjectFactory = mm::ObjectFactory<GC>;

class Object : public test_support::Object<Payload> {
public:
    // No way to directly create or destroy it.
    Object() = delete;
    ~Object() = delete;

    static Object& FromObjHeader(ObjHeader* obj) { return static_cast<Object&>(test_support::Object<Payload>::FromObjHeader(obj)); }

    void InstallExtraData() { mm::ExtraObjectData::Install(header()); }

    bool HasWeakCounter() {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(header())) {
            return extraObjectData->HasWeakReferenceCounter();
        }
        return false;
    }

    void Mark() { objectData().state = GC::ObjectData::State::kMarked; }

    GC::ObjectData::State state() { return objectData().state; }

private:
    GC::ObjectData& objectData() { return ObjectFactory::NodeRef::From(header()).GCObjectData(); }
};

class ObjectArray : public test_support::ObjectArray<3> {
public:
    // No way to directly create or destroy it.
    ObjectArray() = delete;
    ~ObjectArray() = delete;

    static ObjectArray& FromArrayHeader(ArrayHeader* array) {
        return static_cast<ObjectArray&>(test_support::ObjectArray<3>::FromArrayHeader(array));
    }

    void InstallExtraData() { mm::ExtraObjectData::Install(header()); }

    bool HasWeakCounter() {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(header())) {
            return extraObjectData->HasWeakReferenceCounter();
        }
        return false;
    }

    void Mark() { objectData().state = GC::ObjectData::State::kMarked; }

    GC::ObjectData::State state() { return objectData().state; }

private:
    GC::ObjectData& objectData() { return ObjectFactory::NodeRef::From(header()).GCObjectData(); }
};

class CharArray : public test_support::CharArray<3> {
public:
    // No way to directly create or destroy it.
    CharArray() = delete;
    ~CharArray() = delete;

    static CharArray& FromArrayHeader(ArrayHeader* array) {
        return static_cast<CharArray&>(test_support::CharArray<3>::FromArrayHeader(array));
    }

    void InstallExtraData() { mm::ExtraObjectData::Install(header()); }

    bool HasWeakCounter() {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(header())) {
            return extraObjectData->HasWeakReferenceCounter();
        }
        return false;
    }

    void Mark() { objectData().state = GC::ObjectData::State::kMarked; }

    GC::ObjectData::State state() { return objectData().state; }

private:
    GC::ObjectData& objectData() { return ObjectFactory::NodeRef::From(header()).GCObjectData(); }
};

using WeakCounter = test_support::Object<WeakCounterPayload>;

void MarkWeakCounter(WeakCounter& counter) {
    ObjectFactory::NodeRef::From(counter.header()).GCObjectData().state = GC::ObjectData::State::kMarked;
}

GC::ObjectData::State GetWeakCounterState(WeakCounter& counter) {
    return ObjectFactory::NodeRef::From(counter.header()).GCObjectData().state;
}

struct SweepTraits {
    using ObjectFactory = ObjectFactory;

    static bool TryResetMark(ObjectFactory::NodeRef node) {
        GC::ObjectData& objectData = node.GCObjectData();
        switch (objectData.state) {
            case GC::ObjectData::State::kUnmarked:
                return false;
            case GC::ObjectData::State::kMarked:
                objectData.state = GC::ObjectData::State::kMarkReset;
                return true;
            case GC::ObjectData::State::kMarkReset:
                RuntimeFail("Trying to reset mark twice.");
        }
    }
};

class MarkAndSweepUtilsSweepTest : public ::testing::Test {
public:
    ~MarkAndSweepUtilsSweepTest() override {
        for (auto& finalizerQueue : finalizers_) {
            finalizerQueue.Finalize();
        }
        testing::Mock::VerifyAndClear(&finalizerHook());
        // TODO: Figure out a better way to clear up the stuff.
        EXPECT_CALL(finalizerHook(), Call(testing::_)).Times(testing::AnyNumber());
        for (auto node : objectFactory_.Iter()) {
            auto* obj = node->IsArray() ? node->GetArrayHeader()->obj() : node->GetObjHeader();
            if (auto* extraObject = mm::ExtraObjectData::Get(obj)) {
                extraObject->ClearWeakReferenceCounter();
            }
            RunFinalizers(obj);
        }
    }

    KStdVector<ObjHeader*> Sweep() {
        auto finalizers = mm::Sweep<SweepTraits>(objectFactory_);
        KStdVector<ObjHeader*> objects;
        for (auto node : finalizers.IterForTests()) {
            objects.push_back(node.IsArray() ? node.GetArrayHeader()->obj() : node.GetObjHeader());
        }
        finalizers_.push_back(std::move(finalizers));
        return objects;
    }

    KStdVector<ObjHeader*> Alive() {
        KStdVector<ObjHeader*> objects;
        for (auto node : objectFactory_.Iter()) {
            objects.push_back(node.IsArray() ? node.GetArrayHeader()->obj() : node.GetObjHeader());
        }
        return objects;
    }

    Object& AllocateObject(const TypeInfo* typeInfo = typeHolder.typeInfo()) {
        auto* object = objectFactoryThreadQueue_.CreateObject(typeInfo);
        objectFactoryThreadQueue_.Publish();
        return Object::FromObjHeader(object);
    }

    ObjectArray& AllocateObjectArray() {
        auto* array = objectFactoryThreadQueue_.CreateArray(theArrayTypeInfo, 3);
        objectFactoryThreadQueue_.Publish();
        return ObjectArray::FromArrayHeader(array);
    }

    CharArray& AllocateCharArray() {
        auto* array = objectFactoryThreadQueue_.CreateArray(theCharArrayTypeInfo, 3);
        objectFactoryThreadQueue_.Publish();
        return CharArray::FromArrayHeader(array);
    }

    WeakCounter& InstallWeakCounter(ObjHeader* objHeader) {
        auto* weakCounterHeader = objectFactoryThreadQueue_.CreateObject(typeHolderWeakCounter.typeInfo());
        objectFactoryThreadQueue_.Publish();
        auto& weakCounter = WeakCounter::FromObjHeader(weakCounterHeader);
        auto& extraObjectData = mm::ExtraObjectData::GetOrInstall(objHeader);
        *extraObjectData.GetWeakCounterLocation() = weakCounter.header();
        weakCounter->referred = objHeader;
        return weakCounter;
    }

    testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHooks_.finalizerHook(); }

private:
    // TODO: Provide a common base class for all unit tests that require memory initializtion.
    kotlin::ScopedMemoryInit memoryInit;
    FinalizerHooksTestSupport finalizerHooks_;
    GC::ThreadData gcThreadData_;
    ObjectFactory objectFactory_;
    ObjectFactory::ThreadQueue objectFactoryThreadQueue_{objectFactory_, gcThreadData_};
    KStdVector<ObjectFactory::FinalizerQueue> finalizers_;
};

} // namespace

TEST_F(MarkAndSweepUtilsSweepTest, SweepEmpty) {
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre());

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObject) {
    auto& object = AllocateObject();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectArray) {
    auto& array = AllocateObjectArray();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleCharArray) {
    auto& array = AllocateCharArray();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObject) {
    auto& object = AllocateObject();
    object.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(object.state(), GC::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectArray) {
    auto& array = AllocateObjectArray();
    array.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(array.state(), GC::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedCharArray) {
    auto& array = AllocateCharArray();
    array.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(array.state(), GC::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectWithExtraData) {
    auto& object = AllocateObject();
    object.InstallExtraData();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(object.state(), GC::ObjectData::State::kUnmarked);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectArrayWithExtraData) {
    auto& array = AllocateObjectArray();
    array.InstallExtraData();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(array.state(), GC::ObjectData::State::kUnmarked);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleCharArrayWithExtraData) {
    auto& array = AllocateCharArray();
    array.InstallExtraData();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(array.state(), GC::ObjectData::State::kUnmarked);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectWithExtraData) {
    auto& object = AllocateObject();
    object.InstallExtraData();
    object.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(object.state(), GC::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectArrayWithExtraData) {
    auto& array = AllocateObjectArray();
    array.InstallExtraData();
    array.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(array.state(), GC::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedCharArrayWithExtraData) {
    auto& array = AllocateCharArray();
    array.InstallExtraData();
    array.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(array.state(), GC::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectWithFinalizerHook) {
    auto& object = AllocateObject(typeHolderWithFinalizer.typeInfo());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(object.state(), GC::ObjectData::State::kUnmarked);

    EXPECT_CALL(finalizerHook(), Call(object.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectWithFinalizerHook) {
    auto& object = AllocateObject(typeHolderWithFinalizer.typeInfo());
    object.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(object.state(), GC::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectWithWeakCounter) {
    auto& object = AllocateObject();
    auto& weakCounter = InstallWeakCounter(object.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header(), weakCounter.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(object.state(), GC::ObjectData::State::kUnmarked);
    EXPECT_FALSE(object.HasWeakCounter());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectArrayWithWeakCounter) {
    auto& array = AllocateObjectArray();
    auto& weakCounter = InstallWeakCounter(array.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakCounter.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(array.state(), GC::ObjectData::State::kUnmarked);
    EXPECT_FALSE(array.HasWeakCounter());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleCharArrayWithWeakCounter) {
    auto& array = AllocateCharArray();
    auto& weakCounter = InstallWeakCounter(array.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakCounter.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(array.state(), GC::ObjectData::State::kUnmarked);
    EXPECT_FALSE(array.HasWeakCounter());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectWithWeakCounter) {
    auto& object = AllocateObject();
    auto& weakCounter = InstallWeakCounter(object.header());
    object.Mark();
    MarkWeakCounter(weakCounter);
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header(), weakCounter.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object.header(), weakCounter.header()));
    EXPECT_THAT(object.state(), GC::ObjectData::State::kMarkReset);
    EXPECT_THAT(GetWeakCounterState(weakCounter), GC::ObjectData::State::kMarkReset);
    EXPECT_TRUE(object.HasWeakCounter());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectArrayWithWeakCounter) {
    auto& array = AllocateObjectArray();
    auto& weakCounter = InstallWeakCounter(array.header());
    array.Mark();
    MarkWeakCounter(weakCounter);
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakCounter.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakCounter.header()));
    EXPECT_THAT(array.state(), GC::ObjectData::State::kMarkReset);
    EXPECT_THAT(GetWeakCounterState(weakCounter), GC::ObjectData::State::kMarkReset);
    EXPECT_TRUE(array.HasWeakCounter());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedCharArrayWithWeakCounter) {
    auto& array = AllocateCharArray();
    auto& weakCounter = InstallWeakCounter(array.header());
    array.Mark();
    MarkWeakCounter(weakCounter);
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakCounter.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakCounter.header()));
    EXPECT_THAT(array.state(), GC::ObjectData::State::kMarkReset);
    EXPECT_THAT(GetWeakCounterState(weakCounter), GC::ObjectData::State::kMarkReset);
    EXPECT_TRUE(array.HasWeakCounter());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepObjects) {
    auto& object1 = AllocateObject();
    auto& object2 = AllocateObject(typeHolderWithFinalizer.typeInfo());
    auto& object3 = AllocateObject();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object1.header(), object2.header(), object3.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(object2.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());

    EXPECT_CALL(finalizerHook(), Call(object2.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepObjectsMarkAll) {
    auto& object1 = AllocateObject();
    object1.Mark();
    auto& object2 = AllocateObject(typeHolderWithFinalizer.typeInfo());
    object2.Mark();
    auto& object3 = AllocateObject();
    object3.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object1.header(), object2.header(), object3.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object1.header(), object2.header(), object3.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepObjectsMarkFirst) {
    auto& object1 = AllocateObject();
    object1.Mark();
    auto& object2 = AllocateObject(typeHolderWithFinalizer.typeInfo());
    auto& object3 = AllocateObject();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object1.header(), object2.header(), object3.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(object2.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object1.header()));

    EXPECT_CALL(finalizerHook(), Call(object2.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepObjectsMarkSecond) {
    auto& object1 = AllocateObject();
    auto& object2 = AllocateObject(typeHolderWithFinalizer.typeInfo());
    object2.Mark();
    auto& object3 = AllocateObject();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object1.header(), object2.header(), object3.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object2.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepObjectsMarkThird) {
    auto& object1 = AllocateObject();
    auto& object2 = AllocateObject(typeHolderWithFinalizer.typeInfo());
    auto& object3 = AllocateObject();
    object3.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object1.header(), object2.header(), object3.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(object2.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object3.header()));

    EXPECT_CALL(finalizerHook(), Call(object2.header()));
}
