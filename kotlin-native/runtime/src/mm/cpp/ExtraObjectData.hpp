/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_EXTRA_OBJECT_DATA_H
#define RUNTIME_MM_EXTRA_OBJECT_DATA_H

#include <atomic>
#include <cstddef>
#include <cstdint>

#include "Alloc.h"
#include "Memory.h"
#include "TypeInfo.h"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

// Optional data that's lazily allocated only for objects that need it.
class ExtraObjectData : private Pinned, public KonanAllocatorAware {
public:
    enum Flags : uint32_t {
        FLAGS_NONE = 0,
        FLAGS_FROZEN = 1 << 0,
        FLAGS_NEVER_FROZEN = 1 << 1,
    };

    MetaObjHeader* AsMetaObjHeader() noexcept { return reinterpret_cast<MetaObjHeader*>(this); }
    static ExtraObjectData& FromMetaObjHeader(MetaObjHeader* header) noexcept { return *reinterpret_cast<ExtraObjectData*>(header); }

    // Get installed `ExtraObjectData` or `nullptr`.
    static ExtraObjectData* Get(const ObjHeader* object) noexcept {
        return reinterpret_cast<ExtraObjectData*>(object->meta_object_or_null());
    }
    static ExtraObjectData& GetOrInstall(ObjHeader* object) noexcept { return FromMetaObjHeader(object->meta_object()); }

    static ExtraObjectData& Install(ObjHeader* object) noexcept;
    static void Uninstall(ObjHeader* object) noexcept;

#ifdef KONAN_OBJC_INTEROP
    void** GetAssociatedObjectLocation() noexcept { return &associatedObject_; }
#endif

    ObjHeader** GetWeakCounterLocation() noexcept { return &weakReferenceCounter_; }

    std::atomic<Flags>& flags() noexcept { return flags_; }

private:
    explicit ExtraObjectData(const TypeInfo* typeInfo) noexcept : typeInfo_(typeInfo) {}
    ~ExtraObjectData();

    // Must be first to match `TypeInfo` layout.
    const TypeInfo* typeInfo_;

    std::atomic<Flags> flags_ = FLAGS_NONE;

#ifdef KONAN_OBJC_INTEROP
    void* associatedObject_ = nullptr;
#endif

    // TODO: Need to respect when marking.
    ObjHeader* weakReferenceCounter_ = nullptr;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_EXTRA_OBJECT_DATA_H
