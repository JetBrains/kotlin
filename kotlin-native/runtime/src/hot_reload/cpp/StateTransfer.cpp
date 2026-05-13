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

#include <vector>
#include <queue>
#include <unordered_set>
#include <unordered_map>
#include <string>
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

template <typename F>
void visitObjectGraph(ObjHeader* startObject, F processingFunction) {
    // We need to perform a BFS, while ensuring that the world is stopped.
    // Let's collect the root set, and start the graph exploration.
    // At the moment, let's make things simple, and single-threaded (otherwise, well, headaches).
    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    auto processObject = [&](ObjHeader* obj, utility::ReferenceOrigin origin) {
        // const char* originString = originToString(origin);
        if (obj == nullptr || isNullOrMarker(obj)) return;

        // HRLogDebug("processing object of type %s from %s", obj->type_info()->fqName().c_str(), originString);
        if (const auto visited = visitedObjects.find(obj); visited != visitedObjects.end()) return;

        visitedObjects.insert(obj);
        objectsToVisit.push(obj);
    };

    // Let's start collecting the root set
    for (auto& thread : mm::ThreadRegistry::Instance().LockForIter()) {
        auto& shadowStack = thread.shadowStack();
        for (const auto& object : shadowStack) {
            processObject(object, utility::ReferenceOrigin::ShadowStack);
        }
    }

    for (const auto& objRef : kotlin::mm::GlobalData::Instance().globalsRegistry().LockForIter()) {
        if (objRef != nullptr) {
            processObject(*objRef, utility::ReferenceOrigin::Global);
        }
    }

    processObject(startObject, utility::ReferenceOrigin::ObjRef);

    HRLogDebug("Starting object graph visit with %zu nodes", objectsToVisit.size());

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();
        processingFunction(nextObject, processObject);
    }
}

}

StateTransferMap CreateStateTransferMap(const TypeInfo* oldTypeInfo, const TypeInfo* newTypeInfo) {

    const auto oldExt = oldTypeInfo->extendedInfo_;
    const auto newExt = newTypeInfo->extendedInfo_;

    struct OldFieldLayout {
        int32_t offset;
        uint8_t type;
    };

    std::unordered_map<std::string_view, OldFieldLayout> oldFieldLayouts;
    for (auto i = 0; i < oldExt->fieldsCount_; i++) {
        const char* fieldName = oldExt->fieldNames_[i];
        const auto fieldType = oldExt->fieldTypes_[i];
        const auto fieldOffset = oldExt->fieldOffsets_[i];
        oldFieldLayouts.try_emplace(std::string_view{fieldName}, OldFieldLayout{fieldOffset, fieldType});
    }

    std::vector<FieldMapping> fieldMappings;
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

std::vector<ObjHeader*> FindObjectsToReload(const TypeInfo* oldTypeInfo) {
    std::vector<ObjHeader*> existingObjects{};
    const std::string oldTypeFqName = oldTypeInfo->fqName();

    visitObjectGraph(nullptr, [&existingObjects, &oldTypeFqName](ObjHeader* nextObject, auto processObject) {
        // Traverse object references inside class properties
        traverseObjectFieldsInternal(nextObject, [&](const mm::RefFieldAccessor& fieldAccessor) {
            processObject(fieldAccessor.direct(), utility::ReferenceOrigin::ObjRef);
        });

        if (nextObject->type_info()->fqName() == oldTypeFqName) {
            //HRLogDebug("Instance of class '%s' at '%p', must be reloaded", nextObject->type_info()->fqName().c_str(), nextObject);
            existingObjects.emplace_back(nextObject);
        }
    });
    return existingObjects;
}

void RewriteAllReferencesTo(ObjHeader* oldObject, ObjHeader* newObject) {
    // TODO: eventually, those two functions should be merged into one
    UpdateShadowStackReferences(oldObject, newObject);
    UpdateHeapReferences(oldObject, newObject);
}

void UpdateHeapReferences(ObjHeader* oldObject, ObjHeader* newObject) {
    int32_t updatedObjects{0};

    std::queue<ObjHeader*> objectsToVisit{};
    std::unordered_set<ObjHeader*> visitedObjects{};

    auto processObject = [&](ObjHeader* obj, const utility::ReferenceOrigin origin) {
        // const char* originString = utility::referenceOriginToString(origin);
        if (obj == nullptr || isNullOrMarker(obj)) return;

        // When the object has been already colored during the visit, do not insert it again.
        if (const auto visited = visitedObjects.find(obj); visited != visitedObjects.end()) return;
        //HRLogDebug("processing object of type '%s' from %s", obj->type_info()->fqName().c_str(), originString);

        visitedObjects.insert(obj);
        objectsToVisit.push(obj);
    };

    auto threadRegistry = mm::ThreadRegistry::Instance().LockForIter();
    for (auto& thread : threadRegistry) {
        for (const auto& objectLocation : thread.tls()) {
            if (*objectLocation == oldObject) {
                *objectLocation = newObject;
                UpdateHeapRef(objectLocation, newObject);
                updatedObjects++;
            }
            processObject(*objectLocation, utility::ReferenceOrigin::Global);
        }
    }

    auto globalsIterable = mm::GlobalData::Instance().globalsRegistry().LockForIter();
    for (const auto& objectLocation : globalsIterable) {
        if (*objectLocation == oldObject) {
            *objectLocation = newObject;
            UpdateHeapRef(objectLocation, newObject);
            updatedObjects++;
        }

        processObject(*objectLocation, utility::ReferenceOrigin::Global);
    }

    for (auto& thread : mm::ThreadRegistry::Instance().LockForIter()) {
        auto& shadowStack = thread.shadowStack();
        for (const auto& object : shadowStack) {
            processObject(object, utility::ReferenceOrigin::ShadowStack);
        }
    }

    processObject(oldObject, utility::ReferenceOrigin::ObjRef);

    HRLogDebug("Updating Heap References :: starting visit with %zu objects", objectsToVisit.size());

    while (!objectsToVisit.empty()) {
        const auto nextObject = objectsToVisit.front();
        objectsToVisit.pop();

        traverseObjectFields(nextObject, [&](mm::RefFieldAccessor fieldAccessor) {
            ObjHeader* fieldValue = fieldAccessor.direct();
            if (fieldValue == oldObject) {
                // Perform the actual reference update
                fieldAccessor.store(newObject);
                updatedObjects++;
            }
            // `fieldValue` is still pointing to the old object, we need to explore its fields too
            processObject(fieldValue, utility::ReferenceOrigin::ObjRef);
        });
    }
}

void UpdateShadowStackReferences(const ObjHeader* oldObject, ObjHeader* newObject) {
    for (auto& threadData : mm::ThreadRegistry::Instance().LockForIter()) {
        mm::ShadowStack& shadowStack = threadData.shadowStack();
        for (auto it = shadowStack.begin(); it != shadowStack.end(); ++it) {
            if (ObjHeader*& currentRef = *it; currentRef == oldObject) {
                currentRef = newObject;
            }
        }
    }
}

}