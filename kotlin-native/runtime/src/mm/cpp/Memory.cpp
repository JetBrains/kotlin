/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "GlobalsRegistry.hpp"
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

extern "C" MemoryState* InitMemory(bool firstRuntime) {
    return ToMemoryState(mm::ThreadRegistry::Instance().RegisterCurrentThread());
}

extern "C" void DeinitMemory(MemoryState* state, bool destroyRuntime) {
    mm::ThreadRegistry::Instance().Unregister(FromMemoryState(state));
}

extern "C" OBJ_GETTER(InitSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*)) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    // TODO: This should only be called if singleton is actually created here. It's possible that the
    // singleton will be created on a different thread and here we should check that, instead of creating
    // another one (and registering `location` twice).
    mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(threadData, location);
    RuntimeCheck(false, "Unimplemented");
}

extern "C" RUNTIME_NOTHROW void InitAndRegisterGlobal(ObjHeader** location, const ObjHeader* initialValue) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    mm::GlobalsRegistry::Instance().RegisterStorageForGlobal(threadData, location);
    RuntimeCheck(false, "Unimplemented");
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

extern "C" RUNTIME_NOTHROW void* CreateStablePointer(ObjHeader* object) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    return mm::StableRefRegistry::Instance().RegisterStableRef(threadData, object);
}

extern "C" RUNTIME_NOTHROW void DisposeStablePointer(void* pointer) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    auto* node = static_cast<mm::StableRefRegistry::Node*>(pointer);
    mm::StableRefRegistry::Instance().UnregisterStableRef(threadData, node);
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(DerefStablePointer, void* pointer) {
    auto* node = static_cast<mm::StableRefRegistry::Node*>(pointer);
    ObjHeader* object = **node;
    RETURN_OBJ(object);
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(AdoptStablePointer, void* pointer) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    auto* node = static_cast<mm::StableRefRegistry::Node*>(pointer);
    ObjHeader* object = **node;
    UpdateReturnRef(OBJ_RESULT, object);
    mm::StableRefRegistry::Instance().UnregisterStableRef(threadData, node);
    return object;
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
