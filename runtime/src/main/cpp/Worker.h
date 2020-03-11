#ifndef RUNTIME_WORKER_H
#define RUNTIME_WORKER_H

#include "Common.h"
#include "Types.h"

class Worker;

KInt GetWorkerId(Worker* worker);

Worker* WorkerInit(KBoolean errorReporting);
void WorkerDeinit(Worker* worker);
// Clean up all associated thread state, if this was a native worker.
void WorkerDestroyThreadDataIfNeeded(KInt id);
// Wait until all terminating native workers finish termination. Expected to be called once.
void WaitNativeWorkersTermination();

Worker* WorkerSuspend();
void WorkerResume(Worker* worker);

#endif // RUNTIME_WORKER_H