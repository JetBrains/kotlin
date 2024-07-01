/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/Atomic.hpp"
#include "Cleaner.h"
#include "CompilerConstants.hpp"
#include "Exceptions.h"
#include "KAssert.h"
#include "Memory.h"
#include "Logging.hpp"
#include "ObjCExportInit.h"
#include "Porting.h"
#include "Runtime.h"
#include "RuntimePrivate.hpp"
#include "Worker.h"
#include "KString.h"
#include <atomic>
#include <cstdlib>
#include <thread>

using namespace kotlin;

using kotlin::internal::FILE_NOT_INITIALIZED;
using kotlin::internal::FILE_BEING_INITIALIZED;
using kotlin::internal::FILE_INITIALIZED;
using kotlin::internal::FILE_FAILED_TO_INITIALIZE;

namespace {

InitNode* initHeadNode = nullptr;
InitNode* initTailNode = nullptr;

enum class RuntimeStatus {
    kUninitialized,
    kRunning,
    kDestroying,
};

struct RuntimeState {
    MemoryState* memoryState;
    Worker* worker;
    RuntimeStatus status = RuntimeStatus::kUninitialized;
};

// Must be synchronized with IrToBitcode.kt
enum {
  ALLOC_THREAD_LOCAL_GLOBALS = 0,
  INIT_GLOBALS = 1,
  INIT_THREAD_LOCAL_GLOBALS = 2,
  DEINIT_GLOBALS = 3
};

void InitOrDeinitGlobalVariables(int initialize, MemoryState* memory) {
  InitNode* currentNode = initHeadNode;
  while (currentNode != nullptr) {
    currentNode->init(initialize, memory);
    currentNode = currentNode->next;
  }
}

KBoolean g_checkLeaks = false;
KBoolean g_checkLeakedCleaners = false;
KBoolean g_forceCheckedShutdown = false;

constexpr RuntimeState* kInvalidRuntime = nullptr;

THREAD_LOCAL_VARIABLE RuntimeState* runtimeState = kInvalidRuntime;

PERFORMANCE_INLINE inline bool isValidRuntime() {
  return ::runtimeState != kInvalidRuntime;
}

std::atomic<int> aliveRuntimesCount = 0;

enum GlobalRuntimeStatus {
    kGlobalRuntimeUninitialized = 0,
    kGlobalRuntimeRunning,
    kGlobalRuntimeShutdown,
};

std::atomic<GlobalRuntimeStatus> globalRuntimeStatus = kGlobalRuntimeUninitialized;

void Kotlin_deinitRuntimeCallback(void* argument);

NO_INLINE RuntimeState* initRuntime() {
  SetKonanTerminateHandler();
  initObjectPool();
  RuntimeState* result = new RuntimeState();
  if (!result) return kInvalidRuntime;
  RuntimeCheck(!isValidRuntime(), "No active runtimes allowed");
  ::runtimeState = result;

  // First update `aliveRuntimesCount` and then update `globalRuntimeStatus`, for synchronization with
  // runtime shutdown, which does it the other way around.
  ++aliveRuntimesCount;

  bool firstRuntime = initializeGlobalRuntimeIfNeeded();
  result->memoryState = InitMemory();
  // Switch thread state because worker and globals inits require the runnable state.
  // This call may block if GC requested suspending threads.
  ThreadStateGuard stateGuard(result->memoryState, kotlin::ThreadState::kRunnable);
  result->worker = WorkerInit(result->memoryState);

  InitOrDeinitGlobalVariables(ALLOC_THREAD_LOCAL_GLOBALS, result->memoryState);
  CommitTLSStorage(result->memoryState);
  // Keep global variables in state as well.
  if (firstRuntime) {
    InitOrDeinitGlobalVariables(INIT_GLOBALS, result->memoryState);
  }
  InitOrDeinitGlobalVariables(INIT_THREAD_LOCAL_GLOBALS, result->memoryState);
  RuntimeAssert(result->status == RuntimeStatus::kUninitialized, "Runtime must still be in the uninitialized state");
  result->status = RuntimeStatus::kRunning;

  // Register runtime deinit function at thread cleanup.
  konan::onThreadExit(Kotlin_deinitRuntimeCallback, runtimeState);

  return result;
}

void deinitRuntime(RuntimeState* state, bool destroyRuntime) {
  AssertThreadState(state->memoryState, kotlin::ThreadState::kRunnable);
  RuntimeAssert(state->status == RuntimeStatus::kRunning, "Runtime must be in the running state");
  state->status = RuntimeStatus::kDestroying;
  // This may be called after TLS is zeroed out, so ::runtimeState and ::memoryState in Memory cannot be trusted.
  // TODO: This may in fact reallocate TLS without guarantees that it'll be deallocated again.
  ::runtimeState = state;
  --aliveRuntimesCount;
  ClearTLS(state->memoryState);
  if (destroyRuntime)
    InitOrDeinitGlobalVariables(DEINIT_GLOBALS, state->memoryState);

  // Worker deinit must be performed in the runnable state because
  // Worker's destructor unregisters stable refs.
  auto workerId = GetWorkerId(state->worker);
  WorkerDeinit(state->worker);

  // Do not use ThreadStateGuard because memoryState will be destroyed during DeinitMemory.
  kotlin::SwitchThreadState(state->memoryState, kotlin::ThreadState::kNative);
  DeinitMemory(state->memoryState, destroyRuntime);
  delete state;
  WorkerDestroyThreadDataIfNeeded(workerId);
  ::runtimeState = kInvalidRuntime;
}

void Kotlin_deinitRuntimeCallback(void* argument) {
  auto* state = reinterpret_cast<RuntimeState*>(argument);
  // This callback may be called from any state, make sure it runs in the runnable state.
  kotlin::SwitchThreadState(state->memoryState, kotlin::ThreadState::kRunnable, /* reentrant = */ true);
  deinitRuntime(state, false);
}

}  // namespace

bool kotlin::initializeGlobalRuntimeIfNeeded() noexcept {
    auto lastStatus = std_support::atomic_compare_swap_strong(globalRuntimeStatus, kGlobalRuntimeUninitialized, kGlobalRuntimeRunning);
    if (Kotlin_forceCheckedShutdown()) {
        RuntimeAssert(lastStatus != kGlobalRuntimeShutdown, "Kotlin runtime was shut down. Cannot create new runtimes.");
    }
    if (lastStatus != kGlobalRuntimeUninitialized)
        return false;

    konan::consoleInit();
    logging::OnRuntimeInit();
    initGlobalMemory();
#if KONAN_OBJC_INTEROP
    Kotlin_ObjCExport_initialize();
#endif
    return true;
}

extern "C" {

RUNTIME_NOTHROW void AppendToInitializersTail(InitNode *next) {
  // TODO: use RuntimeState.
  if (initHeadNode == nullptr) {
    initHeadNode = next;
  } else {
    initTailNode->next = next;
  }
  initTailNode = next;
}

PERFORMANCE_INLINE RUNTIME_NOTHROW void Kotlin_initRuntimeIfNeeded() {
  if (!isValidRuntime()) {
    initRuntime();
  }
}

void deinitRuntimeIfNeeded() {
  if (isValidRuntime()) {
    deinitRuntime(::runtimeState, false);
  }
}

// TODO: Consider exporting it to interop API.
void Kotlin_shutdownRuntime() {
    auto* runtime = ::runtimeState;
    RuntimeAssert(runtime != kInvalidRuntime, "Current thread must have Kotlin runtime initialized on it");

    bool needsFullShutdown = Kotlin_forceCheckedShutdown() || Kotlin_memoryLeakCheckerEnabled() || Kotlin_cleanersLeakCheckerEnabled();
    if (!needsFullShutdown) {
        auto lastStatus = std_support::atomic_compare_swap_strong(globalRuntimeStatus, kGlobalRuntimeRunning, kGlobalRuntimeShutdown);
        RuntimeAssert(lastStatus == kGlobalRuntimeRunning, "Invalid runtime status for shutdown");
        // The main thread is not doing anything Kotlin anymore, but will stick around to cleanup C++ globals and the like.
        // Mark the thread native, and don't make the GC thread wait on it.
        kotlin::SwitchThreadState(runtime->memoryState, kotlin::ThreadState::kNative);
        return;
    }

    // If we're going to need finalizers for the full shutdown, we need to start the thread before
    // new runtimes are disallowed.
    kotlin::StartFinalizerThreadIfNeeded();

    if (Kotlin_cleanersLeakCheckerEnabled()) {
        // Make sure to collect any lingering cleaners.
        PerformFullGC(runtime->memoryState);
    }

    // Stop cleaner worker. Only execute the cleaners if checker is enabled.
    ShutdownCleaners(Kotlin_cleanersLeakCheckerEnabled());

    // Cleaners are now done, disallow new runtimes.
    auto lastStatus = std_support::atomic_compare_swap_strong(globalRuntimeStatus, kGlobalRuntimeRunning, kGlobalRuntimeShutdown);
    RuntimeAssert(lastStatus == kGlobalRuntimeRunning, "Invalid runtime status for shutdown");

    bool canDestroyRuntime = true;

    // First make sure workers are gone.
    WaitNativeWorkersTermination();

    // Allow the current runtime.
    int knownRuntimes = 1;
    if (kotlin::FinalizersThreadIsRunning()) {
        ++knownRuntimes;
    }

    // Now check for existence of any other runtimes.
    auto otherRuntimesCount = aliveRuntimesCount.load() - knownRuntimes;
    RuntimeAssert(otherRuntimesCount >= 0, "Cannot be negative");
    if (Kotlin_forceCheckedShutdown()) {
        if (otherRuntimesCount > 0) {
            konan::consoleErrorf("Cannot run checkers when there are %d alive runtimes at the shutdown", otherRuntimesCount);
            std::abort();
        }
    } else {
        // Cannot destroy runtime globally if there're some other threads with Kotlin runtime on them.
        canDestroyRuntime = otherRuntimesCount == 0;
    }

    deinitRuntime(runtime, canDestroyRuntime);
}

KInt Konan_Platform_canAccessUnaligned() {
#if KONAN_NO_UNALIGNED_ACCESS
  return 0;
#else
  return 1;
#endif
}

KInt Konan_Platform_isLittleEndian() {
#ifdef __BIG_ENDIAN__
  return 0;
#else
  return 1;
#endif
}

KInt Konan_Platform_getOsFamily() {
#if KONAN_MACOSX
  return 1;
#elif KONAN_IOS
  return 2;
#elif KONAN_LINUX
  return 3;
#elif KONAN_WINDOWS
  return 4;
#elif KONAN_ANDROID
  return 5;
#elif KONAN_TVOS
  return 7;
#elif KONAN_WATCHOS
  return 8;
#else
#warning "Unknown platform"
  return 0;
#endif
}

KInt Konan_Platform_getCpuArchitecture() {
#if KONAN_ARM32
  return 1;
#elif KONAN_ARM64
  return 2;
#elif KONAN_X86
  return 3;
#elif KONAN_X64
  return 4;
#else
#warning "Unknown CPU"
  return 0;
#endif
}

KBoolean Konan_Platform_isDebugBinary() {
  return kotlin::compiler::shouldContainDebugInfo();
}

bool Kotlin_memoryLeakCheckerEnabled() {
  return g_checkLeaks;
}

KBoolean Konan_Platform_getMemoryLeakChecker() {
  return g_checkLeaks;
}

KInt Konan_Platform_getAvailableProcessors() {
    auto res = std::thread::hardware_concurrency();
    // C++ standard says that if this function can return 0 if value is not "well defined or not computable"
    // In current libstdc++ implementation, seems it can happen only on unsupported targets.
    // We consider such systems as single-threaded
    if (res == 0) {
        res = 1;
    }
    // Probably it can't happen, but let's not allow overflows
    if (res > std::numeric_limits<int>::max()) {
        res = std::numeric_limits<int>::max();
    }
    return static_cast<KInt>(res);
}

OBJ_GETTER0(Konan_Platform_getAvailableProcessorsEnv) {
    char* env = getenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS");
    if (env == nullptr) {
        RETURN_OBJ(nullptr)
    }
    RETURN_RESULT_OF(CreateStringFromCString, env)
}



void Konan_Platform_setMemoryLeakChecker(KBoolean value) {
  g_checkLeaks = value;
}

bool Kotlin_cleanersLeakCheckerEnabled() {
    return g_checkLeakedCleaners;
}

KBoolean Konan_Platform_getCleanersLeakChecker() {
    return g_checkLeakedCleaners;
}

void Konan_Platform_setCleanersLeakChecker(KBoolean value) {
    g_checkLeakedCleaners = value;
}

bool Kotlin_forceCheckedShutdown() {
    return g_forceCheckedShutdown;
}

KBoolean Kotlin_Debugging_getForceCheckedShutdown() {
    return g_forceCheckedShutdown;
}

void Kotlin_Debugging_setForceCheckedShutdown(KBoolean value) {
    g_forceCheckedShutdown = value;
}

KBoolean Kotlin_Debugging_isThreadStateRunnable() {
    return kotlin::GetThreadState() == kotlin::ThreadState::kRunnable;
}

KBoolean Kotlin_Debugging_isThreadStateNative() {
    return kotlin::GetThreadState() == kotlin::ThreadState::kNative;
}

KBoolean Kotlin_Debugging_isPermanent(KRef obj) {
    return obj->permanent();
}

RUNTIME_NOTHROW KBoolean Kotlin_Debugging_isLocal(KRef obj) {
    return obj->local();
}

static void CallInitGlobalAwaitInitialized(int *state) {
    int localState;
    // Switch to the native state to avoid dead-locks.
    {
        kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
        do {
            localState = std_support::atomic_ref{*state}.load(std::memory_order_acquire);
        } while (localState != FILE_INITIALIZED && localState != FILE_FAILED_TO_INITIALIZE);
    }
    if (localState == FILE_FAILED_TO_INITIALIZE) ThrowFileFailedToInitializeException(nullptr);
}

NO_INLINE void CallInitGlobalPossiblyLock(int* state, void (*init)()) {
    int localState = std_support::atomic_ref{*state}.load(std::memory_order_acquire);
    if (localState == FILE_INITIALIZED) return;
    if (localState == FILE_FAILED_TO_INITIALIZE)
        ThrowFileFailedToInitializeException(nullptr);
    int threadId = konan::currentThreadId();
    if ((localState & 3) == FILE_BEING_INITIALIZED) {
        if ((localState & ~3) != (threadId << 2)) {
            CallInitGlobalAwaitInitialized(state);
        }
        return;
    }
    if (std_support::atomic_compare_swap_strong(std_support::atomic_ref{*state}, FILE_NOT_INITIALIZED, FILE_BEING_INITIALIZED | (threadId << 2)) == FILE_NOT_INITIALIZED) {
        // actual initialization
        try {
            CurrentFrameGuard guard;
            init();
        } catch (ExceptionObjHolder& e) {
            ObjHolder holder;
            auto *exception = Kotlin_getExceptionObject(&e, holder.slot());
            std_support::atomic_ref{*state}.store(FILE_FAILED_TO_INITIALIZE, std::memory_order_release);
            ThrowFileFailedToInitializeException(exception);
        }
        std_support::atomic_ref{*state}.store(FILE_INITIALIZED, std::memory_order_release);
    } else {
        CallInitGlobalAwaitInitialized(state);
    }
}

void CallInitThreadLocal(int volatile* globalState, int* localState, void (*init)()) {
    if (*localState == FILE_FAILED_TO_INITIALIZE || (globalState != nullptr && *globalState == FILE_FAILED_TO_INITIALIZE))
        ThrowFileFailedToInitializeException(nullptr);
    *localState = FILE_INITIALIZED;
    try {
        CurrentFrameGuard guard;
        init();
    } catch(ExceptionObjHolder& e) {
        ObjHolder holder;
        auto *exception = Kotlin_getExceptionObject(&e, holder.slot());
        *localState = FILE_FAILED_TO_INITIALIZE;
        ThrowFileFailedToInitializeException(exception);
    }
}

}  // extern "C"
