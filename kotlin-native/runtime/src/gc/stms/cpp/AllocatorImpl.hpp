/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#ifdef CUSTOM_ALLOCATOR

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

#include "Allocator.hpp"
#include "ExtraObjectDataFactory.hpp"
#include "GC.hpp"
#include "GlobalData.hpp"
#include "Logging.hpp"
#include "ObjectFactory.hpp"

namespace kotlin::gc {

struct ObjectFactoryTraits {
    using Allocator = AllocatorWithGC<Allocator, ObjectFactoryTraits>;
    using ObjectData = gc::GC::ObjectData;

    Allocator CreateAllocator() noexcept { return Allocator(gc::Allocator(), *this); }

    void OnOOM(size_t size) noexcept {
        RuntimeLogDebug({kTagGC}, "Attempt to GC on OOM at size=%zu", size);
        // TODO: This will print the log for "manual" scheduling. Fix this.
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinished();
    }
};

using ObjectFactory = mm::ObjectFactory<ObjectFactoryTraits>;

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
