/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <cstdint>

#include "Common.h"
#include "CompilerConstants.hpp"
#include "KAssert.h"

struct ObjHeader;

extern "C" uintptr_t Kotlin_mm_compressedRefsBase;

namespace kotlin::mm {

// Objects are 8-byte aligned, so 32-bit offsets address a 32 GB window.
inline constexpr unsigned kCompressedRefsShift = 3;
inline constexpr uint64_t kCompressedRefsWindowSize = uint64_t{1} << (32 + kCompressedRefsShift);

// A reference slot inside an object: a field or an element of an object array. With compressed references
// it holds a 32-bit offset from the window base instead of a full pointer, so it is only accessed through
// the accessors and never dereferenced directly.
struct HeapRefSlot;

ALWAYS_INLINE inline size_t heapRefSlotSize() noexcept {
    return compiler::compressedRefs() ? sizeof(uint32_t) : sizeof(ObjHeader*);
}

ALWAYS_INLINE inline uintptr_t compressedRefsBase() noexcept {
    return Kotlin_mm_compressedRefsBase;
}

ALWAYS_INLINE inline uint32_t compressRef(ObjHeader* object) noexcept {
    if (object == nullptr) return 0;
    auto offset = reinterpret_cast<uintptr_t>(object) - compressedRefsBase();
    RuntimeAssert(offset < kCompressedRefsWindowSize, "%p is outside of the compressed references window", object);
    return static_cast<uint32_t>(offset >> kCompressedRefsShift);
}

ALWAYS_INLINE inline ObjHeader* decompressRef(uint32_t offset) noexcept {
    if (offset == 0) return nullptr;
    return reinterpret_cast<ObjHeader*>(compressedRefsBase() + (static_cast<uintptr_t>(offset) << kCompressedRefsShift));
}

}
