/**
* Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#ifndef STATETRANSFER_HPP
#define STATETRANSFER_HPP

#include <vector>

#include "Memory.h"
#include "Natives.h"

namespace kotlin::hot::state {

struct FieldMapping {
    int32_t oldOffset;
    int32_t newOffset;
    Konan_RuntimeType type;
    uint8_t size;

    FieldMapping(const int32_t oldOffset_, const int32_t newOffset_, const Konan_RuntimeType type_, const uint8_t size_) :
        oldOffset(oldOffset_), newOffset(newOffset_), type(type_), size(size_) {}
};

struct StateTransferMap {
    std::vector<FieldMapping> fieldMappings;
};

StateTransferMap CreateStateTransferMap(const TypeInfo* oldTypeInfo, const TypeInfo* newTypeInfo);

void PerformStateTransfer(ObjHeader* oldObject, ObjHeader* newObject, const StateTransferMap& transferMap);

/// Returns a vector of objects that need to be reloaded due to type change
std::vector<ObjHeader*> FindObjectsToReload(const TypeInfo* oldTypeInfo);

/// Walks all root sets (shadow stacks, TLS, GlobalsRegistry) and the heap reachable
/// from them, rewriting every reference to `oldObject` to point at `newObject`.
void RewriteAllReferencesTo(ObjHeader* oldObject, ObjHeader* newObject);

void UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject);

void UpdateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject);

}

#endif