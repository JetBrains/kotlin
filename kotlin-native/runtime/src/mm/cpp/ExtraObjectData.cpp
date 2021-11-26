/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectData.hpp"

#include "ObjectOps.hpp"
#include "PointerBits.h"
#include "Weak.h"
#include "ExtraObjectDataFactory.hpp"

#ifdef KONAN_OBJC_INTEROP
#include "ObjCMMAPI.h"
#endif

using namespace kotlin;

namespace {

template <typename T>
ALWAYS_INLINE T UnsafeRead(T* location) noexcept {
#if __has_feature(thread_sanitizer)
    // Make TSAN think that this load is fine.
    return __atomic_load_n(location, __ATOMIC_ACQUIRE);
#else
    return *location;
#endif
}

} // namespace

// static
mm::ExtraObjectData& mm::ExtraObjectData::Install(ObjHeader* object) noexcept {
    // TODO: Consider extracting initialization scheme with speculative load.
    // `object->typeInfoOrMeta_` is assigned at most once. If we read some old value (i.e. not a meta object),
    // we will fail at CAS below. If we read the new value, we will immediately return it.
    TypeInfo* typeInfo = UnsafeRead(&object->typeInfoOrMeta_);

    if (auto* metaObject = ObjHeader::AsMetaObject(typeInfo)) {
        return mm::ExtraObjectData::FromMetaObjHeader(metaObject);
    }

    RuntimeCheck(!hasPointerBits(typeInfo, OBJECT_TAG_MASK), "Object must not be tagged");

    auto *threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    auto& data = mm::ExtraObjectDataFactory::Instance().CreateExtraObjectDataForObject(threadData, object, typeInfo);

    TypeInfo* old = __sync_val_compare_and_swap(&object->typeInfoOrMeta_, typeInfo, reinterpret_cast<TypeInfo*>(&data));
    if (old != typeInfo) {
        // Somebody else created `mm::ExtraObjectData` for this object
        mm::ExtraObjectDataFactory::Instance().DestroyExtraObjectData(threadData, data);
        return *reinterpret_cast<mm::ExtraObjectData*>(old);
    }

    return data;
}

// static
void mm::ExtraObjectData::Uninstall(ObjHeader* object) noexcept {
    RuntimeAssert(object->has_meta_object(), "Object must have a meta object set");

    auto& data = ExtraObjectData::FromMetaObjHeader(object->meta_object());

    *const_cast<const TypeInfo**>(&object->typeInfoOrMeta_) = data.typeInfo_;

    auto *threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
#ifdef KONAN_OBJC_INTEROP
    Kotlin_ObjCExport_releaseAssociatedObject(data.associatedObject_);
    data.associatedObject_ = nullptr;
#endif
    mm::ExtraObjectDataFactory::Instance().DestroyExtraObjectData(threadData, data);
}

void mm::ExtraObjectData::DetachAssociatedObject() noexcept {
#ifdef KONAN_OBJC_INTEROP
    Kotlin_ObjCExport_detachAssociatedObject(associatedObject_);
#endif
}

void mm::ExtraObjectData::ClearWeakReferenceCounter() noexcept {
    if (!HasWeakReferenceCounter()) return;

    auto *object = GetBaseObject();
    WeakReferenceCounterClear(GetWeakReferenceCounter());
    // Not using `mm::SetHeapRef here`, because this code is called during sweep phase by the GC thread,
    // and so cannot affect marking.
    // TODO: Asserts on the above?
    weakReferenceCounterOrBaseObject_ = object;
}

mm::ExtraObjectData::~ExtraObjectData() {
    RuntimeAssert(!HasWeakReferenceCounter(), "Object must have cleared weak references");

#ifdef KONAN_OBJC_INTEROP
    RuntimeAssert(associatedObject_ == nullptr, "Object must have cleared associated object");
#endif
}
