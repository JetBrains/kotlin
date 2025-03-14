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
#include "MarkState.hpp"
#include "Memory.h"
#include "ObjectOps.hpp"
#include "ObjectTraversal.hpp"
#include "RootSet.hpp"
#include "Runtime.h"
#include "ExternalRCRefRegistry.hpp"
#include "ThreadData.hpp"
#include "Types.h"

namespace kotlin {
namespace gc {

namespace internal {

template <typename Traits>
void processFieldInMark(void* state, ObjHeader* object, ObjHeader* field) noexcept {
    auto& markState = *static_cast<MarkState<Traits>*>(state);
    if (field->heapNotLocal()) {
        Traits::tryEnqueue(markState.globalQueue, field);
    } else if (field->local()) {
        RuntimeAssert(object->stackOrLocal(), "Heap object %p references local object %p[typeInfo=%p]", object, field,
                      field->type_info());
        Traits::tryEnqueue(markState.localQueue, field);
    } else if (field->stack()) {
        RuntimeAssert(object->stackOrLocal(), "Heap object %p references stack object %p[typeInfo=%p]", object, field,
                      field->type_info());
    }
}

template <typename Traits>
void processObjectInMark(void* state, ObjHeader* object) noexcept {
    traverseClassObjectFields(object, [=] (auto fieldAccessor) noexcept {
        if (ObjHeader* field = fieldAccessor.direct()) {
            processFieldInMark<Traits>(state, object, field);
        }
    });
}

template <typename Traits>
void processArrayInMark(void* state, ArrayHeader* array) noexcept {
    traverseArrayOfObjectsElements(array, [=] (auto elemAccessor) noexcept {
        if (ObjHeader* elem = elemAccessor.direct()) {
            processFieldInMark<Traits>(state, array->obj(), elem);
        }
    });
}

template <typename Traits>
bool collectRoot(MarkState<Traits>& markState, ObjHeader* object) noexcept {
    if (isNullOrMarker(object))
        return false;

    if (object->heapNotLocal()) {
        Traits::tryEnqueue(markState.globalQueue, object);
    } else {
        if (object->local()) {
            Traits::tryEnqueue(markState.localQueue, object);
        } else {
            // Each permanent and stack object has own entry in the root set, so it's okay to only process objects in heap.
            Traits::processInMark(markState, object);
        }
        RuntimeAssert(!object->has_meta_object(), "Non-heap object %p may not have an extra object data", object);
    }
    return true;
}

// TODO: Consider making it noinline to keep loop in `Mark` small.
template <typename Traits>
void processExtraObjectData(GCHandle::GCMarkScope& markHandle, MarkState<Traits>& markState, mm::ExtraObjectData& extraObjectData, ObjHeader* object) noexcept {
    if (auto weakReference = extraObjectData.GetRegularWeakReferenceImpl()) {
        RuntimeAssert(
                weakReference->heapNotLocal(), "Weak reference must be a non-local heap object. object=%p weak=%p permanent=%d stack=%d local=%d", object,
                weakReference, weakReference->permanent(), weakReference->stack(), weakReference->local());
        // Do not schedule RegularWeakReferenceImpl but process it right away.
        // This will skip markQueue interaction.
        if (Traits::tryMark(weakReference)) {
            markHandle.addObject();
            // RegularWeakReferenceImpl is empty, but keeping this just in case.
            Traits::processInMark(markState, weakReference);
        }
    }
}

} // namespace internal

template <typename Traits>
void Mark(GCHandle handle, MarkState<Traits>& markState) noexcept {
    auto markHandle = handle.mark();
    Mark<Traits>(markHandle, markState);
}

template <typename Traits>
void Mark(GCHandle::GCMarkScope& markHandle, MarkState<Traits>& markState) noexcept {
    RuntimeAssert(Traits::isEmpty(markState.localQueue), "localQueue is not empty before Mark");

    while (ObjHeader* top = Traits::tryDequeue(markState.globalQueue)) {
        markHandle.addObject();
        Traits::processInMark(markState, top);
        // TODO: Consider moving it before processInMark to make the latter something of a tail call.
        if (auto* extraObjectData = mm::ExtraObjectData::Get(top)) {
            internal::processExtraObjectData<Traits>(markHandle, markState, *extraObjectData, top);
        }
    }

    RuntimeAssert(Traits::isEmpty(markState.localQueue), "localQueue is not empty after Mark");
}

template <typename Traits>
void collectRootSetForThread(GCHandle gcHandle, MarkState<Traits>& markState, mm::ThreadData& thread) {
    RuntimeAssert(Traits::isEmpty(markState.localQueue), "localQueue is not empty before collectRootSetForThread");

    auto handle = gcHandle.collectThreadRoots(thread);
    // TODO: Remove useless mm::ThreadRootSet abstraction.
    for (auto value : mm::ThreadRootSet(thread)) {
        if (internal::collectRoot<Traits>(markState, value.object)) {
            switch (value.source) {
                case mm::ThreadRootSet::Source::kStack:
                    handle.addStackRoot();
                    break;
                case mm::ThreadRootSet::Source::kTLS:
                    handle.addThreadLocalRoot();
                    break;
            }
        }
    }

    // Local objects go to a separate queue that is handled strictly during STW here.
    // Otherwise `Mark` can try to scan references from local to stack objects that are already removed.
    // See also KT-75861.
    auto markHandle = gcHandle.mark();
    while (ObjHeader* top = Traits::tryDequeue(markState.localQueue)) {
        markHandle.addObject();
        Traits::processInMark(markState, top);
    }
}

template <typename Traits>
void collectRootSetGlobals(GCHandle gcHandle, MarkState<Traits>& markState) noexcept {
    RuntimeAssert(Traits::isEmpty(markState.localQueue), "localQueue is not empty before collectRootSetGlobals");

    auto handle = gcHandle.collectGlobalRoots();
    // TODO: Remove useless mm::GlobalRootSet abstraction.
    for (auto value : mm::GlobalRootSet()) {
        if (internal::collectRoot<Traits>(markState, value.object)) {
            switch (value.source) {
                case mm::GlobalRootSet::Source::kGlobal:
                    handle.addGlobalRoot();
                    break;
                case mm::GlobalRootSet::Source::kStableRef:
                    handle.addStableRoot();
                    break;
            }
        }
    }

    RuntimeAssert(Traits::isEmpty(markState.localQueue), "localQueue is not empty after collectRootSetGlobals");
}

// TODO: This needs some tests now.
template <typename Traits, typename F>
void collectRootSet(GCHandle handle, MarkState<Traits>& markState, F&& filter) noexcept {
    Traits::clear(markState.globalQueue);
    for (auto& thread : mm::GlobalData::Instance().threadRegistry().LockForIter()) {
        if (!filter(thread))
            continue;
        thread.Publish();
        collectRootSetForThread<Traits>(handle, markState, thread);
    }
    collectRootSetGlobals<Traits>(handle, markState);
}

template <typename Traits>
void processWeaks(GCHandle gcHandle, mm::ExternalRCRefRegistry& registry) noexcept {
    auto handle = gcHandle.processWeaks();
    for (auto object : registry.lockForIter()) { // FIXME rename
        auto* obj = object.load(std::memory_order_relaxed);
        if (!obj) {
            // We already processed it at some point.
            handle.addUndisposed();
            continue;
        }
        if (obj->permanent() || Traits::IsMarked(obj)) {
            // TODO: Let's not put permanent objects in here at all?
            // Object is alive. Nothing to do.
            handle.addAlive();
            continue;
        }
        // Object is not alive. Clear it out.
        object.store(nullptr, std::memory_order_relaxed);
        handle.addNulled();
    }
}

struct DefaultProcessWeaksTraits {
    static bool IsMarked(ObjHeader* obj) noexcept { return gc::isMarked(obj); }
};

void stopTheWorld(GCHandle gcHandle, const char* reason) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;

[[nodiscard]] inline auto stopTheWorldInScope(GCHandle gcHandle) noexcept {
    return ScopeGuard([=]() { stopTheWorld(gcHandle, "GC stop the world"); }, [=]() { resumeTheWorld(gcHandle); });
}

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H
