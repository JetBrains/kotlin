/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_RUNTIME_H
#define RUNTIME_RUNTIME_H

#include "Porting.h"
#include "Memory.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*Initializer)(int initialize, MemoryState* memory);
struct InitNode {
  Initializer init;
  InitNode* next;
};

// For experimental MM, if runtime gets initialized, it will be in the native state after this.
RUNTIME_NOTHROW void Kotlin_initRuntimeIfNeeded();
void deinitRuntimeIfNeeded();

// Can only be called once.
// No new runtimes can be initialized on any thread after this.
// Must be called on a thread with active runtime.
// Using already initialized runtimes on any thread after this is undefined behaviour.
void Kotlin_shutdownRuntime();

// Appends given node to an initializer list.
RUNTIME_NOTHROW void AppendToInitializersTail(struct InitNode*);

void CallInitGlobalPossiblyLock(int* state, void (*init)());
void CallInitThreadLocal(int volatile* globalState, int* localState, void (*init)());

bool Kotlin_memoryLeakCheckerEnabled();

bool Kotlin_cleanersLeakCheckerEnabled();

bool Kotlin_forceCheckedShutdown();

#ifdef __cplusplus
} // extern "C"
#endif

namespace kotlin {

// Returns `true` if initialized.
bool initializeGlobalRuntimeIfNeeded() noexcept;

}

#endif // RUNTIME_RUNTIME_H
