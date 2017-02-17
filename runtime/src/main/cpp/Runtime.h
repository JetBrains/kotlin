#ifndef RUNTIME_RUNTIME_H
#define RUNTIME_RUNTIME_H

#include "Types.h"

struct RuntimeState;

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*Initializer)();
struct InitNode {
    Initializer      init;
    struct InitNode* next;
};

void AppendToInitializersTail(struct InitNode*);

RuntimeState* InitRuntime();
void DeinitRuntime(RuntimeState* state);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_RUNTIME_H
