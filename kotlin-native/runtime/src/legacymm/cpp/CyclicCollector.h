#ifndef RUNTIME_CYCLIC_COLLECTOR_H
#define RUNTIME_CYCLIC_COLLECTOR_H

struct ObjHeader;

void cyclicInit();
void cyclicDeinit(bool enabled);
void cyclicAddWorker(void* worker);
void cyclicRemoveWorker(void* worker, bool enabled);
void cyclicAddAtomicRoot(ObjHeader* obj);
void cyclicRemoveAtomicRoot(ObjHeader* obj);
void cyclicMutateAtomicRoot(ObjHeader* newValue);
void cyclicCollectorCallback(void* worker);
void cyclicLocalGC();
void cyclicScheduleGarbageCollect();

#endif  // RUNTIME_CYCLIC_COLLECTOR_H