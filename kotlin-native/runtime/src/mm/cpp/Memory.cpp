/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"
#include "MemoryPrivate.hpp"

#include "Allocator.hpp"
#include "CallsChecker.hpp"
#include "Exceptions.h"
#include "ExtraObjectData.hpp"
#include "GC.hpp"
#include "GlobalsRegistry.hpp"
#include "KAssert.h"
#include "Natives.h"
#include "ObjCBackRef.hpp"
#include "ObjectOps.hpp"
#include "Porting.h"
#include "ReferenceOps.hpp"
#include "Runtime.h"
#include "SafePoint.hpp"
#include "SpecialRefRegistry.hpp"
#include "StableRef.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadState.hpp"
#include "Utils.hpp"
#include "MemoryDump.hpp"

using namespace kotlin;

#ifdef KONAN_OBJC_INTEROP

PERFORMANCE_INLINE void* ObjHeader::GetAssociatedObject() const {
    auto metaObject = meta_object_or_null();
    if (metaObject == nullptr) {
        return nullptr;
    }
    return mm::ExtraObjectData::FromMetaObjHeader(metaObject).AssociatedObject().load(std::memory_order_acquire);
}

PERFORMANCE_INLINE void ObjHeader::SetAssociatedObject(void* obj) {
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

PERFORMANCE_INLINE void* ObjHeader::CasAssociatedObject(void* expectedObj, void* obj) {
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
    alloc::destroyExtraObjectData(extraObject);
}

extern "C" MemoryState* InitMemory() {
    mm::GlobalData::waitInitialized();
    return mm::ToMemoryState(mm::ThreadRegistry::Instance().RegisterCurrentThread());
}

void kotlin::initGlobalMemory() noexcept {
    mm::GlobalData::init();
}

extern "C" void DeinitMemory(MemoryState* state, bool destroyRuntime) {
    // We need the native state to avoid a deadlock on unregistering the thread.
    // The deadlock is possible if we are in the runnable state and the GC already locked
    // the thread registery and waits for threads to suspend or go to the native state.
    AssertThreadState(state, ThreadState::kNative);
    auto* node = mm::FromMemoryState(state);
    if (destroyRuntime) {
        ThreadStateGuard guard(state, ThreadState::kRunnable);
        mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
        // TODO: Why not just destruct `GC` object and its thread data counterpart entirely?
        mm::GlobalData::Instance().gc().StopFinalizerThreadIfRunning();
    }
    if (!konan::isOnThreadExitNotSetOrAlreadyStarted()) {
        // we can clear reference in advance, as Unregister function can't use it anyway
        mm::ThreadRegistry::ClearCurrentThreadData();
    }
    mm::ThreadRegistry::Instance().Unregister(node);
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
        UpdateHeapRef(location, const_cast<ObjHeader*>(initialValue));
    }
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void ZeroHeapRef(ObjHeader** location) {
    mm::RefAccessor<false>{location} = nullptr;
}

extern "C" RUNTIME_NOTHROW void ZeroArrayRefs(ArrayHeader* array) {
    for (uint32_t index = 0; index < array->count_; ++index) {
        ObjHeader** location = ArrayAddressOfElementAt(array, index);
        mm::RefFieldAccessor{location} = nullptr;
    }
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void ZeroStackRef(ObjHeader** location) {
    mm::StackRefAccessor{location} = nullptr;
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void UpdateStackRef(ObjHeader** location, const ObjHeader* object) {
    mm::StackRefAccessor{location} = const_cast<ObjHeader*>(object);
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) {
    mm::RefAccessor<false>{location} = const_cast<ObjHeader*>(object);
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void UpdateVolatileHeapRef(ObjHeader** location, const ObjHeader* object) {
    mm::RefAccessor<false>{location}.storeAtomic(const_cast<ObjHeader*>(object), std::memory_order_seq_cst);
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW OBJ_GETTER(CompareAndSwapVolatileHeapRef, ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue) {
    ObjHeader* actual = expectedValue;
    mm::RefAccessor<false>{location}.compareAndExchange(actual, newValue, std::memory_order_seq_cst);
    RETURN_OBJ(actual);
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW bool CompareAndSetVolatileHeapRef(ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue) {
    return mm::RefAccessor<false>{location}.compareAndExchange(expectedValue, newValue, std::memory_order_seq_cst);
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW OBJ_GETTER(GetAndSetVolatileHeapRef, ObjHeader** location, ObjHeader* newValue) {
    RETURN_OBJ(mm::RefAccessor<false>{location}.exchange(newValue, std::memory_order_seq_cst));
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
    UpdateStackRef(returnSlot, object);
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

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void CheckCurrentFrame(ObjHeader** frame) {
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

extern "C" void Kotlin_native_internal_GC_collect(ObjHeader*) {
    mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
}

extern "C" void Kotlin_native_internal_GC_schedule(ObjHeader*) {
    mm::GlobalData::Instance().gcScheduler().schedule();
}

extern "C" RUNTIME_NOTHROW bool Kotlin_native_runtime_Debugging_dumpMemory(ObjHeader*, int fd) {
    auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
    threadData->suspensionData().requestThreadsSuspension("Memory dump");
    CallsCheckerIgnoreGuard guard;
    // We're in the runnable state, but everything else (including the GC thread) will be suspended.
    // It's fine to wait for that suspension and execute long-running operations (I/O) here.
    mm::WaitForThreadsSuspension();
    bool success = mm::DumpMemory(fd);
    mm::ResumeThreads();
    return success;
}

extern "C" void Kotlin_native_internal_GC_setTuneThreshold(ObjHeader*, KBoolean value) {
    mm::GlobalData::Instance().gcScheduler().config().autoTune = value;
}

extern "C" KBoolean Kotlin_native_internal_GC_getTuneThreshold(ObjHeader*) {
    return mm::GlobalData::Instance().gcScheduler().config().autoTune.load();
}

extern "C" KLong Kotlin_native_internal_GC_getRegularGCIntervalMicroseconds(ObjHeader*) {
    return mm::GlobalData::Instance().gcScheduler().config().regularGcIntervalMicroseconds.load();
}

extern "C" void Kotlin_native_internal_GC_setRegularGCIntervalMicroseconds(ObjHeader*, KLong value) {
    RuntimeAssert(value >= 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gcScheduler().config().regularGcIntervalMicroseconds = value;
}

extern "C" KLong Kotlin_native_internal_GC_getTargetHeapBytes(ObjHeader*) {
    return mm::GlobalData::Instance().gcScheduler().config().targetHeapBytes.load();
}

extern "C" void Kotlin_native_internal_GC_setTargetHeapBytes(ObjHeader*, KLong value) {
    RuntimeAssert(value >= 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gcScheduler().config().targetHeapBytes = value;
}

extern "C" KDouble Kotlin_native_internal_GC_getTargetHeapUtilization(ObjHeader*) {
    return mm::GlobalData::Instance().gcScheduler().config().targetHeapUtilization.load();
}

extern "C" void Kotlin_native_internal_GC_setTargetHeapUtilization(ObjHeader*, KDouble value) {
    RuntimeAssert(value > 0 && value <= 1, "Must be handled by the caller");
    mm::GlobalData::Instance().gcScheduler().config().targetHeapUtilization = value;
}

extern "C" KLong Kotlin_native_internal_GC_getMaxHeapBytes(ObjHeader*) {
    return mm::GlobalData::Instance().gcScheduler().config().maxHeapBytes.load();
}

extern "C" void Kotlin_native_internal_GC_setMaxHeapBytes(ObjHeader*, KLong value) {
    RuntimeAssert(value >= 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gcScheduler().config().maxHeapBytes = value;
}

extern "C" KLong Kotlin_native_internal_GC_getMinHeapBytes(ObjHeader*) {
    return mm::GlobalData::Instance().gcScheduler().config().minHeapBytes.load();
}

extern "C" void Kotlin_native_internal_GC_setMinHeapBytes(ObjHeader*, KLong value) {
    RuntimeAssert(value >= 0, "Must be handled by the caller");
    mm::GlobalData::Instance().gcScheduler().config().minHeapBytes = value;
}

extern "C" KDouble Kotlin_native_internal_GC_getHeapTriggerCoefficient(ObjHeader*) {
    return mm::GlobalData::Instance().gcScheduler().config().heapTriggerCoefficient.load();
}

extern "C" void Kotlin_native_internal_GC_setHeapTriggerCoefficient(ObjHeader*, KDouble value) {
    RuntimeAssert(value > 0 && value <= 1, "Must be handled by the caller");
    mm::GlobalData::Instance().gcScheduler().config().heapTriggerCoefficient = value;
}

extern "C" KBoolean Kotlin_native_internal_GC_getPauseOnTargetHeapOverflow(ObjHeader*) {
    return mm::GlobalData::Instance().gcScheduler().config().mutatorAssists();
}

extern "C" void Kotlin_native_internal_GC_setPauseOnTargetHeapOverflow(ObjHeader*, KBoolean value) {
    mm::GlobalData::Instance().gcScheduler().config().setMutatorAssists(value);
}

extern "C" KBoolean Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_isAvailable(ObjHeader* gc) {
    return mm::GlobalData::Instance().gc().mainThreadFinalizerProcessorAvailable();
}

extern "C" KLong Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_getMaxTimeInTask(ObjHeader* gc) {
    KLong result;
    mm::GlobalData::Instance().gc().configureMainThreadFinalizerProcessor([&](auto& config) noexcept -> void {
        result = std::chrono::duration_cast<std::chrono::microseconds>(config.maxTimeInTask).count();
    });
    return result;
}

extern "C" void Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_setMaxTimeInTask(ObjHeader* gc, KLong value) {
    mm::GlobalData::Instance().gc().configureMainThreadFinalizerProcessor(
            [=](auto& config) noexcept -> void { config.maxTimeInTask = std::chrono::microseconds(value); });
}

extern "C" KLong Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_getMinTimeBetweenTasks(ObjHeader* gc) {
    KLong result;
    mm::GlobalData::Instance().gc().configureMainThreadFinalizerProcessor([&](auto& config) noexcept -> void {
        result = std::chrono::duration_cast<std::chrono::microseconds>(config.minTimeBetweenTasks).count();
    });
    return result;
}

extern "C" void Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_setMinTimeBetweenTasks(ObjHeader* gc, KLong value) {
    mm::GlobalData::Instance().gc().configureMainThreadFinalizerProcessor(
            [=](auto& config) noexcept -> void { config.minTimeBetweenTasks = std::chrono::microseconds(value); });
}

extern "C" KULong Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_getBatchSize(ObjHeader* gc) {
    KULong result;
    mm::GlobalData::Instance().gc().configureMainThreadFinalizerProcessor(
            [&](auto& config) noexcept -> void { result = config.batchSize; });
    return result;
}

extern "C" void Kotlin_native_runtime_GC_MainThreadFinalizerProcessor_setBatchSize(ObjHeader* gc, KULong value) {
    mm::GlobalData::Instance().gc().configureMainThreadFinalizerProcessor([=](auto& config) noexcept -> void { config.batchSize = value; });
}

extern "C" RUNTIME_NOTHROW void PerformFullGC(MemoryState* memory) {
    mm::GlobalData::Instance().gcScheduler().scheduleAndWaitFinalized();
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
    UpdateStackRef(OBJ_RESULT, obj);
    std::move(stableRef).dispose();
    return obj;
}

// it would be inlined manually in RemoveRedundantSafepointsPass
extern "C" RUNTIME_NOTHROW NO_INLINE void Kotlin_mm_safePointFunctionPrologue() {
    mm::safePoint();
}

extern "C" RUNTIME_NOTHROW PERFORMANCE_INLINE void Kotlin_mm_safePointWhileLoopBody() {
    mm::safePoint();
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateNative() {
    SwitchThreadState(mm::ThreadRegistry::Instance().CurrentThreadData(), ThreadState::kNative);
}

extern "C" NO_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateNative_debug() {
    SwitchThreadState(mm::ThreadRegistry::Instance().CurrentThreadData(), ThreadState::kNative);
}

extern "C" PERFORMANCE_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateRunnable() {
    SwitchThreadState(mm::ThreadRegistry::Instance().CurrentThreadData(), ThreadState::kRunnable);
}

extern "C" NO_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateRunnable_debug() {
    SwitchThreadState(mm::ThreadRegistry::Instance().CurrentThreadData(), ThreadState::kRunnable);
}

MemoryState* kotlin::mm::GetMemoryState() noexcept {
    return ToMemoryState(ThreadRegistry::Instance().CurrentThreadDataNode());
}

bool kotlin::mm::IsCurrentThreadRegistered() noexcept {
    return ThreadRegistry::IsCurrentThreadRegistered();
}

PERFORMANCE_INLINE kotlin::CalledFromNativeGuard::CalledFromNativeGuard(bool reentrant) noexcept : reentrant_(reentrant) {
    Kotlin_initRuntimeIfNeeded();
    thread_ = mm::GetMemoryState();
    oldState_ = SwitchThreadState(thread_, ThreadState::kRunnable, reentrant_);
}

void kotlin::StartFinalizerThreadIfNeeded() noexcept {
    mm::GlobalData::Instance().gc().StartFinalizerThreadIfNeeded();
}

bool kotlin::FinalizersThreadIsRunning() noexcept {
    return mm::GlobalData::Instance().gc().FinalizersThreadIsRunning();
}

RUNTIME_NOTHROW extern "C" void Kotlin_processObjectInMark(void* state, ObjHeader* object) {
    gc::GC::processObjectInMark(state, object);
}

RUNTIME_NOTHROW extern "C" void Kotlin_processArrayInMark(void* state, ObjHeader* object) {
    gc::GC::processArrayInMark(state, object->array());
}

RUNTIME_NOTHROW extern "C" void Kotlin_processEmptyObjectInMark(void* state, ObjHeader* object) {
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

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Konan_RegularWeakReferenceImpl_get, ObjHeader* weakRef) {
    RETURN_RESULT_OF(mm::derefRegularWeakReferenceImpl, weakRef);
}

RUNTIME_NOTHROW extern "C" void DisposeRegularWeakReferenceImpl(ObjHeader* weakRef) {
    mm::disposeRegularWeakReferenceImpl(weakRef);
}

void kotlin::OnMemoryAllocation(size_t totalAllocatedBytes) noexcept {
    mm::GlobalData::Instance().gcScheduler().setAllocatedBytes(totalAllocatedBytes);
}

void kotlin::initObjectPool() noexcept {
    alloc::initObjectPool();
}

void kotlin::compactObjectPoolInCurrentThread() noexcept {
    alloc::compactObjectPoolInCurrentThread();
}
