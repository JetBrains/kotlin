/**
* Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#ifndef STATETRANSFER_HPP
#define STATETRANSFER_HPP

#include "Memory.h"
#include "Natives.h"

#include <vector>
#include <unordered_map>
#include <unordered_set>

namespace kotlin::hot::state {

struct FieldMapping {
    int32_t oldOffset;
    int32_t newOffset;
    Konan_RuntimeType type;
    uint8_t size;

    FieldMapping(const int32_t oldOffset_, const int32_t newOffset_, const Konan_RuntimeType type_, const uint8_t size_) :
        oldOffset(oldOffset_), newOffset(newOffset_), type(type_), size(size_) {}
};

struct HeapWalk {
    std::unordered_map<const TypeInfo*, std::vector<ObjHeader*>> instancesByType{};
    std::vector<ObjHeader*> liveObjects{};
};

struct StateTransferMap {
    std::vector<FieldMapping> fieldMappings{};
};

HeapWalk WalkHeapAndBucket(const std::unordered_set<const TypeInfo*>& typeFilter);

StateTransferMap CreateStateTransferMap(const TypeInfo* oldTypeInfo, const TypeInfo* newTypeInfo);

void PerformStateTransfer(ObjHeader* oldObject, ObjHeader* newObject, const StateTransferMap& transferMap);

void RewriteAllReferences(const std::unordered_map<ObjHeader*, ObjHeader*>& remap, const std::vector<ObjHeader*>& liveObjects);

}

#endif