/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MarkAndSweepUtils.hpp"

#include <functional>
#include <TestSupport.hpp>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "FinalizerHooks.hpp"
#include "ObjectTestSupport.hpp"
#include "Utils.hpp"

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

// TODO: This base might belong in `test_support`
class BaseObject {
public:
    enum class Kind {
        kPermanent,
        kHeapLike // Treated as heap object for the purposes of the test.
    };

    virtual ObjHeader* GetObjHeader() = 0;

    void InstallExtraData() { mm::ExtraObjectData::Install(GetObjHeader()); }

    void InstallWeakCounter(BaseObject& counter) {
        auto& extraObjectData = mm::ExtraObjectData::GetOrInstall(GetObjHeader());
        *extraObjectData.GetWeakCounterLocation() = counter.GetObjHeader();
    }

protected:
    void SetKind(Kind kind) {
        switch (kind) {
            case Kind::kPermanent:
                GetObjHeader()->typeInfoOrMeta_ = setPointerBits(GetObjHeader()->typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
                RuntimeAssert(GetObjHeader()->permanent(), "Must be permanent");
                break;
            case Kind::kHeapLike:
                RuntimeAssert(GetObjHeader()->heap(), "Must be heap");
                break;
        }
    }

    void Finalize() {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(GetObjHeader())) {
            extraObjectData->ClearWeakReferenceCounter();
        }
        RunFinalizers(GetObjHeader());
    }
};

class Object : public BaseObject, public test_support::Object<Payload> {
public:
    explicit Object(Kind kind = Kind::kHeapLike) : test_support::Object<Payload>(typeHolder.typeInfo()) { SetKind(kind); }

    ~Object() { Finalize(); }

    ObjHeader* GetObjHeader() override { return header(); }
};

class ObjectArray : public BaseObject, public test_support::ObjectArray<3> {
public:
    explicit ObjectArray(Kind kind = Kind::kHeapLike) : test_support::ObjectArray<3>() { SetKind(kind); }

    ~ObjectArray() { Finalize(); }

    ObjHeader* GetObjHeader() override { return header(); }
};

class CharArray : public BaseObject, public test_support::CharArray<3> {
public:
    explicit CharArray(Kind kind = Kind::kHeapLike) : test_support::CharArray<3>() { SetKind(kind); }

    ~CharArray() { Finalize(); }

    ObjHeader* GetObjHeader() override { return header(); }
};

class ScopedMarkTraits : private Pinned {
public:
    ScopedMarkTraits() {
        RuntimeAssert(instance_ == nullptr, "Only one ScopedMarkTraits is allowed");
        instance_ = this;
    }

    ~ScopedMarkTraits() {
        RuntimeAssert(instance_ == this, "ScopedMarkTraits instance broke");
        instance_ = nullptr;
    }

    const KStdUnorderedSet<ObjHeader*>& marked() const { return marked_; }

    static bool TryMark(ObjHeader* object) noexcept { return instance_->marked_.insert(object).second; }
    static bool IsMarked(ObjHeader* object) noexcept { return instance_->marked_.find(object) != instance_->marked_.end(); }

private:
    static ScopedMarkTraits* instance_;

    KStdUnorderedSet<ObjHeader*> marked_;
};

// static
ScopedMarkTraits* ScopedMarkTraits::instance_ = nullptr;

class MarkAndSweepUtilsMarkTest : public ::testing::Test {
public:
    const KStdUnorderedSet<ObjHeader*>& marked() const { return markTraits_.marked(); }

    auto MarkedMatcher(std::initializer_list<std::reference_wrapper<BaseObject>> expected) {
        KStdVector<ObjHeader*> objects;
        for (auto& object : expected) {
            objects.push_back(object.get().GetObjHeader());
        }
        return testing::UnorderedElementsAreArray(objects);
    }

    void Mark(std::initializer_list<std::reference_wrapper<BaseObject>> graySet) {
        KStdVector<ObjHeader*> objects;
        for (auto& object : graySet) objects.push_back(object.get().GetObjHeader());
        mm::Mark<ScopedMarkTraits>(std::move(objects));
    }

private:
    kotlin::ScopedMemoryInit memoryInit;
    ScopedMarkTraits markTraits_;
};

#define EXPECT_MARKED(...) EXPECT_THAT(marked(), MarkedMatcher({__VA_ARGS__}))

} // namespace

TEST_F(MarkAndSweepUtilsMarkTest, MarkNothing) {
    Mark({});

    EXPECT_MARKED();
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObject) {
    Object object;

    Mark({object});

    EXPECT_MARKED(object);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArray) {
    ObjectArray array;

    Mark({array});

    EXPECT_MARKED(array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArray) {
    CharArray array;

    Mark({array});

    EXPECT_MARKED(array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSinglePermanentObject) {
    Object object{BaseObject::Kind::kPermanent};

    Mark({object});

    EXPECT_MARKED();
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSinglePermanentObjectArray) {
    ObjectArray array{BaseObject::Kind::kPermanent};

    Mark({array});

    EXPECT_MARKED();
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSinglePermanentCharArray) {
    CharArray array{BaseObject::Kind::kPermanent};

    Mark({array});

    EXPECT_MARKED();
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithInvalidFields) {
    Object object;
    object->field1 = kInitializingSingleton;

    Mark({object});

    EXPECT_MARKED(object);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithInvalidFields) {
    ObjectArray array;
    array.elements()[0] = kInitializingSingleton;

    Mark({array});

    EXPECT_MARKED(array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithSomeData) {
    CharArray array;
    array.elements()[0] = 'a';
    array.elements()[1] = 'b';
    array.elements()[2] = 'c';

    Mark({array});

    EXPECT_MARKED(array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithExtraData) {
    Object object;
    object.InstallExtraData();

    Mark({object});

    EXPECT_MARKED(object);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithExtraData) {
    ObjectArray array;
    array.InstallExtraData();

    Mark({array});

    EXPECT_MARKED(array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithExtraData) {
    CharArray array;
    array.InstallExtraData();

    Mark({array});

    EXPECT_MARKED(array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithWeakCounter) {
    Object weakCounter;
    Object object;
    object.InstallWeakCounter(weakCounter);

    Mark({object});

    EXPECT_MARKED(object, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithWeakCounter) {
    Object weakCounter;
    ObjectArray array;
    array.InstallWeakCounter(weakCounter);

    Mark({array});

    EXPECT_MARKED(array, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithWeakCounter) {
    Object weakCounter;
    CharArray array;
    array.InstallWeakCounter(weakCounter);

    Mark({array});

    EXPECT_MARKED(array, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithInvalidFieldsWithWeakCounter) {
    Object weakCounter;
    Object object;
    object->field1 = kInitializingSingleton;
    object.InstallWeakCounter(weakCounter);

    Mark({object});

    EXPECT_MARKED(object, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithInvalidFieldsWithWeakCounter) {
    Object weakCounter;
    ObjectArray array;
    array.elements()[0] = kInitializingSingleton;
    array.InstallWeakCounter(weakCounter);

    Mark({array});

    EXPECT_MARKED(array, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithSomeDataWithWeakCounter) {
    Object weakCounter;
    CharArray array;
    array.elements()[0] = 'a';
    array.elements()[1] = 'b';
    array.elements()[2] = 'c';
    array.InstallWeakCounter(weakCounter);

    Mark({array});

    EXPECT_MARKED(array, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkTree) {
    Object root;
    Object root_field1;
    Object root_field1_field1;
    Object root_field1_field2;
    ObjectArray root_field3;
    Object root_field3_element1;
    ObjectArray root_field3_element2;
    CharArray root_field3_element3;
    root->field1 = root_field1.header();
    root_field1->field1 = root_field1_field1.header();
    root_field1->field2 = root_field1_field2.header();
    root->field3 = root_field3.header();
    root_field3.elements()[0] = root_field3_element1.header();
    root_field3.elements()[1] = root_field3_element2.header();
    root_field3.elements()[2] = root_field3_element3.header();

    Mark({root});

    EXPECT_MARKED(
            root, root_field1, root_field1_field1, root_field1_field2, root_field3, root_field3_element1, root_field3_element2,
            root_field3_element3);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkTreeWithPermanentRoot) {
    Object root{BaseObject::Kind::kPermanent};
    Object root_field1{BaseObject::Kind::kPermanent};
    Object root_field1_field1{BaseObject::Kind::kPermanent};
    Object root_field1_field2{BaseObject::Kind::kPermanent};
    ObjectArray root_field3{BaseObject::Kind::kPermanent};
    Object root_field3_element1{BaseObject::Kind::kPermanent};
    ObjectArray root_field3_element2{BaseObject::Kind::kPermanent};
    CharArray root_field3_element3{BaseObject::Kind::kPermanent};
    root->field1 = root_field1.header();
    root_field1->field1 = root_field1_field1.header();
    root_field1->field2 = root_field1_field2.header();
    root->field3 = root_field3.header();
    root_field3.elements()[0] = root_field3_element1.header();
    root_field3.elements()[1] = root_field3_element2.header();
    root_field3.elements()[2] = root_field3_element3.header();

    Mark({root});

    EXPECT_MARKED();
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkTreeWithPermanentMiddle) {
    Object root;
    Object root_field1{BaseObject::Kind::kPermanent};
    Object root_field1_field1{BaseObject::Kind::kPermanent};
    Object root_field1_field2{BaseObject::Kind::kPermanent};
    ObjectArray root_field3;
    Object root_field3_element1;
    ObjectArray root_field3_element2;
    CharArray root_field3_element3;
    root->field1 = root_field1.header();
    root_field1->field1 = root_field1_field1.header();
    root_field1->field2 = root_field1_field2.header();
    root->field3 = root_field3.header();
    root_field3.elements()[0] = root_field3_element1.header();
    root_field3.elements()[1] = root_field3_element2.header();
    root_field3.elements()[2] = root_field3_element3.header();

    Mark({root});

    EXPECT_MARKED(root, root_field3, root_field3_element1, root_field3_element2, root_field3_element3);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkTreeWithPermanentLeaf) {
    Object root;
    Object root_field1;
    Object root_field1_field1{BaseObject::Kind::kPermanent};
    Object root_field1_field2;
    ObjectArray root_field3;
    Object root_field3_element1;
    ObjectArray root_field3_element2;
    CharArray root_field3_element3;
    root->field1 = root_field1.header();
    root_field1->field1 = root_field1_field1.header();
    root_field1->field2 = root_field1_field2.header();
    root->field3 = root_field3.header();
    root_field3.elements()[0] = root_field3_element1.header();
    root_field3.elements()[1] = root_field3_element2.header();
    root_field3.elements()[2] = root_field3_element3.header();

    Mark({root});

    EXPECT_MARKED(root, root_field1, root_field1_field2, root_field3, root_field3_element1, root_field3_element2, root_field3_element3);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkRecursiveTree) {
    Object root;
    Object inner1;
    ObjectArray inner2;
    root->field1 = inner1.header();
    inner1->field1 = inner2.header();
    inner2.elements()[0] = root.header();

    Mark({root});

    EXPECT_MARKED(root, inner1, inner2);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkRecursiveTreeWithPermanentRoot) {
    Object root{BaseObject::Kind::kPermanent};
    Object inner1{BaseObject::Kind::kPermanent};
    ObjectArray inner2{BaseObject::Kind::kPermanent};
    root->field1 = inner1.header();
    inner1->field1 = inner2.header();
    inner2.elements()[0] = root.header();

    Mark({root});

    EXPECT_MARKED();
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkForest) {
    Object root1;
    ObjectArray root2;
    Object root3;

    Mark({root1, root2, root3});

    EXPECT_MARKED(root1, root2, root3);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkForestWithPermanentFirst) {
    Object root1{BaseObject::Kind::kPermanent};
    ObjectArray root2;
    Object root3;

    Mark({root1, root2, root3});

    EXPECT_MARKED(root2, root3);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkForestWithPermanentSecond) {
    Object root1;
    ObjectArray root2{BaseObject::Kind::kPermanent};
    Object root3;

    Mark({root1, root2, root3});

    EXPECT_MARKED(root1, root3);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkForestWithPermanentThird) {
    Object root1;
    ObjectArray root2;
    Object root3{BaseObject::Kind::kPermanent};

    Mark({root1, root2, root3});

    EXPECT_MARKED(root1, root2);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkForestWithInterconnectedRoots) {
    Object root1;
    ObjectArray root2;
    Object root3;

    root1->field1 = root2.header();
    root2.elements()[0] = root3.header();
    root3->field1 = root1.header();

    Mark({root1, root2, root3});

    EXPECT_MARKED(root1, root2, root3);
}
