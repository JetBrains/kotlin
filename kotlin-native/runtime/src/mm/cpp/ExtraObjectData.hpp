/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_EXTRA_OBJECT_DATA_H
#define RUNTIME_MM_EXTRA_OBJECT_DATA_H

#include <atomic>
#include <cstddef>
#include <cstdint>

#include "Memory.h"
#include "TypeInfo.h"
#include "Utils.hpp"
#include "Weak.hpp"

namespace kotlin {
namespace mm {

// Optional data that's lazily allocated only for objects that need it.
class ExtraObjectData : private Pinned {
public:
    // flags are stored as single atomic uint32, values are bit numbers in that uint32
    enum Flags : uint32_t {
        FLAGS_FROZEN = 0,
        FLAGS_NEVER_FROZEN = 1,
        FLAGS_IN_FINALIZER_QUEUE = 2,
        FLAGS_FINALIZED = 3,
    };

    static constexpr unsigned WEAK_REF_TAG = 1;

    MetaObjHeader* AsMetaObjHeader() noexcept { return reinterpret_cast<MetaObjHeader*>(this); }
    static ExtraObjectData& FromMetaObjHeader(MetaObjHeader* header) noexcept { return *reinterpret_cast<ExtraObjectData*>(header); }

    // Get installed `ExtraObjectData` or `nullptr`.
    static ExtraObjectData* Get(const ObjHeader* object) noexcept {
        return reinterpret_cast<ExtraObjectData*>(object->meta_object_or_null());
    }
    static ExtraObjectData& GetOrInstall(ObjHeader* object) noexcept { return FromMetaObjHeader(object->meta_object()); }

    static ExtraObjectData& Install(ObjHeader* object) noexcept;
    void Uninstall() noexcept;

#ifdef KONAN_OBJC_INTEROP
    std::atomic<void*>& AssociatedObject() noexcept { return associatedObject_; }
#endif
    bool HasAssociatedObject() noexcept;

    bool getFlag(Flags value) noexcept { return (flags_.load() & (1u << static_cast<uint32_t>(value))) != 0; }
    void setFlag(Flags value) noexcept { flags_.fetch_or(1u << static_cast<uint32_t>(value)); }

    bool HasRegularWeakReferenceImpl() noexcept { return hasPointerBits(weakReferenceOrBaseObject_.load(), WEAK_REF_TAG); }
    void ClearRegularWeakReferenceImpl() noexcept; // TODO: Only exists for the sake of GetBaseObject. Refactor to remove the need for it.
    ObjHeader* GetRegularWeakReferenceImpl() noexcept {
        auto* pointer = weakReferenceOrBaseObject_.load();
        if (hasPointerBits(pointer, WEAK_REF_TAG)) return clearPointerBits(pointer, WEAK_REF_TAG);
        return nullptr;
    }
    ObjHeader* GetOrSetRegularWeakReferenceImpl(ObjHeader* object, ObjHeader* weakRef) noexcept {
        if (weakReferenceOrBaseObject_.compare_exchange_strong(object, setPointerBits(weakRef, WEAK_REF_TAG))) {
            return weakRef;
        } else {
            return clearPointerBits(object, WEAK_REF_TAG); // on fail current value of weakRef is stored to object
        }
    }
    ObjHeader* GetBaseObject() noexcept {
        auto* header = weakReferenceOrBaseObject_.load();
        if (hasPointerBits(header, WEAK_REF_TAG)) {
            return regularWeakReferenceImplBaseObjectUnsafe(clearPointerBits(header, WEAK_REF_TAG));
        } else {
            return header;
        }
    }

    // info must be equal to objHeader->type_info(), but it needs to be loaded in advance to avoid data races
    explicit ExtraObjectData(ObjHeader* objHeader, const TypeInfo* info) noexcept :
        typeInfo_(nullptr), weakReferenceOrBaseObject_(objHeader) {
        atomicSetRelease(&typeInfo_, info);
    }
    ~ExtraObjectData();
private:
    // Must be first to match `TypeInfo` layout.
    const TypeInfo* typeInfo_;

    std::atomic<uint32_t> flags_ = 0;

#ifdef KONAN_OBJC_INTEROP
    std::atomic<void*> associatedObject_ = nullptr;
#endif

    std::atomic<ObjHeader*> weakReferenceOrBaseObject_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_EXTRA_OBJECT_DATA_H
