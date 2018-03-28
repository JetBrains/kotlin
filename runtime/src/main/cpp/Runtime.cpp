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
#include "Atomic.h"

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

enum {
  INIT_GLOBALS = 0,
  DEINIT_THREAD_LOCAL_GLOBALS = 1,
  DEINIT_GLOBALS = 2
};

void InitOrDeinitGlobalVariables(int initialize) {
  InitNode *currNode = initHeadNode;
  while (currNode != nullptr) {
    currNode->init(initialize);
    currNode = currNode->next;
  }
}

THREAD_LOCAL_VARIABLE RuntimeState* runtimeState = nullptr;

int aliveRuntimesCount = 0;

RuntimeState* initRuntime() {
  SetKonanTerminateHandler();
  RuntimeState* result = konanConstructInstance<RuntimeState>();
  if (!result) return nullptr;
  result->memoryState = InitMemory();
  // Keep global variables in state as well.
  InitOrDeinitGlobalVariables(INIT_GLOBALS);
  konan::consoleInit();
  atomicAdd(&aliveRuntimesCount, 1);
  return result;
}

void deinitRuntime(RuntimeState* state) {
  bool lastRuntime = atomicAdd(&aliveRuntimesCount, -1) == 0;
  InitOrDeinitGlobalVariables(DEINIT_THREAD_LOCAL_GLOBALS);
  if (lastRuntime)
    InitOrDeinitGlobalVariables(DEINIT_GLOBALS);
  DeinitMemory(state->memoryState);
  konanDestructInstance(state);
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
  if (runtimeState == nullptr) {
    runtimeState = initRuntime();
    // Register runtime deinit function at thread cleanup.
    konan::onThreadExit(Kotlin_deinitRuntimeIfNeeded);
  }
}

void Kotlin_deinitRuntimeIfNeeded() {
  if (runtimeState != nullptr) {
     deinitRuntime(runtimeState);
     runtimeState = nullptr;
  }
}

}  // extern "C"
