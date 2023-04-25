/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <TestSupport.hpp>
#include "MarkAndSweepUtils.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Allocator.hpp"
#include "FinalizerHooksTestSupport.hpp"
#include "ObjectFactory.hpp"
#include "ObjectTestSupport.hpp"
#include "ExtraObjectDataFactory.hpp"
#include "WeakRef.hpp"

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

test_support::TypeInfoHolder typeHolder{test_support::TypeInfoHolder::ObjectBuilder<Payload>()};
test_support::TypeInfoHolder typeHolderWithFinalizer{test_support::TypeInfoHolder::ObjectBuilder<Payload>().addFlag(TF_HAS_FINALIZER)};

struct ObjectFactoryTraits {
    struct ObjectData {
        enum class State {
            kUnmarked,
            kMarked,
            kMarkReset,
        };
        State state = State::kUnmarked;
    };

    using Allocator = gc::Allocator;
};

using ObjectFactory = mm::ObjectFactory<ObjectFactoryTraits>;
using ExtraObjectsDataFactory = mm::ExtraObjectDataFactory;

class Object : public test_support::Object<Payload> {
public:
    // No way to directly create or destroy it.
    Object() = delete;
    ~Object() = delete;

    static Object& FromObjHeader(ObjHeader* obj) { return static_cast<Object&>(test_support::Object<Payload>::FromObjHeader(obj)); }

    bool HasWeakReference() {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(header())) {
            return extraObjectData->HasRegularWeakReferenceImpl();
        }
        return false;
    }

    void Mark() { objectData().state = ObjectFactoryTraits::ObjectData::State::kMarked; }

    ObjectFactoryTraits::ObjectData::State state() { return objectData().state; }

private:
    ObjectFactoryTraits::ObjectData& objectData() { return ObjectFactory::NodeRef::From(header()).ObjectData(); }
};

class ObjectArray : public test_support::ObjectArray<3> {
public:
    // No way to directly create or destroy it.
    ObjectArray() = delete;
    ~ObjectArray() = delete;

    static ObjectArray& FromArrayHeader(ArrayHeader* array) {
        return static_cast<ObjectArray&>(test_support::ObjectArray<3>::FromArrayHeader(array));
    }

    bool HasWeakReference() {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(header())) {
            return extraObjectData->HasRegularWeakReferenceImpl();
        }
        return false;
    }

    void Mark() { objectData().state = ObjectFactoryTraits::ObjectData::State::kMarked; }

    ObjectFactoryTraits::ObjectData::State state() { return objectData().state; }

private:
    ObjectFactoryTraits::ObjectData& objectData() { return ObjectFactory::NodeRef::From(header()).ObjectData(); }
};

class CharArray : public test_support::CharArray<3> {
public:
    // No way to directly create or destroy it.
    CharArray() = delete;
    ~CharArray() = delete;

    static CharArray& FromArrayHeader(ArrayHeader* array) {
        return static_cast<CharArray&>(test_support::CharArray<3>::FromArrayHeader(array));
    }

    bool HasWeakReference() {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(header())) {
            return extraObjectData->HasRegularWeakReferenceImpl();
        }
        return false;
    }

    void Mark() { objectData().state = ObjectFactoryTraits::ObjectData::State::kMarked; }

    ObjectFactoryTraits::ObjectData::State state() { return objectData().state; }

private:
    ObjectFactoryTraits::ObjectData& objectData() { return ObjectFactory::NodeRef::From(header()).ObjectData(); }
};

void MarkWeakReference(test_support::RegularWeakReferenceImpl& weakRef) {
    ObjectFactory::NodeRef::From(weakRef.header()).ObjectData().state = ObjectFactoryTraits::ObjectData::State::kMarked;
}

ObjectFactoryTraits::ObjectData::State GetWeakReferenceState(test_support::RegularWeakReferenceImpl& weakRef) {
    return ObjectFactory::NodeRef::From(weakRef.header()).ObjectData().state;
}

struct SweepTraits {
    using ObjectFactory = ObjectFactory;
    using ExtraObjectsFactory = mm::ExtraObjectDataFactory;

    static bool IsMarkedByExtraObject(mm::ExtraObjectData &object) noexcept {
        auto& objectData = ObjectFactory::NodeRef::From(object.GetBaseObject()).ObjectData();
        return objectData.state != ObjectFactoryTraits::ObjectData::State::kUnmarked;
    }

    static bool TryResetMark(ObjectFactory::NodeRef node) {
        ObjectFactoryTraits::ObjectData& objectData = node.ObjectData();
        switch (objectData.state) {
            case ObjectFactoryTraits::ObjectData::State::kUnmarked:
                return false;
            case ObjectFactoryTraits::ObjectData::State::kMarked:
                objectData.state = ObjectFactoryTraits::ObjectData::State::kMarkReset;
                return true;
            case ObjectFactoryTraits::ObjectData::State::kMarkReset:
                RuntimeFail("Trying to reset mark twice.");
        }
    }
};

struct ProcessWeakTraits {
    static bool IsMarked(ObjHeader* object) noexcept {
        auto& objectData = ObjectFactory::NodeRef::From(object).ObjectData();
        return objectData.state != ObjectFactoryTraits::ObjectData::State::kUnmarked;
    }
};

class MarkAndSweepUtilsSweepTest : public ::testing::Test {
public:
    ~MarkAndSweepUtilsSweepTest() override {
        auto deallocExtraObject = [this](ObjHeader* obj) {
            auto *extraObject = mm::ExtraObjectData::Get(obj);
            extraObject->Uninstall();
            extraObjectFactory_.DestroyExtraObjectData(extraObjectFactoryThreadQueue_, *extraObject);
            extraObjectFactoryThreadQueue_.Publish();
        };
        for (auto& finalizerQueue : finalizers_) {
            for (auto node : finalizerQueue.IterForTests()) {
                auto *object = node->GetObjHeader();
                if (object->has_meta_object()) {
                    deallocExtraObject(object);
                }
            }
            finalizerQueue.Finalize();
        }
        testing::Mock::VerifyAndClear(&finalizerHook());
        // TODO: Figure out a better way to clear up the stuff.
        EXPECT_CALL(finalizerHook(), Call(testing::_)).Times(testing::AnyNumber());
        for (auto node : objectFactory_.LockForIter()) {
            auto* obj = node->GetObjHeader();
            if (auto* extraObject = mm::ExtraObjectData::Get(obj)) {
                extraObject->ClearRegularWeakReferenceImpl();
                deallocExtraObject(obj);
            }
            RunFinalizers(obj);
        }
    }

    std_support::vector<ObjHeader*> Sweep() {
        gc::processWeaks<ProcessWeakTraits>(gc::GCHandle::getByEpoch(0), specialRefRegistry_);
        gc::SweepExtraObjects<SweepTraits>(gc::GCHandle::getByEpoch(0), extraObjectFactory_);
        auto finalizers = gc::Sweep<SweepTraits>(gc::GCHandle::getByEpoch(0), objectFactory_);
        std_support::vector<ObjHeader*> objects;
        for (auto node : finalizers.IterForTests()) {
            objects.push_back(node.GetObjHeader());
        }
        finalizers_.push_back(std::move(finalizers));
        return objects;
    }

    std_support::vector<ObjHeader*> Alive() {
        std_support::vector<ObjHeader*> objects;
        for (auto node : objectFactory_.LockForIter()) {
            objects.push_back(node.GetObjHeader());
        }
        return objects;
    }

    std_support::vector<mm::ExtraObjectData*> AliveExtraObjects() {
        std_support::vector<mm::ExtraObjectData*> objects;
        for (auto &node : extraObjectFactory_.LockForIter()) {
            objects.push_back(&node);
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

    mm::ExtraObjectData& InstallExtraData(ObjHeader *objHeader) {
        auto& extraObjectData = extraObjectFactory_.CreateExtraObjectDataForObject(extraObjectFactoryThreadQueue_, objHeader, objHeader->type_info());
        extraObjectFactoryThreadQueue_.Publish();
        objHeader->typeInfoOrMeta_ = reinterpret_cast<TypeInfo*>(&extraObjectData);
        return *mm::ExtraObjectData::Get(objHeader);
    }

    test_support::RegularWeakReferenceImpl& InstallWeakReference(ObjHeader* objHeader) {
        auto* weakReferenceHeader = objectFactoryThreadQueue_.CreateObject(theRegularWeakReferenceImplTypeInfo);
        objectFactoryThreadQueue_.Publish();
        auto& weakReference = test_support::RegularWeakReferenceImpl::FromObjHeader(weakReferenceHeader);
        auto& extraObjectData = InstallExtraData(objHeader);
        auto* setHeader = extraObjectData.GetOrSetRegularWeakReferenceImpl(objHeader, weakReference.header());
        EXPECT_EQ(setHeader, weakReference.header());
        weakReference->weakRef = static_cast<mm::RawSpecialRef*>(specialRefRegistryThreadQueue_.createWeakRef(objHeader));
        weakReference->referred = objHeader;
        specialRefRegistryThreadQueue_.publish();
        return weakReference;
    }

    testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHooks_.finalizerHook(); }

private:
    // TODO: Provide a common base class for all unit tests that require memory initializtion.
    kotlin::ScopedMemoryInit memoryInit;
    FinalizerHooksTestSupport finalizerHooks_;
    ObjectFactory objectFactory_;
    ObjectFactory::ThreadQueue objectFactoryThreadQueue_{objectFactory_, gc::Allocator()};
    ExtraObjectsDataFactory extraObjectFactory_;
    ExtraObjectsDataFactory::ThreadQueue extraObjectFactoryThreadQueue_{extraObjectFactory_};
    mm::SpecialRefRegistry specialRefRegistry_;
    mm::SpecialRefRegistry::ThreadQueue specialRefRegistryThreadQueue_{specialRefRegistry_};

    std_support::vector<ObjectFactory::FinalizerQueue> finalizers_;
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
    EXPECT_THAT(object.state(), ObjectFactoryTraits::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectArray) {
    auto& array = AllocateObjectArray();
    array.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(array.state(), ObjectFactoryTraits::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedCharArray) {
    auto& array = AllocateCharArray();
    array.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(array.state(), ObjectFactoryTraits::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectWithExtraData) {
    auto& object = AllocateObject();
    InstallExtraData(object.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(AliveExtraObjects(), testing::UnorderedElementsAre());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectArrayWithExtraData) {
    auto& array = AllocateObjectArray();
    InstallExtraData(array.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(AliveExtraObjects(), testing::UnorderedElementsAre());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleCharArrayWithExtraData) {
    auto& array = AllocateCharArray();
    InstallExtraData(array.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(AliveExtraObjects(), testing::UnorderedElementsAre());
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectWithExtraData) {
    auto& object = AllocateObject();
    auto& extra = InstallExtraData(object.header());
    object.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(AliveExtraObjects(), testing::UnorderedElementsAre(&extra));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectArrayWithExtraData) {
    auto& array = AllocateObjectArray();
    auto& extra = InstallExtraData(array.header());
    array.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(AliveExtraObjects(), testing::UnorderedElementsAre(&extra));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedCharArrayWithExtraData) {
    auto& array = AllocateCharArray();
    auto& extra = InstallExtraData(array.header());
    array.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header()));
    EXPECT_THAT(AliveExtraObjects(), testing::UnorderedElementsAre(&extra));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectWithFinalizerHook) {
    auto& object = AllocateObject(typeHolderWithFinalizer.typeInfo());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());
    EXPECT_THAT(object.state(), ObjectFactoryTraits::ObjectData::State::kUnmarked);

    EXPECT_CALL(finalizerHook(), Call(object.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectWithFinalizerHook) {
    auto& object = AllocateObject(typeHolderWithFinalizer.typeInfo());
    object.Mark();
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object.header()));
    EXPECT_THAT(object.state(), ObjectFactoryTraits::ObjectData::State::kMarkReset);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectWithWeakReference) {
    auto& object = AllocateObject();
    auto& weakReference = InstallWeakReference(object.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header(), weakReference.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(weakReference.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());

    EXPECT_CALL(finalizerHook(), Call(weakReference.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleObjectArrayWithWeakReference) {
    auto& array = AllocateObjectArray();
    auto& weakReference = InstallWeakReference(array.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakReference.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(weakReference.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());

    EXPECT_CALL(finalizerHook(), Call(weakReference.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleCharArrayWithWeakReference) {
    auto& array = AllocateCharArray();
    auto& weakReference = InstallWeakReference(array.header());
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakReference.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre(weakReference.header()));
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre());

    EXPECT_CALL(finalizerHook(), Call(weakReference.header()));
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectWithWeakReference) {
    auto& object = AllocateObject();
    auto& weakReference = InstallWeakReference(object.header());
    object.Mark();
    MarkWeakReference(weakReference);
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(object.header(), weakReference.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(object.header(), weakReference.header()));
    EXPECT_THAT(object.state(), ObjectFactoryTraits::ObjectData::State::kMarkReset);
    EXPECT_THAT(GetWeakReferenceState(weakReference), ObjectFactoryTraits::ObjectData::State::kMarkReset);
    EXPECT_TRUE(object.HasWeakReference());
    EXPECT_NE(weakReference.get(), nullptr);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedObjectArrayWithWeakReference) {
    auto& array = AllocateObjectArray();
    auto& weakReference = InstallWeakReference(array.header());
    array.Mark();
    MarkWeakReference(weakReference);
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakReference.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakReference.header()));
    EXPECT_THAT(array.state(), ObjectFactoryTraits::ObjectData::State::kMarkReset);
    EXPECT_THAT(GetWeakReferenceState(weakReference), ObjectFactoryTraits::ObjectData::State::kMarkReset);
    EXPECT_TRUE(array.HasWeakReference());
    EXPECT_NE(weakReference.get(), nullptr);
}

TEST_F(MarkAndSweepUtilsSweepTest, SweepSingleMarkedCharArrayWithWeakReference) {
    auto& array = AllocateCharArray();
    auto& weakReference = InstallWeakReference(array.header());
    array.Mark();
    MarkWeakReference(weakReference);
    ASSERT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakReference.header()));

    auto finalizers = Sweep();

    EXPECT_THAT(finalizers, testing::UnorderedElementsAre());
    EXPECT_THAT(Alive(), testing::UnorderedElementsAre(array.header(), weakReference.header()));
    EXPECT_THAT(array.state(), ObjectFactoryTraits::ObjectData::State::kMarkReset);
    EXPECT_THAT(GetWeakReferenceState(weakReference), ObjectFactoryTraits::ObjectData::State::kMarkReset);
    EXPECT_TRUE(array.HasWeakReference());
    EXPECT_NE(weakReference.get(), nullptr);
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
