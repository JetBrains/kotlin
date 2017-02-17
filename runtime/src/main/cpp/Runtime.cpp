#include <locale.h>

#include "Runtime.h"

struct RuntimeState {
  MemoryState* memoryState;
};

namespace {

InitNode* initHeadNode = nullptr;
InitNode* initTailNode = nullptr;

void InitGlobalVariables() {
  InitNode *currNode = initHeadNode;
  while (currNode != nullptr) {
    currNode->init();
    currNode = currNode->next;
  }
}

}  // namespace

#ifdef __cplusplus
extern "C" {
#endif

void AppendToInitializersTail(struct InitNode *next) {
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
   // Set Unicode locale, otherwise towlower() and friends do not work properly.
  if (setlocale(LC_CTYPE, "en_US.UTF-8") == nullptr) {
    return nullptr;
  }
  RuntimeState* result = new RuntimeState();
  result->memoryState = InitMemory();
  // Keep global variables in state as well.
  InitGlobalVariables();
  return result;
}

void DeinitRuntime(RuntimeState* state) {
   DeinitMemory(state->memoryState);
   delete state;
}

#ifdef __cplusplus
}
#endif
