/**
* Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#ifndef STATETRANSFER_HPP
#define STATETRANSFER_HPP

#include "Memory.h"
#include "Natives.h"

#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/DenseMap.h"
#include "llvm/ADT/DenseSet.h"
#include "llvm/ADT/ArrayRef.h"
#include "llvm/ADT/StringMap.h"

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
    llvm::DenseMap<const TypeInfo*, llvm::SmallVector<ObjHeader*, 8>> instancesByType;
    llvm::SmallVector<ObjHeader*, 0> liveObjects;
};

struct StateTransferMap {
    llvm::SmallVector<FieldMapping, 8> fieldMappings;
};

HeapWalk WalkHeapAndBucket(const llvm::DenseSet<const TypeInfo*>& typeFilter);

StateTransferMap CreateStateTransferMap(const TypeInfo* oldTypeInfo, const TypeInfo* newTypeInfo);

void PerformStateTransfer(ObjHeader* oldObject, ObjHeader* newObject, const StateTransferMap& transferMap);

void RewriteAllReferences(const llvm::DenseMap<ObjHeader*, ObjHeader*>& remap, llvm::ArrayRef<ObjHeader*> liveObjects);

}

#endif