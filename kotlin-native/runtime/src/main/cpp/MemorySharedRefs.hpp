/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYSHAREDREFS_HPP
#define RUNTIME_MEMORYSHAREDREFS_HPP

#include <type_traits>

#include "ExternalRCRef.hpp"
#include "Memory.h"
#include "RawPtr.hpp"

// Used exclusively in BlockPointerSupport.kt
struct KRefSharedHolder {
   ObjHeader* obj_;
   kotlin::raw_ptr<kotlin::mm::RawExternalRCRef> ref_;
};

static_assert(std::is_trivially_destructible_v<KRefSharedHolder>, "KRefSharedHolder destructor is not guaranteed to be called.");

extern "C" {
RUNTIME_NOTHROW void KRefSharedHolder_initLocal(KRefSharedHolder* holder, ObjHeader* obj);
RUNTIME_NOTHROW void KRefSharedHolder_init(KRefSharedHolder* holder, ObjHeader* obj);
RUNTIME_NOTHROW void KRefSharedHolder_dispose(KRefSharedHolder* holder);
RUNTIME_NOTHROW ObjHeader* KRefSharedHolder_ref(const KRefSharedHolder* holder);
} // extern "C"

#endif // RUNTIME_MEMORYSHAREDREFS_HPP
