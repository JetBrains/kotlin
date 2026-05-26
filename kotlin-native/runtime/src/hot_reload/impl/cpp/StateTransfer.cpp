/*
* Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#include "StateTransfer.hpp"

#include "ReferenceOps.hpp"
#include "ThreadRegistry.hpp"
#include "ShadowStack.hpp"
#include "ThreadData.hpp"
#include "RootSet.hpp"
#include "ObjectTraversal.hpp"
#include "HotReloadUtility.hpp"

#include <cinttypes>
#include <vector>
#include <cstring>

namespace kotlin::hot::state {

namespace {
template <typename F>
void traverseObjectFieldsInternal(ObjHeader* object, F process) noexcept(noexcept(process(std::declval<mm::RefFieldAccessor>()))) {
    const TypeInfo* typeInfo = object->type_info();
    // Only consider arrays of objects, not arrays of primitives.
    if (typeInfo != theArrayTypeInfo) {
        for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
            auto fieldPtr = reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(object) + typeInfo->objOffsets_[index]);
            process(mm::RefFieldAccessor(fieldPtr));
        }
    } else {
        ArrayHeader* array = object->array();
        for (uint32_t index = 0; index < array->count_; index++) {
            process(mm::RefFieldAccessor(ArrayAddressOfElementAt(array, index)));
        }
    }
}
}

HeapWalk WalkHeapAndBucket(const llvm::DenseSet<const TypeInfo*>& typeFilter) {
    llvm::DenseMap<const TypeInfo*, llvm::SmallVector<ObjHeader*, 8>> objectsToReload;

    llvm::SmallVector<ObjHeader*, 64> workList;
    llvm::SmallVector<ObjHeader*, 0> liveObjects;
    llvm::DenseSet<ObjHeader*> visited;

    auto enqueue = [&](ObjHeader* obj) {
        if (obj == nullptr || isNullOrMarker(obj)) return;
        if (!visited.insert(obj).second) return;  // already visited
        workList.push_back(obj);
        liveObjects.push_back(obj);
    };

    // So, Kotlin/Native runtime has three roots: Global, TLS and Shadow Stack
    // Let's start collecting the root set

    // Collect roots from global variables
    auto globalsRegistry = mm::GlobalData::Instance().globalsRegistry().LockForIter();
    for (const auto& slot : globalsRegistry) {
        if (slot == nullptr) continue;
        enqueue(*slot);
    }

    auto threadRegistry = mm::ThreadRegistry::Instance().LockForIter();
    for (auto& thread : threadRegistry) {
        for (const auto& slot : thread.tls()) {
            if (slot == nullptr) continue;
            enqueue(*slot);
        }
        for (const auto& object : thread.shadowStack()) {
            enqueue(object);
        }
    }

    while (!workList.empty()) {
        const auto nextObject = workList.pop_back_val();
        const auto objTypeInfo = nextObject->type_info();

        if (typeFilter.contains(objTypeInfo)) {
            objectsToReload[objTypeInfo].push_back(nextObject);
        }

        traverseObjectFieldsInternal(nextObject, [&](const mm::RefFieldAccessor& fieldAccessor) {
            enqueue(fieldAccessor.direct());
        });
    }

    return {std::move(objectsToReload), std::move(liveObjects)};
}

StateTransferMap CreateStateTransferMap(const TypeInfo* oldTypeInfo, const TypeInfo* newTypeInfo) {

    const auto oldExt = oldTypeInfo->extendedInfo_;
    const auto newExt = newTypeInfo->extendedInfo_;

    struct OldFieldLayout {
        int32_t offset;
        uint8_t type;
    };

    llvm::StringMap<OldFieldLayout> oldFieldLayouts;
    for (auto i = 0; i < oldExt->fieldsCount_; i++) {
        const char* fieldName = oldExt->fieldNames_[i];
        const auto fieldType = oldExt->fieldTypes_[i];
        const auto fieldOffset = oldExt->fieldOffsets_[i];
        oldFieldLayouts.try_emplace(llvm::StringRef{fieldName}, OldFieldLayout{fieldOffset, fieldType});
    }

    llvm::SmallVector<FieldMapping, 8> fieldMappings;
    fieldMappings.reserve(newExt->fieldsCount_); // upper bound

    for (auto i = 0; i < newExt->fieldsCount_; i++) {
        const char* fieldName = newExt->fieldNames_[i];
        const auto newType = newExt->fieldTypes_[i];
        const auto newOffset = newExt->fieldOffsets_[i];

        auto oldLayout = oldFieldLayouts.find(std::string_view{fieldName});
        if (oldLayout == oldFieldLayouts.end()) continue; // this is a new field

        const auto [oldOffset, oldType] = oldLayout->second;
        if (oldType != newType) continue; // type mismatch

        RuntimeAssert(newType != RT_INVALID, "field %s has invalid runtime type", fieldName);
        fieldMappings.push_back(FieldMapping(oldOffset, newOffset, static_cast<Konan_RuntimeType>(newType), utility::kRuntimeTypeSize[newType]));
    }

    return StateTransferMap{std::move(fieldMappings)};
}

void PerformStateTransfer(ObjHeader* oldObject, ObjHeader* newObject, const StateTransferMap& transferMap) {

    for (auto& fieldMapping : transferMap.fieldMappings) {

        const auto oldFieldData = reinterpret_cast<uint8_t*>(oldObject) + fieldMapping.oldOffset;
        const auto newFieldData = reinterpret_cast<uint8_t*>(newObject) + fieldMapping.newOffset;

        if (fieldMapping.type == RT_OBJECT) {

            // Handle Kotlin Objects in a different way, the updates must be notified to the GC
            // TODO: investigate null discussion
            auto** oldRef = reinterpret_cast<ObjHeader**>(oldFieldData);
            auto** newRef = reinterpret_cast<ObjHeader**>(newFieldData);

            UpdateHeapRef(newRef, *oldRef);
            *newRef = *oldRef; // Just copy the reference to the previous object

            HRLogWarning("EXPERIMENTAL: Object reference updated from '%p' to '%p'.", *oldRef, *newRef);
            HRLogWarning("EXPERIMENTAL: For the current milestone, object reference type check is omitted.");
        } else {
            // Perform byte-copy of the field
            std::memcpy(newFieldData, oldFieldData, fieldMapping.size);
        }
    }
}

void RewriteAllReferences(const llvm::DenseMap<ObjHeader*, ObjHeader*>& remap, llvm::ArrayRef<ObjHeader*> liveObjects) {

    auto remapField = [&](ObjHeader* current) -> ObjHeader* {
        if (auto it = remap.find(current); it != remap.end()) return it->second;
        return nullptr;
    };

    for (auto* obj : liveObjects) {
        traverseObjectFields(obj, [&](mm::RefFieldAccessor field) {
            if (auto* replacement = remapField(field.direct())) {
                field.store(replacement);
            }
        });
    }

    auto globalsRegistry = mm::GlobalData::Instance().globalsRegistry().LockForIter();
    for (auto* slot : globalsRegistry) {
        if (slot == nullptr) continue;
        if (const auto* replacement = remapField(*slot)) {
            UpdateHeapRef(slot, replacement);
        }
    }

    auto threadRegistry = mm::ThreadRegistry::Instance().LockForIter();
    for (auto& thread : threadRegistry) {
        for (auto* slot : thread.tls()) {
            if (slot == nullptr) continue;
            if (const auto* replacement = remapField(*slot)) {
                UpdateHeapRef(slot, replacement);
            }
        }
        for (auto& stackRef : thread.shadowStack()) {
            if (const auto* replacement = remapField(stackRef)) {
                UpdateStackRef(&stackRef, replacement);
            }
        }
    }
}
}
