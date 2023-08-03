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
#include "SpecialRefRegistry.hpp"
#include "ThreadData.hpp"
#include "Types.h"

namespace kotlin {
namespace gc {

namespace internal {

template <typename Traits>
void processFieldInMark(void* state, ObjHeader* field) noexcept {
    auto& markQueue = *static_cast<typename Traits::MarkQueue*>(state);
    if (field->heap()) {
        Traits::tryEnqueue(markQueue, field);
    }
}

template <typename Traits>
void processObjectInMark(void* state, ObjHeader* object) noexcept {
    traverseClassObjectFields(object, [state] (ObjHeader** fieldLocation) noexcept {
        if (auto field = *fieldLocation) {
            processFieldInMark<Traits>(state, field);
        }
    });
}

template <typename Traits>
void processArrayInMark(void* state, ArrayHeader* array) noexcept {
    traverseArrayOfObjectsElements(array, [state] (ObjHeader** elemLocation) noexcept {
        if (auto elem = *elemLocation) {
            processFieldInMark<Traits>(state, elem);
        }
    });
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
    if (auto weakReference = extraObjectData.GetRegularWeakReferenceImpl()) {
        RuntimeAssert(
                weakReference->heap(), "Weak reference must be a heap object. object=%p weak=%p permanent=%d local=%d", object,
                weakReference, weakReference->permanent(), weakReference->local());
        // Do not schedule RegularWeakReferenceImpl but process it right away.
        // This will skip markQueue interaction.
        if (Traits::tryMark(weakReference)) {
            markHandle.addObject();
            // RegularWeakReferenceImpl is empty, but keeping this just in case.
            Traits::processInMark(markQueue, weakReference);
        }
    }
}

} // namespace internal

template <typename Traits>
void Mark(GCHandle handle, typename Traits::MarkQueue& markQueue) noexcept {
    auto markHandle = handle.mark();
    Mark<Traits>(markHandle, markQueue);
}

template <typename Traits>
void Mark(GCHandle::GCMarkScope& markHandle, typename Traits::MarkQueue& markQueue) noexcept {
    while (ObjHeader* top = Traits::tryDequeue(markQueue)) {
        markHandle.addObject();

        Traits::processInMark(markQueue, top);

        // TODO: Consider moving it before processInMark to make the latter something of a tail call.
        if (auto* extraObjectData = mm::ExtraObjectData::Get(top)) {
            internal::processExtraObjectData<Traits>(markHandle, markQueue, *extraObjectData, top);
        }
    }
}

template <typename Traits>
void collectRootSetForThread(GCHandle gcHandle, typename Traits::MarkQueue& markQueue, mm::ThreadData& thread) {
    auto handle = gcHandle.collectThreadRoots(thread);
    // TODO: Remove useless mm::ThreadRootSet abstraction.
    for (auto value : mm::ThreadRootSet(thread)) {
        if (internal::collectRoot<Traits>(markQueue, value.object)) {
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
}

template <typename Traits>
void collectRootSetGlobals(GCHandle gcHandle, typename Traits::MarkQueue& markQueue) noexcept {
    auto handle = gcHandle.collectGlobalRoots();
    // TODO: Remove useless mm::GlobalRootSet abstraction.
    for (auto value : mm::GlobalRootSet()) {
        if (internal::collectRoot<Traits>(markQueue, value.object)) {
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

template <typename Traits>
void processWeaks(GCHandle gcHandle, mm::SpecialRefRegistry& registry) noexcept {
    auto handle = gcHandle.processWeaks();
    for (auto& object : registry.lockForIter()) {
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

void stopTheWorld(GCHandle gcHandle) noexcept;
void resumeTheWorld(GCHandle gcHandle) noexcept;

[[nodiscard]] inline auto stopTheWorldInScope(GCHandle gcHandle) noexcept {
    return ScopeGuard([=]() { stopTheWorld(gcHandle); }, [=]() { resumeTheWorld(gcHandle); });
}

} // namespace gc
} // namespace kotlin

#endif // RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H
