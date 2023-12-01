/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExtraObjectData.hpp"

#include "PointerBits.h"
#include "ThreadData.hpp"

#ifdef KONAN_OBJC_INTEROP
#include "ObjCMMAPI.h"
#endif

using namespace kotlin;

// static
mm::ExtraObjectData& mm::ExtraObjectData::Install(ObjHeader* object) noexcept {
    // TODO: Consider extracting initialization scheme with speculative load.
    // `object->typeInfoOrMeta_` is assigned at most once. If we read some old value (i.e. not a meta object),
    // we will fail at CAS below. If we read the new value, we will immediately return it.
    TypeInfo* typeInfo = object->typeInfoOrMetaAcquire();

    if (auto* metaObject = ObjHeader::AsMetaObject(typeInfo)) {
        return mm::ExtraObjectData::FromMetaObjHeader(metaObject);
    }

    RuntimeCheck(!hasPointerBits(typeInfo, OBJECT_TAG_MASK), "Object must not be tagged");

    auto& allocator = mm::ThreadRegistry::Instance().CurrentThreadData()->allocator();
    auto& data = allocator.allocateExtraObjectData(object, typeInfo);
    if (!compareExchange(object->typeInfoOrMeta_, typeInfo, reinterpret_cast<TypeInfo*>(&data))) {
        // Somebody else created `mm::ExtraObjectData` for this object.
        allocator.destroyUnattachedExtraObjectData(data);
        return *reinterpret_cast<mm::ExtraObjectData*>(typeInfo);
    }

    return data;
}

void mm::ExtraObjectData::UnlinkFromBaseObject() noexcept {
    auto* object = weakReferenceOrBaseObject_.exchange(nullptr);
    RuntimeAssert(
            !hasPointerBits(object, WEAK_REF_TAG), "ExtraObjectData %p has uncleared weak reference %p during unlink", this,
            clearPointerBits(object, WEAK_REF_TAG));
    atomicSetRelease(const_cast<const TypeInfo**>(&object->typeInfoOrMeta_), typeInfo_);
    RuntimeAssert(
            !object->has_meta_object(), "Object %p has metaobject %p after removing metaobject %p", object, object->meta_object_or_null(),
            this);
}

void mm::ExtraObjectData::ReleaseAssociatedObject() noexcept {
#ifdef KONAN_OBJC_INTEROP
    if (void* associatedObject = associatedObject_) {
        Kotlin_ObjCExport_releaseAssociatedObject(associatedObject);
        associatedObject_ = nullptr;
    }
#endif
}

void mm::ExtraObjectData::Uninstall() noexcept {
    UnlinkFromBaseObject();
    ReleaseAssociatedObject();
}

bool mm::ExtraObjectData::HasAssociatedObject() noexcept {
#ifdef KONAN_OBJC_INTEROP
    return associatedObject_ != nullptr;
#else
    return false;
#endif
}

void mm::ExtraObjectData::ClearRegularWeakReferenceImpl() noexcept {
    auto *object = GetBaseObject();
    // Not using `mm::SetHeapRef here`, because this code is called during sweep phase by the GC thread,
    // and so cannot affect marking.
    // TODO: Asserts on the above?
    weakReferenceOrBaseObject_ = object;
}

mm::ExtraObjectData::~ExtraObjectData() {
    auto* weakReference = weakReferenceOrBaseObject_.load(std::memory_order_relaxed);
    if (hasPointerBits(weakReference, WEAK_REF_TAG)) {
        weakReference = clearPointerBits(weakReference, WEAK_REF_TAG);
    } else {
        weakReference = nullptr;
    }
    RuntimeAssert(weakReference == nullptr, "ExtraObjectData %p must have cleared weak reference %p", this, weakReference);

#ifdef KONAN_OBJC_INTEROP
    auto* associatedObject = associatedObject_.load(std::memory_order_relaxed);
    RuntimeAssert(associatedObject == nullptr, "ExtraObjectData %p must have cleared associated object %p", this, associatedObject);
#endif
}
