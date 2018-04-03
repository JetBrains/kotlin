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

#include <string.h>
#include <stdio.h>

#include <cstddef> // for offsetof

#include "Alloc.h"
#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "MemoryPrivate.hpp"
#include "Natives.h"
#include "Porting.h"
#include "Atomic.h"

// If garbage collection algorithm for cyclic garbage to be used.
// We are using the Bacon's algorithm for GC, see
// http://researcher.watson.ibm.com/researcher/files/us-bacon/Bacon03Pure.pdf.
#define USE_GC 1
// Define to 1 to print all memory operations.
#define TRACE_MEMORY 0
// Collect memory manager events statistics.
#define COLLECT_STATISTIC 0
// Auto-adjust GC thresholds.
#define GC_ERGONOMICS 1

// TODO: ensure it is read-only.
ContainerHeader ObjHeader::theStaticObjectsContainer = {
  CONTAINER_TAG_PERMANENT | CONTAINER_TAG_INCREMENT,
  0 /* Object count */
};

namespace {

// Granularity of arena container chunks.
constexpr container_size_t kContainerAlignment = 1024;
// Single object alignment.
constexpr container_size_t kObjectAlignment = 8;

#if TRACE_MEMORY
#define MEMORY_LOG(...) konan::consolePrintf(__VA_ARGS__);
#else
#define MEMORY_LOG(...)
#endif

#if USE_GC
// Collection threshold default (collect after having so many elements in the
// release candidates set).
constexpr size_t kGcThreshold = 4 * 1024;
#if GC_ERGONOMICS
// Ergonomic thresholds.
// If GC to computations time ratio is above that value,
// increase GC threshold by 1.5 times.
constexpr double kGcToComputeRatioThreshold = 0.5;
// Never exceed this value when increasing GC threshold.
constexpr size_t kMaxErgonomicThreshold = 1024 * 1024;
#endif  // GC_ERGONOMICS

typedef KStdDeque<ContainerHeader*> ContainerHeaderDeque;
#endif

}  // namespace

#if TRACE_MEMORY || USE_GC
typedef KStdUnorderedSet<ContainerHeader*> ContainerHeaderSet;
typedef KStdVector<ContainerHeader*> ContainerHeaderList;
typedef KStdVector<KRef*> KRefPtrList;
#endif

struct FrameOverlay {
  ArenaContainer* arena;
};

// A little hack that allows to enable -O2 optimizations
// Prevents clang from replacing FrameOverlay struct
// with single pointer.
// Can be removed when FrameOverlay will become more complex
FrameOverlay exportFrameOverlay;

// Current number of allocated containers.
int allocCount = 0;
int aliveMemoryStatesCount = 0;

// Forward declarations.
void FreeContainer(ContainerHeader* header);

#if COLLECT_STATISTIC
class MemoryStatistic {
public:
  // UpdateRef per-object type counters.
  uint64_t updateCounters[4][4];
  // Alloc per container type counters.
  uint64_t containerAllocs[4][2];
  // Free per container type counters.
  uint64_t objectAllocs[4][2];
  // Histogram of allocation size distribution.
  KStdUnorderedMap<int, int>* allocationHistogram;
  // Number of allocation cache hits.
  int allocCacheHit;
  // Number of allocation cache misses.
  int allocCacheMiss;

  // Map of array index to human readable name.
  static constexpr const char* indexToName[] = { "normal", "stack ", "perm  ", "null  " };

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

  void incUpdateRef(const ObjHeader* objOld, const ObjHeader* objNew) {
    updateCounters[toIndex(objOld)][toIndex(objNew)]++;
  }

  void incAlloc(size_t size, const ContainerHeader* header) {
    containerAllocs[toIndex(header)][0]++;
    ++(*allocationHistogram)[size];
#if 0
    auto queue = memoryState->finalizerQueue;
    bool hit = false;
    for (int i = 0; i < queue->size(); i++) {
      auto container = (*queue)[i];
      if (containerSize(container) == size) {
        hit = true;
        break;
      }
    }
    if (hit)
      allocCacheHit++;
    else
      allocCacheMiss++;
#endif  // USE_GC
  }

  void incFree(const ContainerHeader* header) {
    containerAllocs[toIndex(header)][1]++;
  }

  void incAlloc(size_t size, const ObjHeader* header) {
    objectAllocs[toIndex(header)][0]++;
  }

  void incFree(const ObjHeader* header) {
    objectAllocs[toIndex(header)][1]++;
  }

  static int toIndex(const ObjHeader* obj) {
    if (obj == nullptr) return 3;
    return toIndex(obj->container());
  }

  static int toIndex(const ContainerHeader* header) {
    switch (header->tag()) {
      case CONTAINER_TAG_NORMAL   : return 0;
      case CONTAINER_TAG_STACK    : return 1;
      case CONTAINER_TAG_PERMANENT: return 2;
    }
    RuntimeAssert(false, "unknown container type");
    return -1;
  }

  void printStatistic() {
    konan::consolePrintf("\nMemory manager statistic:\n\n");
    for (int i = 0; i < 2; i++) {
      konan::consolePrintf("Container %s alloc: %lld, free: %lld\n",
                           indexToName[i], containerAllocs[i][0],
                           containerAllocs[i][1]);
    }
    for (int i = 0; i < 2; i++) {
      konan::consolePrintf("Object %s alloc: %lld, free: %lld\n",
                           indexToName[i], objectAllocs[i][0],
                           objectAllocs[i][1]);
    }

    konan::consolePrintf("\n");
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        konan::consolePrintf("UpdateRef[%s -> %s]: %lld\n",
                             indexToName[i], indexToName[j], updateCounters[i][j]);
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
    for (auto& it : keys) {
      konan::consolePrintf(
          "%d bytes -> %d times\n", it, (*allocationHistogram)[it]);
    }

#if USE_GC
    konan::consolePrintf(
        "alloc cache: %d hits/%d misses\n", allocCacheHit, allocCacheMiss);
#endif  // USE_GC
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
  // Finalizer queue.
  ContainerHeaderDeque* finalizerQueue;

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
  // How many candidate elements in toFree shall trigger collection.
  size_t gcThreshold;
  // If collection is in progress.
  bool gcInProgress;
  int finalizerQueueSuspendCount;

#if GC_ERGONOMICS
  uint64_t lastGcTimestamp;
#endif

#endif // USE_GC

#if COLLECT_STATISTIC
  #define CONTAINER_ALLOC_STAT(state, size, container) state->statistic.incAlloc(size, container);
  #define CONTAINER_FREE_STAT(state, container)
  #define CONTAINER_DESTROY_STAT(state, container) \
    state->statistic.incFree(container);
  #define OBJECT_ALLOC_STAT(state, size, object) \
    state->statistic.incAlloc(size, object);
  #define OBJECT_FREE_STAT(state, size, object) \
    state->statistic.incFree(object);
#define UPDATE_REF_STAT(state, oldRef, newRef, slot)         \
    state->statistic.incUpdateRef(oldRef, newRef);
  #define INIT_STAT(state) \
    state->statistic.init();
  #define DEINIT_STAT(state) \
    state->statistic.deinit();
  #define PRINT_STAT(state) \
    state->statistic.printStatistic();
  MemoryStatistic statistic;
#else
  #define CONTAINER_ALLOC_STAT(state, size, container)
  #define CONTAINER_FREE_STAT(state, container)
  #define CONTAINER_DESTROY_STAT(state, container)
  #define OBJECT_ALLOC_STAT(state, size, object)
  #define OBJECT_FREE_STAT(state, object)
  #define UPDATE_REF_STAT(state, oldRef, newRef, slot)
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
#define CONTAINER_FREE_TRACE(state, container)  \
  MEMORY_LOG("Container free %p\n", container)
#define CONTAINER_DESTROY_TRACE(state, container) \
  MEMORY_LOG("Container destroy %p\n", container)
#define OBJECT_ALLOC_TRACE(state, size, object) \
  MEMORY_LOG("Object alloc %d at %p\n", size, object)
#define OBJECT_FREE_TRACE(state, object) \
  MEMORY_LOG("Object free %p\n", object)
#define UPDATE_REF_TRACE(state, oldRef, newRef, slot) \
  MEMORY_LOG("UpdateRef *%p: %p -> %p\n", slot, oldRef, newRef)

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
// Called on container freeing (memory is still in use).
#define CONTAINER_FREE_EVENT(state, container) \
  CONTAINER_FREE_STAT(state, container) \
  CONTAINER_FREE_TRACE(state, container)
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
#define UPDATE_REF_EVENT(state, oldRef, newRef, slot) \
  UPDATE_REF_STAT(state, oldRef, newRef, slot) \
  UPDATE_REF_TRACE(state, oldRef, newRef, slot)
// Infomation shall be printed as worker is exiting.
#define PRINT_EVENT(state) \
  PRINT_STAT(state)

namespace {

// TODO: can we pass this variable as an explicit argument?
THREAD_LOCAL_VARIABLE MemoryState* memoryState = nullptr;

constexpr int kFrameOverlaySlots = sizeof(FrameOverlay) / sizeof(ObjHeader**);

inline bool isFreeable(const ContainerHeader* header) {
  return header->tag() < CONTAINER_TAG_PERMANENT;
}

inline bool isArena(const ContainerHeader* header) {
  return header->stack();
}

inline bool isAggregatingFrozenContainer(const ContainerHeader* header) {
  return header->frozen() && header->objectCount() > 1;
}

inline container_size_t alignUp(container_size_t size, int alignment) {
  return (size + alignment - 1) & ~(alignment - 1);
}

// TODO: shall we do padding for alignment?
inline container_size_t objectSize(const ObjHeader* obj) {
  const TypeInfo* type_info = obj->type_info();
  container_size_t size = (type_info->instanceSize_ < 0 ?
      // An array.
      ArrayDataSizeBytes(obj->array()) + sizeof(ArrayHeader)
      :
      type_info->instanceSize_ + sizeof(ObjHeader));
  return alignUp(size, kObjectAlignment);
}

inline bool isArenaSlot(ObjHeader** slot) {
  return (reinterpret_cast<uintptr_t>(slot) & ARENA_BIT) != 0;
}

inline ObjHeader** asArenaSlot(ObjHeader** slot) {
  return reinterpret_cast<ObjHeader**>(
      reinterpret_cast<uintptr_t>(slot) & ~ARENA_BIT);
}

inline FrameOverlay* asFrameOverlay(ObjHeader** slot) {
  return reinterpret_cast<FrameOverlay*>(slot);
}

inline bool isRefCounted(KConstRef object) {
  return isFreeable(object->container());
}
} // namespace

extern "C" {

void objc_release(void* ptr);
void Kotlin_ObjCExport_releaseAssociatedObject(void* associatedObject);
RUNTIME_NORETURN void ThrowFreezingException();
RUNTIME_NORETURN void ThrowInvalidMutabilityException();

}  // extern "C"

inline void runDeallocationHooks(ObjHeader* obj) {
  if (obj->has_meta_object()) {
    ObjHeader::destroyMetaObject(&obj->typeInfoOrMeta_);
  }
#if KONAN_OBJC_INTEROP
  if (obj->type_info() == theObjCPointerHolderTypeInfo) {
    void* objcPtr =  *reinterpret_cast<void**>(obj + 1); // TODO: use more reliable layout description
    objc_release(objcPtr);
  }
#endif
}

inline void runDeallocationHooks(ContainerHeader* container) {
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(container + 1);

  for (int index = 0; index < container->objectCount(); index++) {
    runDeallocationHooks(obj);

    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }
}

static inline void DeinitInstanceBodyImpl(const TypeInfo* typeInfo, void* body) {
  for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
    ObjHeader** location = reinterpret_cast<ObjHeader**>(
        reinterpret_cast<uintptr_t>(body) + typeInfo->objOffsets_[index]);
    UpdateRef(location, nullptr);
  }
}

void DeinitInstanceBody(const TypeInfo* typeInfo, void* body) {
  DeinitInstanceBodyImpl(typeInfo, body);
}

namespace {

template<typename func>
inline void traverseContainerObjectFields(ContainerHeader* container, func process) {
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(container + 1);
  for (int object = 0; object < container->objectCount(); object++) {
    const TypeInfo* typeInfo = obj->type_info();
    for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
      ObjHeader** location = reinterpret_cast<ObjHeader**>(
          reinterpret_cast<uintptr_t>(obj + 1) + typeInfo->objOffsets_[index]);
      process(location);
    }
    if (typeInfo == theArrayTypeInfo) {
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

#if USE_GC

inline bool isMarkedAsRemoved(ContainerHeader* container) {
  return (reinterpret_cast<uintptr_t>(container) & 1) != 0;
}

inline ContainerHeader* markAsRemoved(ContainerHeader* container) {
  return reinterpret_cast<ContainerHeader*>(reinterpret_cast<uintptr_t>(container) | 1);
}

inline void processFinalizerQueue(MemoryState* state) {
  // TODO: reuse elements of finalizer queue for new allocations.
  while (!state->finalizerQueue->empty()) {
    auto container = memoryState->finalizerQueue->back();
    state->finalizerQueue->pop_back();
#if TRACE_MEMORY
    state->containers->erase(container);
#endif
    CONTAINER_DESTROY_EVENT(state, container)
    konanFreeMemory(container);
    atomicAdd(&allocCount, -1);
  }
}
#endif

inline void scheduleDestroyContainer(
    MemoryState* state, ContainerHeader* container, bool clearExternalRefs) {
  if (clearExternalRefs) {
    traverseContainerObjectFields(container, [](ObjHeader** location) {
        ObjHeader* ref = *location;
        // Frozen object references do not participate in trial deletion, so shall be explicitly freed.
        if (ref != nullptr && ref->container()->frozen())
          UpdateRef(location, nullptr);
      });
  }
#if USE_GC
  state->finalizerQueue->push_front(container);
  // We cannot clean finalizer queue while in GC.
  if (!state->gcInProgress && state->finalizerQueueSuspendCount == 0 && state->finalizerQueue->size() > 256) {
    processFinalizerQueue(state);
  }
#else
  atomicAdd(&allocCount, -1);
  CONTAINER_DESTROY_EVENT(state, header)
  konanFreeMemory(header);
#endif
}


#if !USE_GC

template <bool Atomic>
inline void IncrementRC(ContainerHeader* container) {
  container->incRefCount<Atomic>();
}

template <bool Atomic>
inline void DecrementRC(ContainerHeader* container, bool useCycleCollector) {
  if (container->decRefCount<Atomic>() == 0) {
    FreeContainer(container);
  }
}

#else // USE_GC

inline uint32_t freeableSize(MemoryState* state) {
  return state->toFree->size();
}

template <bool Atomic>
inline void IncrementRC(ContainerHeader* container) {
  container->incRefCount<Atomic>();
  container->setColor(CONTAINER_TAG_GC_BLACK);
}

template <bool Atomic>
inline void DecrementRC(ContainerHeader* container, bool useCycleCollector) {
  if (container->decRefCount<Atomic>() == 0) {
    FreeContainer(container);
  } else if (!Atomic && useCycleCollector) { // Possible root.
    // Do not use cycle collector for frozen objects, as we already detected possible cycles during
    // freezing.
    if (container->color() != CONTAINER_TAG_GC_PURPLE) {
      container->setColor(CONTAINER_TAG_GC_PURPLE);
      if (!container->buffered()) {
        container->setBuffered();
        auto state = memoryState;
        state->toFree->push_back(container);
        if (state->gcSuspendCount == 0 && freeableSize(state) >= state->gcThreshold) {
          GarbageCollect();
        }
      }
    }
  }
}

inline void initThreshold(MemoryState* state, uint32_t gcThreshold) {
  state->gcThreshold = gcThreshold;
  state->toFree->reserve(gcThreshold);
}
#endif // USE_GC

#if TRACE_MEMORY || USE_GC

void dumpWorker(const char* prefix, ContainerHeader* header, ContainerHeaderSet* seen) {
  MEMORY_LOG("%s: %p (%08x): %d refs\n", prefix, header, header->refCount_,
             header->refCount_ >> CONTAINER_TAG_SHIFT)
  seen->insert(header);
  traverseContainerReferredObjects(header, [prefix, seen](ObjHeader* ref) {
    auto child = ref->container();
    RuntimeAssert(!isArena(child), "A reference to local object is encountered");
    if (!child->permanent() && (seen->count(child) == 0)) {
      dumpWorker(prefix, child, seen);
    }
  });
}

void dumpReachable(const char* prefix, const ContainerHeaderSet* roots) {
  ContainerHeaderSet seen;
  for (auto container : *roots) {
    MEMORY_LOG("%p: %s%s%s\n", container,
        container->frozen() ? "frozen " : "",
        container->permanent() ? "permanent " : "",
        container->stack() ? "stack " : "")
    dumpWorker(prefix, container, &seen);
  }
}

#endif

void MarkRoots(MemoryState*);
void DeleteCorpses(MemoryState*);
void ScanRoots(MemoryState*);
void CollectRoots(MemoryState*);

template<bool useColor>
void MarkGray(ContainerHeader* container) {
  if (useColor) {
    if (container->color() == CONTAINER_TAG_GC_GRAY) return;
  } else {
    if (container->marked()) return;
  }
  if (useColor) {
    container->setColor(CONTAINER_TAG_GC_GRAY);
  } else {
    container->mark();
  }
  traverseContainerReferredObjects(container, [](ObjHeader* ref) {
    auto childContainer = ref->container();
    RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");

    if (!childContainer->permanentOrFrozen()) {
      childContainer->decRefCount<false>();
      MarkGray<useColor>(childContainer);
    }
  });
}

void Scan(ContainerHeader* container);

template<bool useColor>
void ScanBlack(ContainerHeader* container) {
  if (useColor) {
    container->setColor(CONTAINER_TAG_GC_BLACK);
  } else {
    container->unMark();
  }
  traverseContainerReferredObjects(container, [](ObjHeader* ref) {
    auto childContainer = ref->container();
    RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
    if (!childContainer->permanentOrFrozen()) {
      childContainer->incRefCount<false>();
      if (useColor) {
        if (childContainer->color() != CONTAINER_TAG_GC_BLACK)
          ScanBlack<useColor>(childContainer);
      } else {
        if (childContainer->marked())
          ScanBlack<useColor>(childContainer);
      }
    }
  });
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
    auto color = container->color();
    auto rcIsZero = container->refCount() == 0;
    if (color == CONTAINER_TAG_GC_PURPLE && !rcIsZero) {
      MarkGray<true>(container);
      state->roots->push_back(container);
    } else {
      container->resetBuffered();
      if (color == CONTAINER_TAG_GC_BLACK && rcIsZero) {
        scheduleDestroyContainer(state, container, true);
      }
    }
  }
}

void ScanRoots(MemoryState* state) {
  for (auto container : *(state->roots)) {
    Scan(container);
  }
}

void CollectRoots(MemoryState* state) {
  for (auto container : *(state->roots)) {
    container->resetBuffered();
    CollectWhite(state, container);
  }
}

void Scan(ContainerHeader* container) {
  if (container->color() != CONTAINER_TAG_GC_GRAY) return;
  if (container->refCount() != 0) {
    ScanBlack<true>(container);
    return;
  }
  container->setColor(CONTAINER_TAG_GC_WHITE);
  traverseContainerReferredObjects(container, [](ObjHeader* ref) {
    auto childContainer = ref->container();
    RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
    if (!childContainer->permanentOrFrozen()) {
      Scan(childContainer);
    }
  });
}

void CollectWhite(MemoryState* state, ContainerHeader* container) {
  if (container->color() != CONTAINER_TAG_GC_WHITE
        || container->buffered())
    return;
  container->setColor(CONTAINER_TAG_GC_BLACK);
  traverseContainerReferredObjects(container, [state](ObjHeader* ref) {
    auto childContainer = ref->container();
    RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
    if (!childContainer->permanentOrFrozen()) {
      CollectWhite(state, childContainer);
    }
  });
  scheduleDestroyContainer(state, container, true);
}

inline void AddRef(ContainerHeader* header) {
  // Looking at container type we may want to skip AddRef() totally
  // (non-escaping stack objects, constant objects).
  switch (header->refCount_ & CONTAINER_TAG_MASK) {
    case CONTAINER_TAG_STACK:
    case CONTAINER_TAG_PERMANENT:
      break;
    case CONTAINER_TAG_NORMAL:
      IncrementRC<false>(header);
      break;
    case CONTAINER_TAG_FROZEN:
      IncrementRC<true>(header);
      break;
    default:
      RuntimeAssert(false, "unknown container type");
      break;
  }
}

inline void Release(ContainerHeader* header, bool useCycleCollector) {
  // Looking at container type we may want to skip Release() totally
  // (non-escaping stack objects, constant objects).
  switch (header->refCount_ & CONTAINER_TAG_MASK) {
    case CONTAINER_TAG_PERMANENT:
    case CONTAINER_TAG_STACK:
      break;
    case CONTAINER_TAG_NORMAL:
      DecrementRC<false>(header, useCycleCollector);
      break;
    case CONTAINER_TAG_FROZEN:
      DecrementRC<true>(header, useCycleCollector);
      break;
    default:
      RuntimeAssert(false, "unknown container type");
      break;
  }
}

// We use first slot as place to store frame-local arena container.
// TODO: create ArenaContainer object on the stack, so that we don't
// do two allocations per frame (ArenaContainer + actual container).
inline ArenaContainer* initedArena(ObjHeader** auxSlot) {
  auto frame = asFrameOverlay(auxSlot);
  auto arena = frame->arena;
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
    obj = reinterpret_cast<ObjHeader*>(
        reinterpret_cast<uintptr_t>(obj) + size);
  }
  return result;
}

}  // namespace

MetaObjHeader* ObjHeader::createMetaObject(TypeInfo** location) {
  MetaObjHeader* meta = konanConstructInstance<MetaObjHeader>();
  TypeInfo* typeInfo = *location;
  meta->typeInfo_ = typeInfo;
#if KONAN_NO_THREADS
  *location = reinterpret_cast<TypeInfo*>(meta);
#else
  TypeInfo* old = __sync_val_compare_and_swap(location, typeInfo, reinterpret_cast<TypeInfo*>(meta));
  if (old->typeInfo_ != old) {
    // Someone installed a new meta-object since the check.
    konanFreeMemory(meta);
    meta = reinterpret_cast<MetaObjHeader*>(old);
  }
#endif
  return meta;
}

void ObjHeader::destroyMetaObject(TypeInfo** location) {
  MetaObjHeader* meta = *(reinterpret_cast<MetaObjHeader**>(location));
  *const_cast<const TypeInfo**>(location) = meta->typeInfo_;
  if (meta->counter_ != nullptr) {
    WeakReferenceCounterClear(meta->counter_);
    UpdateRef(&meta->counter_, nullptr);
  }

#ifdef KONAN_OBJC_INTEROP
  Kotlin_ObjCExport_releaseAssociatedObject(meta->associatedObject);
#endif

  konanFreeMemory(meta);
}

ContainerHeader* AllocContainer(size_t size) {
  auto state = memoryState;
#if USE_GC
  // TODO: try to reuse elements of finalizer queue for new allocations, question
  // is how to get actual size of container.
#endif
  ContainerHeader* result = konanConstructSizedInstance<ContainerHeader>(alignUp(size, kObjectAlignment));
  CONTAINER_ALLOC_EVENT(state, size, result);
#if TRACE_MEMORY
  state->containers->insert(result);
#endif
  atomicAdd(&allocCount, 1);
  return result;
}

ContainerHeader* AllocAggregatingFrozenContainer(KStdVector<ContainerHeader*>& containers) {
  auto componentSize = containers.size();
  auto superContainer = AllocContainer(sizeof(ContainerHeader) + sizeof(void*) * componentSize);
  auto place = reinterpret_cast<ContainerHeader**>(superContainer + 1);
  for (auto* container : containers) {
    *place++ = container;
    // Set link to the new container.
    auto obj = reinterpret_cast<ObjHeader*>(container + 1);
    obj->container_ = superContainer;
    MEMORY_LOG("Set fictitious frozen container for %p: %p\n", obj, superContainer);
  }
  superContainer->setObjectCount(componentSize);
  superContainer->freeze();
  return superContainer;
}

void FreeAggregatingFrozenContainer(ContainerHeader* container) {
  auto state = memoryState;
  RuntimeAssert(isAggregatingFrozenContainer(container), "expected fictitious frozen container");
  MEMORY_LOG("%p is fictitious frozen container\n", container);
  RuntimeAssert(!container->buffered(), "frozen objects must not participate in GC")
  // Forbid finalizerQueue handling.
  ++state->finalizerQueueSuspendCount;
  // Special container for frozen objects.
  ContainerHeader** subContainer = reinterpret_cast<ContainerHeader**>(container + 1);
  MEMORY_LOG("Total subcontainers = %d\n", container->objectCount());
  for (int i = 0; i < container->objectCount(); ++i) {
    MEMORY_LOG("Freeing subcontainer %p\n", *subContainer);
    FreeContainer(*subContainer++);
  }
  --state->finalizerQueueSuspendCount;
  scheduleDestroyContainer(state, container, false);
  MEMORY_LOG("Freeing subcontainers done\n");
}

void FreeContainer(ContainerHeader* container) {
  RuntimeAssert(!container->permanent(), "this kind of container shalln't be freed");
  auto state = memoryState;

  CONTAINER_FREE_EVENT(state, container)

  if (isAggregatingFrozenContainer(container)) {
    FreeAggregatingFrozenContainer(container);
    return;
  } else {
    runDeallocationHooks(container);
  }

  // Now let's clean all object's fields in this container.
  traverseContainerObjectFields(container, [](ObjHeader** location) {
    UpdateRef(location, nullptr);
  });

  // And release underlying memory.
  if (isFreeable(container)) {
    container->setColor(CONTAINER_TAG_GC_BLACK);
    if (!container->buffered())
      scheduleDestroyContainer(state, container, false);
  }
}

void ObjectContainer::Init(const TypeInfo* typeInfo) {
  RuntimeAssert(typeInfo->instanceSize_ >= 0, "Must be an object");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ObjHeader) + typeInfo->instanceSize_;
  header_ = AllocContainer(alloc_size);
  if (header_) {
    // One object in this container.
    header_->setObjectCount(1);
     // header->refCount_ is zero initialized by AllocContainer().
    SetHeader(GetPlace(), typeInfo);
    MEMORY_LOG("object at %p\n", GetPlace())
    OBJECT_ALLOC_EVENT(memoryState, type_info->instanceSize_, GetPlace())
  }
}

void ArrayContainer::Init(const TypeInfo* typeInfo, uint32_t elements) {
  RuntimeAssert(typeInfo->instanceSize_ < 0, "Must be an array");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ArrayHeader) -
      typeInfo->instanceSize_ * elements;
  header_ = AllocContainer(alloc_size);
  RuntimeAssert(header_ != nullptr, "Cannot alloc memory");
  if (header_) {
    // One object in this container.
    header_->setObjectCount(1);
    // header->refCount_ is zero initialized by AllocContainer().
    GetPlace()->count_ = elements;
    SetHeader(GetPlace()->obj(), typeInfo);
    MEMORY_LOG("array at %p\n", GetPlace())
    OBJECT_ALLOC_EVENT(
        memoryState, -typeInfo->instanceSize_ * elements, GetPlace()->obj())
  }
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
    // FreeContainer() doesn't release memory when CONTAINER_TAG_STACK is set.
    MEMORY_LOG("Arena::Deinit free chunk %p\n", chunk)
    FreeContainer(chunk->asHeader());
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
  RuntimeAssert(result != nullptr, "Cannot alloc memory");
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
  uint32_t size = type_info->instanceSize_ + sizeof(ObjHeader);
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
  container_size_t size = sizeof(ArrayHeader) - type_info->instanceSize_ * count;
  ArrayHeader* result = reinterpret_cast<ArrayHeader*>(place(size));
  if (!result) {
    return nullptr;
  }
  OBJECT_ALLOC_EVENT(memoryState, -type_info->instanceSize_ * count, result->obj())
  currentChunk_->asHeader()->incObjectCount();
  setHeader(result->obj(), type_info);
  result->count_ = count;
  return result;
}

inline void AddRef(const ObjHeader* object) {
  MEMORY_LOG("AddRef on %p in %p\n", object, object->container())
  AddRef(object->container());
}

inline void ReleaseRef(const ObjHeader* object) {
  MEMORY_LOG("ReleaseRef on %p in %p\n", object, object->container())
  // Use cycle collector only for objects having object fields, or if container is multiobject.
  auto container = object->container();
  Release(container, (object->type_info()->objOffsetsCount_ > 0) || (container->objectCount() > 1));
}

void AddRefFromAssociatedObject(const ObjHeader* object) {
  AddRef(object);
}

void ReleaseRefFromAssociatedObject(const ObjHeader* object) {
  ReleaseRef(object);
}

extern "C" {

MemoryState* InitMemory() {
  RuntimeAssert(offsetof(ArrayHeader, typeInfoOrMeta_)
                ==
                offsetof(ObjHeader,   typeInfoOrMeta_),
                "Layout mismatch");
  RuntimeAssert(offsetof(ArrayHeader, container_)
                ==
                offsetof(ObjHeader  , container_),
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
  memoryState->finalizerQueue = konanConstructInstance<ContainerHeaderDeque>();
  memoryState->toFree = konanConstructInstance<ContainerHeaderList>();
  memoryState->roots = konanConstructInstance<ContainerHeaderList>();
  memoryState->gcInProgress = false;
  initThreshold(memoryState, kGcThreshold);
  memoryState->gcSuspendCount = 0;
#endif
  atomicAdd(&aliveMemoryStatesCount, 1);
  return memoryState;
}

void DeinitMemory(MemoryState* memoryState) {
#if USE_GC
  GarbageCollect();
  RuntimeAssert(memoryState->toFree->size() == 0, "Some memory have not been released after GC");
  konanDestructInstance(memoryState->toFree);
  konanDestructInstance(memoryState->roots);

  konanDestructInstance(memoryState->finalizerQueue);
  memoryState->finalizerQueue = nullptr;

#endif // USE_GC

  bool lastMemoryState = atomicAdd(&aliveMemoryStatesCount, -1) == 0;

#if TRACE_MEMORY
  if (lastMemoryState && allocCount > 0) {
    MEMORY_LOG("*** Memory leaks, leaked %d containers ***\n", allocCount);
    dumpReachable("", memoryState->containers);
  }
#else
  if (lastMemoryState)
    RuntimeAssert(allocCount == 0, "Memory leaks found");
#endif

  PRINT_EVENT(memoryState)
  DEINIT_EVENT(memoryState)

  konanFreeMemory(memoryState);
  ::memoryState = nullptr;
}

OBJ_GETTER(AllocInstance, const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  if (isArenaSlot(OBJ_RESULT)) {
    auto arena = initedArena(asArenaSlot(OBJ_RESULT));
    auto result = arena->PlaceObject(type_info);
    MEMORY_LOG("instance %p in arena: %p\n", result, arena)
    return result;
  }
  RETURN_OBJ(ObjectContainer(type_info).GetPlace());
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* type_info, uint32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  if (isArenaSlot(OBJ_RESULT)) {
    auto arena = initedArena(asArenaSlot(OBJ_RESULT));
    auto result = arena->PlaceArray(type_info, elements)->obj();
    MEMORY_LOG("array[%d] %p in arena: %p\n", elements, result, arena)
    return result;
  }
  RETURN_OBJ(ArrayContainer(type_info, elements).GetPlace()->obj());
}

OBJ_GETTER(InitInstance,
    ObjHeader** location, const TypeInfo* type_info, void (*ctor)(ObjHeader*)) {
  ObjHeader* value = *location;

  if (value != nullptr) {
    // OK'ish, inited by someone else.
    RETURN_OBJ(value);
  }

  ObjHeader* object = AllocInstance(type_info, OBJ_RESULT);
  MEMORY_LOG("Calling UpdateRef from InitInstance\n")
  UpdateRef(location, object);
#if KONAN_NO_EXCEPTIONS
  ctor(object);
  return object;
#else
  try {
    ctor(object);
    return object;
  } catch (...) {
    UpdateRef(OBJ_RESULT, nullptr);
    UpdateRef(location, nullptr);
    throw;
  }
#endif
}

OBJ_GETTER(InitSharedInstance,
    ObjHeader** location, ObjHeader** localLocation, const TypeInfo* type_info, void (*ctor)(ObjHeader*)) {
#if KONAN_NO_THREADS
  RETURN_RESULT_OF(InitInstance, location, type_info, ctor);
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
  MEMORY_LOG("Calling UpdateRef from InitInstance\n")
  UpdateRef(localLocation, object);
#if KONAN_NO_EXCEPTIONS
  ctor(object);
  if (!object->container()->frozen())
    ThrowFreezingException();
  UpdateRef(location, object);
  __sync_synchronize();
  return object;
#else
  try {
    ctor(object);
    if (!object->container()->frozen())
      ThrowFreezingException();
    UpdateRef(location, object);
    __sync_synchronize();
    return object;
  } catch (...) {
    UpdateRef(OBJ_RESULT, nullptr);
    UpdateRef(location, nullptr);
    __sync_synchronize();
    throw;
  }
#endif
#endif
}

void SetRef(ObjHeader** location, const ObjHeader* object) {
  MEMORY_LOG("SetRef *%p: %p\n", location, object)
  *const_cast<const ObjHeader**>(location) = object;
  if (object != nullptr)
    AddRef(object);
}

ObjHeader** GetReturnSlotIfArena(ObjHeader** returnSlot, ObjHeader** localSlot) {
  return isArenaSlot(returnSlot) ? returnSlot : localSlot;
}

ObjHeader** GetParamSlotIfArena(ObjHeader* param, ObjHeader** localSlot) {
  if (param == nullptr) return localSlot;
  auto container = param->container();
  if ((container->refCount_ & CONTAINER_TAG_MASK) != CONTAINER_TAG_STACK)
    return localSlot;
  auto chunk = reinterpret_cast<ContainerChunk*>(container) - 1;
  return reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(&chunk->arena) | ARENA_BIT);
}

void UpdateRef(ObjHeader** location, const ObjHeader* object) {
  RuntimeAssert(!isArenaSlot(location), "must not be a slot");
  ObjHeader* old = *location;
  UPDATE_REF_EVENT(memoryState, old, object, location)
  if (old != object) {
    if (object != nullptr) {
      AddRef(object);
    }
    *const_cast<const ObjHeader**>(location) = object;
    if (reinterpret_cast<uintptr_t>(old) > 1) {
      ReleaseRef(old);
    }
  }
}

void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
  if (isArenaSlot(returnSlot)) {
    // Not a subject of reference counting.
    if (object == nullptr || !isRefCounted(object)) return;
    auto arena = initedArena(asArenaSlot(returnSlot));
    returnSlot = arena->getSlot();
  }
  UpdateRef(returnSlot, object);
}

void UpdateRefIfNull(ObjHeader** location, const ObjHeader* object) {
  if (object != nullptr) {
#if KONAN_NO_THREADS
    ObjHeader* old = *location;
    if (old == nullptr) {
      AddRef(object);
      *const_cast<const ObjHeader**>(location) = object;
    }
#else
    AddRef(object);
    auto old = __sync_val_compare_and_swap(location, nullptr, const_cast<ObjHeader*>(object));
    if (old != nullptr) {
      // Failed to store, was not null.
      ReleaseRef(object);
    }
#endif
  }
}

void EnterFrame(ObjHeader** start, int parameters, int count) {
  MEMORY_LOG("EnterFrame %p .. %p\n", start, start + count + parameters)
}

void LeaveFrame(ObjHeader** start, int parameters, int count) {
  MEMORY_LOG("LeaveFrame %p .. %p\n", start, start + count + parameters)
  ReleaseRefs(start + parameters + kFrameOverlaySlots, count - kFrameOverlaySlots - parameters);
  if (*start != nullptr) {
    auto arena = initedArena(start);
    MEMORY_LOG("LeaveFrame: free arena %p\n", arena)
    arena->Deinit();
    konanFreeMemory(arena);
    MEMORY_LOG("LeaveFrame: free arena done %p\n", arena)
  }
}

void ReleaseRefs(ObjHeader** start, int count) {
  MEMORY_LOG("ReleaseRefs %p .. %p\n", start, start + count)
  ObjHeader** current = start;
  auto state = memoryState;
  while (count-- > 0) {
    ObjHeader* object = *current;
    if (object != nullptr) {
      ReleaseRef(object);
      // Just for sanity, optional.
      *current = nullptr;
    }
    current++;
  }
}

#if USE_GC

void GarbageCollect() {
  MemoryState* state = memoryState;
  RuntimeAssert(!state->gcInProgress, "Recursive GC is disallowed");

  MEMORY_LOG("Garbage collect\n")

#if GC_ERGONOMICS
  auto gcStartTime = konan::getTimeMicros();
#endif

  state->gcInProgress = true;

  while (state->toFree->size() > 0) {
    CollectCycles(state);
    processFinalizerQueue(state);
  }

  state->gcInProgress = false;

#if GC_ERGONOMICS
  auto gcEndTime = konan::getTimeMicros();
  auto gcToComputeRatio = double(gcEndTime - gcStartTime) / (gcStartTime - state->lastGcTimestamp + 1);
  if (gcToComputeRatio > kGcToComputeRatioThreshold) {
     auto newThreshold = state->gcThreshold * 3 / 2 + 1;
     if (newThreshold < kMaxErgonomicThreshold) {
        MEMORY_LOG("Adjusting GC threshold to %d\n", newThreshold);
        initThreshold(state, newThreshold);
     }
  }
  MEMORY_LOG("Garbage collect: GC length=%lld sinceLast=%lld\n",
             (gcEndTime - gcStartTime), gcStartTime - state->lastGcTimestamp);
  state->lastGcTimestamp = gcEndTime;
#endif
}

#endif // USE_GC

void Kotlin_konan_internal_GC_collect(KRef) {
#if USE_GC
  GarbageCollect();
#endif
}

void Kotlin_konan_internal_GC_suspend(KRef) {
#if USE_GC
  memoryState->gcSuspendCount++;
#endif
}

void Kotlin_konan_internal_GC_resume(KRef) {
#if USE_GC
  MemoryState* state = memoryState;
  if (state->gcSuspendCount > 0) {
    state->gcSuspendCount--;
    if (state->toFree != nullptr &&
        freeableSize(state) >= state->gcThreshold) {
      GarbageCollect();
    }
  }
#endif
}

void Kotlin_konan_internal_GC_stop(KRef) {
#if USE_GC
  if (memoryState->toFree != nullptr) {
    GarbageCollect();
    konanDestructInstance(memoryState->toFree);
    konanDestructInstance(memoryState->roots);
    memoryState->toFree = nullptr;
    memoryState->roots = nullptr;
  }
#endif
}

void Kotlin_konan_internal_GC_start(KRef) {
#if USE_GC
  if (memoryState->toFree == nullptr) {
    memoryState->toFree = konanConstructInstance<ContainerHeaderList>();
    memoryState->roots = konanConstructInstance<ContainerHeaderList>();
  }
#endif
}

void Kotlin_konan_internal_GC_setThreshold(KRef, KInt value) {
#if USE_GC
  if (value > 0) {
    initThreshold(memoryState, value);
  }
#endif
}

KInt Kotlin_konan_internal_GC_getThreshold(KRef) {
#if USE_GC
  return memoryState->gcThreshold;
#else
  return -1;
#endif
}

KNativePtr CreateStablePointer(KRef any) {
  if (any == nullptr) return nullptr;
  AddRef(any->container());
  return reinterpret_cast<KNativePtr>(any);
}

void DisposeStablePointer(KNativePtr pointer) {
  if (pointer == nullptr) return;
  KRef ref = reinterpret_cast<KRef>(pointer);
  ReleaseRef(ref);
}

OBJ_GETTER(DerefStablePointer, KNativePtr pointer) {
  KRef ref = reinterpret_cast<KRef>(pointer);
  RETURN_OBJ(ref);
}

OBJ_GETTER(AdoptStablePointer, KNativePtr pointer) {
#ifndef KONAN_NO_THREADS
  __sync_synchronize();
#endif
  KRef ref = reinterpret_cast<KRef>(pointer);
  UpdateRef(OBJ_RESULT, nullptr);
  // Somewhat hacky.
  *OBJ_RESULT = ref;
  return ref;
}

#if USE_GC

bool hasExternalRefs(ContainerHeader* container, ContainerHeaderSet* visited) {
  visited->insert(container);
  bool result = container->refCount() != 0;
  traverseContainerReferredObjects(container, [&result, visited](ObjHeader* ref) {
    auto child = ref->container();
    if (!child->permanentOrFrozen() && (visited->find(child) == visited->end())) {
      result |= hasExternalRefs(child, visited);
    }
  });
  return result;
}
#endif

bool ClearSubgraphReferences(ObjHeader* root, bool checked) {
#if USE_GC
  if (root != nullptr) {
    auto state = memoryState;
    auto container = root->container();

    if (container->frozen())
      // We assume, that frozen objects can be safely passed and are already removed
      // GC candidate list.
      return true;

    ContainerHeaderSet visited;
    if (!checked) {
      hasExternalRefs(container, &visited);
    } else {
      if (!container->permanentOrFrozen()) {
        container->decRefCount<false>();
        MarkGray<false>(container);
        auto bad = hasExternalRefs(container, &visited);
        ScanBlack<false>(container);
        container->incRefCount<false>();
        if (bad) return false;
      }
    }

    // TODO: not very effecient traversal.
    for (auto it = state->toFree->begin(); it != state->toFree->end(); ++it) {
      auto container = *it;
      if (visited.find(container) != visited.end()) {
        container->resetBuffered();
        container->setColor(CONTAINER_TAG_GC_BLACK);
        *it = markAsRemoved(container);
      }
    }
  }
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
void depthFirstTraversal(ContainerHeader* container, bool* hasCycles, KStdVector<ContainerHeader*>& order) {
  // Mark GRAY.
  container->setSeen();

  traverseContainerReferredObjects(container, [hasCycles, &order](ObjHeader* obj) {
      ContainerHeader* objContainer = obj->container();
      if (!objContainer->permanentOrFrozen()) {
        // Marked GREY, there's cycle.
        if (objContainer->seen()) *hasCycles = true;

        // Go deeper if WHITE.
        if (!objContainer->seen() && !objContainer->marked()) {
          depthFirstTraversal(objContainer, hasCycles, order);
        }
      }
  });
  // Mark BLACK.
  container->resetSeen();
  container->mark();
  order.push_back(container);
}

void traverseStronglyConnectedComponent(ContainerHeader* container,
                                        KStdUnorderedMap<ContainerHeader*, KStdVector<ContainerHeader*>> const& reversedEdges,
                                        KStdVector<ContainerHeader*>& component) {
  component.push_back(container);
  container->mark();
  auto it = reversedEdges.find(container);
  RuntimeAssert(it != reversedEdges.end(), "unknown node during condensation building");
  for (auto* nextContainer : it->second) {
    if (!nextContainer->marked())
      traverseStronglyConnectedComponent(nextContainer, reversedEdges, component);
  }
}

/**
 * Theory of operations.
 *
 * Kotlin/Native supports object graph freezing, allowing to make certain subgraph immutable and thus
 * suitable for safe sharing amongs multiple concurrent executors. This operation recursively operates
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
 *   - artifical container sums up outer reference counters of all its objects (i.e.
 *     incoming references from the same strongly connected component are not counted)
 *   - mark all object's headers as frozen
 *
 *  Further reference counting on frozen objects is performed with the atomic operations, and so frozen
 * references could be passed accross multiple threads.
 */
void FreezeSubgraph(ObjHeader* root) {
  // TODO: for now, we just check that passed object graph has no cycles, and throw an exception,
  // if it does. Next version will run Kosoraju-Sharir if cycles are found.
  ContainerHeader* rootContainer = root->container();
  if (rootContainer->permanentOrFrozen()) return;

  // Do DFS cycle detection.
  bool hasCycles = false;
  KStdVector<ContainerHeader*> order;
  depthFirstTraversal(rootContainer, &hasCycles, order);

  KStdUnorderedMap<ContainerHeader*, KStdVector<ContainerHeader*>> reversedEdges;
  // Now unmark all marked objects, and freeze them, if no cycles detected.
  KStdDeque<ContainerHeader*> queue;
  queue.push_back(rootContainer);
  while (!queue.empty()) {
    ContainerHeader* current = queue.front();
    queue.pop_front();
    current->unMark();

    if (hasCycles) {
      reversedEdges.emplace(current, KStdVector<ContainerHeader*>(0));
    } else {
      current->resetBuffered();
      current->setColor(CONTAINER_TAG_GC_BLACK);
      // Note, that once object is frozen, it could be concurrently accessed, so
      // color and similar attributes shall not be used.
      current->freeze();
    }
    traverseContainerReferredObjects(current, [hasCycles, current, &queue, &reversedEdges](ObjHeader* obj) {
        ContainerHeader* objContainer = obj->container();
        if (!objContainer->permanentOrFrozen()) {
          if (objContainer->marked())
            queue.push_back(objContainer);
          if (hasCycles)
            reversedEdges.emplace(objContainer, KStdVector<ContainerHeader*>(0)).first->second.push_back(current);
        }
    });
  }

  if (hasCycles) {
    KStdVector<KStdVector<ContainerHeader*>> components;
    MEMORY_LOG("Condensation:\n");
    // Enumerate in topological order.
    for (auto it = order.rbegin(); it != order.rend(); ++it) {
      auto* container = *it;
      if (container->marked()) continue;
      KStdVector<ContainerHeader*> component;
      traverseStronglyConnectedComponent(container, reversedEdges, component);
      MEMORY_LOG("SCC:\n");
#if TRACE_MEMORY
      for (auto c : component)
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
            if (!obj->container()->permanentOrFrozen())
              ++internalRefsCount;
        });
      }
      auto superContainer = component.size() == 1
                              ? component[0]
                              : AllocAggregatingFrozenContainer(component); // Create fictitious container for the whole component.
      // Don't count internal references.
      superContainer->setRefCount(totalCount - internalRefsCount);

      // Freeze component.
      for (auto* container : component) {
        container->resetBuffered();
        container->setColor(CONTAINER_TAG_GC_BLACK);
        // Note, that once object is frozen, it could be concurrently accessed, so
        // color and similar attributes shall not be used.
        container->freeze();
      }
    }
  }

  // Now remove frozen objects from the toFree list.
  // TODO: optimize it by keeping ignored (i.e. freshly frozen) objects in the set,
  // and use it when analyzing toFree during collection.
  auto state = memoryState;
  for (auto& container : *(state->toFree)) {
      if (!isMarkedAsRemoved(container) && container->frozen())
        container = markAsRemoved(container);
  }
}

// This function is called from field mutators to check if object's header is frozen.
// If object is frozen, an exception is thrown.
void MutationCheck(ObjHeader* obj) {
  if (obj->container()->frozen()) ThrowInvalidMutabilityException();
}

} // extern "C"
