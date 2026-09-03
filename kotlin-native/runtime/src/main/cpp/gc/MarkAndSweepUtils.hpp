/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H
#define RUNTIME_GC_COMMON_MARK_AND_SWEEP_UTILS_H

#include <cstdint>
#include "stackmap/DeltaMainStackMap.hpp"
#include "KAssert.h"
#include "stackmap/RootsInfo.hpp"
#include "mm/ExtraObjectData.hpp"
#include "FinalizerHooks.hpp"
#include "mm/GlobalData.hpp"
#include "gc/GCStatistics.hpp"
#include "Logging.hpp"
#include "Memory.h"
#include "mm/ObjectOps.hpp"
#include "mm/ObjectTraversal.hpp"
#include "mm/RootSet.hpp"
#include "mm/ExternalRCRefRegistry.hpp"
#include "mm/ThreadData.hpp"

namespace kotlin {
namespace gc {

namespace internal {

template <typename Traits>
void processFieldInMark(void* state, ObjHeader* object, ObjHeader* field) noexcept {
    auto& markQueue = *static_cast<typename Traits::MarkQueue*>(state);
    if (field->heap()) {
        Traits::tryEnqueue(markQueue, field);
    }
    if constexpr (!Traits::kAllowHeapToStackRefs) {
        if (object->heap()) {
            RuntimeAssert(!field->stack(), "Heap object %p references stack object %p[typeInfo=%p]", object, field, field->type_info());
        }
    }
}

template <typename Traits>
void processObjectInMark(void* state, ObjHeader* object) noexcept {
    traverseClassObjectFields(object, [=](auto fieldAccessor) noexcept {
        if (ObjHeader* field = fieldAccessor.direct()) {
            processFieldInMark<Traits>(state, object, field);
        }
    });
}

template <typename Traits>
void processArrayInMark(void* state, ArrayHeader* array) noexcept {
    traverseArrayOfObjectsElements(array, [=](auto elemAccessor) noexcept {
        if (ObjHeader* elem = elemAccessor.direct()) {
            processFieldInMark<Traits>(state, array->obj(), elem);
        }
    });
}

template <typename Traits>
bool collectRoot(typename Traits::MarkQueue& markQueue, ObjHeader* object) noexcept {
    if (isNullOrMarker(object)) return false;

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
void processExtraObjectData(
        GCHandle::GCMarkScope& markHandle,
        typename Traits::MarkQueue& markQueue,
        mm::ExtraObjectData& extraObjectData,
        ObjHeader* object) noexcept {
    if (auto weakReference = extraObjectData.GetRegularWeakReferenceImpl()) {
        RuntimeAssert(
                weakReference->heap(), "Weak reference must be a heap object. object=%p weak=%p permanent=%d stack=%d", object,
                weakReference, weakReference->permanent(), weakReference->stack());
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
void collectRootSetFromMapForThread(GCHandle gcHandle, typename Traits::MarkQueue& markQueue, kotlin::stackMap::DeltaMainStackMapBuilder& stackMapBuilder, mm::ThreadData& thread) {
    const int maxHopAmount = 4;
    auto handle = gcHandle.collectThreadRoots(thread);

    for (auto anchor : thread.frameAnchors()) {
        uint64_t* fp = anchor.fp;
        uint64_t* pc = anchor.pc;
        RuntimeLogDebug({logging::Tag::kGC}, "Start new anchor pc=%p fp=%p", pc, fp);

        int i = 0;
        while (i < maxHopAmount
               && (stackMapBuilder.pc2RootsInfo().find((uintptr_t) pc) == stackMapBuilder.pc2RootsInfo().end())) {
            pc = (uint64_t*) (*(fp + 1));
            fp = (uint64_t*)(*fp);
            RuntimeLogDebug({logging::Tag::kGC}, "Hop one frame up pc=%p fp=%p", pc, fp);
            i++;
        }

        if (stackMapBuilder.pc2RootsInfo().find((uintptr_t) pc) == stackMapBuilder.pc2RootsInfo().end()) {
            RuntimeLogDebug({logging::Tag::kGC}, "Could not find live kotlin frame");

        }

        while (stackMapBuilder.pc2RootsInfo().find((uintptr_t) pc) != stackMapBuilder.pc2RootsInfo().end()) {
            RuntimeLogDebug({logging::Tag::kGC}, "Start new frame pc=%p fp=%p", pc, fp);
            for (stackMap::RootLocation rootsInfo : stackMapBuilder.pc2RootsInfo().at((uintptr_t)pc)) {
                if (rootsInfo.Type == stackMap::RootLocation::Indirect) {
                    uint8_t* address = (uint8_t*) fp + rootsInfo.Offset;
                    RuntimeLogDebug({logging::Tag::kGC}, "Trying to collect root slot pc=%p fp=%p address=%p", pc, fp, address);
                    ObjHeader* object = *reinterpret_cast<ObjHeader**>(address);
                    RuntimeLogDebug({logging::Tag::kGC}, "Object address=%p", object);
                    if (internal::collectRoot<Traits>(markQueue, object)) {
                        handle.addStackRoot();
                        RuntimeLogDebug({logging::Tag::kGC}, "collected root slot pc=%p fp=%p address=%p", pc, fp, address);
                    } else {
                        RuntimeLogDebug({logging::Tag::kGC}, "root slot is not collected pc=%p fp=%p address=%p", pc, fp, address);
                    }

                } else {
                    RuntimeFail("Indirect only expected");
                }
            }

            pc = (uint64_t*) (*(fp + 1));
            fp = (uint64_t*)(*fp);
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
        if (!filter(thread)) continue;
        thread.Publish();
        collectRootSetForThread<Traits>(handle, markQueue, thread);
    }
    collectRootSetGlobals<Traits>(handle, markQueue);
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
