/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

ALWAYS_INLINE bool isFrozen(const ObjHeader* obj) {
    RuntimeCheck(false, "Unimplemented");
}

ALWAYS_INLINE bool isPermanentOrFrozen(const ObjHeader* obj) {
    RuntimeCheck(false, "Unimplemented");
}

ALWAYS_INLINE bool isShareable(const ObjHeader* obj) {
    RuntimeCheck(false, "Unimplemented");
}

ObjHeader** ObjHeader::GetWeakCounterLocation() {
    RuntimeCheck(false, "Unimplemented");
}

#ifdef KONAN_OBJC_INTEROP

void* ObjHeader::GetAssociatedObject() {
    RuntimeCheck(false, "Unimplemented");
}

void** ObjHeader::GetAssociatedObjectLocation() {
    RuntimeCheck(false, "Unimplemented");
}

void ObjHeader::SetAssociatedObject(void* obj) {
    RuntimeCheck(false, "Unimplemented");
}

#endif // KONAN_OBJC_INTEROP

static MetaObjHeader* createMetaObject(TypeInfo** location) {
    RuntimeCheck(false, "Unimplemented");
}

static void destroyMetaObject(TypeInfo** location) {
    RuntimeCheck(false, "Unimplemented");
}

extern "C" {

MemoryState* InitMemory() {
    RuntimeCheck(false, "Unimplemented");
}

void DeinitMemory(MemoryState*) {
    RuntimeCheck(false, "Unimplemented");
}

void RestoreMemory(MemoryState* memoryState) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW OBJ_GETTER(AllocInstance, const TypeInfo* type_info) {
    RuntimeCheck(false, "Unimplemented");
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* type_info, int32_t elements) {
    RuntimeCheck(false, "Unimplemented");
}

OBJ_GETTER(InitInstance, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    RuntimeCheck(false, "Unimplemented");
}

OBJ_GETTER(InitSharedInstance, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    RuntimeCheck(false, "Unimplemented");
}

extern const bool IsStrictMemoryModel = true;

RUNTIME_NOTHROW void SetStackRef(ObjHeader** location, const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void SetHeapRef(ObjHeader** location, const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void ZeroHeapRef(ObjHeader** location) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void ZeroArrayRefs(ArrayHeader* array) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void ZeroStackRef(ObjHeader** location) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void UpdateStackRef(ObjHeader** location, const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void UpdateHeapRefIfNull(ObjHeader** location, const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW OBJ_GETTER(
        SwapHeapRefLocked, ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue, int32_t* spinlock, int32_t* cookie) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void SetHeapRefLocked(ObjHeader** location, ObjHeader* newValue, int32_t* spinlock, int32_t* cookie) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW OBJ_GETTER(ReadHeapRefLocked, ObjHeader** location, int32_t* spinlock, int32_t* cookie) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void EnterFrame(ObjHeader** start, int parameters, int count) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void LeaveFrame(ObjHeader** start, int parameters, int count) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW bool ClearSubgraphReferences(ObjHeader* root, bool checked) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void* CreateStablePointer(ObjHeader* obj) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void DisposeStablePointer(void* pointer) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW OBJ_GETTER(DerefStablePointer, void*) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW OBJ_GETTER(AdoptStablePointer, void*) {
    RuntimeCheck(false, "Unimplemented");
}

void MutationCheck(ObjHeader* obj) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void CheckLifetimesConstraint(ObjHeader* obj, ObjHeader* pointee) {
    RuntimeCheck(false, "Unimplemented");
}

void FreezeSubgraph(ObjHeader* obj) {
    RuntimeCheck(false, "Unimplemented");
}

void EnsureNeverFrozen(ObjHeader* obj) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void AddTLSRecord(MemoryState* memory, void** key, int size) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void ClearTLSRecord(MemoryState* memory, void** key) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW ObjHeader** LookupTLS(void** key, int index) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void GC_RegisterWorker(void* worker) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void GC_UnregisterWorker(void* worker) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void GC_CollectorCallback(void* worker) {
    RuntimeCheck(false, "Unimplemented");
}

bool Kotlin_Any_isShareable(ObjHeader* thiz) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void PerformFullGC() {
    RuntimeCheck(false, "Unimplemented");
}

bool TryAddHeapRef(const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void ReleaseHeapRef(const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

RUNTIME_NOTHROW void ReleaseHeapRefNoCollect(const ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

ForeignRefContext InitLocalForeignRef(ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

ForeignRefContext InitForeignRef(ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

void DeinitForeignRef(ObjHeader* object, ForeignRefContext context) {
    RuntimeCheck(false, "Unimplemented");
}

bool IsForeignRefAccessible(ObjHeader* object, ForeignRefContext context) {
    RuntimeCheck(false, "Unimplemented");
}

void AdoptReferenceFromSharedVariable(ObjHeader* object) {
    RuntimeCheck(false, "Unimplemented");
}

} // extern "C"
