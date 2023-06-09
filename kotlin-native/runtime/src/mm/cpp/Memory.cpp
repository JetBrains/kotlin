/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"
#include "MemoryPrivate.hpp"

#include "Exceptions.h"
#include "ExtraObjectData.hpp"
#include "Freezing.hpp"
#include "GC.hpp"
#include "GlobalsRegistry.hpp"
#include "KAssert.h"
#include "Natives.h"
#include "ObjectOps.hpp"
#include "Porting.h"
#include "Runtime.h"
#include "StableRef.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadState.hpp"
#include "Utils.hpp"

using namespace kotlin;

ObjHeader* ObjHeader::GetWeakCounter() {
    RuntimeFail("Only for legacy MM");
}

ObjHeader* ObjHeader::GetOrSetWeakCounter(ObjHeader* counter) {
    RuntimeFail("Only for legacy MM");
}

#ifdef KONAN_OBJC_INTEROP

void* ObjHeader::GetAssociatedObject() const {
    auto metaObject = meta_object_or_null();
    if (metaObject == nullptr) {
        return nullptr;
    }
    return mm::ExtraObjectData::FromMetaObjHeader(metaObject).AssociatedObject().load(std::memory_order_acquire);
}

void ObjHeader::SetAssociatedObject(void* obj) {
    auto& extraObject = mm::ExtraObjectData::FromMetaObjHeader(meta_object());
    // TODO: Consider additional filtering based on types:
    //       * have some kind of an allowlist that can be populated by the user
    //         to specify that objects of these types must be finalized only on
    //         the main thread.
    //       * prepopulate it for the system frameworks.
    //       * if that were to be done at runtime, library authors could register
    //         their types in a library initialization code.
    if (pthread_main_np() == 1) {
        extraObject.setFlag(mm::ExtraObjectData::FLAGS_RELEASE_ON_MAIN_QUEUE);
    }
    return extraObject.AssociatedObject().store(obj, std::memory_order_release);
}

void* ObjHeader::CasAssociatedObject(void* expectedObj, void* obj) {
    auto& extraObject = mm::ExtraObjectData::FromMetaObjHeader(meta_object());
    bool success = extraObject.AssociatedObject().compare_exchange_strong(expectedObj, obj);
    // TODO: Consider additional filtering outlined above.
    if (success && pthread_main_np() == 1) {
        extraObject.setFlag(mm::ExtraObjectData::FLAGS_RELEASE_ON_MAIN_QUEUE);
    }
    return expectedObj;
}

#endif // KONAN_OBJC_INTEROP

// static
MetaObjHeader* ObjHeader::createMetaObject(ObjHeader* object) {
    return mm::ExtraObjectData::Install(object).AsMetaObjHeader();
}

// static
void ObjHeader::destroyMetaObject(ObjHeader* object) {
    RuntimeAssert(object->has_meta_object(), "Object must have a meta object set");
    auto &extraObject = *mm::ExtraObjectData::Get(object);
#ifdef CUSTOM_ALLOCATOR
    extraObject.ReleaseAssociatedObject();
    extraObject.setFlag(mm::ExtraObjectData::FLAGS_FINALIZED);
#else
    extraObject.Uninstall();
    auto *threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    mm::ExtraObjectDataFactory::Instance().DestroyExtraObjectData(threadData, extraObject);
#endif
}

ALWAYS_INLINE bool isPermanentOrFrozen(const ObjHeader* obj) {
    // TODO: Freeze TF_IMMUTABLE objects upon creation.
    if (!compiler::freezingChecksEnabled()) return false;
    return mm::IsFrozen(obj) || ((obj->type_info()->flags_ & TF_IMMUTABLE) != 0);
}

ALWAYS_INLINE bool isShareable(const ObjHeader* obj) {
    // TODO: Remove when legacy MM is gone.
    return true;
}

extern "C" MemoryState* InitMemory(bool firstRuntime) {
    return mm::ToMemoryState(mm::ThreadRegistry::Instance().RegisterCurrentThread());
}

extern "C" void DeinitMemory(MemoryState* state, bool destroyRuntime) {
    // We need the native state to avoid a deadlock on unregistering the thread.
    // The deadlock is possible if we are in the runnable state and the GC already locked
    // the thread registery and waits for threads to suspend or go to the native state.
    AssertThreadState(state, ThreadState::kNative);
    auto* node = mm::FromMemoryState(state);
    if (destroyRuntime) {
        ThreadStateGuard guard(state, ThreadState::kRunnable);
        node->Get()->gc().ScheduleAndWaitFullGCWithFinalizers();
        // TODO: Why not just destruct `GC` object and its thread data counterpart entirely?
        mm::GlobalData::Instance().gc().StopFinalizerThreadIfRunning();
    }
    if (!konan::isOnThreadExitNotSetOrAlreadyStarted()) {
        // we can clear reference in advance, as Unregister function can't use it anyway
        mm::ThreadRegistry::ClearCurrentThreadData();
    }
    mm::ThreadRegistry::Instance().Unregister(node);
}

extern "C" void RestoreMemory(MemoryState*) {
    // TODO: Remove when legacy MM is gone.
}

extern "C" void ClearMemoryForTests(MemoryState* state) {
    state->GetThreadData()->ClearForTests();
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

extern "C" RUNTIME_NOTHROW void InitAndRegisterGlobal(ObjHeader** location, const ObjHeader* initialValue) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
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

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void UpdateVolatileHeapRef(ObjHeader** location, const ObjHeader* object) {
    mm::SetHeapRefAtomicSeqCst(location, const_cast<ObjHeader*>(object));
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW OBJ_GETTER(CompareAndSwapVolatileHeapRef, ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue) {
    RETURN_RESULT_OF(mm::CompareAndSwapHeapRef, location, expectedValue, newValue);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW bool CompareAndSetVolatileHeapRef(ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue) {
    return mm::CompareAndSetHeapRef(location, expectedValue, newValue);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW OBJ_GETTER(GetAndSetVolatileHeapRef, ObjHeader** location, ObjHeader* newValue) {
    RETURN_RESULT_OF(mm::GetAndSetHeapRef, location, newValue);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void UpdateHeapRefIfNull(ObjHeader** location, const ObjHeader* object) {
    if (object == nullptr) return;
    ObjHeader* result = nullptr; // No need to store this value in a rootset.
    mm::CompareAndSwapHeapRef(location, nullptr, const_cast<ObjHeader*>(object), &result);
}

extern "C" ALWAYS_INLINE RUNTIME_NOTHROW void UpdateHeapRefsInsideOneArray(const ArrayHeader* array, int fromIndex,
                                                                           int toIndex, int count) {
    RuntimeFail("Only for legacy MM");
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
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->shadowStack().EnterFrame(start, parameters, count);
}

extern "C" RUNTIME_NOTHROW void LeaveFrame(ObjHeader** start, int parameters, int count) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->shadowStack().LeaveFrame(start, parameters, count);
}

extern "C" RUNTIME_NOTHROW void SetCurrentFrame(ObjHeader** start) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->shadowStack().SetCurrentFrame(start);
}

extern "C" RUNTIME_NOTHROW FrameOverlay* getCurrentFrame() {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    return threadData->shadowStack().getCurrentFrame();
}

extern "C" RUNTIME_NOTHROW ALWAYS_INLINE void CheckCurrentFrame(ObjHeader** frame) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    return threadData->shadowStack().checkCurrentFrame(reinterpret_cast<FrameOverlay*>(frame));
}

extern "C" RUNTIME_NOTHROW void AddTLSRecord(MemoryState* memory, void** key, int size) {
    auto* threadData = memory->GetThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->tls().AddRecord(key, size);
}

extern "C" RUNTIME_NOTHROW void CommitTLSStorage(MemoryState* memory) {
    auto* threadData = memory->GetThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->tls().Commit();
}

extern "C" RUNTIME_NOTHROW void ClearTLS(MemoryState* memory) {
    auto* threadData = memory->GetThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->tls().Clear();
}

extern "C" RUNTIME_NOTHROW ObjHeader** LookupTLS(void** key, int index) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    return threadData->tls().Lookup(key, index);
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
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->gc().ScheduleAndWaitFullGCWithFinalizers();
}

extern "C" void Kotlin_native_internal_GC_schedule(ObjHeader*) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->gc().Schedule();
}

extern "C" void Kotlin_native_internal_GC_collectCyclic(ObjHeader*) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do
}

// TODO: Maybe a pair of suspend/resume or start/stop may be useful in the future?
//       The other pair is likely to be removed.

extern "C" void Kotlin_native_internal_GC_suspend(ObjHeader*) {
    // Nothing to do
}

extern "C" void Kotlin_native_internal_GC_resume(ObjHeader*) {
    // Nothing to do
}

extern "C" void Kotlin_native_internal_GC_stop(ObjHeader*) {
    // Nothing to do
}

extern "C" void Kotlin_native_internal_GC_start(ObjHeader*) {
    // Nothing to do
}

extern "C" void Kotlin_native_internal_GC_setThreshold(ObjHeader*, KInt value) {
    RuntimeAssert(value > 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gc().gcSchedulerConfig().threshold = value;
}

extern "C" KInt Kotlin_native_internal_GC_getThreshold(ObjHeader*) {
    return mm::GlobalData::Instance().gc().gcSchedulerConfig().threshold.load();
}

extern "C" void Kotlin_native_internal_GC_setCollectCyclesThreshold(ObjHeader*, int64_t value) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do
}

extern "C" int64_t Kotlin_native_internal_GC_getCollectCyclesThreshold(ObjHeader*) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do
    return -1;
}

extern "C" void Kotlin_native_internal_GC_setThresholdAllocations(ObjHeader*, int64_t value) {
    RuntimeAssert(value > 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gc().gcSchedulerConfig().allocationThresholdBytes = value;
}

extern "C" int64_t Kotlin_native_internal_GC_getThresholdAllocations(ObjHeader*) {
    return mm::GlobalData::Instance().gc().gcSchedulerConfig().allocationThresholdBytes.load();
}

extern "C" void Kotlin_native_internal_GC_setTuneThreshold(ObjHeader*, KBoolean value) {
    mm::GlobalData::Instance().gc().gcSchedulerConfig().autoTune = value;
}

extern "C" KBoolean Kotlin_native_internal_GC_getTuneThreshold(ObjHeader*) {
    return mm::GlobalData::Instance().gc().gcSchedulerConfig().autoTune.load();
}

extern "C" KLong Kotlin_native_internal_GC_getRegularGCIntervalMicroseconds(ObjHeader*) {
    return mm::GlobalData::Instance().gc().gcSchedulerConfig().regularGcIntervalMicroseconds.load();
}

extern "C" void Kotlin_native_internal_GC_setRegularGCIntervalMicroseconds(ObjHeader*, KLong value) {
    RuntimeAssert(value >= 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gc().gcSchedulerConfig().regularGcIntervalMicroseconds = value;
}

extern "C" KLong Kotlin_native_internal_GC_getTargetHeapBytes(ObjHeader*) {
    return mm::GlobalData::Instance().gc().gcSchedulerConfig().targetHeapBytes.load();
}

extern "C" void Kotlin_native_internal_GC_setTargetHeapBytes(ObjHeader*, KLong value) {
    RuntimeAssert(value >= 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gc().gcSchedulerConfig().targetHeapBytes = value;
}

extern "C" KDouble Kotlin_native_internal_GC_getTargetHeapUtilization(ObjHeader*) {
    return mm::GlobalData::Instance().gc().gcSchedulerConfig().targetHeapUtilization.load();
}

extern "C" void Kotlin_native_internal_GC_setTargetHeapUtilization(ObjHeader*, KDouble value) {
    RuntimeAssert(value > 0 && value <= 1, "Must be handled by the caller");
    mm::GlobalData::Instance().gc().gcSchedulerConfig().targetHeapUtilization = value;
}

extern "C" KLong Kotlin_native_internal_GC_getMaxHeapBytes(ObjHeader*) {
    return mm::GlobalData::Instance().gc().gcSchedulerConfig().maxHeapBytes.load();
}

extern "C" void Kotlin_native_internal_GC_setMaxHeapBytes(ObjHeader*, KLong value) {
    RuntimeAssert(value >= 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gc().gcSchedulerConfig().maxHeapBytes = value;
}

extern "C" KLong Kotlin_native_internal_GC_getMinHeapBytes(ObjHeader*) {
    return mm::GlobalData::Instance().gc().gcSchedulerConfig().minHeapBytes.load();
}

extern "C" void Kotlin_native_internal_GC_setMinHeapBytes(ObjHeader*, KLong value) {
    RuntimeAssert(value >= 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gc().gcSchedulerConfig().minHeapBytes = value;
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
    // Nothing to do.
    return false;
}

extern "C" void Kotlin_native_internal_GC_setCyclicCollector(ObjHeader* gc, bool value) {
    // TODO: Remove when legacy MM is gone.
    // Nothing to do.
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
    auto* threadData = memory->GetThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->gc().ScheduleAndWaitFullGCWithFinalizers();
}

extern "C" RUNTIME_NOTHROW bool ClearSubgraphReferences(ObjHeader* root, bool checked) {
    // TODO: Remove when legacy MM is gone.
    return true;
}

extern "C" RUNTIME_NOTHROW void* CreateStablePointer(ObjHeader* object) {
    if (!object)
        return nullptr;

    AssertThreadState(ThreadState::kRunnable);
    return static_cast<mm::RawSpecialRef*>(mm::StableRef::create(object));
}

extern "C" RUNTIME_NOTHROW void DisposeStablePointer(void* pointer) {
    if (!pointer) return;

    // Can be safely called in any thread state.
    mm::StableRef(static_cast<mm::RawSpecialRef*>(pointer)).dispose();
}

extern "C" RUNTIME_NOTHROW void DisposeStablePointerFor(MemoryState* memoryState, void* pointer) {
    if (!pointer)
        return;

    // Can be safely called in any thread state.
    mm::StableRef(static_cast<mm::RawSpecialRef*>(pointer)).disposeOn(*mm::FromMemoryState(memoryState)->Get());
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(DerefStablePointer, void* pointer) {
    if (!pointer)
        RETURN_OBJ(nullptr);

    AssertThreadState(ThreadState::kRunnable);
    RETURN_OBJ(*mm::StableRef(static_cast<mm::RawSpecialRef*>(pointer)));
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(AdoptStablePointer, void* pointer) {
    if (!pointer)
        RETURN_OBJ(nullptr);

    AssertThreadState(ThreadState::kRunnable);
    mm::StableRef stableRef(static_cast<mm::RawSpecialRef*>(pointer));
    auto* obj = *stableRef;
    mm::SetStackRef(OBJ_RESULT, obj);
    std::move(stableRef).dispose();
    return obj;
}

extern "C" void MutationCheck(ObjHeader* obj) {
    if (obj->local()) return;
    if (!isPermanentOrFrozen(obj)) return;

    ThrowInvalidMutabilityException(obj);
}

extern "C" RUNTIME_NOTHROW void CheckLifetimesConstraint(ObjHeader* obj, ObjHeader* pointee) {
    RuntimeAssert(obj->local() || pointee == nullptr || !pointee->local(),
                  "Attempt to store a stack object %p into a heap object %p. "
                  "This is a compiler bug, please report it to https://kotl.in/issue",
                  pointee, obj);
}

extern "C" void FreezeSubgraph(ObjHeader* obj) {
    if (auto* blocker = mm::FreezeSubgraph(obj)) {
        ThrowFreezingException(obj, blocker);
    }
}

extern "C" void EnsureNeverFrozen(ObjHeader* obj) {
    if (!mm::EnsureNeverFrozen(obj)) {
        ThrowFreezingException(obj, obj);
    }
}

extern "C" void CheckGlobalsAccessible() {
    // TODO: Remove when legacy MM is gone.
    // Always accessible
}

// it would be inlined manually in RemoveRedundantSafepointsPass
extern "C" RUNTIME_NOTHROW NO_INLINE void Kotlin_mm_safePointFunctionPrologue() {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->gc().SafePointFunctionPrologue();
}

extern "C" RUNTIME_NOTHROW CODEGEN_INLINE_POLICY void Kotlin_mm_safePointWhileLoopBody() {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    AssertThreadState(threadData, ThreadState::kRunnable);
    threadData->gc().SafePointLoopBody();
}

extern "C" CODEGEN_INLINE_POLICY RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateNative() {
    SwitchThreadState(mm::ThreadRegistry::Instance().CurrentThreadData(), ThreadState::kNative);
}

extern "C" CODEGEN_INLINE_POLICY RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateRunnable() {
    SwitchThreadState(mm::ThreadRegistry::Instance().CurrentThreadData(), ThreadState::kRunnable);
}

MemoryState* kotlin::mm::GetMemoryState() noexcept {
    return ToMemoryState(ThreadRegistry::Instance().CurrentThreadDataNode());
}

bool kotlin::mm::IsCurrentThreadRegistered() noexcept {
    return ThreadRegistry::Instance().IsCurrentThreadRegistered();
}

ALWAYS_INLINE kotlin::CalledFromNativeGuard::CalledFromNativeGuard(bool reentrant) noexcept : reentrant_(reentrant) {
    Kotlin_initRuntimeIfNeeded();
    thread_ = mm::GetMemoryState();
    oldState_ = SwitchThreadState(thread_, ThreadState::kRunnable, reentrant_);
}

const bool kotlin::kSupportsMultipleMutators = kotlin::gc::kSupportsMultipleMutators;

void kotlin::StartFinalizerThreadIfNeeded() noexcept {
    mm::GlobalData::Instance().gc().StartFinalizerThreadIfNeeded();
}

bool kotlin::FinalizersThreadIsRunning() noexcept {
    return mm::GlobalData::Instance().gc().FinalizersThreadIsRunning();
}

RUNTIME_NOTHROW ALWAYS_INLINE extern "C" void Kotlin_processObjectInMark(void* state, ObjHeader* object) {
    gc::GC::processObjectInMark(state, object);
}

RUNTIME_NOTHROW ALWAYS_INLINE extern "C" void Kotlin_processArrayInMark(void* state, ObjHeader* object) {
    gc::GC::processArrayInMark(state, object->array());
}

RUNTIME_NOTHROW ALWAYS_INLINE extern "C" void Kotlin_processFieldInMark(void* state, ObjHeader* field) {
    gc::GC::processFieldInMark(state, field);
}

RUNTIME_NOTHROW ALWAYS_INLINE extern "C" void Kotlin_processEmptyObjectInMark(void* state, ObjHeader* object) {
    // Empty object. Nothing to do.
    // TODO: Try to generate it in the code generator.
}

extern "C" OBJ_GETTER(makePermanentWeakReferenceImpl, ObjHeader*);
extern "C" OBJ_GETTER(makeObjCWeakReferenceImpl, void*);

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Konan_getWeakReferenceImpl, ObjHeader* referred) {
    if (referred->permanent()) {
        RETURN_RESULT_OF(makePermanentWeakReferenceImpl, referred);
    }
#if KONAN_OBJC_INTEROP
    if (IsInstanceInternal(referred, theObjCObjectWrapperTypeInfo)) {
        RETURN_RESULT_OF(makeObjCWeakReferenceImpl, referred->GetAssociatedObject());
    }
#endif // KONAN_OBJC_INTEROP
    RETURN_RESULT_OF(mm::createRegularWeakReferenceImpl, referred);
}

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Konan_WeakReferenceCounterLegacyMM_get, ObjHeader* counter) {
    RuntimeFail("Legacy MM only");
}

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Konan_RegularWeakReferenceImpl_get, ObjHeader* weakRef) {
    RETURN_RESULT_OF(mm::derefRegularWeakReferenceImpl, weakRef);
}

RUNTIME_NOTHROW extern "C" void DisposeRegularWeakReferenceImpl(ObjHeader* weakRef) {
    mm::disposeRegularWeakReferenceImpl(weakRef);
}
