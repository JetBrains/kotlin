/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "Exceptions.h"
#include "ExtraObjectData.hpp"
#include "GlobalsRegistry.hpp"
#include "InitializationScheme.hpp"
#include "KAssert.h"
#include "Natives.h"
#include "Porting.h"
#include "ObjectOps.hpp"
#include "StableRefRegistry.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

using namespace kotlin;

// Delete all means of creating this type directly as it only serves
// as a typedef for `mm::ThreadRegistry::Node`.
extern "C" struct MemoryState : Pinned {
    MemoryState() = delete;
    ~MemoryState() = delete;
};

// TODO: This name does not make sense anymore.
// Delete all means of creating this type directly as it only serves
// as a typedef for `mm::StableRefRegistry::Node`.
class ForeignRefManager : Pinned {
public:
    ForeignRefManager() = delete;
    ~ForeignRefManager() = delete;
};

namespace {

// `reinterpret_cast` to it and back to the same type
// will yield precisely the same pointer, so it's safe.
ALWAYS_INLINE MemoryState* ToMemoryState(mm::ThreadRegistry::Node* data) {
    return reinterpret_cast<MemoryState*>(data);
}

ALWAYS_INLINE mm::ThreadRegistry::Node* FromMemoryState(MemoryState* state) {
    return reinterpret_cast<mm::ThreadRegistry::Node*>(state);
}

ALWAYS_INLINE ForeignRefManager* ToForeignRefManager(mm::StableRefRegistry::Node* data) {
    return reinterpret_cast<ForeignRefManager*>(data);
}

ALWAYS_INLINE mm::StableRefRegistry::Node* FromForeignRefManager(ForeignRefManager* manager) {
    return reinterpret_cast<mm::StableRefRegistry::Node*>(manager);
}

ALWAYS_INLINE mm::ThreadData* GetThreadData(MemoryState* state) {
    return FromMemoryState(state)->Get();
}

} // namespace

ObjHeader** ObjHeader::GetWeakCounterLocation() {
    return mm::ExtraObjectData::FromMetaObjHeader(this->meta_object()).GetWeakCounterLocation();
}

#ifdef KONAN_OBJC_INTEROP

void* ObjHeader::GetAssociatedObject() {
    if (!has_meta_object()) {
        return nullptr;
    }
    return *GetAssociatedObjectLocation();
}

void** ObjHeader::GetAssociatedObjectLocation() {
    return mm::ExtraObjectData::FromMetaObjHeader(this->meta_object()).GetAssociatedObjectLocation();
}

void ObjHeader::SetAssociatedObject(void* obj) {
    *GetAssociatedObjectLocation() = obj;
}

#endif // KONAN_OBJC_INTEROP

// static
MetaObjHeader* ObjHeader::createMetaObject(ObjHeader* object) {
    return mm::ExtraObjectData::Install(object).AsMetaObjHeader();
}

// static
void ObjHeader::destroyMetaObject(ObjHeader* object) {
    mm::ExtraObjectData::Uninstall(object);
}

ALWAYS_INLINE bool isPermanentOrFrozen(const ObjHeader* obj) {
    return obj->permanent() || isFrozen(obj);
}

ALWAYS_INLINE bool isShareable(const ObjHeader* obj) {
    // TODO: Remove when legacy MM is gone.
    return true;
}

extern "C" MemoryState* InitMemory(bool firstRuntime) {
    return ToMemoryState(mm::ThreadRegistry::Instance().RegisterCurrentThread());
}

extern "C" void DeinitMemory(MemoryState* state, bool destroyRuntime) {
    mm::ThreadRegistry::Instance().Unregister(FromMemoryState(state));
}

extern "C" void RestoreMemory(MemoryState*) {
    // TODO: Remove when legacy MM is gone.
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(AllocInstance, const TypeInfo* typeInfo) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    RETURN_RESULT_OF(mm::AllocateObject, threadData, typeInfo);
}

extern "C" OBJ_GETTER(AllocArrayInstance, const TypeInfo* typeInfo, int32_t elements) {
    if (elements < 0) {
        ThrowIllegalArgumentException();
    }
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    RETURN_RESULT_OF(mm::AllocateArray, threadData, typeInfo, static_cast<uint32_t>(elements));
}

extern "C" ALWAYS_INLINE OBJ_GETTER(InitThreadLocalSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();

    RETURN_RESULT_OF(mm::InitThreadLocalSingleton, threadData, location, typeInfo, ctor);
}

extern "C" ALWAYS_INLINE OBJ_GETTER(InitSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();

    RETURN_RESULT_OF(mm::InitSingleton, threadData, location, typeInfo, ctor);
}

extern "C" RUNTIME_NOTHROW void InitAndRegisterGlobal(ObjHeader** location, const ObjHeader* initialValue) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(threadData, location);
    // Null `initialValue` means that the appropriate value was already set by static initialization.
    if (initialValue != nullptr) {
        mm::SetHeapRef(location, const_cast<ObjHeader*>(initialValue));
    }
}

extern "C" const MemoryModel CurrentMemoryModel = MemoryModel::kExperimental;

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void SetStackRef(ObjHeader** location, const ObjHeader* object) {
    mm::SetStackRef(location, const_cast<ObjHeader*>(object));
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void SetHeapRef(ObjHeader** location, const ObjHeader* object) {
    mm::SetHeapRef(location, const_cast<ObjHeader*>(object));
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void ZeroHeapRef(ObjHeader** location) {
    mm::SetHeapRef(location, nullptr);
}

extern "C" RUNTIME_NOTHROW void ZeroArrayRefs(ArrayHeader* array) {
    for (uint32_t index = 0; index < array->count_; ++index) {
        ObjHeader** location = ArrayAddressOfElementAt(array, index);
        mm::SetHeapRef(location, nullptr);
    }
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void ZeroStackRef(ObjHeader** location) {
    mm::SetStackRef(location, nullptr);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void UpdateStackRef(ObjHeader** location, const ObjHeader* object) {
    mm::SetStackRef(location, const_cast<ObjHeader*>(object));
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) {
    mm::SetHeapRef(location, const_cast<ObjHeader*>(object));
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void UpdateHeapRefIfNull(ObjHeader** location, const ObjHeader* object) {
    if (object == nullptr) return;
    ObjHeader* result = nullptr; // No need to store this value in a rootset.
    mm::CompareAndSwapHeapRef(location, nullptr, const_cast<ObjHeader*>(object), &result);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
    mm::SetStackRef(returnSlot, const_cast<ObjHeader*>(object));
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW OBJ_GETTER(
        SwapHeapRefLocked, ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue, int32_t* spinlock, int32_t* cookie) {
    RETURN_RESULT_OF(mm::CompareAndSwapHeapRef, location, expectedValue, newValue);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void SetHeapRefLocked(
        ObjHeader** location, ObjHeader* newValue, int32_t* spinlock, int32_t* cookie) {
    mm::SetHeapRefAtomic(location, newValue);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW OBJ_GETTER(ReadHeapRefLocked, ObjHeader** location, int32_t* spinlock, int32_t* cookie) {
    RETURN_RESULT_OF(mm::ReadHeapRefAtomic, location);
}

extern "C" OBJ_GETTER(ReadHeapRefNoLock, ObjHeader* object, int32_t index) {
    // TODO: Remove when legacy MM is gone.
    ThrowNotImplementedError();
}

extern "C" RUNTIME_NOTHROW void EnterFrame(ObjHeader** start, int parameters, int count) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->shadowStack().EnterFrame(start, parameters, count);
}

extern "C" RUNTIME_NOTHROW void LeaveFrame(ObjHeader** start, int parameters, int count) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->shadowStack().LeaveFrame(start, parameters, count);
}

extern "C" RUNTIME_NOTHROW void AddTLSRecord(MemoryState* memory, void** key, int size) {
    GetThreadData(memory)->tls().AddRecord(key, size);
}

extern "C" RUNTIME_NOTHROW void CommitTLSStorage(MemoryState* memory) {
    GetThreadData(memory)->tls().Commit();
}

extern "C" RUNTIME_NOTHROW void ClearTLS(MemoryState* memory) {
    GetThreadData(memory)->tls().Clear();
}

extern "C" RUNTIME_NOTHROW ObjHeader** LookupTLS(void** key, int index) {
    return mm::ThreadRegistry::Instance().CurrentThreadData()->tls().Lookup(key, index);
}

extern "C" RUNTIME_NOTHROW void GC_RegisterWorker(void* worker) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do
}

extern "C" RUNTIME_NOTHROW void GC_UnregisterWorker(void* worker) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do
}

extern "C" RUNTIME_NOTHROW void GC_CollectorCallback(void* worker) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do
}

extern "C" void Kotlin_native_internal_GC_collect(ObjHeader*) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->gc().PerformFullGC();
}

extern "C" void Kotlin_native_internal_GC_collectCyclic(ObjHeader*) {
    // TODO: Remove when legacy MM is gone.
    ThrowIllegalArgumentException();
}

extern "C" void Kotlin_native_internal_GC_setThreshold(ObjHeader*, int32_t value) {
    if (value < 0) {
        ThrowIllegalArgumentException();
    }
    mm::GlobalData::Instance().gc().SetThreshold(static_cast<size_t>(value));
}

extern "C" int32_t Kotlin_native_internal_GC_getThreshold(ObjHeader*) {
    auto threshold = mm::GlobalData::Instance().gc().GetThreshold();
    auto maxValue = std::numeric_limits<int32_t>::max();
    if (threshold > static_cast<size_t>(maxValue)) {
        return maxValue;
    }
    return static_cast<int32_t>(maxValue);
}

extern "C" void Kotlin_native_internal_GC_setCollectCyclesThreshold(ObjHeader*, int64_t value) {
    // TODO: Remove when legacy MM is gone.
    ThrowIllegalArgumentException();
}

extern "C" int64_t Kotlin_native_internal_GC_getCollectCyclesThreshold(ObjHeader*) {
    // TODO: Remove when legacy MM is gone.
    ThrowIllegalArgumentException();
}

extern "C" void Kotlin_native_internal_GC_setThresholdAllocations(ObjHeader*, int64_t value) {
    if (value < 0) {
        ThrowIllegalArgumentException();
    }
    mm::GlobalData::Instance().gc().SetAllocationThresholdBytes(static_cast<size_t>(value));
}

extern "C" int64_t Kotlin_native_internal_GC_getThresholdAllocations(ObjHeader*) {
    auto threshold = mm::GlobalData::Instance().gc().GetAllocationThresholdBytes();
    auto maxValue = std::numeric_limits<int64_t>::max();
    if (threshold > static_cast<size_t>(maxValue)) {
        return maxValue;
    }
    return static_cast<int64_t>(maxValue);
}

extern "C" OBJ_GETTER(Kotlin_native_internal_GC_detectCycles, ObjHeader*) {
    // TODO: Remove when legacy MM is gone.
    RETURN_OBJ(nullptr);
}

extern "C" OBJ_GETTER(Kotlin_native_internal_GC_findCycle, ObjHeader*, ObjHeader* root) {
    // TODO: Remove when legacy MM is gone.
    RETURN_OBJ(nullptr);
}

extern "C" bool Kotlin_native_internal_GC_getCyclicCollector(ObjHeader* gc) {
    // TODO: Remove when legacy MM is gone.
    return false;
}

extern "C" void Kotlin_native_internal_GC_setCyclicCollector(ObjHeader* gc, bool value) {
    // TODO: Remove when legacy MM is gone.
    if (value)
        ThrowIllegalArgumentException();
}

extern "C" bool Kotlin_Any_isShareable(ObjHeader* thiz) {
    // TODO: Remove when legacy MM is gone.
    return true;
}

extern "C" void Kotlin_Any_share(ObjHeader* thiz) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do
}

extern "C" RUNTIME_NOTHROW void PerformFullGC(MemoryState* memory) {
    GetThreadData(memory)->gc().PerformFullGC();
}

extern "C" RUNTIME_NOTHROW bool ClearSubgraphReferences(ObjHeader* root, bool checked) {
    // TODO: Remove when legacy MM is gone.
    return true;
}

extern "C" RUNTIME_NOTHROW void* CreateStablePointer(ObjHeader* object) {
    if (!object)
        return nullptr;

    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    return mm::StableRefRegistry::Instance().RegisterStableRef(threadData, object);
}

extern "C" RUNTIME_NOTHROW void DisposeStablePointer(void* pointer) {
    if (!pointer)
        return;

    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    auto* node = static_cast<mm::StableRefRegistry::Node*>(pointer);
    mm::StableRefRegistry::Instance().UnregisterStableRef(threadData, node);
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(DerefStablePointer, void* pointer) {
    if (!pointer)
        RETURN_OBJ(nullptr);

    auto* node = static_cast<mm::StableRefRegistry::Node*>(pointer);
    ObjHeader* object = **node;
    RETURN_OBJ(object);
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(AdoptStablePointer, void* pointer) {
    if (!pointer)
        RETURN_OBJ(nullptr);

    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    auto* node = static_cast<mm::StableRefRegistry::Node*>(pointer);
    ObjHeader* object = **node;
    // Make sure `object` stays in the rootset: put it on the stack before removing it from `StableRefRegistry`.
    mm::SetStackRef(OBJ_RESULT, object);
    mm::StableRefRegistry::Instance().UnregisterStableRef(threadData, node);
    return object;
}

extern "C" RUNTIME_NOTHROW void CheckLifetimesConstraint(ObjHeader* obj, ObjHeader* pointee) {
    // TODO: Consider making it a `RuntimeCheck`. Probably all `RuntimeCheck`s and `RuntimeAssert`s should specify
    //       that their firing is a compiler bug and should be reported.
    if (!obj->local() && pointee != nullptr && pointee->local()) {
        konan::consolePrintf("Attempt to store a stack object %p into a heap object %p\n", pointee, obj);
        konan::consolePrintf("This is a compiler bug, please report it to https://kotl.in/issue\n");
        konan::abort();
    }
}

extern "C" ForeignRefContext InitForeignRef(ObjHeader* object) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    auto* node = mm::StableRefRegistry::Instance().RegisterStableRef(threadData, object);
    return ToForeignRefManager(node);
}

extern "C" void DeinitForeignRef(ObjHeader* object, ForeignRefContext context) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    auto* node = FromForeignRefManager(context);
    RuntimeAssert(object == **node, "Must correspond to the same object");
    mm::StableRefRegistry::Instance().UnregisterStableRef(threadData, node);
}

extern "C" bool IsForeignRefAccessible(ObjHeader* object, ForeignRefContext context) {
    // TODO: Remove when legacy MM is gone.
    return true;
}

extern "C" void AdoptReferenceFromSharedVariable(ObjHeader* object) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do.
}

extern "C" void CheckGlobalsAccessible() {
    // TODO: Remove when legacy MM is gone.
    // Always accessible
}

extern "C" RUNTIME_NOTHROW void Kotlin_mm_safePointFunctionEpilogue() {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->gc().SafePointFunctionEpilogue();
}

extern "C" RUNTIME_NOTHROW void Kotlin_mm_safePointWhileLoopBody() {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->gc().SafePointLoopBody();
}

extern "C" RUNTIME_NOTHROW void Kotlin_mm_safePointExceptionUnwind() {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->gc().SafePointExceptionUnwind();
}
