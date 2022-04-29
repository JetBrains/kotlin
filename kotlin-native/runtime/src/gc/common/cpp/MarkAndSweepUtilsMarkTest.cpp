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
#include "std_support/UnorderedSet.hpp"
#include "std_support/Vector.hpp"

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
        kStackLocal,
        kHeapLike // Treated as heap object for the purposes of the test.
    };

    virtual ObjHeader* GetObjHeader() = 0;

    void InstallExtraData() { mm::ExtraObjectData::Install(GetObjHeader()); }

    void InstallWeakCounter(BaseObject& counter) {
        auto& extraObjectData = mm::ExtraObjectData::GetOrInstall(GetObjHeader());
        auto *setCounter = extraObjectData.GetOrSetWeakReferenceCounter(GetObjHeader(), counter.GetObjHeader());
        EXPECT_EQ(setCounter, counter.GetObjHeader());
        EXPECT_EQ(extraObjectData.GetBaseObject(), GetObjHeader());
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
            case Kind::kStackLocal:
                GetObjHeader()->typeInfoOrMeta_ = setPointerBits(GetObjHeader()->typeInfoOrMeta_,
                                                                 OBJECT_TAG_PERMANENT_CONTAINER | OBJECT_TAG_NONTRIVIAL_CONTAINER);
                RuntimeAssert(GetObjHeader()->local(), "Must be stack local");
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
    using MarkQueue = std_support::vector<ObjHeader*>;

    ScopedMarkTraits() {
        RuntimeAssert(instance_ == nullptr, "Only one ScopedMarkTraits is allowed");
        instance_ = this;
    }

    ~ScopedMarkTraits() {
        RuntimeAssert(instance_ == this, "ScopedMarkTraits instance broke");
        instance_ = nullptr;
    }

    const std_support::unordered_set<ObjHeader*>& marked() const { return marked_; }

    static bool isEmpty(const MarkQueue& queue) noexcept {
        return queue.empty();
    }

    static void clear(MarkQueue& queue) noexcept {
        queue.clear();
    }

    static ObjHeader* dequeue(MarkQueue& queue) noexcept {
        auto top = queue.back();
        queue.pop_back();
        return top;
    }

    static void enqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        auto result = instance_->marked_.insert(object);
        if (result.second) {
            queue.push_back(object);
        }
    }

private:
    static ScopedMarkTraits* instance_;

    std_support::unordered_set<ObjHeader*> marked_;
};

// static
ScopedMarkTraits* ScopedMarkTraits::instance_ = nullptr;

class MarkAndSweepUtilsMarkTest : public ::testing::Test {
public:
    const std_support::unordered_set<ObjHeader*>& marked() const { return markTraits_.marked(); }

    auto MarkedMatcher(std::initializer_list<std::reference_wrapper<BaseObject>> expected) {
        std_support::vector<ObjHeader*> objects;
        for (auto& object : expected) {
            objects.push_back(object.get().GetObjHeader());
        }
        return testing::UnorderedElementsAreArray(objects);
    }

    gc::MarkStats Mark(std::initializer_list<std::reference_wrapper<BaseObject>> graySet) {
        std_support::vector<ObjHeader*> objects;
        for (auto& object : graySet) ScopedMarkTraits::enqueue(objects, object.get().GetObjHeader());
        return gc::Mark<ScopedMarkTraits>(objects);
    }

private:
    kotlin::ScopedMemoryInit memoryInit;
    ScopedMarkTraits markTraits_;
};

size_t GetObjectsSize(std::initializer_list<std::reference_wrapper<BaseObject>> objects) {
    size_t size = 0;
    for (auto& object : objects) {
        size += mm::GetAllocatedHeapSize(object.get().GetObjHeader());
    }
    return size;
}

#define EXPECT_MARKED(stats, ...) \
    do { \
        std::initializer_list<std::reference_wrapper<BaseObject>> objects = {__VA_ARGS__}; \
        EXPECT_THAT(stats.aliveHeapSet, objects.size()); \
        EXPECT_THAT(stats.aliveHeapSetBytes, GetObjectsSize(objects)); \
        EXPECT_THAT(marked(), MarkedMatcher(objects)); \
    } while (false)

} // namespace

TEST_F(MarkAndSweepUtilsMarkTest, MarkNothing) {
    auto stats = Mark({});

    EXPECT_MARKED(stats);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObject) {
    Object object;

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArray) {
    ObjectArray array;

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArray) {
    CharArray array;

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithInvalidFields) {
    Object object;
    object->field1 = kInitializingSingleton;

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithInvalidFields) {
    ObjectArray array;
    array.elements()[0] = kInitializingSingleton;

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithSomeData) {
    CharArray array;
    array.elements()[0] = 'a';
    array.elements()[1] = 'b';
    array.elements()[2] = 'c';

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithExtraData) {
    Object object;
    object.InstallExtraData();

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithExtraData) {
    ObjectArray array;
    array.InstallExtraData();

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithExtraData) {
    CharArray array;
    array.InstallExtraData();

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithWeakCounter) {
    Object weakCounter;
    Object object;
    weakCounter->field1 = object.header();
    object.InstallWeakCounter(weakCounter);

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithWeakCounter) {
    Object weakCounter;
    ObjectArray array;
    weakCounter->field1 = array.header();
    array.InstallWeakCounter(weakCounter);

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithWeakCounter) {
    Object weakCounter;
    CharArray array;
    weakCounter->field1 = array.header();
    array.InstallWeakCounter(weakCounter);

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithInvalidFieldsWithWeakCounter) {
    Object weakCounter;
    Object object;
    object->field1 = kInitializingSingleton;
    weakCounter->field1 = object.header();
    object.InstallWeakCounter(weakCounter);

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithInvalidFieldsWithWeakCounter) {
    Object weakCounter;
    ObjectArray array;
    array.elements()[0] = kInitializingSingleton;
    weakCounter->field1 = array.header();
    array.InstallWeakCounter(weakCounter);

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array, weakCounter);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithSomeDataWithWeakCounter) {
    Object weakCounter;
    CharArray array;
    array.elements()[0] = 'a';
    array.elements()[1] = 'b';
    array.elements()[2] = 'c';
    weakCounter->field1 = array.header();
    array.InstallWeakCounter(weakCounter);

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array, weakCounter);
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

    auto stats = Mark({root});

    EXPECT_MARKED(
            stats, root, root_field1, root_field1_field1, root_field1_field2, root_field3, root_field3_element1, root_field3_element2,
            root_field3_element3);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkRecursiveTree) {
    Object root;
    Object inner1;
    ObjectArray inner2;
    root->field1 = inner1.header();
    inner1->field1 = inner2.header();
    inner2.elements()[0] = root.header();

    auto stats = Mark({root});

    EXPECT_MARKED(stats, root, inner1, inner2);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkForest) {
    Object root1;
    ObjectArray root2;
    Object root3;

    auto stats = Mark({root1, root2, root3});

    EXPECT_MARKED(stats, root1, root2, root3);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkForestWithInterconnectedRoots) {
    Object root1;
    ObjectArray root2;
    Object root3;

    root1->field1 = root2.header();
    root2.elements()[0] = root3.header();
    root3->field1 = root1.header();

    auto stats = Mark({root1, root2, root3});

    EXPECT_MARKED(stats, root1, root2, root3);
}
