/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FreezeHooks.hpp"

#include "Memory.h"
#include "Types.h"
#include "WorkerBoundReference.h"

using namespace kotlin;

namespace {

void (*g_hookOverrideForTesting)(ObjHeader*) = nullptr;

NO_INLINE void RunFreezeHooksImpl(ObjHeader* object, const TypeInfo* type) noexcept {
    if (g_hookOverrideForTesting != nullptr) {
        g_hookOverrideForTesting(object);
        return;
    }
    // TODO: Consider some global registration.
    if (type == theWorkerBoundReferenceTypeInfo) {
        WorkerBoundReferenceFreezeHook(object);
    }
}

} // namespace

void kotlin::RunFreezeHooks(ObjHeader* object) noexcept {
    auto* type = object->type_info();
    if ((type->flags_ & TF_HAS_FREEZE_HOOK) == 0) {
        return;
    }
    // This is a cold path.
    RunFreezeHooksImpl(object, type);
}

void kotlin::SetFreezeHookForTesting(void (*hook)(ObjHeader*)) noexcept {
    g_hookOverrideForTesting = hook;
}
