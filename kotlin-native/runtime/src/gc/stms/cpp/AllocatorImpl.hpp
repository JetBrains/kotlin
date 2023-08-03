/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#ifdef CUSTOM_ALLOCATOR

// TODO: Move into alloc/custom/AllocatorImpl.hpp

#include "CustomAllocator.hpp"
#include "CustomFinalizerProcessor.hpp"
#include "GCApi.hpp"
#include "Heap.hpp"

namespace kotlin::gc {

inline GC::ObjectData& objectDataForObject(ObjHeader* object) noexcept {
    return kotlin::alloc::objectDataForObject(object);
}

inline ObjHeader* objectForObjectData(GC::ObjectData& objectData) noexcept {
    return kotlin::alloc::objectForObjectData(objectData);
}

using FinalizerQueue = alloc::FinalizerQueue;
using FinalizerQueueTraits = alloc::FinalizerQueueTraits;

} // namespace kotlin::gc

#else

// TODO: Move into alloc/legacy/AllocatorImpl.hpp

#include "ExtraObjectDataFactory.hpp"
#include "GC.hpp"
#include "GlobalData.hpp"
#include "Logging.hpp"
#include "ObjectFactory.hpp"
#include "ObjectFactoryAllocator.hpp"
#include "ObjectFactorySweep.hpp"

namespace kotlin::gc {

struct ObjectFactoryTraits {
    using Allocator = alloc::AllocatorWithGC<alloc::AllocatorBasic, ObjectFactoryTraits>;
    using ObjectData = gc::GC::ObjectData;

    Allocator CreateAllocator() noexcept { return Allocator(alloc::AllocatorBasic(), *this); }

    void OnOOM(size_t size) noexcept {
        RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
        // TODO: This will print the log for "manual" scheduling. Fix this.
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinished();
    }
};

using ObjectFactory = alloc::ObjectFactory<ObjectFactoryTraits>;

inline GC::ObjectData& objectDataForObject(ObjHeader* object) noexcept {
    return ObjectFactory::NodeRef::From(object).ObjectData();
}

inline ObjHeader* objectForObjectData(GC::ObjectData& objectData) noexcept {
    return ObjectFactory::NodeRef::From(objectData)->GetObjHeader();
}

using FinalizerQueue = ObjectFactory::FinalizerQueue;
using FinalizerQueueTraits = ObjectFactory::FinalizerQueueTraits;

} // namespace kotlin::gc

#endif
