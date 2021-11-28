/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FinalizerHooks.hpp"

#include "Cleaner.h"
#include "Memory.h"
#include "Types.h"
#include "WorkerBoundReference.h"

using namespace kotlin;

namespace {

void (*g_hookOverrideForTesting)(ObjHeader*) = nullptr;

// Not inlining this call as it affects deallocation performance for
// all types.
NO_INLINE void RunFinalizerHooksImpl(ObjHeader* object, const TypeInfo* type) noexcept {
    if (g_hookOverrideForTesting != nullptr) {
        g_hookOverrideForTesting(object);
        return;
    }
    // TODO: Consider some global registration.
    if (type == theCleanerImplTypeInfo) {
        DisposeCleaner(object);
    } else if (type == theWorkerBoundReferenceTypeInfo) {
        DisposeWorkerBoundReference(object);
    }
}

} // namespace

ALWAYS_INLINE bool kotlin::HasFinalizers(ObjHeader* object) noexcept {
    return object->has_meta_object() || (object->type_info()->flags_ & TF_HAS_FINALIZER) != 0;
}

ALWAYS_INLINE void kotlin::RunFinalizers(ObjHeader* object) noexcept {
    auto* type = object->type_info();
    if ((type->flags_ & TF_HAS_FINALIZER) != 0) {
        // This is a cold path.
        RunFinalizerHooksImpl(object, type);
    }
    if (object->has_meta_object()) {
        ObjHeader::destroyMetaObject(object);
    }
}

void kotlin::SetFinalizerHookForTesting(void (*hook)(ObjHeader*)) noexcept {
    g_hookOverrideForTesting = hook;
}
