/*
* Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/
#pragma once

#include "Memory.h"
#include "Natives.h"
#include "GlobalData.hpp"

namespace kotlin::hot::state {

ObjHeader* PerformStateTransfer(mm::ThreadData& currentThreadData, ObjHeader* oldObject, const TypeInfo* newTypeInfo);

/// Returns a vector of objects that need to be reloaded due to type change
std::vector<ObjHeader*> FindObjectsToReload(const TypeInfo* oldTypeInfo);

/// Walks all root sets (shadow stacks, TLS, GlobalsRegistry) and the heap reachable
/// from them, rewriting every reference to `oldObject` to point at `newObject`.
void RewriteAllReferencesTo(ObjHeader* oldObject, ObjHeader* newObject);

void UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject);

void UpdateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject);

}

