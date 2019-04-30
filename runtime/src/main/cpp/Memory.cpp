/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

#include <string.h>
#include <stdio.h>

#include <cstddef> // for offsetof

#include "Alloc.h"
#include "KAssert.h"
#include "Atomic.h"
#include "Exceptions.h"
#include "KString.h"
#include "Memory.h"
#include "MemoryPrivate.hpp"
#include "Natives.h"
#include "Porting.h"
#include "Runtime.h"

// If garbage collection algorithm for cyclic garbage to be used.
// We are using the Bacon's algorithm for GC, see
// http://researcher.watson.ibm.com/researcher/files/us-bacon/Bacon03Pure.pdf.
#define USE_GC 1
// Define to 1 to print all memory operations.
#define TRACE_MEMORY 0
// Define to 1 to print major GC events.
#define TRACE_GC 0
// Collect memory manager events statistics.
#define COLLECT_STATISTIC 0
// Auto-adjust GC thresholds.
#define GC_ERGONOMICS 1

namespace {

// Granularity of arena container chunks.
constexpr container_size_t kContainerAlignment = 1024;
// Single object alignment.
constexpr container_size_t kObjectAlignment = 8;

// Required e.g. for object size computations to be correct.
static_assert(sizeof(ContainerHeader) % kObjectAlignment == 0, "sizeof(ContainerHeader) is not aligned");

#if TRACE_MEMORY
#undef TRACE_GC
#define TRACE_GC 1
#define MEMORY_LOG(...) konan::consolePrintf(__VA_ARGS__);
#else
#define MEMORY_LOG(...)
#endif

#if TRACE_GC
#define GC_LOG(...) konan::consolePrintf(__VA_ARGS__);
#else
#define GC_LOG(...)
#endif

#if USE_GC
// Collection threshold default (collect after having so many elements in the
// release candidates set).
constexpr size_t kGcThreshold = 16 * 1024;
#if GC_ERGONOMICS
// Ergonomic thresholds.
// If GC to computations time ratio is above that value,
// increase GC threshold by 1.5 times.
constexpr double kGcToComputeRatioThreshold = 0.5;
// Never exceed this value when increasing GC threshold.
constexpr size_t kMaxErgonomicThreshold = 1024 * 1024;
#endif  // GC_ERGONOMICS
// Threshold of size for toFree set, triggering actual cycle collector.
constexpr size_t kMaxToFreeSize = 8 * 1024;
// How many elements in finalizer queue allowed before cleaning it up.
constexpr size_t kFinalizerQueueThreshold = 32;
#endif  // USE_GC

}  // namespace

typedef KStdUnorderedSet<ContainerHeader*> ContainerHeaderSet;
typedef KStdVector<ContainerHeader*> ContainerHeaderList;
typedef KStdVector<KRef*> KRefPtrList;
typedef KStdDeque<ContainerHeader*> ContainerHeaderDeque;

// A little hack that allows to enable -O2 optimizations
// Prevents clang from replacing FrameOverlay struct
// with single pointer.
// Can be removed when FrameOverlay will become more complex.
FrameOverlay exportFrameOverlay;

// Current number of allocated containers.
volatile int allocCount = 0;
volatile int aliveMemoryStatesCount = 0;

// Forward declarations.
void freeContainer(ContainerHeader* header) NO_INLINE;
#if USE_GC
void garbageCollect(MemoryState* state, bool force) NO_INLINE;
void rememberNewContainer(ContainerHeader* container);
#endif  // USE_GC

#if COLLECT_STATISTIC
class MemoryStatistic {
public:
  // UpdateRef per-object type counters.
  uint64_t updateCounters[10][10];
  // Alloc per container type counters.
  uint64_t containerAllocs[2];
  // Free per container type counters.
  uint64_t objectAllocs[5];
  // Histogram of allocation size distribution.
  KStdUnorderedMap<int, int>* allocationHistogram;
  // Number of allocation cache hits.
  int allocCacheHit;
  // Number of allocation cache misses.
  int allocCacheMiss;
  // Number of regular reference increments.
  uint64_t addRefs;
  // Number of atomic reference increments.
  uint64_t atomicAddRefs;
  // Number of regular reference decrements.
  uint64_t releaseRefs;
  // Number of atomic reference decrements.
  uint64_t atomicReleaseRefs;
  // Number of potential cycle candidates.
  uint64_t releaseCyclicRefs;

  // Map of array index to human readable name.
  static constexpr const char* indexToName[] = {
    "normal", "stack ", "perm  ", "frozen", "null  " };

  void init() {
    memset(containerAllocs, 0, sizeof(containerAllocs));
    memset(objectAllocs, 0, sizeof(objectAllocs));
    memset(updateCounters, 0, sizeof(updateCounters));
    allocationHistogram = konanConstructInstance<KStdUnorderedMap<int, int>>();
    allocCacheHit = 0;
    allocCacheMiss = 0;
  }

  void deinit() {
    konanDestructInstance(allocationHistogram);
    allocationHistogram = nullptr;
  }

  void incAddRef(const ContainerHeader* header, bool atomic, int stack) {
    if (atomic) atomicAddRefs++; else addRefs++;
  }

  void incReleaseRef(const ContainerHeader* header, bool atomic, bool cyclic, int stack) {
   if (atomic) {
      atomicReleaseRefs++;
    } else {
      if (cyclic) releaseCyclicRefs++; else releaseRefs++;
    }
  }

  void incUpdateRef(const ObjHeader* objOld, const ObjHeader* objNew, int stack) {
    updateCounters[toIndex(objOld, stack)][toIndex(objNew, stack)]++;
  }

  void incAlloc(size_t size, const ContainerHeader* header) {
    containerAllocs[0]++;
    ++(*allocationHistogram)[size];
  }

  void incFree(const ContainerHeader* header) {
    containerAllocs[1]++;
  }

  void incAlloc(size_t size, const ObjHeader* header) {
    objectAllocs[toIndex(header, 0)]++;
  }

  static int toIndex(const ObjHeader* obj, int stack) {
    if (reinterpret_cast<uintptr_t>(obj) > 1)
        return toIndex(obj->container(), stack);
    else
        return 4 + stack * 5;
  }

  static int toIndex(const ContainerHeader* header, int stack) {
    if (header == nullptr) return 2 + stack * 5; // permanent.
    switch (header->tag()) {
      case CONTAINER_TAG_NORMAL   : return 0 + stack * 5;
      case CONTAINER_TAG_STACK    : return 1 + stack * 5;
      case CONTAINER_TAG_FROZEN:    return 3 + stack * 5;
    }
    RuntimeAssert(false, "unknown container type");
    return -1;
  }

  static double percents(uint64_t value, uint64_t all) {
   return all == 0 ? 0 : ((double)value / (double)all) * 100.0;
  }

  void printStatistic() {
    konan::consolePrintf("\nMemory manager statistic:\n\n");
    konan::consolePrintf("Container alloc: %lld, free: %lld\n",
                           containerAllocs[0], containerAllocs[1]);
    for (int i = 0; i < 5; i++) {
      // Only normal and frozen can be allocated.
      if (i == 0 || i == 3)
        konan::consolePrintf("Object %s alloc: %lld\n", indexToName[i], objectAllocs[i]);
    }
    konan::consolePrintf("\n");

    uint64_t allUpdateRefs = 0, heapUpdateRefs = 0, stackUpdateRefs = 0;
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
         allUpdateRefs += updateCounters[i][j];
         if (i < 5 && j < 5)
           heapUpdateRefs += updateCounters[i][j];
         if (i >= 5 && j >= 5)
           stackUpdateRefs += updateCounters[i][j];
      }
    }
    konan::consolePrintf("Total updates: %lld, stack: %lld(%.2lf%%), heap: %lld(%.2lf%%)\n",
        allUpdateRefs,
        stackUpdateRefs, percents(stackUpdateRefs, allUpdateRefs),
        heapUpdateRefs, percents(heapUpdateRefs, allUpdateRefs));
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        if (updateCounters[i][j] != 0)
            konan::consolePrintf("UpdateHeapRef[%s -> %s]: %lld (%.2lf%% of all, %.2lf%% of heap)\n",
                             indexToName[i], indexToName[j], updateCounters[i][j],
                             percents(updateCounters[i][j], allUpdateRefs),
                             percents(updateCounters[i][j], heapUpdateRefs));
      }
    }
    for (int i = 5; i < 10; i++) {
        for (int j = 5; j < 10; j++) {
            if (updateCounters[i][j] != 0)
                konan::consolePrintf("UpdateStackRef[%s -> %s]: %lld (%.2lf%% of all, %.2lf%% of stack)\n",
                                 indexToName[i - 5], indexToName[j - 5],
                                 updateCounters[i][j],
                                 percents(updateCounters[i][j], allUpdateRefs),
                                 percents(updateCounters[i][j], stackUpdateRefs));
        }
    }
    konan::consolePrintf("\n");

    konan::consolePrintf("Allocation histogram:\n");
    KStdVector<int> keys(allocationHistogram->size());
    int index = 0;
    for (auto& it : *allocationHistogram) {
      keys[index++] = it.first;
    }
    std::sort(keys.begin(), keys.end());
    int perLine = 4;
    int count = 0;
    for (auto it : keys) {
      konan::consolePrintf(
          "%d bytes -> %d times  ", it, (*allocationHistogram)[it]);
      if (++count % perLine == (perLine - 1) || (count == keys.size()))
        konan::consolePrintf("\n");
    }


    uint64_t allAddRefs = addRefs + atomicAddRefs;
    uint64_t allReleases = releaseRefs + atomicReleaseRefs + releaseCyclicRefs;
    konan::consolePrintf("AddRefs:\t%lld/%lld (%.2lf%% of atomic)\n"
                         "Releases:\t%lld/%lld (%.2lf%% of atomic)\n"
                         "ReleaseRefs affecting cycle collector   : %lld (%.2lf%% of cyclic)\n",
                         addRefs, atomicAddRefs, percents(atomicAddRefs, allAddRefs),
                         releaseRefs, atomicReleaseRefs, percents(atomicReleaseRefs, allReleases),
                         releaseCyclicRefs, percents(releaseCyclicRefs, allReleases));
  }
};

constexpr const char* MemoryStatistic::indexToName[];

#endif  // COLLECT_STATISTIC

struct MemoryState {
#if TRACE_MEMORY
  // Set of all containers.
  ContainerHeaderSet* containers;
#endif

#if USE_GC
  // Finalizer queue - linked list of containers scheduled for finalization.
  ContainerHeader* finalizerQueue;
  int finalizerQueueSize;
  int finalizerQueueSuspendCount;
  /*
   * Typical scenario for GC is as following:
   * we have 90% of objects with refcount = 0 which will be deleted during
   * the first phase of the algorithm.
   * We could mark them with a bit in order to tell the next two phases to skip them
   * and thus requiring only one list, but the downside is that both of the
   * next phases would iterate over the whole list of objects instead of only 10%.
   */
  ContainerHeaderList* toFree; // List of all cycle candidates.
  ContainerHeaderList* roots; // Real candidates excluding those with refcount = 0.
  // How many GC suspend requests happened.
  int gcSuspendCount;
  // How many candidate elements in toRelease shall trigger collection.
  size_t gcThreshold;
  // If collection is in progress.
  bool gcInProgress;
  // Objects to be released.
  ContainerHeaderList* toRelease;

#if GC_ERGONOMICS
  uint64_t lastGcTimestamp;
#endif

#endif // USE_GC

#if COLLECT_STATISTIC
  #define CONTAINER_ALLOC_STAT(state, size, container) state->statistic.incAlloc(size, container);
  #define CONTAINER_DESTROY_STAT(state, container) \
    state->statistic.incFree(container);
  #define OBJECT_ALLOC_STAT(state, size, object) \
    state->statistic.incAlloc(size, object); \
    state->statistic.incAddRef(object->container(), 0, 0);
  #define UPDATE_REF_STAT(state, oldRef, newRef, slot, stack) \
    state->statistic.incUpdateRef(oldRef, newRef, stack);
  #define UPDATE_ADDREF_STAT(state, obj, atomic, stack) \
      state->statistic.incAddRef(obj, atomic, stack);
  #define UPDATE_RELEASEREF_STAT(state, obj, atomic, cyclic, stack) \
        state->statistic.incReleaseRef(obj, atomic, cyclic, stack);
  #define INIT_STAT(state) \
    state->statistic.init();
  #define DEINIT_STAT(state) \
    state->statistic.deinit();
  #define PRINT_STAT(state) \
    state->statistic.printStatistic();
  MemoryStatistic statistic;
#else
  #define CONTAINER_ALLOC_STAT(state, size, container)
  #define CONTAINER_DESTROY_STAT(state, container)
  #define OBJECT_ALLOC_STAT(state, size, object)
  #define UPDATE_REF_STAT(state, oldRef, newRef, slot, stack)
  #define UPDATE_ADDREF_STAT(state, obj, atomic, stack)
  #define UPDATE_RELEASEREF_STAT(state, obj, atomic, cyclic, stack)
  #define INIT_STAT(state)
  #define DEINIT_STAT(state)
  #define PRINT_STAT(state)
#endif // COLLECT_STATISTIC
};

#if TRACE_MEMORY
#define INIT_TRACE(state) \
  memoryState->containers = konanConstructInstance<ContainerHeaderSet>();
#define DEINIT_TRACE(state) \
   konanDestructInstance(memoryState->containers); \
   memoryState->containers = nullptr;
#else
#define INIT_TRACE(state)
#define DEINIT_TRACE(state)
#endif
#define CONTAINER_ALLOC_TRACE(state, size, container) \
  MEMORY_LOG("Container alloc %d at %p\n", size, container)
#define CONTAINER_DESTROY_TRACE(state, container) \
  MEMORY_LOG("Container destroy %p\n", container)
#define OBJECT_ALLOC_TRACE(state, size, object) \
  MEMORY_LOG("Object alloc %d at %p\n", size, object)
#define UPDATE_REF_TRACE(state, oldRef, newRef, slot, stack) \
  MEMORY_LOG("UpdateRef %s*%p: %p -> %p\n", stack ? "stack " : "heap ", slot, oldRef, newRef)

// Events macro definitions.
// Called on worker's memory init.
#define INIT_EVENT(state) \
  INIT_STAT(state) \
  INIT_TRACE(state)
// Called on worker's memory deinit.
#define DEINIT_EVENT(state) \
  DEINIT_STAT(state)
// Called on container allocation.
#define CONTAINER_ALLOC_EVENT(state, size, container) \
  CONTAINER_ALLOC_STAT(state, size, container) \
  CONTAINER_ALLOC_TRACE(state, size, container)
// Called on container destroy (memory is released to allocator).
#define CONTAINER_DESTROY_EVENT(state, container) \
  CONTAINER_DESTROY_STAT(state, container) \
  CONTAINER_DESTROY_TRACE(state, container)
// Object was just allocated.
#define OBJECT_ALLOC_EVENT(state, size, object) \
  OBJECT_ALLOC_STAT(state, size, object) \
  OBJECT_ALLOC_TRACE(state, size, object)
// Object is freed.
#define OBJECT_FREE_EVENT(state, size, object)  \
  OBJECT_FREE_STAT(state, size, object) \
  OBJECT_FREE_TRACE(state, object)
// Reference in memory is being updated.
#define UPDATE_REF_EVENT(state, oldRef, newRef, slot, stack) \
  UPDATE_REF_STAT(state, oldRef, newRef, slot, stack) \
  UPDATE_REF_TRACE(state, oldRef, newRef, slot, stack)
// Infomation shall be printed as worker is exiting.
#define PRINT_EVENT(state) \
  PRINT_STAT(state)

namespace {

// Container for a single object.
class ObjectContainer : public Container {
 public:
  // Single instance.
  explicit ObjectContainer(MemoryState* state, const TypeInfo* type_info) {
    Init(state, type_info);
  }

  // Object container shalln't have any dtor, as it's being freed by
  // ::Release().

  ObjHeader* GetPlace() const {
    return reinterpret_cast<ObjHeader*>(header_ + 1);
  }

 private:
  void Init(MemoryState* state, const TypeInfo* type_info);
};


class ArrayContainer : public Container {
 public:
  ArrayContainer(MemoryState* state, const TypeInfo* type_info, uint32_t elements) {
    Init(state, type_info, elements);
  }

  // Array container shalln't have any dtor, as it's being freed by ::Release().

  ArrayHeader* GetPlace() const {
    return reinterpret_cast<ArrayHeader*>(header_ + 1);
  }

 private:
  void Init(MemoryState* state, const TypeInfo* type_info, uint32_t elements);
};

// Class representing arena-style placement container.
// Container is used for reference counting, and it is assumed that objects
// with related placement will share container. Only
// whole container can be freed, individual objects are not taken into account.
class ArenaContainer;

struct ContainerChunk {
  ContainerChunk* next;
  ArenaContainer* arena;
  // Then we have ContainerHeader here.
  ContainerHeader* asHeader() {
    return reinterpret_cast<ContainerHeader*>(this + 1);
  }
};

class ArenaContainer {
 public:
  void Init();
  void Deinit();

  // Place individual object in this container.
  ObjHeader* PlaceObject(const TypeInfo* type_info);

  // Places an array of certain type in this container. Note that array_type_info
  // is type info for an array, not for an individual element. Also note that exactly
  // same operation could be used to place strings.
  ArrayHeader* PlaceArray(const TypeInfo* array_type_info, container_size_t count);

  ObjHeader** getSlot();

 private:
  void* place(container_size_t size);

  bool allocContainer(container_size_t minSize);

  void setHeader(ObjHeader* obj, const TypeInfo* typeInfo) {
    obj->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    obj->setContainer(currentChunk_->asHeader());
    // Here we do not take into account typeInfo's immutability for ARC strategy, as there's no ARC.
  }

  ContainerChunk* currentChunk_;
  uint8_t* current_;
  uint8_t* end_;
  ArrayHeader* slots_;
  uint32_t slotsCount_;
};


// TODO: can we pass this variable as an explicit argument?
THREAD_LOCAL_VARIABLE MemoryState* memoryState = nullptr;
THREAD_LOCAL_VARIABLE FrameOverlay* currentFrame = nullptr;

constexpr int kFrameOverlaySlots = sizeof(FrameOverlay) / sizeof(ObjHeader**);

inline bool isFreeable(const ContainerHeader* header) {
  return header != nullptr && header->tag() != CONTAINER_TAG_STACK;
}

inline bool isArena(const ContainerHeader* header) {
  return header != nullptr && header->stack();
}

inline bool isAggregatingFrozenContainer(const ContainerHeader* header) {
  return header != nullptr && header->frozen() && header->objectCount() > 1;
}

inline container_size_t alignUp(container_size_t size, int alignment) {
  return (size + alignment - 1) & ~(alignment - 1);
}

inline ContainerHeader* realShareableContainer(ContainerHeader* container) {
  RuntimeAssert(container->shareable(), "Only makes sense on shareable objects");
  return reinterpret_cast<ObjHeader*>(container + 1)->container();
}

inline uint32_t arrayObjectSize(const TypeInfo* typeInfo, uint32_t count) {
  // Note: array body is aligned, but for size computation it is enough to align the sum.
  static_assert(kObjectAlignment % alignof(KLong) == 0, "");
  static_assert(kObjectAlignment % alignof(KDouble) == 0, "");
  return alignUp(sizeof(ArrayHeader) - typeInfo->instanceSize_ * count, kObjectAlignment);
}

inline uint32_t arrayObjectSize(const ArrayHeader* obj) {
  return arrayObjectSize(obj->type_info(), obj->count_);
}

// TODO: shall we do padding for alignment?
inline container_size_t objectSize(const ObjHeader* obj) {
  const TypeInfo* type_info = obj->type_info();
  container_size_t size = (type_info->instanceSize_ < 0 ?
      // An array.
      arrayObjectSize(obj->array())
      :
      type_info->instanceSize_);
  return alignUp(size, kObjectAlignment);
}

inline FrameOverlay* asFrameOverlay(ObjHeader** slot) {
  return reinterpret_cast<FrameOverlay*>(slot);
}

inline bool isRefCounted(KConstRef object) {
  return isFreeable(object->container());
}

inline void lock(KInt* spinlock) {
  while (compareAndSwap(spinlock, 0, 1) != 0) {}
}

inline void unlock(KInt* spinlock) {
  RuntimeCheck(compareAndSwap(spinlock, 1, 0) == 1, "Must succeed");
}

} // namespace

ObjHeader* KRefSharedHolder::ref() const {
  verifyRefOwner();
  return obj_;
}

void KRefSharedHolder::initRefOwner() {
  RuntimeAssert(owner_ == nullptr, "Must be uninitialized");
  owner_ = memoryState;
}

void KRefSharedHolder::verifyRefOwner() const {
  // Note: checking for 'shareable()' and retrieving 'type_info()'
  // are supposed to be correct even for unowned object.
  if (owner_ != memoryState) {
    // Initialized runtime is required to throw the exception below
    // or to provide proper execution context for shared objects:
    if (memoryState == nullptr) Kotlin_initRuntimeIfNeeded();
    auto* container = obj_->container();
    if (!Shareable(container)) {
      // TODO: add some info about the owner.
      ThrowIllegalObjectSharingException(obj_->type_info(), obj_);
    }
  }
}

extern "C" {

void objc_release(void* ptr);
void Kotlin_ObjCExport_releaseAssociatedObject(void* associatedObject);
RUNTIME_NORETURN void ThrowFreezingException(KRef toFreeze, KRef blocker);

}  // extern "C"

void runDeallocationHooks(ContainerHeader* container) {
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(container + 1);

  for (int index = 0; index < container->objectCount(); index++) {
    if (obj->has_meta_object()) {
      ObjHeader::destroyMetaObject(&obj->typeInfoOrMeta_);
    }

    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }
}

void DeinitInstanceBody(const TypeInfo* typeInfo, void* body) {
  for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
    ObjHeader** location = reinterpret_cast<ObjHeader**>(
        reinterpret_cast<uintptr_t>(body) + typeInfo->objOffsets_[index]);
    ZeroHeapRef(location);
  }
}

namespace {

template<typename func>
inline void traverseContainerObjectFields(ContainerHeader* container, func process) {
  RuntimeAssert(!isAggregatingFrozenContainer(container), "Must not be called on such containers");
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(container + 1);
  for (int object = 0; object < container->objectCount(); object++) {
    const TypeInfo* typeInfo = obj->type_info();
    if (typeInfo != theArrayTypeInfo) {
      for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
        ObjHeader** location = reinterpret_cast<ObjHeader**>(
            reinterpret_cast<uintptr_t>(obj) + typeInfo->objOffsets_[index]);
        process(location);
      }
    } else {
      ArrayHeader* array = obj->array();
      for (int index = 0; index < array->count_; index++) {
        process(ArrayAddressOfElementAt(array, index));
      }
    }
    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }
}

template<typename func>
inline void traverseContainerReferredObjects(ContainerHeader* container, func process) {
  traverseContainerObjectFields(container, [process](ObjHeader** location) {
    ObjHeader* ref = *location;
    if (ref != nullptr) process(ref);
  });
}

inline bool isMarkedAsRemoved(ContainerHeader* container) {
  return (reinterpret_cast<uintptr_t>(container) & 1) != 0;
}

inline ContainerHeader* markAsRemoved(ContainerHeader* container) {
  return reinterpret_cast<ContainerHeader*>(reinterpret_cast<uintptr_t>(container) | 1);
}

inline ContainerHeader* clearRemoved(ContainerHeader* container) {
  return reinterpret_cast<ContainerHeader*>(
    reinterpret_cast<uintptr_t>(container) & ~static_cast<uintptr_t>(1));
}

#if USE_GC

void processFinalizerQueue(MemoryState* state) {
  // TODO: reuse elements of finalizer queue for new allocations.
  while (state->finalizerQueue != nullptr) {
    auto* container = state->finalizerQueue;
    state->finalizerQueue = container->nextLink();
    state->finalizerQueueSize--;
#if TRACE_MEMORY
    state->containers->erase(container);
#endif
    CONTAINER_DESTROY_EVENT(state, container)
    konanFreeMemory(container);
    atomicAdd(&allocCount, -1);
  }
  RuntimeAssert(state->finalizerQueueSize == 0, "Queue must be empty here");
}
#endif

void scheduleDestroyContainer(MemoryState* state, ContainerHeader* container) {
#if USE_GC
  RuntimeAssert(container != nullptr, "Cannot destroy null container");
  container->setNextLink(state->finalizerQueue);
  state->finalizerQueue = container;
  state->finalizerQueueSize++;
  // We cannot clean finalizer queue while in GC.
  if (!state->gcInProgress && state->finalizerQueueSuspendCount == 0 &&
      state->finalizerQueueSize >= kFinalizerQueueThreshold) {
    processFinalizerQueue(state);
  }
#else
  konanFreeMemory(container);
  atomicAdd(&allocCount, -1);
  CONTAINER_DESTROY_EVENT(state, container);
#endif
}

#if !USE_GC

template <bool Atomic>
inline void IncrementRC(ContainerHeader* container) {
  container->incRefCount<Atomic>();
}

template <bool Atomic, bool UseCycleCollector>
inline void DecrementRC(ContainerHeader* container) {
  if (container->decRefCount<Atomic>() == 0) {
    freeContainer(container);
  }
}

inline void DecrementRC(ContainerHeader* container) {
  if (Shareable(container))
    DecrementRC<true, false>(container);
  else
    DecrementRC<false, false>(container);
}

template <bool CanCollect>
inline void EnqueueDecrementRC(ContainerHeader* container) {
  RuntimeCheck(false, "Not yet implemeneted");
}

#else // USE_GC

template <bool Atomic>
inline void IncrementRC(ContainerHeader* container) {
  container->incRefCount<Atomic>();
}

template <bool Atomic, bool UseCycleCollector>
inline void DecrementRC(ContainerHeader* container) {
  // TODO: enable me, once account for inner references in frozen objects correctly.
  // RuntimeAssert(container->refCount() > 0, "Must be positive");
  if (container->decRefCount<Atomic>() == 0) {
    freeContainer(container);
  } else if (UseCycleCollector) { // Possible root.
    RuntimeAssert(container->refCount() > 0, "Must be positive");
    RuntimeAssert(!Atomic && !container->shareable(), "Cycle collector shalln't be used with shared objects yet");
    RuntimeAssert(container->objectCount() == 1, "cycle collector shall only work with single object containers");
    // We do not use cycle collector for frozen objects, as we already detected
    // possible cycles during freezing.
    // Also do not use cycle collector for provable acyclic objects.
    int color = container->color();
    if (color != CONTAINER_TAG_GC_PURPLE && color != CONTAINER_TAG_GC_GREEN) {
      container->setColorAssertIfGreen(CONTAINER_TAG_GC_PURPLE);
      if (!container->buffered()) {
        auto* state = memoryState;
        container->setBuffered();
        if (state->toFree != nullptr) {
          state->toFree->push_back(container);
          MEMORY_LOG("toFree is now %d\n", state->toFree->size())
          if (state->gcSuspendCount == 0 && state->toRelease->size() >= state->gcThreshold) {
            GC_LOG("Calling GC from DecrementRC: %d\n", state->toRelease->size())
            garbageCollect(state, false);
          }
        }
      }
    }
  }
}

inline void DecrementRC(ContainerHeader* container) {
  auto* state = memoryState;
  RuntimeAssert(state->gcInProgress, "Must only be called during GC");
  // TODO: enable me, once account for inner references in frozen objects correctly.
  // RuntimeAssert(container->refCount() > 0, "Must be positive");
  bool useCycleCollector = container->tag() == CONTAINER_TAG_NORMAL;
  if (container->decRefCount() == 0) {
    freeContainer(container);
  } else if (useCycleCollector && state->toFree != nullptr) {
      RuntimeAssert(container->refCount() > 0, "Must be positive");
      RuntimeAssert(!container->shareable(), "Cycle collector shalln't be used with shared objects yet");
      RuntimeAssert(container->objectCount() == 1, "cycle collector shall only work with single object containers");
      // We do not use cycle collector for frozen objects, as we already detected
      // possible cycles during freezing.
      // Also do not use cycle collector for provable acyclic objects.
      int color = container->color();
      if (color != CONTAINER_TAG_GC_PURPLE && color != CONTAINER_TAG_GC_GREEN) {
        container->setColorAssertIfGreen(CONTAINER_TAG_GC_PURPLE);
        if (!container->buffered()) {
          container->setBuffered();
          state->toFree->push_back(container);
        }
      }
  }
}

template <bool CanCollect>
inline void EnqueueDecrementRC(ContainerHeader* container) {
  auto* state = memoryState;
  if (CanCollect) {
    if (state->toRelease->size() >= state->gcThreshold && state->gcSuspendCount == 0) {
      GC_LOG("Calling GC from EnqueueDecrementRC: %d\n", state->toRelease->size())
      garbageCollect(state, false);
    }
  }
  state->toRelease->push_back(container);
}

inline void initGcThreshold(MemoryState* state, uint32_t gcThreshold) {
  state->gcThreshold = gcThreshold;
  state->toRelease->reserve(gcThreshold);
}

#if GC_ERGONOMICS
inline void increaseGcThreshold(MemoryState* state) {
  auto newThreshold = state->gcThreshold * 3 / 2 + 1;
  if (newThreshold <= kMaxErgonomicThreshold) {
    initGcThreshold(state, newThreshold);
  }
}
#endif  // GC_ERGONOMICS

#endif // USE_GC

#if TRACE_MEMORY && USE_GC

const char* colorNames[] = {"BLACK", "GRAY", "WHITE", "PURPLE", "GREEN", "ORANGE", "RED"};

void dumpObject(ObjHeader* ref, int indent) {
  for (int i = 0; i < indent; i++) MEMORY_LOG(" ");
  auto* typeInfo = ref->type_info();
  auto* packageName =
    typeInfo->packageName_ != nullptr ? CreateCStringFromString(typeInfo->packageName_) : nullptr;
  auto* relativeName =
    typeInfo->relativeName_ != nullptr ? CreateCStringFromString(typeInfo->relativeName_) : nullptr;
  MEMORY_LOG("%p %s.%s\n", ref,
    packageName ? packageName : "<unknown>", relativeName ? relativeName : "<unknown>");
  if (packageName) konan::free(packageName);
  if (relativeName) konan::free(relativeName);
}

void dumpContainerContent(ContainerHeader* container) {
  if (container->refCount() < 0) {
    MEMORY_LOG("%p has negative RC %d, likely a memory bug\n", container, container->refCount())
    return;
  }
  if (isAggregatingFrozenContainer(container)) {
    MEMORY_LOG("%s aggregating container %p with %d objects rc=%d\n",
               colorNames[container->color()], container, container->objectCount(), container->refCount());
    ContainerHeader** subContainer = reinterpret_cast<ContainerHeader**>(container + 1);
    for (int i = 0; i < container->objectCount(); ++i) {
      ContainerHeader* sub = *subContainer++;
      MEMORY_LOG("    container %p\n ", sub);
      dumpContainerContent(sub);
    }
  } else {
    MEMORY_LOG("%s regular %s%scontainer %p with %d objects rc=%d\n",
               colorNames[container->color()],
               container->frozen() ? "frozen " : "",
               container->stack() ? "stack " : "",
               container, container->objectCount(),
               container->refCount());
    ObjHeader* obj = reinterpret_cast<ObjHeader*>(container + 1);
    dumpObject(obj, 4);
  }
}

void dumpWorker(const char* prefix, ContainerHeader* header, ContainerHeaderSet* seen) {
  dumpContainerContent(header);
  seen->insert(header);
  if (!isAggregatingFrozenContainer(header)) {
    traverseContainerReferredObjects(header, [prefix, seen](ObjHeader* ref) {
      auto* child = ref->container();
      RuntimeAssert(!isArena(child), "A reference to local object is encountered");
      if (child != nullptr && (seen->count(child) == 0)) {
        dumpWorker(prefix, child, seen);
      }
    });
  }
}

void dumpReachable(const char* prefix, const ContainerHeaderSet* roots) {
  ContainerHeaderSet seen;
  for (auto* container : *roots) {
    dumpWorker(prefix, container, &seen);
  }
}

#endif

#if USE_GC

void MarkRoots(MemoryState*);
void ScanRoots(MemoryState*);
void CollectRoots(MemoryState*);
void Scan(ContainerHeader* container);

template<bool useColor>
void MarkGray(ContainerHeader* start) {
  ContainerHeaderDeque toVisit;
  toVisit.push_front(start);

  while (!toVisit.empty()) {
    auto* container = toVisit.front();
    MEMORY_LOG("MarkGray visit %p [%s]\n", container, colorNames[container->color()]);
    toVisit.pop_front();
    if (useColor) {
      int color = container->color();
      if (color == CONTAINER_TAG_GC_GRAY) continue;
      // If see an acyclic object not being garbage - ignore it. We must properly traverse garbage, although.
      if (color == CONTAINER_TAG_GC_GREEN && container->refCount() != 0) {
        continue;
      }
      // Only garbage green object could be recolored here.
      container->setColorEvenIfGreen(CONTAINER_TAG_GC_GRAY);
    } else {
      if (container->marked()) continue;
      container->mark();
    }

    traverseContainerReferredObjects(container, [&toVisit](ObjHeader* ref) {
      auto* childContainer = ref->container();
      RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
      if (!Shareable(childContainer)) {
        childContainer->decRefCount<false>();
        toVisit.push_front(childContainer);
      }
    });
  }
}

template<bool useColor>
void ScanBlack(ContainerHeader* start) {
  ContainerHeaderDeque toVisit;
  toVisit.push_front(start);
  while (!toVisit.empty()) {
    auto* container = toVisit.front();
    MEMORY_LOG("ScanBlack visit %p [%s]\n", container, colorNames[container->color()]);
    toVisit.pop_front();
    if (useColor) {
      auto color = container->color();
      if (color == CONTAINER_TAG_GC_GREEN || color == CONTAINER_TAG_GC_BLACK) continue;
      container->setColorAssertIfGreen(CONTAINER_TAG_GC_BLACK);
    } else {
      if (!container->marked()) continue;
      container->unMark();
    }
    traverseContainerReferredObjects(container, [&toVisit](ObjHeader* ref) {
        auto childContainer = ref->container();
        RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
        if (!Shareable(childContainer)) {
          childContainer->incRefCount<false>();
          if (useColor) {
            int color = childContainer->color();
            if (color != CONTAINER_TAG_GC_BLACK)
              toVisit.push_front(childContainer);
          } else {
            if (childContainer->marked())
              toVisit.push_front(childContainer);
          }
        }
    });
  }
}

void CollectWhite(MemoryState*, ContainerHeader* container);

void CollectCycles(MemoryState* state) {
  MarkRoots(state);
  ScanRoots(state);
  CollectRoots(state);
  state->toFree->clear();
  state->roots->clear();
}

void MarkRoots(MemoryState* state) {
  for (auto container : *(state->toFree)) {
    if (isMarkedAsRemoved(container))
      continue;
    // Acyclic containers cannot be in this list.
    RuntimeCheck(container->color() != CONTAINER_TAG_GC_GREEN, "Must not be green");
    auto color = container->color();
    auto rcIsZero = container->refCount() == 0;
    if (color == CONTAINER_TAG_GC_PURPLE && !rcIsZero) {
      MarkGray<true>(container);
      state->roots->push_back(container);
    } else {
      container->resetBuffered();
      RuntimeAssert(color != CONTAINER_TAG_GC_GREEN, "Must not be green");
      if (color == CONTAINER_TAG_GC_BLACK && rcIsZero) {
        scheduleDestroyContainer(state, container);
      }
    }
  }
}

void ScanRoots(MemoryState* state) {
  for (auto* container : *(state->roots)) {
    Scan(container);
  }
}

void CollectRoots(MemoryState* state) {
  // Here we might free some objects and call deallocation hooks on them,
  // which in turn might call DecrementRC and trigger new GC - forbid that.
  state->gcSuspendCount++;
  for (auto* container : *(state->roots)) {
    container->resetBuffered();
    CollectWhite(state, container);
  }
  state->gcSuspendCount--;
}

void Scan(ContainerHeader* start) {
  ContainerHeaderDeque toVisit;
  toVisit.push_front(start);

  while (!toVisit.empty()) {
     auto* container = toVisit.front();
     toVisit.pop_front();
     if (container->color() != CONTAINER_TAG_GC_GRAY) continue;
     if (container->refCount() != 0) {
       ScanBlack<true>(container);
       continue;
     }
     container->setColorAssertIfGreen(CONTAINER_TAG_GC_WHITE);
     traverseContainerReferredObjects(container, [&toVisit](ObjHeader* ref) {
       auto* childContainer = ref->container();
       RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
       if (!Shareable(childContainer)) {
         toVisit.push_front(childContainer);
       }
     });
   }
}

void CollectWhite(MemoryState* state, ContainerHeader* start) {
   ContainerHeaderDeque toVisit;
   toVisit.push_back(start);

   while (!toVisit.empty()) {
     auto* container = toVisit.front();
     toVisit.pop_front();
     if (container->color() != CONTAINER_TAG_GC_WHITE || container->buffered()) continue;
     container->setColorAssertIfGreen(CONTAINER_TAG_GC_BLACK);
     traverseContainerObjectFields(container, [state, &toVisit](ObjHeader** location) {
        auto* ref = *location;
        if (ref == nullptr) return;
        auto* childContainer = ref->container();
        RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
        if (Shareable(childContainer)) {
          ZeroHeapRef(location);
        } else {
          toVisit.push_front(childContainer);
        }
     });
    runDeallocationHooks(container);
    scheduleDestroyContainer(state, container);
  }
}
#endif

inline bool needAtomicAccess(ContainerHeader* container) {
  return container->shareable();
}

inline bool canBeCyclic(ContainerHeader* container) {
  if (container->refCount() == 1) return false;
  if (container->color() == CONTAINER_TAG_GC_GREEN) return false;
  return true;
}

inline void AddHeapRef(ContainerHeader* container) {
  MEMORY_LOG("AddHeapRef %p: rc=%d\n", container, container->refCount())
  UPDATE_ADDREF_STAT(memoryState, container, needAtomicAccess(container), 0)
  switch (container->tag()) {
    case CONTAINER_TAG_STACK:
      break;
    case CONTAINER_TAG_NORMAL:
      IncrementRC</* Atomic = */ false>(container);
      break;
    /* case CONTAINER_TAG_FROZEN: case CONTAINER_TAG_ATOMIC: */
    default:
      IncrementRC</* Atomic = */ true>(container);
      break;
  }
}

inline void AddHeapRef(const ObjHeader* header) {
  auto* container = header->container();
  if (container != nullptr)
    AddHeapRef(const_cast<ContainerHeader*>(container));
}

inline void ReleaseHeapRef(ContainerHeader* container) {
  MEMORY_LOG("ReleaseHeapRef %p: rc=%d\n", container, container->refCount())
  UPDATE_RELEASEREF_STAT(memoryState, container, needAtomicAccess(container), canBeCyclic(container), 0)
  if (container->tag() != CONTAINER_TAG_STACK) {
    EnqueueDecrementRC</* CanCollect = */ true>(container);
  }
}

inline void ReleaseHeapRef(const ObjHeader* header) {
  auto* container = header->container();
  if (container != nullptr)
    ReleaseHeapRef(const_cast<ContainerHeader*>(container));
}

// We use first slot as place to store frame-local arena container.
// TODO: create ArenaContainer object on the stack, so that we don't
// do two allocations per frame (ArenaContainer + actual container).
inline ArenaContainer* initedArena(ObjHeader** auxSlot) {
  auto frame = asFrameOverlay(auxSlot);
  auto arena = reinterpret_cast<ArenaContainer*>(frame->arena);
  if (!arena) {
    arena = konanConstructInstance<ArenaContainer>();
    MEMORY_LOG("Initializing arena in %p\n", frame)
    arena->Init();
    frame->arena = arena;
  }
  return arena;
}

inline size_t containerSize(const ContainerHeader* container) {
  size_t result = 0;
  const ObjHeader* obj = reinterpret_cast<const ObjHeader*>(container + 1);
  for (int object = 0; object < container->objectCount(); object++) {
    size_t size = objectSize(obj);
    result += size;
    obj = reinterpret_cast<ObjHeader*>(reinterpret_cast<uintptr_t>(obj) + size);
  }
  return result;
}

}  // namespace

MetaObjHeader* ObjHeader::createMetaObject(TypeInfo** location) {
  TypeInfo* typeInfo = *location;
  RuntimeCheck(!hasPointerBits(typeInfo, OBJECT_TAG_MASK), "Object must not be tagged");

#if !KONAN_NO_THREADS
  if (typeInfo->typeInfo_ != typeInfo) {
    // Someone installed a new meta-object since the check.
    return reinterpret_cast<MetaObjHeader*>(typeInfo);
  }
#endif

  MetaObjHeader* meta = konanConstructInstance<MetaObjHeader>();
  meta->typeInfo_ = typeInfo;
#if KONAN_NO_THREADS
  *location = reinterpret_cast<TypeInfo*>(meta);
#else
  TypeInfo* old = __sync_val_compare_and_swap(location, typeInfo, reinterpret_cast<TypeInfo*>(meta));
  if (old != typeInfo) {
    // Someone installed a new meta-object since the check.
    konanFreeMemory(meta);
    meta = reinterpret_cast<MetaObjHeader*>(old);
  }
#endif
  return meta;
}

void ObjHeader::destroyMetaObject(TypeInfo** location) {
  MetaObjHeader* meta = clearPointerBits(*(reinterpret_cast<MetaObjHeader**>(location)), OBJECT_TAG_MASK);
  *const_cast<const TypeInfo**>(location) = meta->typeInfo_;
  if (meta->counter_ != nullptr) {
    WeakReferenceCounterClear(meta->counter_);
    ZeroHeapRef(&meta->counter_);
  }

#ifdef KONAN_OBJC_INTEROP
  Kotlin_ObjCExport_releaseAssociatedObject(meta->associatedObject_);
#endif

  konanFreeMemory(meta);
}

ContainerHeader* AllocContainer(MemoryState* state, size_t size) {
 ContainerHeader* result = nullptr;
#if USE_GC
  // We recycle elements of finalizer queue for new allocations, to avoid trashing memory manager.
  ContainerHeader* container = state != nullptr ? state->finalizerQueue : nullptr;
  ContainerHeader* previous = nullptr;
  while (container != nullptr) {
    // TODO: shall it be == instead?
    if (container->hasContainerSize() &&
        container->containerSize() >= size && container->containerSize() <= size + 16) {
      MEMORY_LOG("recycle %p for request %d\n", container, size)
      result = container;
      if (previous == nullptr)
        state->finalizerQueue = container->nextLink();
      else
        previous->setNextLink(container->nextLink());
      state->finalizerQueueSize--;
      memset(container, 0, size);
      break;
    }
    previous = container;
    container = container->nextLink();
  }
#endif
  if (result == nullptr) {
    result = konanConstructSizedInstance<ContainerHeader>(alignUp(size, kObjectAlignment));
    atomicAdd(&allocCount, 1);
  }
  if (state != nullptr) {
    CONTAINER_ALLOC_EVENT(state, size, result);
#if TRACE_MEMORY
    state->containers->insert(result);
#endif
  }
  return result;
}

ContainerHeader* AllocAggregatingFrozenContainer(KStdVector<ContainerHeader*>& containers) {
  auto componentSize = containers.size();
  auto* superContainer = AllocContainer(memoryState, sizeof(ContainerHeader) + sizeof(void*) * componentSize);
  auto* place = reinterpret_cast<ContainerHeader**>(superContainer + 1);
  for (auto* container : containers) {
    *place++ = container;
    // Set link to the new container.
    auto* obj = reinterpret_cast<ObjHeader*>(container + 1);
    obj->setContainer(superContainer);
    MEMORY_LOG("Set fictitious frozen container for %p: %p\n", obj, superContainer);
  }
  superContainer->setObjectCount(componentSize);
  superContainer->freeze();
  return superContainer;
}

void FreeAggregatingFrozenContainer(ContainerHeader* container) {
  auto* state = memoryState;
  RuntimeAssert(isAggregatingFrozenContainer(container), "expected fictitious frozen container");
  MEMORY_LOG("%p is fictitious frozen container\n", container);
  RuntimeAssert(!container->buffered(), "frozen objects must not participate in GC")
#if USE_GC
  // Forbid finalizerQueue handling.
  ++state->finalizerQueueSuspendCount;
#endif
  // Special container for frozen objects.
  ContainerHeader** subContainer = reinterpret_cast<ContainerHeader**>(container + 1);
  MEMORY_LOG("Total subcontainers = %d\n", container->objectCount());
  for (int i = 0; i < container->objectCount(); ++i) {
    MEMORY_LOG("Freeing subcontainer %p\n", *subContainer);
    freeContainer(*subContainer++);
  }
#if USE_GC
  --state->finalizerQueueSuspendCount;
#endif
  scheduleDestroyContainer(state, container);
  MEMORY_LOG("Freeing subcontainers done\n");
}

void freeContainer(ContainerHeader* container) {
  RuntimeAssert(container != nullptr, "this kind of container shalln't be freed");

  if (isAggregatingFrozenContainer(container)) {
    FreeAggregatingFrozenContainer(container);
    return;
  }

  runDeallocationHooks(container);

  // Now let's clean all object's fields in this container.
  traverseContainerObjectFields(container, [container](ObjHeader** location) {
      ZeroHeapRef(location);
  });

  // And release underlying memory.
  if (isFreeable(container)) {
    container->setColorEvenIfGreen(CONTAINER_TAG_GC_BLACK);
    if (!container->buffered())
      scheduleDestroyContainer(memoryState, container);
  }
}

void ObjectContainer::Init(MemoryState* state, const TypeInfo* typeInfo) {
  RuntimeAssert(typeInfo->instanceSize_ >= 0, "Must be an object");
  uint32_t alloc_size = sizeof(ContainerHeader) + typeInfo->instanceSize_;
  header_ = AllocContainer(state, alloc_size);
  RuntimeCheck(header_ != nullptr, "Cannot alloc memory");
  // One object in this container, no need to set.
  header_->setContainerSize(alloc_size);
  RuntimeAssert(header_->objectCount() == 1, "Must work properly");
  // header->refCount_ is zero initialized by AllocContainer().
  SetHeader(GetPlace(), typeInfo);
  OBJECT_ALLOC_EVENT(memoryState, typeInfo->instanceSize_, GetPlace())
}

void ArrayContainer::Init(MemoryState* state, const TypeInfo* typeInfo, uint32_t elements) {
  RuntimeAssert(typeInfo->instanceSize_ < 0, "Must be an array");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + arrayObjectSize(typeInfo, elements);
  header_ = AllocContainer(state, alloc_size);
  RuntimeCheck(header_ != nullptr, "Cannot alloc memory");
  // One object in this container, no need to set.
  header_->setContainerSize(alloc_size);
  RuntimeAssert(header_->objectCount() == 1, "Must work properly");
  // header->refCount_ is zero initialized by AllocContainer().
  GetPlace()->count_ = elements;
  SetHeader(GetPlace()->obj(), typeInfo);
  OBJECT_ALLOC_EVENT(memoryState, arrayObjectSize(typeInfo, elements), GetPlace()->obj())
}

// TODO: store arena containers in some reuseable data structure, similar to
// finalizer queue.
void ArenaContainer::Init() {
  allocContainer(1024);
}

void ArenaContainer::Deinit() {
  MEMORY_LOG("Arena::Deinit start: %p\n", this)
  auto chunk = currentChunk_;
  while (chunk != nullptr) {
    // freeContainer() doesn't release memory when CONTAINER_TAG_STACK is set.
    MEMORY_LOG("Arena::Deinit free chunk %p\n", chunk)
    freeContainer(chunk->asHeader());
    chunk = chunk->next;
  }
  chunk = currentChunk_;
  while (chunk != nullptr) {
    auto toRemove = chunk;
    chunk = chunk->next;
    konanFreeMemory(toRemove);
  }
}

bool ArenaContainer::allocContainer(container_size_t minSize) {
  auto size = minSize + sizeof(ContainerHeader) + sizeof(ContainerChunk);
  size = alignUp(size, kContainerAlignment);
  // TODO: keep simple cache of container chunks.
  ContainerChunk* result = konanConstructSizedInstance<ContainerChunk>(size);
  RuntimeCheck(result != nullptr, "Cannot alloc memory");
  if (result == nullptr) return false;
  result->next = currentChunk_;
  result->arena = this;
  result->asHeader()->refCount_ = (CONTAINER_TAG_STACK | CONTAINER_TAG_INCREMENT);
  currentChunk_ = result;
  current_ = reinterpret_cast<uint8_t*>(result->asHeader() + 1);
  end_ = reinterpret_cast<uint8_t*>(result) + size;
  return true;
}

void* ArenaContainer::place(container_size_t size) {
  size = alignUp(size, kObjectAlignment);
  // Fast path.
  if (current_ + size < end_) {
    void* result = current_;
    current_ += size;
    return result;
  }
  if (!allocContainer(size)) {
    return nullptr;
  }
  void* result = current_;
  current_ += size;
  RuntimeAssert(current_ <= end_, "Must not overflow");
  return result;
}

#define ARENA_SLOTS_CHUNK_SIZE 16

ObjHeader** ArenaContainer::getSlot() {
  if (slots_ == nullptr || slotsCount_ >= ARENA_SLOTS_CHUNK_SIZE) {
    slots_ = PlaceArray(theArrayTypeInfo, ARENA_SLOTS_CHUNK_SIZE);
    slotsCount_ = 0;
  }
  return ArrayAddressOfElementAt(slots_, slotsCount_++);
}

ObjHeader* ArenaContainer::PlaceObject(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  uint32_t size = type_info->instanceSize_;
  ObjHeader* result = reinterpret_cast<ObjHeader*>(place(size));
  if (!result) {
    return nullptr;
  }
  OBJECT_ALLOC_EVENT(memoryState, type_info->instanceSize_, result)
  currentChunk_->asHeader()->incObjectCount();
  setHeader(result, type_info);
  return result;
}

ArrayHeader* ArenaContainer::PlaceArray(const TypeInfo* type_info, uint32_t count) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  container_size_t size = arrayObjectSize(type_info, count);
  ArrayHeader* result = reinterpret_cast<ArrayHeader*>(place(size));
  if (!result) {
    return nullptr;
  }
  OBJECT_ALLOC_EVENT(memoryState, arrayObjectSize(type_info, count), result->obj())
  currentChunk_->asHeader()->incObjectCount();
  setHeader(result->obj(), type_info);
  result->count_ = count;
  return result;
}

void AddRefFromAssociatedObject(const ObjHeader* object) {
  AddHeapRef(const_cast<ObjHeader*>(object));
}

void ReleaseRefFromAssociatedObject(const ObjHeader* object) {
  ReleaseHeapRef(const_cast<ObjHeader*>(object));
}

#if USE_GC
void incrementStack(MemoryState* state) {
  FrameOverlay* frame = currentFrame;
  while (frame != nullptr) {
    ObjHeader** current = reinterpret_cast<ObjHeader**>(frame + 1) + frame->parameters;
    ObjHeader** end = current + frame->count - kFrameOverlaySlots - frame->parameters;
    while (current < end) {
      ObjHeader* obj = *current++;
      if (obj != nullptr) {
        auto* container = obj->container();
        if (container == nullptr) continue;
        if (container->shareable()) {
          IncrementRC<true>(container);
        } else {
          IncrementRC<false>(container);
        }
      }
    }
    frame = frame->previous;
  }
}

void processDecrements(MemoryState* state) {
  auto* toRelease = state->toRelease;
  state->gcSuspendCount++;
  while (toRelease->size() > 0) {
     auto* container = toRelease->back();
     toRelease->pop_back();
     if (isMarkedAsRemoved(container))
       continue;
     if (container->shareable())
       container = realShareableContainer(container);
     DecrementRC(container);
  }
  state->gcSuspendCount--;
}

void decrementStack(MemoryState* state) {
  state->gcSuspendCount++;
  FrameOverlay* frame = currentFrame;
  while (frame != nullptr) {
    ObjHeader** current = reinterpret_cast<ObjHeader**>(frame + 1) + frame->parameters;
    ObjHeader** end = current + frame->count - kFrameOverlaySlots - frame->parameters;
    while (current < end) {
      ObjHeader* obj = *current++;
      if (obj != nullptr) {
        auto* container = obj->container();
        if (container != nullptr)
          EnqueueDecrementRC</* CanCollect = */ false>(container);
      }
    }
    frame = frame->previous;
  }
  state->gcSuspendCount--;
}

void garbageCollect(MemoryState* state, bool force) {
  RuntimeAssert(!state->gcInProgress, "Recursive GC is disallowed");

  GC_LOG(">>> %s GC: threshold = %d toFree %d toRelease %d\n", \
     force ? "forced" : "regular", state->gcThreshold, state->toFree->size(), state->toRelease->size())

#if GC_ERGONOMICS
  auto gcStartTime = konan::getTimeMicros();
#endif

  state->gcInProgress = true;

  incrementStack(state);
  processDecrements(state);
  size_t beforeDecrements = state->toRelease->size();
  decrementStack(state);
  size_t afterDecrements = state->toRelease->size();
  ssize_t stackReferences = afterDecrements - beforeDecrements;
  if (stackReferences * 5 > state->gcThreshold) {
#if GC_ERGONOMICS
     increaseGcThreshold(state);
     GC_LOG("||| GC: too many stack references, increased threshold to \n", state->gcThreshold);
#else
     GC_LOG("Too many stack references for the threshold: %d vs %d\n", stackReferences, state->gcThreshold)
#endif
  }

  GC_LOG("||| GC: toFree %d toRelease %d\n", state->toFree->size(), state->toRelease->size())

  processFinalizerQueue(state);

  if (force || state->toFree->size() > kMaxToFreeSize) {
    while (state->toFree->size() > 0) {
      CollectCycles(state);
      processFinalizerQueue(state);
    }
  }

  state->gcInProgress = false;

#if GC_ERGONOMICS
  auto gcEndTime = konan::getTimeMicros();
  auto gcToComputeRatio = double(gcEndTime - gcStartTime) / (gcStartTime - state->lastGcTimestamp + 1);
  if (gcToComputeRatio > kGcToComputeRatioThreshold) {
     increaseGcThreshold(state);
     GC_LOG("Adjusting GC threshold to %d\n", state->gcThreshold);
  }
  GC_LOG("GC: duration=%lld sinceLast=%lld\n", (gcEndTime - gcStartTime), gcStartTime - state->lastGcTimestamp);
  state->lastGcTimestamp = gcEndTime;
#endif

  GC_LOG("<<< GC: toFree %d toRelease %d\n", state->toFree->size(), state->toRelease->size())
}

void rememberNewContainer(ContainerHeader* container) {
  if (container == nullptr) return;
  // Instances can be allocated before actual runtime init - be prepared for that.
  if (memoryState != nullptr) {
    IncrementRC</* Atomic = */ true>(container);
    // We cannot collect until reference will be stored into the stack slot.
    EnqueueDecrementRC</* CanCollect = */ true>(container);
  }
}

#endif  // USE_GC

extern "C" {

MemoryState* InitMemory() {
  RuntimeAssert(offsetof(ArrayHeader, typeInfoOrMeta_)
                ==
                offsetof(ObjHeader,   typeInfoOrMeta_),
                "Layout mismatch");
  RuntimeAssert(offsetof(TypeInfo, typeInfo_)
                ==
                offsetof(MetaObjHeader, typeInfo_),
                "Layout mismatch");
  RuntimeAssert(sizeof(FrameOverlay) % sizeof(ObjHeader**) == 0, "Frame overlay should contain only pointers")
  RuntimeAssert(memoryState == nullptr, "memory state must be clear");
  memoryState = konanConstructInstance<MemoryState>();
  INIT_EVENT(memoryState)
#if USE_GC
  memoryState->toFree = konanConstructInstance<ContainerHeaderList>();
  memoryState->roots = konanConstructInstance<ContainerHeaderList>();
  memoryState->gcInProgress = false;
  memoryState->gcSuspendCount = 0;
  memoryState->toRelease = konanConstructInstance<ContainerHeaderList>();
  initGcThreshold(memoryState, kGcThreshold);
#endif
  atomicAdd(&aliveMemoryStatesCount, 1);
  return memoryState;
}

void DeinitMemory(MemoryState* memoryState) {
#if USE_GC
  do {
    GC_LOG("Calling GarbageCollect from DeinitMemory()\n")
    garbageCollect(memoryState, true);
  } while (memoryState->toRelease->size() > 0);
  RuntimeAssert(memoryState->toFree->size() == 0, "Some memory have not been released after GC");
  RuntimeAssert(memoryState->toRelease->size() == 0, "Some memory have not been released after GC");
  konanDestructInstance(memoryState->toFree);
  konanDestructInstance(memoryState->roots);
  konanDestructInstance(memoryState->toRelease);
  RuntimeAssert(memoryState->finalizerQueue == nullptr, "Finalizer queue must be empty");
  RuntimeAssert(memoryState->finalizerQueueSize == 0, "Finalizer queue must be empty");

#endif // USE_GC

  bool lastMemoryState = atomicAdd(&aliveMemoryStatesCount, -1) == 0;

#if TRACE_MEMORY
  if (lastMemoryState && allocCount > 0) {
    MEMORY_LOG("*** Memory leaks, leaked %d containers ***\n", allocCount);
    dumpReachable("", memoryState->containers);
  }
#else
#if USE_GC
  if (lastMemoryState)
    RuntimeAssert(allocCount == 0, "Memory leaks found");
#endif
#endif

  PRINT_EVENT(memoryState)
  DEINIT_EVENT(memoryState)

  konanFreeMemory(memoryState);
  ::memoryState = nullptr;
}

MemoryState* SuspendMemory() {
    auto result = ::memoryState;
    ::memoryState = nullptr;
    return result;
}

void ResumeMemory(MemoryState* state) {
    RuntimeAssert(::memoryState == nullptr, "Cannot schedule on existing state");
    ::memoryState = state;
}

OBJ_GETTER(AllocInstance, const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  auto* state = memoryState;
  auto container = ObjectContainer(state, type_info);
#if USE_GC
  rememberNewContainer(container.header());
#endif  // USE_GC
  RETURN_OBJ(container.GetPlace());
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* type_info, int32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  if (elements < 0) ThrowIllegalArgumentException();
  auto* state = memoryState;
  auto container = ArrayContainer(state, type_info, elements);
#if USE_GC
  rememberNewContainer(container.header());
#endif  // USE_GC
  RETURN_OBJ(container.GetPlace()->obj());
}

OBJ_GETTER(InitInstance,
    ObjHeader** location, const TypeInfo* type_info, void (*ctor)(ObjHeader*)) {
  ObjHeader* value = *location;
  if (value != nullptr) {
    // OK'ish, inited by someone else.
    RETURN_OBJ(value);
  }
  ObjHeader* object = AllocInstance(type_info, OBJ_RESULT);
  UpdateHeapRef(location, object);
#if KONAN_NO_EXCEPTIONS
  ctor(object);
  return object;
#else
  try {
    ctor(object);
    return object;
  } catch (...) {
    UpdateReturnRef(OBJ_RESULT, nullptr);
    ZeroHeapRef(location);
    throw;
  }
#endif
}

OBJ_GETTER(InitSharedInstance,
    ObjHeader** location, ObjHeader** localLocation, const TypeInfo* type_info, void (*ctor)(ObjHeader*)) {
#if KONAN_NO_THREADS
  ObjHeader* value = *location;
  if (value != nullptr) {
    // OK'ish, inited by someone else.
    RETURN_OBJ(value);
  }
  ObjHeader* object = AllocInstance(type_info, OBJ_RESULT);
  UpdateHeapRef(location, object);
#if KONAN_NO_EXCEPTIONS
  ctor(object);
  FreezeSubgraph(object);
  return object;
#else
  try {
    ctor(object);
    FreezeSubgraph(object);
    return object;
  } catch (...) {
    UpdateReturnRef(OBJ_RESULT, nullptr);
    ZeroHeapRef(location);
    throw;
  }
#endif
#else
  ObjHeader* value = *localLocation;
  if (value != nullptr) RETURN_OBJ(value);

  ObjHeader* initializing = reinterpret_cast<ObjHeader*>(1);

  // Spin lock.
  while ((value = __sync_val_compare_and_swap(location, nullptr, initializing)) == initializing);
  if (value != nullptr) {
    // OK'ish, inited by someone else.
    RETURN_OBJ(value);
  }
  ObjHeader* object = AllocInstance(type_info, OBJ_RESULT);
  RuntimeAssert(object->container()->normal() , "Shared object cannot be co-allocated");
  UpdateHeapRef(localLocation, object);
#if KONAN_NO_EXCEPTIONS
  ctor(object);
  FreezeSubgraph(object);
  UpdateHeapRef(location, object);
  synchronize();
  return object;
#else
  try {
    ctor(object);
    FreezeSubgraph(object);
    UpdateHeapRef(location, object);
    synchronize();
    return object;
  } catch (...) {
    UpdateReturnRef(OBJ_RESULT, nullptr);
    ZeroHeapRef(location);
    ZeroHeapRef(localLocation);
    synchronize();
    throw;
  }
#endif
#endif
}

void SetStackRef(ObjHeader** location, const ObjHeader* object) {
  MEMORY_LOG("SetStackRef *%p: %p\n", location, object)
  UPDATE_REF_EVENT(memoryState, nullptr, object, location, 1);
  *const_cast<const ObjHeader**>(location) = object;
}

void SetHeapRef(ObjHeader** location, const ObjHeader* object) {
  MEMORY_LOG("SetHeapRef *%p: %p\n", location, object)
  UPDATE_REF_EVENT(memoryState, nullptr, object, location, 0);
  if (object != nullptr)
    AddHeapRef(const_cast<ObjHeader*>(object));
  *const_cast<const ObjHeader**>(location) = object;
}

void ZeroHeapRef(ObjHeader** location) {
  MEMORY_LOG("ZeroHeapRef %p\n", location)
  auto* value = *location;
  if (value != nullptr) {
    UPDATE_REF_EVENT(memoryState, value, nullptr, location, 0);
    *location = nullptr;
    ReleaseHeapRef(value);
  }
}

void ZeroStackRef(ObjHeader** location) {
  MEMORY_LOG("ZeroStackRef %p\n", location)
#if TRACE_MEMORY
  auto* value = *location;
  if (value != nullptr) {
    UPDATE_REF_EVENT(memoryState, value, nullptr, location, 1);
    *location = nullptr;
  }
#else
  *location = nullptr;
#endif
}

void UpdateStackRef(ObjHeader** location, const ObjHeader* object) {
  UPDATE_REF_EVENT(memoryState, *location, object, location, 1)
  RuntimeAssert(object != reinterpret_cast<ObjHeader*>(1), "Markers disallowed here");
  *const_cast<const ObjHeader**>(location) = object;
}

void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) {
  UPDATE_REF_EVENT(memoryState, *location, object, location, 0);
  ObjHeader* old = *location;
  if (old != object) {
    if (object != nullptr) {
      AddHeapRef(object);
    }
    *const_cast<const ObjHeader**>(location) = object;
    if (reinterpret_cast<uintptr_t>(old) > 1) {
      ReleaseHeapRef(old);
    }
  }
}

ObjHeader** GetReturnSlotIfArena(ObjHeader** returnSlot, ObjHeader** localSlot) {
  RuntimeCheck(false, "No longer supported");
  return nullptr;
}

ObjHeader** GetParamSlotIfArena(ObjHeader** returnSlot, ObjHeader** localSlot) {
  RuntimeCheck(false, "No longer supported");
  return nullptr;
}

void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* value) {
  UpdateStackRef(returnSlot, value);
}

void UpdateHeapRefIfNull(ObjHeader** location, const ObjHeader* object) {
  if (object != nullptr) {
#if KONAN_NO_THREADS
    ObjHeader* old = *location;
    if (old == nullptr) {
      AddHeapRef(const_cast<ObjHeader*>(object));
      *const_cast<const ObjHeader**>(location) = object;
    }
#else
    AddHeapRef(const_cast<ObjHeader*>(object));
    auto old = __sync_val_compare_and_swap(location, nullptr, const_cast<ObjHeader*>(object));
    if (old != nullptr) {
      // Failed to store, was not null.
      ReleaseHeapRef(const_cast<ObjHeader*>(object));
    }
#endif
    UPDATE_REF_EVENT(memoryState, old, object, location, 0);
  }
}

void EnterFrame(ObjHeader** start, int parameters, int count) {
  MEMORY_LOG("EnterFrame %p: %d parameters %d locals\n", start, parameters, count)
  FrameOverlay* frame = reinterpret_cast<FrameOverlay*>(start);
  frame->previous = currentFrame;
  currentFrame = frame;
  // TODO: maybe compress in single value somehow.
  frame->parameters = parameters;
  frame->count = count;
}

void LeaveFrame(ObjHeader** start, int parameters, int count) {
  MEMORY_LOG("LeaveFrame %p: %d parameters %d locals\n", start, parameters, count)
  FrameOverlay* frame = reinterpret_cast<FrameOverlay*>(start);
  currentFrame = frame->previous;
}

#if USE_GC

void GarbageCollect() {
  garbageCollect(memoryState, true);
}

#endif // USE_GC

void Kotlin_native_internal_GC_collect(KRef) {
#if USE_GC
  GC_LOG("Kotlin_native_internal_GC_collect\n")
  GarbageCollect();
#endif
}

void Kotlin_native_internal_GC_suspend(KRef) {
#if USE_GC
   GC_LOG("Kotlin_native_internal_GC_suspend\n")
  memoryState->gcSuspendCount++;
#endif
}

void Kotlin_native_internal_GC_resume(KRef) {
#if USE_GC
  MemoryState* state = memoryState;
  if (state->gcSuspendCount > 0) {
    state->gcSuspendCount--;
    if (state->toRelease != nullptr &&
        state->toRelease->size() >= state->gcThreshold &&
        state->gcSuspendCount == 0) {
      GC_LOG("Kotlin_native_internal_GC_resume\n")
      garbageCollect(state, false);
    }
  }
#endif
}

void Kotlin_native_internal_GC_stop(KRef) {
#if USE_GC
  GC_LOG("Kotlin_native_internal_GC_stop\n")
  if (memoryState->toRelease != nullptr) {
    memoryState->gcSuspendCount = 0;
    garbageCollect(memoryState, true);
    konanDestructInstance(memoryState->toRelease);
    konanDestructInstance(memoryState->toFree);
    konanDestructInstance(memoryState->roots);
    memoryState->toRelease = nullptr;
    memoryState->toFree = nullptr;
    memoryState->roots = nullptr;
  }
#endif
}

void Kotlin_native_internal_GC_start(KRef) {
#if USE_GC
  GC_LOG("Kotlin_native_internal_GC_start\n")
  if (memoryState->toFree == nullptr) {
    memoryState->toFree = konanConstructInstance<ContainerHeaderList>();
    memoryState->toRelease = konanConstructInstance<ContainerHeaderList>();
    memoryState->roots = konanConstructInstance<ContainerHeaderList>();
    memoryState->gcSuspendCount = 0;
  }
#endif
}

void Kotlin_native_internal_GC_setThreshold(KRef, KInt value) {
#if USE_GC
  GC_LOG("Kotlin_native_internal_setThreshold %d\n", value)
  if (value > 0) {
    initGcThreshold(memoryState, value);
  }
#endif
}

KInt Kotlin_native_internal_GC_getThreshold(KRef) {
#if USE_GC
  GC_LOG("Kotlin_native_internal_getThreshold %d\n")
  return memoryState->gcThreshold;
#else
  return -1;
#endif
}

KNativePtr CreateStablePointer(KRef any) {
  if (any == nullptr) return nullptr;
  MEMORY_LOG("CreateStablePointer for %p rc=%d\n", any, any->container() ? any->container()->refCount() : 0)
  AddHeapRef(any);
  return reinterpret_cast<KNativePtr>(any);
}

void DisposeStablePointer(KNativePtr pointer) {
  if (pointer == nullptr) return;
  KRef ref = reinterpret_cast<KRef>(pointer);
  ReleaseHeapRef(ref);
}

OBJ_GETTER(DerefStablePointer, KNativePtr pointer) {
  KRef ref = reinterpret_cast<KRef>(pointer);
#if USE_GC
  if (ref != nullptr)
    rememberNewContainer(ref->container());
#endif  // USE_GC
  RETURN_OBJ(ref);
}

OBJ_GETTER(AdoptStablePointer, KNativePtr pointer) {
  synchronize();
  KRef ref = reinterpret_cast<KRef>(pointer);
  MEMORY_LOG("adopting stable pointer %p, rc=%d\n", \
     ref, (ref && ref->container()) ? ref->container()->refCount() : -1)
  UpdateReturnRef(OBJ_RESULT, ref);
  DisposeStablePointer(pointer);
  return ref;
}

#if USE_GC
bool hasExternalRefs(ContainerHeader* start, ContainerHeaderSet* visited) {
  ContainerHeaderDeque toVisit;
  toVisit.push_back(start);
  while (!toVisit.empty()) {
    auto* container = toVisit.front();
    toVisit.pop_front();
    visited->insert(container);
    if (container->refCount() > 0) {
      MEMORY_LOG("container %p with rc %d blocks transfer\n", container, container->refCount())
      return true;
    }
    traverseContainerReferredObjects(container, [&toVisit, visited](ObjHeader* ref) {
        auto* child = ref->container();
        if (!Shareable(child) && (visited->count(child) == 0)) {
           toVisit.push_front(child);
        }
     });
  }
  return false;
}
#endif  // USE_GC

bool ClearSubgraphReferences(ObjHeader* root, bool checked) {
#if USE_GC
  MEMORY_LOG("ClearSubgraphReferences %p\n", root)
  if (root == nullptr) return true;
  auto state = memoryState;
  auto* container = root->container();

  if (Shareable(container))
    // We assume, that frozen/shareable objects can be safely passed and not present
    // in the GC candidate list.
    // TODO: assert for that?
    return true;

  ContainerHeaderSet visited;
  if (!checked) {
    hasExternalRefs(container, &visited);
  } else {
    // Now decrement RC of elements in toRelease set for reachibility analysis.
    for (auto it = state->toRelease->begin(); it != state->toRelease->end(); ++it) {
      auto released = *it;
      if (!isMarkedAsRemoved(released) && released->tag() == CONTAINER_TAG_NORMAL) {
        released->decRefCount<false>();
      }
    }
    container->decRefCount<false>();
    MarkGray<false>(container);
    auto bad = hasExternalRefs(container, &visited);
    ScanBlack<false>(container);
    // Restore original RC.
    container->incRefCount<false>();
    for (auto it = state->toRelease->begin(); it != state->toRelease->end(); ++it) {
       auto released = *it;
       if (!isMarkedAsRemoved(released) && released->tag() == CONTAINER_TAG_NORMAL) {
         released->incRefCount<false>();
       }
    }
    if (bad) {
      return false;
    }
  }

  // Remove all no longer owned containers from GC structures.
  // TODO: not very efficient traversal.
  for (auto it = state->toFree->begin(); it != state->toFree->end(); ++it) {
    auto container = *it;
    if (visited.count(container) != 0) {
      MEMORY_LOG("removing %p from the toFree list\n", container)
      container->resetBuffered();
      container->setColorAssertIfGreen(CONTAINER_TAG_GC_BLACK);
      *it = markAsRemoved(container);
    }
  }
  for (auto it = state->toRelease->begin(); it != state->toRelease->end(); ++it) {
    auto container = *it;
    if (!isMarkedAsRemoved(container) && visited.count(container) != 0) {
      MEMORY_LOG("removing %p from the toRelease list\n", container)
      container->decRefCount<false>();
      *it = markAsRemoved(container);
    }
  }

#if TRACE_MEMORY
  // Forget transferred containers.
  for (auto* it: visited) {
    state->containers->erase(it);
  }
#endif

#endif  // USE_GC
  return true;
}

/**
  * Do DFS cycle detection with three colors:
  *  - 'marked' bit as BLACK marker (object and its descendants processed)
  *  - 'seen' bit as GRAY marker (object is being processed)
  *  - not 'marked' and not 'seen' as WHITE marker (object is unprocessed)
  * When we see GREY during DFS, it means we see cycle.
  */
void depthFirstTraversal(ContainerHeader* start, bool* hasCycles,
                         KRef* firstBlocker, KStdVector<ContainerHeader*>* order) {
  ContainerHeaderDeque toVisit;
  toVisit.push_back(start);
  start->setSeen();

  while (!toVisit.empty()) {
    auto* container = toVisit.front();
    toVisit.pop_front();
    if (isMarkedAsRemoved(container)) {
      container = clearRemoved(container);
      // Mark BLACK.
      container->resetSeen();
      container->mark();
      order->push_back(container);
      continue;
    }
    toVisit.push_front(markAsRemoved(container));
    traverseContainerReferredObjects(container, [hasCycles, firstBlocker, &order, &toVisit](ObjHeader* obj) {
      if (*firstBlocker != nullptr)
        return;
      if (obj->has_meta_object() && ((obj->meta_object()->flags_ & MF_NEVER_FROZEN) != 0)) {
          *firstBlocker = obj;
          return;
      }
      ContainerHeader* objContainer = obj->container();
      if (!Shareable(objContainer)) {
        // Marked GREY, there's cycle.
        if (objContainer->seen()) *hasCycles = true;

        // Go deeper if WHITE.
        if (!objContainer->seen() && !objContainer->marked()) {
          // Mark GRAY.
          objContainer->setSeen();
          toVisit.push_front(objContainer);
        }
      }
    });
  }
}

void traverseStronglyConnectedComponent(ContainerHeader* start,
                                        KStdUnorderedMap<ContainerHeader*,
                                            KStdVector<ContainerHeader*>> const* reversedEdges,
                                        KStdVector<ContainerHeader*>* component) {
  ContainerHeaderDeque toVisit;
  toVisit.push_back(start);
  start->mark();

  while (!toVisit.empty()) {
    auto* container = toVisit.front();
    toVisit.pop_front();
    component->push_back(container);
    auto it = reversedEdges->find(container);
    RuntimeAssert(it != reversedEdges->end(), "unknown node during condensation building");
    for (auto* nextContainer : it->second) {
      if (!nextContainer->marked()) {
          nextContainer->mark();
          toVisit.push_front(nextContainer);
      }
    }
  }
}

void freezeAcyclic(ContainerHeader* rootContainer, ContainerHeaderSet* newlyFrozen) {
  KStdDeque<ContainerHeader*> queue;
  queue.push_back(rootContainer);
  while (!queue.empty()) {
    ContainerHeader* current = queue.front();
    queue.pop_front();
    current->unMark();
    current->resetBuffered();
    current->setColorUnlessGreen(CONTAINER_TAG_GC_BLACK);
    // Note, that once object is frozen, it could be concurrently accessed, so
    // color and similar attributes shall not be used.
    if (current->tag() == CONTAINER_TAG_NORMAL)
      newlyFrozen->insert(current);
    MEMORY_LOG("freezing %p\n", current)
    current->freeze();
    traverseContainerReferredObjects(current, [current, &queue](ObjHeader* obj) {
        ContainerHeader* objContainer = obj->container();
        if (!Shareable(objContainer)) {
          if (objContainer->marked())
            queue.push_back(objContainer);
        }
    });
  }
}

void freezeCyclic(ContainerHeader* rootContainer,
                  const KStdVector<ContainerHeader*>& order,
                  ContainerHeaderSet* newlyFrozen) {
  KStdUnorderedMap<ContainerHeader*, KStdVector<ContainerHeader*>> reversedEdges;
  KStdDeque<ContainerHeader*> queue;
  queue.push_back(rootContainer);
  while (!queue.empty()) {
    ContainerHeader* current = queue.front();
    queue.pop_front();
    current->unMark();
    reversedEdges.emplace(current, KStdVector<ContainerHeader*>(0));
    traverseContainerReferredObjects(current, [current, &queue, &reversedEdges](ObjHeader* obj) {
          ContainerHeader* objContainer = obj->container();
          if (!Shareable(objContainer)) {
            if (objContainer->marked())
              queue.push_back(objContainer);
            reversedEdges.emplace(objContainer, KStdVector<ContainerHeader*>(0)).first->second.push_back(current);
          }
      });
    }

    KStdVector<KStdVector<ContainerHeader*>> components;
    MEMORY_LOG("Condensation:\n");
    // Enumerate in the topological order.
    for (auto it = order.rbegin(); it != order.rend(); ++it) {
      auto* container = *it;
      if (container->marked()) continue;
      KStdVector<ContainerHeader*> component;
      traverseStronglyConnectedComponent(container, &reversedEdges, &component);
      MEMORY_LOG("SCC:\n");
  #if TRACE_MEMORY
      for (auto c: component)
        konan::consolePrintf("    %p\n", c);
  #endif
      components.push_back(std::move(component));
    }

    // Enumerate strongly connected components in reversed topological order.
  for (auto it = components.rbegin(); it != components.rend(); ++it) {
    auto& component = *it;
    int internalRefsCount = 0;
    int totalCount = 0;
    for (auto* container : component) {
      totalCount += container->refCount();
      traverseContainerReferredObjects(container, [&internalRefsCount](ObjHeader* obj) {
          auto* container = obj->container();
          if (!Shareable(container))
              ++internalRefsCount;
        });
      }

    // Freeze component.
    for (auto* container : component) {
      container->resetBuffered();
      container->setColorUnlessGreen(CONTAINER_TAG_GC_BLACK);
      if (container->tag() == CONTAINER_TAG_NORMAL)
        newlyFrozen->insert(container);
      // Note, that once object is frozen, it could be concurrently accessed, so
      // color and similar attributes shall not be used.
      MEMORY_LOG("freezing %p\n", container)
      container->freeze();
      // We set refcount of original container to zero, so that it is seen as such after removal
      // meta-object, where aggregating container is stored.
      container->setRefCount(0);
    }
    // Create fictitious container for the whole component.
    auto superContainer = component.size() == 1 ? component[0] : AllocAggregatingFrozenContainer(component);
    // Don't count internal references.
    MEMORY_LOG("Setting aggregating %p rc to %d (total %d inner %d)\n", \
        superContainer, totalCount - internalRefsCount, totalCount, internalRefsCount)
    superContainer->setRefCount(totalCount - internalRefsCount);
    newlyFrozen->insert(superContainer);
  }
}

/**
 * Theory of operations.
 *
 * Kotlin/Native supports object graph freezing, allowing to make certain subgraph immutable and thus
 * suitable for safe sharing amongst multiple concurrent executors. This operation recursively operates
 * on all objects reachable from the given object, and marks them as frozen. In frozen state object's
 * fields cannot be modified, and so, lifetime of frozen objects correlates. Practically, it means
 * that lifetimes of all strongly connected components are fully controlled by incoming reference
 * counters, and so if we place all members of strongly connected component to the single container
 * it could be correctly released by just atomic decrement on reference counter, without additional
 * cycle collector run.
 * So during subgraph freezing operation, we perform the following steps:
 *   - run Kosoraju-Sharir algorithm to find strongly connected components
 *   - put all objects in each strongly connected component into an artificial container
 *     (we assume that they all were in single element containers initially), single-object
 *     components remain in the same container
 *   - artificial container sums up outer reference counters of all its objects (i.e.
 *     incoming references from the same strongly connected component are not counted)
 *   - mark all object's headers as frozen
 *
 *  Further reference counting on frozen objects is performed with atomic operations, and so frozen
 * references could be passed across multiple threads.
 */
void FreezeSubgraph(ObjHeader* root) {
  if (root == nullptr) return;
  // First check that passed object graph has no cycles.
  // If there are cycles - run graph condensation on cyclic graphs using Kosoraju-Sharir.
  ContainerHeader* rootContainer = root->container();
  if (Shareable(rootContainer)) return;

  MEMORY_LOG("Freeze subgraph of %p\n", root)

  // Do DFS cycle detection.
  bool hasCycles = false;
  KRef firstBlocker = root->has_meta_object() && ((root->meta_object()->flags_ & MF_NEVER_FROZEN) != 0) ?
    root : nullptr;
  KStdVector<ContainerHeader*> order;
  depthFirstTraversal(rootContainer, &hasCycles, &firstBlocker, &order);
  if (firstBlocker != nullptr) {
    MEMORY_LOG("See freeze blocker for %p: %p\n", root, firstBlocker)
    ThrowFreezingException(root, firstBlocker);
  }
  ContainerHeaderSet newlyFrozen;
  // Now unmark all marked objects, and freeze them, if no cycles detected.
  if (hasCycles) {
    freezeCyclic(rootContainer, order, &newlyFrozen);
  } else {
    freezeAcyclic(rootContainer, &newlyFrozen);
  }
  MEMORY_LOG("Graph of %p is %s with %d elements\n", root, hasCycles ? "cyclic" : "acyclic", newlyFrozen.size())

#if USE_GC
  // Now remove frozen objects from the toFree list.
  // TODO: optimize it by keeping ignored (i.e. freshly frozen) objects in the set,
  // and use it when analyzing toFree during collection.
  auto state = memoryState;
  for (auto& container : *(state->toFree)) {
    if (!isMarkedAsRemoved(container) && container->frozen()) {
      RuntimeAssert(newlyFrozen.count(container) != 0, "Must be newly frozen");
      container = markAsRemoved(container);
    }
  }
#endif
}

// This function is called from field mutators to check if object's header is frozen.
// If object is frozen, an exception is thrown.
void MutationCheck(ObjHeader* obj) {
  auto* container = obj->container();
  if (container != nullptr && container->frozen()) ThrowInvalidMutabilityException(obj);
}

OBJ_GETTER(SwapHeapRefLocked,
    ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue, int32_t* spinlock) {
  lock(spinlock);
  ObjHeader* oldValue = *location;
  bool shallRelease = false;
  // We do not use UpdateRef() here to avoid having ReleaseRef() on return slot under the lock.
  if (oldValue == expectedValue) {
    SetHeapRef(location, newValue);
    shallRelease = oldValue != nullptr;
  }
  unlock(spinlock);
  if (shallRelease) {
    ReleaseHeapRef(oldValue);
  }
  // No need to rememberNewContainer(), as oldValue is already
  // present on this worker.
  UpdateReturnRef(OBJ_RESULT, oldValue);
  return oldValue;
}

void SetHeapRefLocked(ObjHeader** location, ObjHeader* newValue, int32_t* spinlock) {
  lock(spinlock);
  ObjHeader* oldValue = *location;
  // We do not use UpdateRef() here to avoid having ReleaseRef() on old value under the lock.
  SetHeapRef(location, newValue);
  unlock(spinlock);
  if (oldValue != nullptr)
    ReleaseHeapRef(oldValue);
}

OBJ_GETTER(ReadHeapRefLocked, ObjHeader** location, int32_t* spinlock) {
  MEMORY_LOG("ReadHeapRefLocked: %p\n", location)
  lock(spinlock);
  ObjHeader* value = *location;
  auto* container = value ? value->container() : nullptr;
  if (container != nullptr)
    IncrementRC<true>(container);
  unlock(spinlock);
  if (container != nullptr)
    EnqueueDecrementRC</* CanCollect = */ true>(container);
  RETURN_OBJ(value);
}

OBJ_GETTER(ReadHeapRefNoLock, ObjHeader* object, KInt index) {
  MEMORY_LOG("ReadHeapRefNoLock: %p index %d\n", object, index)
  ObjHeader** location = reinterpret_cast<ObjHeader**>(
    reinterpret_cast<uintptr_t>(object) + object->type_info()->objOffsets_[index]);
  ObjHeader* value = *location;
#if USE_GC
  if (value != nullptr)
    rememberNewContainer(value->container());
#endif  // USE_GC
  RETURN_OBJ(value);
}

void EnsureNeverFrozen(ObjHeader* object) {
   auto* container = object->container();
   if (container == nullptr || container->frozen())
      ThrowFreezingException(object, object);
   // TODO: note, that this API could not not be called on frozen objects, so no need to care much about concurrency,
   // although there's subtle race with case, where other thread freezes the same object after check.
   object->meta_object()->flags_ |= MF_NEVER_FROZEN;
}

KBoolean Konan_ensureAcyclicAndSet(ObjHeader* where, KInt index, ObjHeader* what) {
    RuntimeAssert(where->container() != nullptr && where->container()->frozen(), "Must be used on frozen objects only");
    RuntimeAssert(what == nullptr || PermanentOrFrozen(what),
        "Must be used with an immutable value");
    if (what != nullptr) {
        // Now we check that `where` is not reachable from `what`.
        // As we cannot modify objects while traversing, instead we remember all seen objects in a set.
        KStdUnorderedSet<ContainerHeader*> seen;
        KStdDeque<ContainerHeader*> queue;
        if (what->container() != nullptr)
            queue.push_back(what->container());
        bool acyclic = true;
        while (!queue.empty() && acyclic) {
            ContainerHeader* current = queue.front();
            queue.pop_front();
            seen.insert(current);
            if (isAggregatingFrozenContainer(current)) {
                ContainerHeader** subContainer = reinterpret_cast<ContainerHeader**>(current + 1);
                for (int i = 0; i < current->objectCount(); ++i) {
                    if (seen.count(*subContainer) == 0)
                        queue.push_back(*subContainer++);
                }
            } else {
              traverseContainerReferredObjects(current, [where, &queue, &acyclic, &seen](ObjHeader* obj) {
                if (obj == where) {
                    acyclic = false;
                } else {
                    auto* objContainer = obj->container();
                    if (objContainer != nullptr && seen.count(objContainer) == 0)
                        queue.push_back(objContainer);
                }
              });
            }
          }
        if (!acyclic) return false;
    }
    UpdateHeapRef(reinterpret_cast<ObjHeader**>(
            reinterpret_cast<uintptr_t>(where) + where->type_info()->objOffsets_[index]), what);
    // Fence on updated location?
    return true;
}



void Kotlin_Any_share(ObjHeader* obj) {
    auto* container = obj->container();
    if (Shareable(container)) return;
    RuntimeCheck(container->objectCount() == 1, "Must be a single object container");
    container->makeShareable();
}

} // extern "C"
