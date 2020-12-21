/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "KAssert.h"

ALWAYS_INLINE bool isFrozen(const ObjHeader* obj) {
    TODO();
}

ALWAYS_INLINE bool isPermanentOrFrozen(const ObjHeader* obj) {
    TODO();
}

ObjHeader** ObjHeader::GetWeakCounterLocation() {
    TODO();
}

#ifdef KONAN_OBJC_INTEROP

void* ObjHeader::GetAssociatedObject() {
    TODO();
}

void** ObjHeader::GetAssociatedObjectLocation() {
    TODO();
}

void ObjHeader::SetAssociatedObject(void* obj) {
    TODO();
}

#endif // KONAN_OBJC_INTEROP

static MetaObjHeader* createMetaObject(TypeInfo** location) {
    TODO();
}

static void destroyMetaObject(TypeInfo** location) {
    TODO();
}

extern "C" {

OBJ_GETTER(InitThreadLocalSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    TODO();
}

RUNTIME_NOTHROW void SetStackRef(ObjHeader** location, const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void SetHeapRef(ObjHeader** location, const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void ZeroHeapRef(ObjHeader** location) {
    TODO();
}

RUNTIME_NOTHROW void ZeroArrayRefs(ArrayHeader* array) {
    TODO();
}

RUNTIME_NOTHROW void ZeroStackRef(ObjHeader** location) {
    TODO();
}

RUNTIME_NOTHROW void UpdateStackRef(ObjHeader** location, const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void UpdateHeapRefIfNull(ObjHeader** location, const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW OBJ_GETTER(
        SwapHeapRefLocked, ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue, int32_t* spinlock, int32_t* cookie) {
    TODO();
}

RUNTIME_NOTHROW void SetHeapRefLocked(ObjHeader** location, ObjHeader* newValue, int32_t* spinlock, int32_t* cookie) {
    TODO();
}

RUNTIME_NOTHROW OBJ_GETTER(ReadHeapRefLocked, ObjHeader** location, int32_t* spinlock, int32_t* cookie) {
    TODO();
}

RUNTIME_NOTHROW void EnterFrame(ObjHeader** start, int parameters, int count) {
    TODO();
}

RUNTIME_NOTHROW void LeaveFrame(ObjHeader** start, int parameters, int count) {
    TODO();
}

void MutationCheck(ObjHeader* obj) {
    TODO();
}

void FreezeSubgraph(ObjHeader* obj) {
    TODO();
}

void EnsureNeverFrozen(ObjHeader* obj) {
    TODO();
}

RUNTIME_NOTHROW void PerformFullGC(MemoryState* memory) {
    TODO();
}

bool TryAddHeapRef(const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void ReleaseHeapRef(const ObjHeader* object) {
    TODO();
}

RUNTIME_NOTHROW void ReleaseHeapRefNoCollect(const ObjHeader* object) {
    TODO();
}

ForeignRefContext InitLocalForeignRef(ObjHeader* object) {
    TODO();
}

} // extern "C"
