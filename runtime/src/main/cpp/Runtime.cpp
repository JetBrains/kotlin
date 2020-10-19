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

struct RuntimeState {
  MemoryState* memoryState;
  Worker* worker;
  volatile int executionStatus;
};

typedef void (*Initializer)(int initialize, MemoryState* memory);
struct InitNode {
  Initializer init;
  InitNode* next;
};

namespace {

InitNode* initHeadNode = nullptr;
InitNode* initTailNode = nullptr;

enum {
  INIT_GLOBALS = 0,
  INIT_THREAD_LOCAL_GLOBALS = 1,
  DEINIT_THREAD_LOCAL_GLOBALS = 2,
  DEINIT_GLOBALS = 3
};

enum {
  SUSPENDED = 0,
  RUNNING,
  DESTROYING
};

bool updateStatusIf(RuntimeState* state, int oldStatus, int newStatus) {
#if KONAN_NO_THREADS
    if (state->executionStatus == oldStatus) {
        state->executionStatus = newStatus;
        return true;
    }
    return false;
#else
    return __sync_bool_compare_and_swap(&state->executionStatus, oldStatus, newStatus);
#endif
}

void InitOrDeinitGlobalVariables(int initialize, MemoryState* memory) {
  InitNode* currentNode = initHeadNode;
  while (currentNode != nullptr) {
    currentNode->init(initialize, memory);
    currentNode = currentNode->next;
  }
}

KBoolean g_checkLeaks = KonanNeedDebugInfo;
KBoolean g_checkLeakedCleaners = KonanNeedDebugInfo;

constexpr RuntimeState* kInvalidRuntime = nullptr;

THREAD_LOCAL_VARIABLE RuntimeState* runtimeState = kInvalidRuntime;
THREAD_LOCAL_VARIABLE int isMainThread = 0;

inline bool isValidRuntime() {
  return ::runtimeState != kInvalidRuntime;
}

volatile int aliveRuntimesCount = 0;

RuntimeState* initRuntime() {
  SetKonanTerminateHandler();
  RuntimeState* result = konanConstructInstance<RuntimeState>();
  if (!result) return kInvalidRuntime;
  RuntimeCheck(!isValidRuntime(), "No active runtimes allowed");
  ::runtimeState = result;
  result->memoryState = InitMemory();
  result->worker = WorkerInit(true);
  bool firstRuntime = atomicAdd(&aliveRuntimesCount, 1) == 1;
  // Keep global variables in state as well.
  if (firstRuntime) {
    isMainThread = 1;
    konan::consoleInit();
#if KONAN_OBJC_INTEROP
    Kotlin_ObjCExport_initialize();
#endif
    InitOrDeinitGlobalVariables(INIT_GLOBALS, result->memoryState);
  }
  InitOrDeinitGlobalVariables(INIT_THREAD_LOCAL_GLOBALS, result->memoryState);
  return result;
}

void deinitRuntime(RuntimeState* state) {
  ResumeMemory(state->memoryState);
  bool lastRuntime = atomicAdd(&aliveRuntimesCount, -1) == 0;
  InitOrDeinitGlobalVariables(DEINIT_THREAD_LOCAL_GLOBALS, state->memoryState);
  if (lastRuntime)
    InitOrDeinitGlobalVariables(DEINIT_GLOBALS, state->memoryState);
  auto workerId = GetWorkerId(state->worker);
  WorkerDeinit(state->worker);
  DeinitMemory(state->memoryState);
  konanDestructInstance(state);
  WorkerDestroyThreadDataIfNeeded(workerId);
}

void Kotlin_deinitRuntimeCallback(void* argument) {
  auto* state = reinterpret_cast<RuntimeState*>(argument);
  RuntimeCheck(updateStatusIf(state, RUNNING, DESTROYING), "Cannot transition state to DESTROYING");
  deinitRuntime(state);
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
    RuntimeCheck(updateStatusIf(::runtimeState, SUSPENDED, RUNNING), "Cannot transition state to RUNNING for init");
    // Register runtime deinit function at thread cleanup.
    konan::onThreadExit(Kotlin_deinitRuntimeCallback, runtimeState);
  }
}

void Kotlin_deinitRuntimeIfNeeded() {
  if (isValidRuntime()) {
    deinitRuntime(::runtimeState);
    ::runtimeState = kInvalidRuntime;
  }
}

RuntimeState* Kotlin_createRuntime() {
  return initRuntime();
}

void Kotlin_destroyRuntime(RuntimeState* state) {
 RuntimeCheck(updateStatusIf(state, SUSPENDED, DESTROYING), "Cannot transition state to DESTROYING");
 deinitRuntime(state);
}

RuntimeState* Kotlin_suspendRuntime() {
    RuntimeCheck(isValidRuntime(), "Runtime must be active on the current thread");
    auto result = ::runtimeState;
    RuntimeCheck(updateStatusIf(result, RUNNING, SUSPENDED), "Cannot transition state to SUSPENDED for suspend");
    result->memoryState = SuspendMemory();
    result->worker = WorkerSuspend();
    ::runtimeState = kInvalidRuntime;
    return result;
}

void Kotlin_resumeRuntime(RuntimeState* state) {
    RuntimeCheck(!isValidRuntime(), "Runtime must not be active on the current thread");
    RuntimeCheck(updateStatusIf(state, SUSPENDED, RUNNING), "Cannot transition state to RUNNING for resume");
    ::runtimeState = state;
    ResumeMemory(state->memoryState);
    WorkerResume(state->worker);
}

RuntimeState* Kotlin_getRuntime() {
  RuntimeCheck(isValidRuntime(), "Runtime must be active on the current thread");
  return ::runtimeState;
}

bool Kotlin_hasRuntime() {
  return isValidRuntime();
}

void CheckIsMainThread() {
  if (!isMainThread)
    ThrowIncorrectDereferenceException();
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
  return IsStrictMemoryModel ? 0 : 1;
}

KBoolean Konan_Platform_isDebugBinary() {
  return KonanNeedDebugInfo ? true : false;
}

void Kotlin_zeroOutTLSGlobals() {
  if (runtimeState != nullptr && runtimeState->memoryState != nullptr)
    InitOrDeinitGlobalVariables(DEINIT_THREAD_LOCAL_GLOBALS, runtimeState->memoryState);
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

}  // extern "C"
