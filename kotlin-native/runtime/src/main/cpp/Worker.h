#ifndef RUNTIME_WORKER_H
#define RUNTIME_WORKER_H

#include "Types.h"

class Worker;

using MemoryState = kotlin::mm::ThreadData;

KInt GetWorkerId(Worker* worker);

Worker* WorkerInit(MemoryState* memoryState);
void WorkerDeinit(Worker* worker);
// Clean up all associated thread state, if this was a native worker.
void WorkerDestroyThreadDataIfNeeded(KInt id);

#endif // RUNTIME_WORKER_H
