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
#include "Natives.h"

// If garbage collection algorithm for cyclic garbage to be used.
#define USE_GC 1
// Optimize management of cyclic garbage (increases memory footprint).
// Not recommended for low-end embedded targets.
#define OPTIMIZE_GC 1
// Define to 1 to print all memory operations.
#define TRACE_MEMORY 0
// Trace garbage collection phases.
#define TRACE_GC_PHASES 0

ContainerHeader ObjHeader::theStaticObjectsContainer = {
  CONTAINER_TAG_PERMANENT | CONTAINER_TAG_INCREMENT
};

namespace {

// Granularity of arena container chunks.
constexpr container_size_t kContainerAlignment = 1024;
// Single object alignment.
constexpr container_size_t kObjectAlignment = 8;

}  // namespace

#if USE_GC
// Collection threshold default (collect after having so many elements in the
// release candidates set). Better be a prime number.
constexpr size_t kGcThreshold = 9341;

typedef KStdDeque<ContainerHeader*> ContainerHeaderDeque;
#endif

#if TRACE_MEMORY || USE_GC
typedef KStdUnorderedSet<ContainerHeader*> ContainerHeaderSet;
typedef KStdVector<ContainerHeader*> ContainerHeaderList;
typedef KStdVector<KRef*> KRefPtrList;
#endif

struct MemoryState {
  // Current number of allocated containers.
  int allocCount = 0;

#if TRACE_MEMORY
  // List of all global objects addresses.
  KRefPtrList* globalObjects;
  // Set of all containers.
  ContainerHeaderSet* containers;
#endif

#if USE_GC
  // Finalizer queue.
  ContainerHeaderDeque* finalizerQueue;

  // Set of references to release.
  ContainerHeaderSet* toFree;
  // How many GC suspend requests happened.
  int gcSuspendCount;
  // How many candidate elements in toFree shall trigger collection.
  size_t gcThreshold;
  // If collection is in progress.
  bool gcInProgress;
#if OPTIMIZE_GC
  // Cache backed by toFree set.
  ContainerHeader** toFreeCache;
  // Current number of elements in the cache.
  uint32_t cacheSize;
#endif
#endif
};

namespace {

// TODO: can we pass this variable as an explicit argument?
THREAD_LOCAL_VARIABLE MemoryState* memoryState = nullptr;

inline bool isFreeable(const ContainerHeader* header) {
  return (header->refCount_ & CONTAINER_TAG_MASK) < CONTAINER_TAG_PERMANENT;
}

inline bool isPermanent(const ContainerHeader* header) {
  return (header->refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_PERMANENT;
}

inline container_size_t alignUp(container_size_t size, int alignment) {
  return (size + alignment - 1) & ~(alignment - 1);
}

// TODO: shall we do padding for alignment?
inline container_size_t objectSize(const ObjHeader* obj) {
  const TypeInfo* type_info = obj->type_info();
  container_size_t size = type_info->instanceSize_ < 0 ?
      // An array.
      ArrayDataSizeBytes(obj->array()) + sizeof(ArrayHeader)
      :
      type_info->instanceSize_ + sizeof(ObjHeader);
  return alignUp(size, kObjectAlignment);
}

inline bool isArenaSlot(ObjHeader** slot) {
  return (reinterpret_cast<uintptr_t>(slot) & ARENA_BIT) != 0;
}

inline ObjHeader** asArenaSlot(ObjHeader** slot) {
  return reinterpret_cast<ObjHeader**>(
      reinterpret_cast<uintptr_t>(slot) & ~ARENA_BIT);
}

#if USE_GC

inline void processFinalizerQueue(MemoryState* state) {
  // TODO: reuse elements of finalizer queue for new allocations.
  while (!state->finalizerQueue->empty()) {
    auto container = memoryState->finalizerQueue->back();
    state->finalizerQueue->pop_back();
    konanFreeMemory(container);
    state->allocCount--;
  }
}
#endif

inline void scheduleDestroyContainer(
    MemoryState* state, ContainerHeader* container) {
#if USE_GC
  state->finalizerQueue->push_front(container);
  // We cannot clean finalizer queue while in GC.
  if (!state->gcInProgress && state->finalizerQueue->size() > 256) {
    processFinalizerQueue(state);
  }
#else
  state->allocCount--;
  konanFreeMemory(header);
#endif
}


#if USE_GC

inline uint32_t hashOf(ContainerHeader* container) {
  uintptr_t value = reinterpret_cast<uintptr_t>(container);
  return static_cast<uint32_t>(value >> 3) ^ static_cast<uint32_t>(static_cast<uint64_t>(value) >> 32);
}

inline uint32_t freeableSize(MemoryState* state) {
#if OPTIMIZE_GC
  return state->cacheSize + state->toFree->size();
#else
  return state->toFree->size();
#endif
}

inline void addFreeable(MemoryState* state, ContainerHeader* container) {
  if (memoryState->toFree == nullptr || !isFreeable(container))
    return;
#if OPTIMIZE_GC
  auto hash = hashOf(container) % state->gcThreshold;
  auto value = state->toFreeCache[hash];
  if (value == container) {
    return;
  }
  if (value == nullptr) {
    memoryState->cacheSize++;
    state->toFreeCache[hash] = container;
    return;
  }
  state->toFree->insert(container);
  if (value != (ContainerHeader*)0x1) {
    memoryState->cacheSize--;
    state->toFree->insert(value);
    state->toFreeCache[hash] = (ContainerHeader*)0x1;
  }
#else
  state->toFree->insert(container);
#endif
  if (state->gcSuspendCount == 0 &&
      freeableSize(memoryState) > state->gcThreshold) {
    GarbageCollect();
  }
}

inline void removeFreeable(MemoryState* state, ContainerHeader* container) {
  if (state->toFree == nullptr || !isFreeable(container))
    return;
#if OPTIMIZE_GC
  auto hash = hashOf(container) % state->gcThreshold;
  auto value = state->toFreeCache[hash];
  if (value == container) {
    state->cacheSize--;
    state->toFreeCache[hash] = nullptr;
    return;
  }
#endif
  state->toFree->erase(container);
}

// Must only be called in context of GC.
inline void flushFreeableCache(MemoryState* state) {
#if OPTIMIZE_GC
  for (auto i = 0; i < state->gcThreshold; i++) {
    if ((uintptr_t)state->toFreeCache[i] > 0x1) {
      state->toFree->insert(state->toFreeCache[i]);
    }
  }
  // Mass-clear cache.
  memset(state->toFreeCache, 0,
         sizeof(ContainerHeader*) * state->gcThreshold);
  state->cacheSize = 0;
#endif
}

inline void initThreshold(MemoryState* state, uint32_t gcThreshold) {
#if OPTIMIZE_GC
  if (state->toFreeCache != nullptr) {
    GarbageCollect();
    konanFreeMemory(state->toFreeCache);
  }
  state->toFreeCache = reinterpret_cast<ContainerHeader**>(
      konanAllocMemory(sizeof(ContainerHeader*) * gcThreshold));
  state->cacheSize = 0;
#endif
  state->gcThreshold = gcThreshold;
}

// Must be vector or map 'container -> number', to keep reference counters correct.
ContainerHeaderList collectMutableReferred(ContainerHeader* header) {
  ContainerHeaderList result;
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(header + 1);
  for (int object = 0; object < header->objectCount(); object++) {
    const TypeInfo* typeInfo = obj->type_info();
    // TODO: generalize iteration over all references.
    for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
      ObjHeader** location = reinterpret_cast<ObjHeader**>(
          reinterpret_cast<uintptr_t>(obj + 1) + typeInfo->objOffsets_[index]);
      ObjHeader* ref = *location;
      if (ref != nullptr && !isPermanent(ref->container())) {
        result.push_back(ref->container());
      }
    }
    if (typeInfo == theArrayTypeInfo) {
      ArrayHeader* array = obj->array();
      for (int index = 0; index < array->count_; index++) {
        ObjHeader* ref = *ArrayAddressOfElementAt(array, index);
        if (ref != nullptr && !isPermanent(ref->container())) {
          result.push_back(ref->container());
        }
      }
    }
    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }
  return result;
}

void dumpWorker(const char* prefix, ContainerHeader* header, ContainerHeaderSet* seen) {
  fprintf(stderr, "%s: %p (%08x): %d refs %s\n",
          prefix,
          header, header->refCount_, header->refCount_ >> CONTAINER_TAG_SHIFT,
          (header->refCount_ & CONTAINER_TAG_SEEN) != 0 ? "X" : "-");
  seen->insert(header);
  auto children = collectMutableReferred(header);
  for (auto child : children) {
    if (seen->count(child) == 0) {
      dumpWorker(prefix, child, seen);
    }
  }
}

void dumpReachable(const char* prefix, const ContainerHeaderSet* roots) {
  ContainerHeaderSet seen;
  for (auto container : *roots) {
    fprintf(stderr, "%p is root\n", container);
    dumpWorker(prefix, container, &seen);
  }
}

void phase1(ContainerHeader* header) {
  if ((header->refCount_ & CONTAINER_TAG_SEEN) != 0)
    return;
  header->refCount_ |= CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
    container->decRefCount();
    phase1(container);
  }
}

void phase2(ContainerHeader* header, ContainerHeaderSet* rootset) {
  if ((header->refCount_ & CONTAINER_TAG_SEEN) == 0)
    return;
  if ((header->refCount_ >> CONTAINER_TAG_SHIFT) != 0)
    rootset->insert(header);
  header->refCount_ &= ~CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
    phase2(container, rootset);
  }
}

void phase3(ContainerHeader* header) {
  if ((header->refCount_ & CONTAINER_TAG_SEEN) != 0) {
    return;
  }
  header->refCount_ |= CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
    container->incRefCount();
    phase3(container);
  }
}

void phase4(MemoryState* state, ContainerHeader* header) {
  auto refCount = header->refCount_ >> CONTAINER_TAG_SHIFT;
  bool seen = (refCount > 0 && (header->refCount_ & CONTAINER_TAG_SEEN) == 0) ||
      (refCount == 0 && (header->refCount_ & CONTAINER_TAG_SEEN) != 0);
  if (seen) return;

  // Add to finalize queue and update seen bit.
  if (refCount == 0) {
    scheduleDestroyContainer(state, header);
    header->refCount_ |= CONTAINER_TAG_SEEN;
  } else {
    header->refCount_ &= ~CONTAINER_TAG_SEEN;
  }
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
    phase4(state, container);
  }
}

#endif // USE_GC

// We use first slot as place to store frame-local arena container.
// TODO: create ArenaContainer object on the stack, so that we don't
// do two allocations per frame (ArenaContainer + actual container).
inline ArenaContainer* initedArena(ObjHeader** auxSlot) {
  ObjHeader* slotValue = *auxSlot;
  if (slotValue) return reinterpret_cast<ArenaContainer*>(slotValue);
  ArenaContainer* arena = konanConstructInstance<ArenaContainer>();
  arena->Init();
  *auxSlot = reinterpret_cast<ObjHeader*>(arena);
  return arena;
}

}  // namespace

ContainerHeader* AllocContainer(size_t size) {
  auto state = memoryState;
#if USE_GC
  // TODO: try to reuse elements of finalizer queue for new allocations, question
  // is how to get actual size of container.
#endif
  ContainerHeader* result = konanConstructSizedInstance<ContainerHeader>(size);
#if TRACE_MEMORY
  fprintf(stderr, ">>> alloc %d -> %p\n", static_cast<int>(size), result);
  state->containers->insert(result);
#endif
  state->allocCount++;
  return result;
}

extern "C" {
void objc_release(void* ptr);
}

inline void runDeallocationHooks(ObjHeader* obj) {
#if KONAN_OBJC_INTEROP
  if (obj->type_info() == theObjCPointerHolderTypeInfo) {
    void* objcPtr =  *reinterpret_cast<void**>(obj + 1); // TODO: use more reliable layout description
    objc_release(objcPtr);
  }
#endif
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

void FreeContainer(ContainerHeader* header) {
  RuntimeAssert(!isPermanent(header), "this kind of container shalln't be freed");
  auto state = memoryState;
#if TRACE_MEMORY
  if (isFreeable(header)) {
    fprintf(stderr, "<<< free %p\n", header);
    state->containers->erase(header);
  }
#endif

#if USE_GC
  removeFreeable(state, header);
#endif
  // Now let's clean all object's fields in this container.
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(header + 1);

  for (int index = 0; index < header->objectCount(); index++) {
    runDeallocationHooks(obj);

    const TypeInfo* typeInfo = obj->type_info();

    DeinitInstanceBodyImpl(typeInfo, reinterpret_cast<void*>(obj + 1));

    // Object arrays are *special*.
    if (typeInfo == theArrayTypeInfo) {
      ArrayHeader* array = obj->array();
      ReleaseRefs(ArrayAddressOfElementAt(array, 0), array->count_);
    }
    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }

  // And release underlying memory.
  if (isFreeable(header)) {
    scheduleDestroyContainer(state, header);
  }
}

#if USE_GC
void FreeContainerNoRef(MemoryState* state, ContainerHeader* header) {
  RuntimeAssert(isFreeable(header), "this kind of container shalln't be freed");
#if TRACE_MEMORY
  fprintf(stderr, "<<< free %p\n", header);
  state->containers->erase(header);
#endif
#if USE_GC
  removeFreeable(state, header);
#endif
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(header + 1);

  for (int index = 0; index < header->objectCount(); index++) {
    runDeallocationHooks(obj);
    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }

  scheduleDestroyContainer(state, header);
}
#endif

void ObjectContainer::Init(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "Must be an object");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ObjHeader) + type_info->instanceSize_;
  header_ = AllocContainer(alloc_size);
  if (header_) {
    // One object in this container.
    header_->setObjectCount(1);
     // header->refCount_ is zero initialized by AllocContainer().
    SetMeta(GetPlace(), type_info);
#if TRACE_MEMORY
    fprintf(stderr, "object at %p\n", GetPlace());
#endif
  }
}

void ArrayContainer::Init(const TypeInfo* type_info, uint32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "Must be an array");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ArrayHeader) -
      type_info->instanceSize_ * elements;
  header_ = AllocContainer(alloc_size);
  RuntimeAssert(header_ != nullptr, "Cannot alloc memory");
  if (header_) {
    // One object in this container.
    header_->setObjectCount(1);
    // header->refCount_ is zero initialized by AllocContainer().
    GetPlace()->count_ = elements;
    SetMeta(GetPlace()->obj(), type_info);
#if TRACE_MEMORY
    fprintf(stderr, "array at %p\n", GetPlace());
#endif
  }
}

// TODO: store arena containers in some reuseable data structure, similar to
// finalizer queue.
void ArenaContainer::Init() {
  allocContainer(1024);
}

void ArenaContainer::Deinit() {
  auto chunk = currentChunk_;
  while (chunk != nullptr) {
    // FreeContainer() doesn't release memory when CONTAINER_TAG_STACK is set.
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
  currentChunk_->asHeader()->incObjectCount();
  setMeta(result, type_info);
  return result;
}

ArrayHeader* ArenaContainer::PlaceArray(const TypeInfo* type_info, uint32_t count) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  container_size_t size = sizeof(ArrayHeader) - type_info->instanceSize_ * count;
  ArrayHeader* result = reinterpret_cast<ArrayHeader*>(place(size));
  if (!result) {
    return nullptr;
  }
  currentChunk_->asHeader()->incObjectCount();
  setMeta(result->obj(), type_info);
  result->count_ = count;
  return result;
}

inline void AddRef(const ObjHeader* object) {
#if TRACE_MEMORY
  fprintf(stderr, "AddRef on %p in %p\n", object, object->container());
#endif
  AddRef(object->container());
#if USE_GC
  // TODO: one could remove from toFree set here, as now container is reachable
  // from the rootset, so cannot be cycle collection candidate.
  // removeFreeable(memoryState, object->container());
#endif
}

inline void ReleaseRef(const ObjHeader* object) {
#if TRACE_MEMORY
  fprintf(stderr, "ReleaseRef on %p in %p\n", object, object->container());
#endif
#if USE_GC
  // If object is not a cycle candidate - just return.
  if (Release(object->container())) {
    return;
  }
#if TRACE_MEMORY
  fprintf(stderr, "%p is release candidate\n", object->container());
#endif
  addFreeable(memoryState, object->container());
#else // !USE_GC
  Release(object->container());
#endif // USE_GC
}

extern "C" {

MemoryState* InitMemory() {
  RuntimeAssert(offsetof(ArrayHeader, type_info_)
                ==
                offsetof(ObjHeader,   type_info_),
                "Layout mismatch");
  RuntimeAssert(offsetof(ArrayHeader, container_offset_negative_)
                ==
                offsetof(ObjHeader  , container_offset_negative_),
                "Layout mismatch");
  RuntimeAssert(memoryState == nullptr, "memory state must be clear");
  memoryState = konanConstructInstance<MemoryState>();
  // TODO: initialize heap here.
  memoryState->allocCount = 0;
#if TRACE_MEMORY
  memoryState->globalObjects = konanConstructInstance<KRefPtrList>();
  memoryState->containers = konanConstructInstance<ContainerHeaderSet>();
#endif
#if USE_GC
  memoryState->finalizerQueue = konanConstructInstance<ContainerHeaderDeque>();
  memoryState->toFree = konanConstructInstance<ContainerHeaderSet>();
  memoryState->gcInProgress = false;
  initThreshold(memoryState, kGcThreshold);
  memoryState->gcSuspendCount = 0;
#endif
  return memoryState;
}

void DeinitMemory(MemoryState* memoryState) {
#if TRACE_MEMORY
  // Free all global objects, to ensure no memory leaks happens.
  for (auto location: *memoryState->globalObjects) {
    fprintf(stderr, "Release global in *%p: %p\n", location, *location);
    UpdateRef(location, nullptr);
  }
  konanDestructInstance(memoryState->globalObjects);
  memoryState->globalObjects = nullptr;
#endif

#if USE_GC
  GarbageCollect();
  konanDestructInstance(memoryState->toFree);
  memoryState->toFree = nullptr;

#if OPTIMIZE_GC
  if (memoryState->toFreeCache != nullptr) {
    konanFreeMemory(memoryState->toFreeCache);
    memoryState->toFreeCache = nullptr;
  }
#endif

  konanDestructInstance(memoryState->finalizerQueue);
  memoryState->finalizerQueue = nullptr;

#endif // USE_GC

  if (memoryState->allocCount > 0) {
    fprintf(stderr, "*** Memory leaks, leaked %d containers ***\n",
            memoryState->allocCount);
#if TRACE_MEMORY
    dumpReachable("", memoryState->containers);
    konanDestructInstance(memoryState->containers);
    memoryState->containers = nullptr;
#endif
  }

  konanFreeMemory(memoryState);
  ::memoryState = nullptr;
}

OBJ_GETTER(AllocInstance, const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  if (isArenaSlot(OBJ_RESULT)) {
    auto arena = initedArena(asArenaSlot(OBJ_RESULT));
    auto result = arena->PlaceObject(type_info);
#if TRACE_MEMORY
    fprintf(stderr, "instance %p in arena: %p\n", result, arena);
#endif
    return result;
  }
  RETURN_OBJ(ObjectContainer(type_info).GetPlace());
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* type_info, uint32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  if (isArenaSlot(OBJ_RESULT)) {
    auto arena = initedArena(asArenaSlot(OBJ_RESULT));
    auto result = arena->PlaceArray(type_info, elements)->obj();
#if TRACE_MEMORY
    fprintf(stderr, "array[%d] %p in arena: %p\n", elements, result, arena);
#endif
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
  UpdateRef(location, object);
#if KONAN_NO_EXCEPTIONS
  ctor(object);
#if TRACE_MEMORY
  memoryState->globalObjects->push_back(location);
#endif
  return object;
#else
  try {
    ctor(object);
#if TRACE_MEMORY
    memoryState->globalObjects->push_back(location);
#endif
    return object;
  } catch (...) {
    UpdateRef(OBJ_RESULT, nullptr);
    UpdateRef(location, nullptr);
    throw;
  }
#endif
}

void SetRef(ObjHeader** location, const ObjHeader* object) {
#if TRACE_MEMORY
  fprintf(stderr, "SetRef *%p: %p\n", location, object);
#endif
  *const_cast<const ObjHeader**>(location) = object;
  if (object != nullptr) {
    AddRef(object);
  }
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

void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
  if (isArenaSlot(returnSlot)) {
    if (object == nullptr
        || (object->container()->refCount_ & CONTAINER_TAG_MASK) > CONTAINER_TAG_NORMAL) {
        // Not a subject of reference counting.
        return;
    }
    auto arena = initedArena(asArenaSlot(returnSlot));
    returnSlot = arena->getSlot();
  }
  ObjHeader* old = *returnSlot;
#if TRACE_MEMORY
  fprintf(stderr, "UpdateReturnRef *%p: %p -> %p\n", returnSlot, old, object);
#endif
  if (old != object) {
    if (object != nullptr) {
      AddRef(object);
    }
    *const_cast<const ObjHeader**>(returnSlot) = object;
    if (old > reinterpret_cast<ObjHeader*>(1)) {
      ReleaseRef(old);
    }
  }
}

void UpdateRef(ObjHeader** location, const ObjHeader* object) {
  RuntimeAssert(!isArenaSlot(location), "must not be a slot");
  ObjHeader* old = *location;
#if TRACE_MEMORY
  fprintf(stderr, "UpdateRef *%p: %p -> %p\n", location, old, object);
#endif
  if (old != object) {
    if (object != nullptr) {
      AddRef(object);
    }
    *const_cast<const ObjHeader**>(location) = object;
    if (old > reinterpret_cast<ObjHeader*>(1)) {
      ReleaseRef(old);
    }
  }
}

void LeaveFrame(ObjHeader** start, int count) {
#if TRACE_MEMORY
    fprintf(stderr, "LeaveFrame %p .. %p\n", start, start + count);
#endif
  ReleaseRefs(start + 1, count - 1);
  if (*start != nullptr) {
    auto arena = initedArena(start);
#if TRACE_MEMORY
    fprintf(stderr, "LeaveFrame: free arena %p\n", arena);
#endif
    arena->Deinit();
    konanFreeMemory(arena);
  }
}

void ReleaseRefs(ObjHeader** start, int count) {
#if TRACE_MEMORY
  fprintf(stderr, "ReleaseRefs %p .. %p\n", start, start + count);
#endif
  ObjHeader** current = start;
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
  RuntimeAssert(state->toFree != nullptr, "GC must not be stopped");
  RuntimeAssert(!state->gcInProgress, "Recursive GC is disallowed");

  // Flush cache.
  flushFreeableCache(state);

  state->gcInProgress = true;
  // Traverse inner pointers in the closure of release candidates, and
  // temporary decrement refs on them. Set CONTAINER_TAG_SEEN while traversing.
#if TRACE_GC_PHASES
  dumpReachable("P0", state->toFree);
#endif
  for (auto container : *state->toFree) {
    phase1(container);
  }
#if TRACE_GC_PHASES
  dumpReachable("P1", state->toFree);
#endif

  // Collect rootset from containers with non-zero reference counter. Those must
  // be referenced from outside of newly released object graph.
  // Clear CONTAINER_TAG_SEEN while traversing.
  ContainerHeaderSet rootset;
  for (auto container : *state->toFree) {
    phase2(container, &rootset);
  }
#if TRACE_GC_PHASES
  dumpReachable("P2", state->toFree);
#endif

  // Increment references for all elements reachable from the rootset.
  // Set CONTAINER_TAG_SEEN while traversing.
  for (auto container : rootset) {
#if TRACE_MEMORY
    fprintf(stderr, "rootset %p\n", container);
#endif
    phase3(container);
  }
#if TRACE_GC_PHASES
  dumpReachable("P3", state->toFree);
#endif

  // Traverse all elements, and collect those not having CONTAINER_TAG_SEEN and zero RC.
  // Clear CONTAINER_TAG_SEEN while traversing on live elements, set in on dead elements.
  for (auto container : *state->toFree) {
    phase4(state, container);
  }
#if TRACE_GC_PHASES
  dumpReachable("P4", state->toFree);
#endif

  // Clear cycle candidates list.
  state->toFree->clear();

  processFinalizerQueue(state);

  state->gcInProgress = false;
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
    memoryState->toFree = nullptr;
  }
#endif
}

void Kotlin_konan_internal_GC_start(KRef) {
#if USE_GC
  if (memoryState->toFree == nullptr) {
    memoryState->toFree = konanConstructInstance<ContainerHeaderSet>();
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
  ::AddRef(any->container());
  return reinterpret_cast<KNativePtr>(any);
}

void DisposeStablePointer(KNativePtr pointer) {
  if (pointer == nullptr) return;
  KRef ref = reinterpret_cast<KRef>(pointer);
  ::Release(ref->container());
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
  // Somewhat hacky.
  *OBJ_RESULT = ref;
  return ref;
}

bool ClearSubgraphReferences(ObjHeader* root, bool checked) {
#if USE_GC
  if (root != nullptr) {
    auto state = memoryState;
    auto container = root->container();
    ContainerHeaderList todo;
    ContainerHeaderSet subgraph;
    todo.push_back(container);
    while (todo.size() > 0) {
      auto header = todo.back();
      todo.pop_back();
      if (subgraph.count(header) != 0)
        continue;
      subgraph.insert(header);
      removeFreeable(state, header);
      auto children = collectMutableReferred(header);
      for (auto child : children) {
        todo.push_back(child);
      }
    }
  }
#endif  // USE_GC
  // TODO: perform trial deletion starting from this root, if in checked mode.
  return true;
}

} // extern "C"
