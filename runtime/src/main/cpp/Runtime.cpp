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
#include "Exceptions.h"
#include "Memory.h"
#include "Porting.h"
#include "Runtime.h"

struct RuntimeState {
  MemoryState* memoryState;
};

typedef void (*Initializer)(int initialize);
struct InitNode {
    Initializer      init;
    InitNode* next;
};

namespace {

InitNode* initHeadNode = nullptr;
InitNode* initTailNode = nullptr;

void InitOrDeinitGlobalVariables(int initialize) {
  InitNode *currNode = initHeadNode;
  while (currNode != nullptr) {
    currNode->init(initialize);
    currNode = currNode->next;
  }
}

THREAD_LOCAL_VARIABLE RuntimeState* runtimeState = nullptr;

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

// TODO: properly use RuntimeState.
RuntimeState* InitRuntime() {
  SetKonanTerminateHandler();
  RuntimeState* result = konanConstructInstance<RuntimeState>();
  result->memoryState = InitMemory();
  // Keep global variables in state as well.
  InitOrDeinitGlobalVariables(true);
  konan::consoleInit();
  runtimeState = result;
  return result;
}

void DeinitRuntime(RuntimeState* state) {
  if (state != nullptr) {
    InitOrDeinitGlobalVariables(false);
    DeinitMemory(state->memoryState);
    konanDestructInstance(state);
  }
}

void Kotlin_initRuntimeIfNeeded() {
  if (runtimeState == nullptr) {
    runtimeState = InitRuntime();
    // Register runtime deinit function at thread cleanup.
    konan::onThreadExit(Kotlin_deinitRuntimeIfNeeded);
#ifndef KONAN_WASM
    // `onThreadExit` doesn't work on main thread, use `atexit`:

    static bool deinitScheduledAtexit = false;
    if (!deinitScheduledAtexit) {
      deinitScheduledAtexit = true; // Having data race is OK here.
      ::atexit(Kotlin_deinitRuntimeIfNeeded);
    }
#endif
  }
}

void Kotlin_deinitRuntimeIfNeeded() {
  if (runtimeState != nullptr) {
     DeinitRuntime(runtimeState);
     runtimeState = nullptr;
  }
}

}  // extern "C"
