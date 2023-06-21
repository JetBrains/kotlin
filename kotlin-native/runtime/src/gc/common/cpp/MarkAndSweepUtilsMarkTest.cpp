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

void InstallWeakReference(test_support::Any& object, test_support::RegularWeakReferenceImpl& weakRef) noexcept {
    auto& extraObjectData = mm::ExtraObjectData::GetOrInstall(object.header());
    weakRef->referred = object.header();
    auto* setWeakRef = extraObjectData.GetOrSetRegularWeakReferenceImpl(object.header(), weakRef.header());
    EXPECT_EQ(setWeakRef, weakRef.header());
    EXPECT_EQ(extraObjectData.GetBaseObject(), object.header());
}

void Finalize(test_support::Any& object) noexcept {
    if (auto* extraObjectData = mm::ExtraObjectData::Get(object.header())) {
        extraObjectData->ClearRegularWeakReferenceImpl();
    }
    RunFinalizers(object.header());
}

class Object : public test_support::Object<Payload> {
public:
    Object() : test_support::Object<Payload>(typeHolder.typeInfo()) {}

    ~Object() { Finalize(*this); }
};

class ObjectArray : public test_support::ObjectArray<3> {
public:
    ObjectArray() : test_support::ObjectArray<3>() {}

    ~ObjectArray() { Finalize(*this); }
};

class CharArray : public test_support::CharArray<3> {
public:
    CharArray() : test_support::CharArray<3>() {}

    ~CharArray() { Finalize(*this); }
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

    static void clear(MarkQueue& queue) noexcept {
        queue.clear();
    }

    static ObjHeader* tryDequeue(MarkQueue& queue) noexcept {
        if (queue.empty()) return nullptr;
        auto top = queue.back();
        queue.pop_back();
        return top;
    }

    static bool tryEnqueue(MarkQueue& queue, ObjHeader* object) noexcept {
        auto result = instance_->marked_.insert(object);
        if (result.second) {
            queue.push_back(object);
        }
        return result.second;
    }

    static bool tryMark(ObjHeader* object) noexcept {
        auto result = instance_->marked_.insert(object);
        return result.second;
    }

    static void processInMark(MarkQueue& markQueue, ObjHeader* object) noexcept {
        if (object->type_info() == theArrayTypeInfo) {
            gc::internal::processArrayInMark<ScopedMarkTraits>(static_cast<void*>(&markQueue), object->array());
        } else {
            gc::internal::processObjectInMark<ScopedMarkTraits>(static_cast<void*>(&markQueue), object);
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

    auto MarkedMatcher(std::initializer_list<std::reference_wrapper<test_support::Any>> expected) {
        std_support::vector<ObjHeader*> objects;
        for (auto& object : expected) {
            objects.push_back(object.get().header());
        }
        return testing::UnorderedElementsAreArray(objects);
    }

    gc::MarkStats Mark(std::initializer_list<std::reference_wrapper<test_support::Any>> graySet) {
        std_support::vector<ObjHeader*> objects;
        for (auto& object : graySet) ScopedMarkTraits::tryEnqueue(objects, object.get().header());
        auto handle = gc::GCHandle::create(epoch_++);
        gc::Mark<ScopedMarkTraits>(handle, objects);
        handle.finished();
        return handle.getMarked();
    }

    ~MarkAndSweepUtilsMarkTest() { mm::GlobalData::Instance().gc().ClearForTests(); }

private:
    uint64_t epoch_ = 0;
    kotlin::ScopedMemoryInit memoryInit;
    ScopedMarkTraits markTraits_;
};

#define EXPECT_MARKED(stats, ...) \
    do { \
        std::initializer_list<std::reference_wrapper<test_support::Any>> objects = {__VA_ARGS__}; \
        EXPECT_THAT(stats.markedCount, objects.size()); \
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
    object->field1 = nullptr;

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithInvalidFields) {
    ObjectArray array;
    array.elements()[0] = nullptr;

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
    object.installMetaObject();

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithExtraData) {
    ObjectArray array;
    array.installMetaObject();

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithExtraData) {
    CharArray array;
    array.installMetaObject();

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithWeakReference) {
    test_support::RegularWeakReferenceImpl weakReference;
    Object object;
    InstallWeakReference(object, weakReference);

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object, weakReference);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithWeakReference) {
    test_support::RegularWeakReferenceImpl weakReference;
    ObjectArray array;
    InstallWeakReference(array, weakReference);

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array, weakReference);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithWeakReference) {
    test_support::RegularWeakReferenceImpl weakReference;
    CharArray array;
    InstallWeakReference(array, weakReference);

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array, weakReference);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectWithInvalidFieldsWithWeakReference) {
    test_support::RegularWeakReferenceImpl weakReference;
    Object object;
    object->field1 = nullptr;
    InstallWeakReference(object, weakReference);

    auto stats = Mark({object});

    EXPECT_MARKED(stats, object, weakReference);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleObjectArrayWithInvalidFieldsWithWeakReference) {
    test_support::RegularWeakReferenceImpl weakReference;
    ObjectArray array;
    array.elements()[0] = nullptr;
    InstallWeakReference(array, weakReference);

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array, weakReference);
}

TEST_F(MarkAndSweepUtilsMarkTest, MarkSingleCharArrayWithSomeDataWithWeakReference) {
    test_support::RegularWeakReferenceImpl weakReference;
    CharArray array;
    array.elements()[0] = 'a';
    array.elements()[1] = 'b';
    array.elements()[2] = 'c';
    InstallWeakReference(array, weakReference);

    auto stats = Mark({array});

    EXPECT_MARKED(stats, array, weakReference);
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
