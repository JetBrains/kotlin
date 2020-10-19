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

#ifndef RUNTIME_RUNTIME_H
#define RUNTIME_RUNTIME_H

#include "Porting.h"

struct RuntimeState;
struct InitNode;

#ifdef __cplusplus
extern "C" {
#endif

void Kotlin_initRuntimeIfNeeded();
void Kotlin_deinitRuntimeIfNeeded();

// Operations below allow flexible runtime scheduling on different threads.
// Created runtime is in SUSPENDED state, and need to be resumed for actual execution.
RuntimeState* Kotlin_createRuntime();
// Runtime must be in SUSPENDED state, before it could be destroyed.
void Kotlin_destroyRuntime(RuntimeState*);

// Transition current runtime from RUNNING to SUSPENDED state, and clearing thread local variable caching
// the runtime. After suspension, runtime could be rescheduled to a different thread.
RuntimeState* Kotlin_suspendRuntime();
// Transition runtime from SUSPENDED to RUNNING state, and sets thread local variable caching
// the runtime. After resume, current thread could be used for executing Kotlin code.
void Kotlin_resumeRuntime(RuntimeState*);

// Gets currently active runtime, fails if no runtime is currently available.
RuntimeState* Kotlin_getRuntime();

bool Kotlin_hasRuntime();

// Appends given node to an initializer list.
void AppendToInitializersTail(struct InitNode*);

// Zero out all Kotlin thread local globals.
void Kotlin_zeroOutTLSGlobals();

bool Kotlin_memoryLeakCheckerEnabled();

bool Kotlin_cleanersLeakCheckerEnabled();

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_RUNTIME_H
