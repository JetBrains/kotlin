/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "Alloc.h"
#include "Atomic.h"
#include "Cleaner.h"
#include "Exceptions.h"
#include "KAssert.h"
#include "Memory.h"
#include "ObjCExportInit.h"
#include "Porting.h"
#include "Runtime.h"
#include "Worker.h"

typedef void (*Initializer)(int initialize, MemoryState* memory);
struct InitNode {
  Initializer init;
  InitNode* next;
};

// This global is overriden by the compiler.
RUNTIME_WEAK DestroyRuntimeMode Kotlin_destroyRuntimeMode = DESTROY_RUNTIME_ON_SHUTDOWN;

DestroyRuntimeMode Kotlin_getDestroyRuntimeMode() {
    return Kotlin_destroyRuntimeMode;
}

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

inline bool isValidRuntime() {
  return ::runtimeState != kInvalidRuntime;
}

volatile int aliveRuntimesCount = 0;

enum GlobalRuntimeStatus {
    kGlobalRuntimeUninitialized = 0,
    kGlobalRuntimeRunning,
    kGlobalRuntimeShutdown,
};

volatile GlobalRuntimeStatus globalRuntimeStatus = kGlobalRuntimeUninitialized;

RuntimeState* initRuntime() {
  SetKonanTerminateHandler();
  RuntimeState* result = konanConstructInstance<RuntimeState>();
  if (!result) return kInvalidRuntime;
  RuntimeCheck(!isValidRuntime(), "No active runtimes allowed");
  ::runtimeState = result;

  bool firstRuntime = false;
  switch (Kotlin_getDestroyRuntimeMode()) {
      case DESTROY_RUNTIME_LEGACY:
          compareAndSwap(&globalRuntimeStatus, kGlobalRuntimeUninitialized, kGlobalRuntimeRunning);
          result->memoryState = InitMemory(false); // The argument will be ignored for legacy DestroyRuntimeMode
          result->worker = WorkerInit(true);
          firstRuntime = atomicAdd(&aliveRuntimesCount, 1) == 1;
          break;
      case DESTROY_RUNTIME_ON_SHUTDOWN:
          // First update `aliveRuntimesCount` and then update `globalRuntimeStatus`, for synchronization with
          // runtime shutdown, which does it the other way around.
          atomicAdd(&aliveRuntimesCount, 1);
          auto lastStatus = compareAndSwap(&globalRuntimeStatus, kGlobalRuntimeUninitialized, kGlobalRuntimeRunning);
          if (Kotlin_forceCheckedShutdown()) {
              RuntimeAssert(lastStatus != kGlobalRuntimeShutdown, "Kotlin runtime was shut down. Cannot create new runtimes.");
          }
          firstRuntime = lastStatus == kGlobalRuntimeUninitialized;
          result->memoryState = InitMemory(firstRuntime);
          result->worker = WorkerInit(true);
  }

  InitOrDeinitGlobalVariables(ALLOC_THREAD_LOCAL_GLOBALS, result->memoryState);
  CommitTLSStorage(result->memoryState);
  // Keep global variables in state as well.
  if (firstRuntime) {
    konan::consoleInit();
#if KONAN_OBJC_INTEROP
    Kotlin_ObjCExport_initialize();
#endif
    InitOrDeinitGlobalVariables(INIT_GLOBALS, result->memoryState);
  }
  InitOrDeinitGlobalVariables(INIT_THREAD_LOCAL_GLOBALS, result->memoryState);
  RuntimeAssert(result->status == RuntimeStatus::kUninitialized, "Runtime must still be in the uninitialized state");
  result->status = RuntimeStatus::kRunning;
  return result;
}

void deinitRuntime(RuntimeState* state, bool destroyRuntime) {
  RuntimeAssert(state->status == RuntimeStatus::kRunning, "Runtime must be in the running state");
  state->status = RuntimeStatus::kDestroying;
  // This may be called after TLS is zeroed out, so ::runtimeState and ::memoryState in Memory cannot be trusted.
  // TODO: This may in fact reallocate TLS without guarantees that it'll be deallocated again.
  ::runtimeState = state;
  RestoreMemory(state->memoryState);
  bool lastRuntime = atomicAdd(&aliveRuntimesCount, -1) == 0;
  switch (Kotlin_getDestroyRuntimeMode()) {
    case DESTROY_RUNTIME_LEGACY:
      destroyRuntime = lastRuntime;
      break;
    case DESTROY_RUNTIME_ON_SHUTDOWN:
      // Nothing to do.
      break;
  }
  ClearTLS(state->memoryState);
  if (destroyRuntime)
    InitOrDeinitGlobalVariables(DEINIT_GLOBALS, state->memoryState);
  auto workerId = GetWorkerId(state->worker);
  WorkerDeinit(state->worker);
  DeinitMemory(state->memoryState, destroyRuntime);
  konanDestructInstance(state);
  WorkerDestroyThreadDataIfNeeded(workerId);
  ::runtimeState = kInvalidRuntime;
}

void Kotlin_deinitRuntimeCallback(void* argument) {
  auto* state = reinterpret_cast<RuntimeState*>(argument);
  deinitRuntime(state, false);
}

}  // namespace

extern "C" {

void AppendToInitializersTail(InitNode *next) {
  // TODO: use RuntimeState.
  if (initHeadNode == nullptr) {
    initHeadNode = next;
  } else {
    initTailNode->next = next;
  }
  initTailNode = next;
}

void Kotlin_initRuntimeIfNeeded() {
  if (!isValidRuntime()) {
    initRuntime();
    // Register runtime deinit function at thread cleanup.
    konan::onThreadExit(Kotlin_deinitRuntimeCallback, runtimeState);
  }
}

void Kotlin_deinitRuntimeIfNeeded() {
  if (isValidRuntime()) {
    deinitRuntime(::runtimeState, false);
  }
}

// TODO: Consider exporting it to interop API.
void Kotlin_shutdownRuntime() {
    auto* runtime = ::runtimeState;
    RuntimeAssert(runtime != kInvalidRuntime, "Current thread must have Kotlin runtime initialized on it");

    bool needsFullShutdown = false;
    switch (Kotlin_getDestroyRuntimeMode()) {
        case DESTROY_RUNTIME_LEGACY:
            needsFullShutdown = true;
            break;
        case DESTROY_RUNTIME_ON_SHUTDOWN:
            needsFullShutdown = Kotlin_forceCheckedShutdown() || Kotlin_memoryLeakCheckerEnabled() || Kotlin_cleanersLeakCheckerEnabled();
            break;
    }
    if (!needsFullShutdown) {
        auto lastStatus = compareAndSwap(&globalRuntimeStatus, kGlobalRuntimeRunning, kGlobalRuntimeShutdown);
        RuntimeAssert(lastStatus == kGlobalRuntimeRunning, "Invalid runtime status for shutdown");
        return;
    }

    if (Kotlin_cleanersLeakCheckerEnabled()) {
        // Make sure to collect any lingering cleaners.
        PerformFullGC(runtime->memoryState);
    }

    // Stop cleaner worker. Only execute the cleaners if checker is enabled.
    ShutdownCleaners(Kotlin_cleanersLeakCheckerEnabled());

    // Cleaners are now done, disallow new runtimes.
    auto lastStatus = compareAndSwap(&globalRuntimeStatus, kGlobalRuntimeRunning, kGlobalRuntimeShutdown);
    RuntimeAssert(lastStatus == kGlobalRuntimeRunning, "Invalid runtime status for shutdown");

    bool canDestroyRuntime = true;

    // TODO: When legacy mode is gone, this `if` will become unnecessary.
    if (Kotlin_forceCheckedShutdown() || Kotlin_memoryLeakCheckerEnabled() || Kotlin_cleanersLeakCheckerEnabled()) {
        // First make sure workers are gone.
        WaitNativeWorkersTermination();

        // Now check for existence of any other runtimes.
        auto otherRuntimesCount = atomicGet(&aliveRuntimesCount) - 1;
        RuntimeAssert(otherRuntimesCount >= 0, "Cannot be negative");
        if (Kotlin_forceCheckedShutdown()) {
            if (otherRuntimesCount > 0) {
                konan::consoleErrorf("Cannot run checkers when there are %d alive runtimes at the shutdown", otherRuntimesCount);
                konan::abort();
            }
        } else {
            // Cannot destroy runtime globally if there're some other threads with Kotlin runtime on them.
            canDestroyRuntime = otherRuntimesCount == 0;
        }
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
#elif KONAN_WASM
  return 6;
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
#elif KONAN_MIPS32
  return 5;
#elif KONAN_MIPSEL32
  return 6;
#elif KONAN_WASM
  return 7;
#else
#warning "Unknown CPU"
  return 0;
#endif
}

KInt Konan_Platform_getMemoryModel() {
    return static_cast<KInt>(CurrentMemoryModel);
}

KBoolean Konan_Platform_isDebugBinary() {
  return KonanNeedDebugInfo ? true : false;
}

bool Kotlin_memoryLeakCheckerEnabled() {
  return g_checkLeaks;
}

KBoolean Konan_Platform_getMemoryLeakChecker() {
  return g_checkLeaks;
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
    switch (Kotlin_getDestroyRuntimeMode()) {
        case DESTROY_RUNTIME_LEGACY:
            // Only applicable to ON_SHUTDOWN modes.
            return;
        case DESTROY_RUNTIME_ON_SHUTDOWN:
            break;
    }
    g_forceCheckedShutdown = value;
}

}  // extern "C"
