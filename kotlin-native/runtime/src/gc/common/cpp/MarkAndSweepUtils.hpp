/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H
#define RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H

#include "ExtraObjectData.hpp"
#include "FinalizerHooks.hpp"
#include "GlobalData.hpp"
#include "GCStatistics.hpp"
#include "Logging.hpp"
#include "Memory.h"
#include "ObjectOps.hpp"
#include "ObjectTraversal.hpp"
#include "RootSet.hpp"
#include "Runtime.h"
#include "StableRefRegistry.hpp"
#include "ThreadData.hpp"
#include "Types.h"

namespace kotlin {
namespace gc {

namespace internal {

class ObjFieldIterable {
public:
    class Iterator {
    public:
        Iterator(ObjFieldIterable& owner, std::size_t fieldIndex) noexcept
            : owner_(owner), fieldIndex_(fieldIndex) {}

        KRef* operator*() noexcept {
            auto obj = owner_.obj_;
            auto offs = owner_.typeInfo_->objOffsets_[fieldIndex_];
            return reinterpret_cast<KRef*>(reinterpret_cast<uintptr_t>(obj) + offs);
        }
        Iterator& operator++() noexcept {
            ++fieldIndex_;
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return &owner_ == &rhs.owner_ && fieldIndex_ == rhs.fieldIndex_; }
        bool operator!=(const Iterator& rhs) const noexcept { return !(*this == rhs); }
    private:
        ObjFieldIterable& owner_;
        std::size_t fieldIndex_;
    };

    explicit ObjFieldIterable(ObjHeader* obj) noexcept : obj_(obj), typeInfo_(obj->type_info()) {
        RuntimeAssert(obj->type_info() != theArrayTypeInfo, "Must not be an array of objects");
    }
    Iterator begin() noexcept { return { *this, 0 }; }
    Iterator end() noexcept { return { *this, static_cast<std::size_t>(typeInfo_->objOffsetsCount_) }; }

private:
    ObjHeader* obj_;
    const TypeInfo* typeInfo_;
};

class ArrayElemIterable {
public:
    class Iterator {
    public:
        explicit Iterator(KRef* elemPtr) : cur_(elemPtr) {}
        Iterator(ArrayHeader* array, std::size_t idx) : Iterator(ArrayAddressOfElementAt(array, idx)) {}
        KRef* operator*() noexcept { return cur_; }
        Iterator& operator++() noexcept {
            ++cur_;
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return cur_ == rhs.cur_; }
        bool operator!=(const Iterator& rhs) const noexcept { return !(*this == rhs); }
    private:
        KRef* cur_;
    };
    explicit ArrayElemIterable(ArrayHeader* array) noexcept : array_(array) {
        RuntimeAssert(array->type_info() == theArrayTypeInfo, "Must be an array of objects");
    }
    Iterator begin() noexcept { return {array_, 0}; }
    Iterator end() noexcept { return {array_, array_->count_}; }

private:
    ArrayHeader* array_;
};

// TODO consider making an iterator
// TODO consider moving somewhere else
template<typename Fun>
void forEachRefField(ObjHeader* obj, Fun fun) {
    auto* typeInfo = obj->type_info();
    if (typeInfo == theArrayTypeInfo) {
        auto array = reinterpret_cast<ArrayHeader*>(obj);
        for (auto elemPtr: gc::internal::ArrayElemIterable(array)) {
            fun(elemPtr);
        }
    } else {
        for (auto elemPtr: gc::internal::ObjFieldIterable(obj)) {
            fun(elemPtr);
        }
    }
}

template <typename Traits>
void processFieldInMark(void* state, ObjHeader* field) noexcept {
    auto& markQueue = *static_cast<typename Traits::MarkQueue*>(state);
    if (field->heap()) {
        Traits::tryEnqueue(markQueue, field);
    }
}

template <typename Traits>
void processObjectInMark(void* state, ObjHeader* object) noexcept {
    for (auto fieldPtr: ObjFieldIterable(object)) {
        auto field = *fieldPtr;
        if (!field) continue;
        // FIXME not long
        long offs = reinterpret_cast<void**>(fieldPtr) - reinterpret_cast<void**>(object);
        // TODO TraceMark("Marking field *(%p + %" PRIdPTR ") -> %p", object, offs, field);
        TraceMark("Marking field *(%p + %ld) -> %p", object, offs, field);
        processFieldInMark<Traits>(state, field);
    }
}

template <typename Traits>
void processArrayInMark(void* state, ArrayHeader* array) noexcept {
    std::size_t idx = 0;
    for (auto elemPtr: ArrayElemIterable(array)) {
        auto elem = *elemPtr;
        auto curIdx = idx++;
        if (!elem) continue;
        TraceMark("Marking array element %p[%zu] -> %p", array, curIdx, elem);
        processFieldInMark<Traits>(state, elem);
    }
}

template <typename Traits>
bool collectRoot(typename Traits::MarkQueue& markQueue, ObjHeader* object) noexcept {
    if (isNullOrMarker(object))
        return false;

    if (object->heap()) {
        Traits::tryEnqueue(markQueue, object);
    } else {
        // Each permanent and stack object has own entry in the root set, so it's okay to only process objects in heap.
        Traits::processInMark(markQueue, object);
        RuntimeAssert(!object->has_meta_object(), "Non-heap object %p may not have an extra object data", object);
    }
    return true;
}

// TODO: Consider making it noinline to keep loop in `Mark` small.
template <typename Traits>
void processExtraObjectData(GCHandle::GCMarkScope& markHandle, typename Traits::MarkQueue& markQueue, mm::ExtraObjectData& extraObjectData, ObjHeader* object) noexcept {
    if (auto weakCounter = extraObjectData.GetWeakReferenceCounter()) {
        RuntimeAssert(
                weakCounter->heap(), "Weak counter must be a heap object. object=%p counter=%p permanent=%d local=%d", object, weakCounter,
                weakCounter->permanent(), weakCounter->local());
        // Do not schedule WeakReferenceCounter but process it right away.
        // This will skip markQueue interaction.
        if (Traits::tryMark(weakCounter)) {
            markHandle.addObject(mm::GetAllocatedHeapSize(weakCounter));
            // WeakReferenceCounter is empty, but keeping this just in case.
            Traits::processInMark(markQueue, weakCounter);
        }
    }
}

} // namespace internal

template <typename Traits>
void Mark(GCHandle handle, typename Traits::MarkQueue& markQueue) noexcept {
    auto markHandle = handle.mark();
    while (ObjHeader* top = Traits::tryDequeue(markQueue)) {
        // TODO: Consider moving it to the sweep phase to make this loop more tight.
        //       This, however, requires care with scheduler interoperation.
        markHandle.addObject(mm::GetAllocatedHeapSize(top));

        Traits::processInMark(markQueue, top);

        // TODO: Consider moving it before processInMark to make the latter something of a tail call.
        if (auto* extraObjectData = mm::ExtraObjectData::Get(top)) {
            internal::processExtraObjectData<Traits>(markHandle, markQueue, *extraObjectData, top);
        }
    }
}

template <typename Traits>
void SweepExtraObjects(GCHandle handle, typename Traits::ExtraObjectsFactory& objectFactory) noexcept {
    objectFactory.ProcessDeletions();
    auto sweepHandle = handle.sweepExtraObjects();
    auto iter = objectFactory.LockForIter();
    for (auto it = iter.begin(); it != iter.end();) {
        auto &extraObject = *it;
        if (!extraObject.getFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE) && !Traits::IsMarkedByExtraObject(extraObject)) {
            extraObject.ClearWeakReferenceCounter();
            if (extraObject.HasAssociatedObject()) {
                extraObject.DetachAssociatedObject();
                extraObject.setFlag(mm::ExtraObjectData::FLAGS_IN_FINALIZER_QUEUE);
                ++it;
            } else {
                extraObject.Uninstall();
                objectFactory.EraseAndAdvance(it);
            }
        } else {
            ++it;
        }
    }
}

template <typename Traits>
typename Traits::ObjectFactory::FinalizerQueue Sweep(GCHandle handle, typename Traits::ObjectFactory::Iterable& objectFactoryIter) noexcept {
    typename Traits::ObjectFactory::FinalizerQueue finalizerQueue;
    auto sweepHandle = handle.sweep();

    for (auto it = objectFactoryIter.begin(); it != objectFactoryIter.end();) {
        if (Traits::TryResetMark(*it)) {
            ++it;
            continue;
        }
        auto* objHeader = it->GetObjHeader();
        if (HasFinalizers(objHeader)) {
            objectFactoryIter.MoveAndAdvance(finalizerQueue, it);
        } else {
            objectFactoryIter.EraseAndAdvance(it);
        }
    }

    return finalizerQueue;
}
template <typename Traits>
typename Traits::ObjectFactory::FinalizerQueue Sweep(GCHandle handle, typename Traits::ObjectFactory& objectFactory) noexcept {
    auto iter = objectFactory.LockForIter();
    return Sweep<Traits>(handle, iter);
}

template <typename Traits>
void collectRootSetForThread(GCHandle gcHandle, typename Traits::MarkQueue& markQueue, mm::ThreadData& thread) {
    auto handle = gcHandle.collectThreadRoots(thread);
    thread.gc().OnStoppedForGC();
    // TODO: Remove useless mm::ThreadRootSet abstraction.
    for (auto value : mm::ThreadRootSet(thread)) {
        if (internal::collectRoot<Traits>(markQueue, value.object)) {
            const char* sourceStr = nullptr;
            switch (value.source) {
                case mm::ThreadRootSet::Source::kStack:
                    handle.addStackRoot();
                    sourceStr = "Stack";
                    break;
                case mm::ThreadRootSet::Source::kTLS:
                    handle.addThreadLocalRoot();
                    sourceStr = "TLS";
                    break;
            }
            TraceMark("Found %s root %p", sourceStr, value.object);
        }
    }
}

template <typename Traits>
void collectRootSetGlobals(GCHandle gcHandle, typename Traits::MarkQueue& markQueue) noexcept {
    auto handle = gcHandle.collectGlobalRoots();
    mm::StableRefRegistry::Instance().ProcessDeletions();
    // TODO: Remove useless mm::GlobalRootSet abstraction.
    for (auto value : mm::GlobalRootSet()) {
        if (internal::collectRoot<Traits>(markQueue, value.object)) {
            const char* sourceStr = nullptr;
            switch (value.source) {
                case mm::GlobalRootSet::Source::kGlobal:
                    handle.addGlobalRoot();
                    sourceStr = "Global";
                    break;
                case mm::GlobalRootSet::Source::kStableRef:
                    handle.addStableRoot();
                    sourceStr = "StableRef";
                    break;
            }
            TraceMark("Found %s root %p", sourceStr, value.object);
        }
    }
}

// TODO: This needs some tests now.
template <typename Traits, typename F>
void collectRootSet(GCHandle handle, typename Traits::MarkQueue& markQueue, F&& filter) noexcept {
    Traits::clear(markQueue);
    for (auto& thread : mm::GlobalData::Instance().threadRegistry().LockForIter()) {
        if (!filter(thread))
            continue;
        thread.Publish();
        collectRootSetForThread<Traits>(handle, markQueue, thread);
    }
    collectRootSetGlobals<Traits>(handle, markQueue);
}

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H
