/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "AllocatorImpl.hpp"

using namespace kotlin;

gc::GC::ObjectData& alloc::objectDataForObject(ObjHeader* object) noexcept {
    return ObjectFactoryImpl::NodeRef::From(object).ObjectData();
}

ObjHeader* alloc::objectForObjectData(gc::GC::ObjectData& objectData) noexcept {
    return ObjectFactoryImpl::NodeRef::From(objectData)->GetObjHeader();
}

size_t alloc::allocatedHeapSize(ObjHeader* object) noexcept {
    return ObjectFactoryImpl::GetAllocatedHeapSize(object);
}
