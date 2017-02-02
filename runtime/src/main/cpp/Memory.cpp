#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include <cstddef> // for offsetof
#include <unordered_set>
#include <vector>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"

// Define to 1 to use in the multithreaded environment.
#define CONCURRENT 0
// If garbage collection algorithm for cyclic garbage to be used.
#define USE_GC 1
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

#if USE_GC
// Collection threshold default (collect after having so many elements in the
// release candidates set).
constexpr size_t kGcThreshold = 10000;
#endif

#if TRACE_MEMORY || USE_GC
typedef std::unordered_set<ContainerHeader*> ContainerHeaderSet;
typedef std::vector<ContainerHeader*> ContainerHeaderList;
typedef std::vector<KRef*> KRefPtrList;
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
  // Set of references to release.
  ContainerHeaderSet* toFree;
  // How many GC suspend requests happened.
  int gcSuspendCount;
  // How many candidate elements in toFree shall trigger collection.
  size_t gcThreshold;
  // If collection is in progress.
  bool gcInProgress;
#endif
};

MemoryState* memoryState = nullptr;

// TODO: use those allocators for STL containers as well.
template <typename T>
inline T* allocMemory(container_size_t size) {
  return reinterpret_cast<T*>(calloc(1, size));
}

inline void freeMemory(void* memory) {
  free(memory);
}

inline bool isFreeable(const ContainerHeader* header) {
  return (header->refCount_ & CONTAINER_TAG_MASK) < CONTAINER_TAG_PERMANENT;
}

inline bool isPermanent(const ContainerHeader* header) {
  return (header->refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_PERMANENT;
}

inline container_size_t alignUp(container_size_t size, int alignment) {
  return (size + alignment - 1) & ~(alignment - 1);
}

inline bool isArenaSlot(ObjHeader** slot) {
  return (reinterpret_cast<uintptr_t>(slot) & ARENA_BIT) != 0;
}

inline ObjHeader** asArenaSlot(ObjHeader** slot) {
  return reinterpret_cast<ObjHeader**>(
      reinterpret_cast<uintptr_t>(slot) & ~ARENA_BIT);
}

#if USE_GC

// Must be vector or map 'container -> number', to keep reference counters correct.
ContainerHeaderList collectMutableReferred(ContainerHeader* header) {
  ContainerHeaderList result;
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(header + 1);
  const TypeInfo* typeInfo = obj->type_info();
  // TODO: generalize iteration over all references.
  // TODO: this code relies on single object per container assumption.
  for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
    ObjHeader** location = reinterpret_cast<ObjHeader**>(
      reinterpret_cast<uintptr_t>(obj + 1) + typeInfo->objOffsets_[index]);
    ObjHeader* obj = *location;
    if (obj != nullptr && !isPermanent(obj->container())) {
      result.push_back(obj->container());
    }
  }
  if (typeInfo == theArrayTypeInfo) {
    ArrayHeader* array = obj->array();
    for (int index = 0; index < array->count_; index++) {
      ObjHeader* obj = *ArrayAddressOfElementAt(array, index);
      if (obj != nullptr && !isPermanent(obj->container())) {
        result.push_back(obj->container());
      }
    }
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
    dumpWorker(prefix, container, &seen);
  }
}

void phase1(ContainerHeader* header) {
  if ((header->refCount_ & CONTAINER_TAG_SEEN) != 0)
    return;
  header->refCount_ |= CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
      container->refCount_ -= CONTAINER_TAG_INCREMENT;
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
    container->refCount_ += CONTAINER_TAG_INCREMENT;
    phase3(container);
  }
}

void phase4(ContainerHeader* header, ContainerHeaderSet* toRemove) {
  auto refCount = header->refCount_ >> CONTAINER_TAG_SHIFT;
  bool seen = (refCount > 0 && (header->refCount_ & CONTAINER_TAG_SEEN) == 0) ||
      (refCount == 0 && (header->refCount_ & CONTAINER_TAG_SEEN) != 0);
  if (seen) return;

  // Add to toRemove set.
  if (refCount == 0)
    toRemove->insert(header);

  // Update seen bit.
  if (refCount == 0)
    header->refCount_ |= CONTAINER_TAG_SEEN;
  else
    header->refCount_ &= ~CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
    phase4(container, toRemove);
  }
}

#endif // USE_GC

// We use first slot as place to store frame-local arena container.
// TODO: create ArenaContainer object on the stack, so that we don't
// do two allocations per frame (ArenaContainer + actual container).
inline ArenaContainer* initedArena(ObjHeader** auxSlot) {
  ObjHeader* slotValue = *auxSlot;
  if (slotValue) return reinterpret_cast<ArenaContainer*>(slotValue);
  ArenaContainer* arena = allocMemory<ArenaContainer>(sizeof(ArenaContainer));
  arena->Init();
  *auxSlot = reinterpret_cast<ObjHeader*>(arena);
  return arena;
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

}  // namespace

ContainerHeader* AllocContainer(size_t size) {
  ContainerHeader* result = allocMemory<ContainerHeader>(size);
#if TRACE_MEMORY
  fprintf(stderr, ">>> alloc %d -> %p\n", static_cast<int>(size), result);
  memoryState->containers->insert(result);
#endif
  // TODO: atomic increment in concurrent case.
  memoryState->allocCount++;
  return result;
}



void FreeContainer(ContainerHeader* header) {
  RuntimeAssert(!isPermanent(header), "this kind of container shalln't be freed");
#if TRACE_MEMORY
  if (isFreeable(header)) {
    fprintf(stderr, "<<< free %p\n", header);
    memoryState->containers->erase(header);
  }
#endif

#if USE_GC
  if (memoryState->toFree && isFreeable(header))
    memoryState->toFree->erase(header);
#endif
  // Now let's clean all object's fields in this container.
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(header + 1);

  for (int index = 0; index < header->objectCount_; index++) {
    const TypeInfo* typeInfo = obj->type_info();

    // We use *local* versions as no other threads could see dead objects.
    for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
      ObjHeader** location = reinterpret_cast<ObjHeader**>(
          reinterpret_cast<uintptr_t>(obj + 1) + typeInfo->objOffsets_[index]);
      UpdateLocalRef(location, nullptr);
    }
    // Object arrays are *special*.
    if (typeInfo == theArrayTypeInfo) {
      ArrayHeader* array = obj->array();
      ReleaseLocalRefs(ArrayAddressOfElementAt(array, 0), array->count_);
    }
    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }

  // And release underlying memory.
  if (isFreeable(header)) {
    // TODO: atomic decrement in concurrent case.
#if CONCURRENT
    #error "Atomic update of allocCount"
#endif
    memoryState->allocCount--;
    freeMemory(header);
  }
}

#if USE_GC
void FreeContainerNoRef(ContainerHeader* header) {
  RuntimeAssert(isFreeable(header), "this kind of container shalln't be freed");
#if TRACE_MEMORY
  fprintf(stderr, "<<< free %p\n", header);
  memoryState->containers->erase(header);
#endif
#if USE_GC
  if (memoryState->toFree)
    memoryState->toFree->erase(header);
#endif
  memoryState->allocCount--;
  freeMemory(header);
}
#endif

void ObjectContainer::Init(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "Must be an object");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ObjHeader) + type_info->instanceSize_;
  header_ = AllocContainer(alloc_size);
  if (header_) {
    // One object in this container.
    header_->objectCount_ = 1;
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
    header_->objectCount_ = 1;
    // header->refCount_ is zero initialized by AllocContainer().
    GetPlace()->count_ = elements;
    SetMeta(GetPlace()->obj(), type_info);
#if TRACE_MEMORY
    fprintf(stderr, "array at %p\n", GetPlace());
#endif
  }
}

void ArenaContainer::Init() {
  allocContainer(1024);
}

void ArenaContainer::Deinit() {
  auto chunk = currentChunk_;
  while (chunk != nullptr) {
    auto toRemove = chunk;
    // FreeContainer() doesn't release memory when CONTAINER_TAG_STACK is set.
    FreeContainer(chunk->asHeader());
    chunk = chunk->next;
    freeMemory(toRemove);
  }
}

bool ArenaContainer::allocContainer(container_size_t minSize) {
  auto size = minSize + sizeof(ContainerHeader) + sizeof(ContainerChunk);
  size = alignUp(size, kContainerAlignment);
  // TODO: keep simple cache of container chunks.
  ContainerChunk* result = allocMemory<ContainerChunk>(size);
  RuntimeAssert(result != nullptr, "Cannot alloc memory");
  if (result == nullptr) return false;
  result->next = currentChunk_;
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

ObjHeader* ArenaContainer::PlaceObject(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  uint32_t size = type_info->instanceSize_ + sizeof(ObjHeader);
  ObjHeader* result = reinterpret_cast<ObjHeader*>(place(size));
  if (!result) {
      return nullptr;
  }
  currentChunk_->asHeader()->objectCount_++;
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
  currentChunk_->asHeader()->objectCount_++;
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
  // memoryState->toFree->erase(object->container());
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
  if (memoryState->toFree != nullptr) {
    memoryState->toFree->insert(object->container());
    if (memoryState->gcSuspendCount == 0 &&
        memoryState->toFree->size() > memoryState->gcThreshold)
      GarbageCollect();
  }
#else // !USE_GC
  Release(object->container());
#endif // USE_GC
}

extern "C" {

void InitMemory() {
  RuntimeAssert(offsetof(ArrayHeader, type_info_)
                ==
                offsetof(ObjHeader,   type_info_),
                "Layout mismatch");
  RuntimeAssert(offsetof(ArrayHeader, container_offset_negative_)
                ==
                offsetof(ObjHeader  , container_offset_negative_),
                "Layout mismatch");
  RuntimeAssert(memoryState == nullptr, "memory state must be clear");
  memoryState = new MemoryState();
  // TODO: initialize heap here.
  memoryState->allocCount = 0;
#if TRACE_MEMORY
  memoryState->globalObjects = new KRefPtrList();
  memoryState->containers = new ContainerHeaderSet();
#endif
#if USE_GC
#if CONCURRENT
  #error "Concurrent GC is not yet implemented"
#endif
  memoryState->toFree = new ContainerHeaderSet();
  memoryState->gcInProgress = false;
  memoryState->gcThreshold = kGcThreshold;
  memoryState->gcSuspendCount = 0;
#endif
}

void DeinitMemory() {
#if TRACE_MEMORY
  // Free all global objects, to ensure no memory leaks happens.
  for (auto location: *memoryState->globalObjects) {
    fprintf(stderr, "Release global in *%p: %p\n", location, *location);
    UpdateGlobalRef(location, nullptr);
  }
  delete memoryState->globalObjects;
  memoryState->globalObjects = nullptr;
#endif

#if USE_GC
  GarbageCollect();
  delete memoryState->toFree;
  memoryState->toFree = nullptr;
#endif // USE_GC

  if (memoryState->allocCount > 0) {
#if TRACE_MEMORY
    fprintf(stderr, "*** Memory leaks, leaked %d containers ***\n", memoryState->allocCount);
    dumpReachable("", memoryState->containers);
    delete memoryState->containers;
    memoryState->containers = nullptr;
#endif
  }

  delete memoryState;
  memoryState = nullptr;
}

OBJ_GETTER(AllocInstance, const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  if (isArenaSlot(OBJ_RESULT)) {
    auto arena = initedArena(asArenaSlot(OBJ_RESULT));
    auto result = arena->PlaceObject(type_info);
#if TRACE_MEMORY
    fprintf(stderr, "instace %p in arena: %p\n", result, arena);
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

OBJ_GETTER(AllocStringInstance, const char* data, uint32_t length) {
  ArrayHeader* array = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(array, 0),
      data,
      length);
  RETURN_OBJ(array->obj());
}

OBJ_GETTER(InitInstance,
    ObjHeader** location, const TypeInfo* type_info, void (*ctor)(ObjHeader*)) {
  ObjHeader* sentinel = reinterpret_cast<ObjHeader*>(1);
  ObjHeader* value;
  // Wait until other initializers.
  // TODO: check CONCURRENT!
  while ((value = __sync_val_compare_and_swap(
             location, nullptr, sentinel)) == sentinel) {
    // TODO: consider yielding.
  }

  if (value != nullptr) {
    // OK'ish, inited by someone else.
    RETURN_OBJ(value);
  }

  ObjHeader* object = AllocInstance(type_info, OBJ_RESULT);
  UpdateGlobalRef(location, object);
  try {
    ctor(object);
#if CONCURRENT
    // TODO: locking or smth lock-free in MT case?
#endif
#if TRACE_MEMORY
    memoryState->globalObjects->push_back(location);
#endif
    return object;
  } catch (...) {
    UpdateLocalRef(OBJ_RESULT, nullptr);
    UpdateGlobalRef(location, nullptr);
    throw;
  }
}

void SetLocalRef(ObjHeader** location, const ObjHeader* object) {
#if TRACE_MEMORY
  fprintf(stderr, "SetLocalRef *%p: %p\n", location, object);
#endif
  *const_cast<const ObjHeader**>(location) = object;
  if (object != nullptr) {
    AddRef(object);
  }
}

void SetGlobalRef(ObjHeader** location, const ObjHeader* object) {
#if TRACE_MEMORY
  fprintf(stderr, "SetGlobalRef *%p: %p\n", location, object);
#endif
  *const_cast<const ObjHeader**>(location) = object;
   if (object != nullptr) {
      AddRef(object);
   }
#if CONCURRENT
   // TODO: memory fence here.
#endif
}

void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
  if (isArenaSlot(returnSlot)) return;
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

void UpdateLocalRef(ObjHeader** location, const ObjHeader* object) {
  RuntimeAssert(!isArenaSlot(location), "must not be a slot");
  ObjHeader* old = *location;
#if TRACE_MEMORY
  fprintf(stderr, "UpdateLocalRef *%p: %p -> %p\n", location, old, object);
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

void UpdateGlobalRef(ObjHeader** location, const ObjHeader* object) {
  RuntimeAssert(!isArenaSlot(location), "Must not be an arena");
#if CONCURRENT
  ObjHeader* old = *location;
#if TRACE_MEMORY
  fprintf(stderr, "UpdateGlobalRef *%p: %p -> %p\n", location, old, object);
#endif
  if (old != object) {
    if (object != nullptr) {
      AddRef(object);
    }
    bool written = __sync_bool_compare_and_swap(
        location, old, const_cast<ObjHeader*>(object));
    if (written) {
      if (old > reinterpret_cast<ObjHeader*>(1)) {
        ReleaseRef(old);
      }
    } else {
      if (object != nullptr) {
        ReleaseRef(object);
      }
    }
  }
#else
  UpdateLocalRef(location, object);
#endif
}

void LeaveFrame(ObjHeader** start, int count) {
#if TRACE_MEMORY
    fprintf(stderr, "LeaveFrame %p .. %p\n", start, start + count);
#endif
  ReleaseLocalRefs(start + 1, count - 1);
  if (*start != nullptr) {
    auto arena = initedArena(start);
#if TRACE_MEMORY
    fprintf(stderr, "LeaveFrame: free arena %p\n", arena);
#endif
    arena->Deinit();
    freeMemory(arena);
  }
}

void ReleaseLocalRefs(ObjHeader** start, int count) {
#if TRACE_MEMORY
  fprintf(stderr, "ReleaseLocalRefs %p .. %p\n", start, start + count);
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

void ReleaseGlobalRefs(ObjHeader** start, int count) {
#if TRACE_MEMORY
  fprintf(stderr, "ReleaseGlobalRefs %p .. %p\n", start, start + count);
#endif
#if CONCURRENT
  ObjHeader** current = start;
  while (count-- > 0) {
    ObjHeader* object = *current;
    if (object != nullptr) {
      bool written = __sync_bool_compare_and_swap(
          current, object, nullptr);
      if (written)
        ReleaseRef(object);
    }
    current++;
  }
#else
  ObjHeader** current = start;
  while (count-- > 0) {
    ObjHeader* object = *current;
    if (object != nullptr) {
      ReleaseRef(object);
      // Usually required.
      *current = nullptr;
    }
    current++;
  }
#endif
}

#if USE_GC
void GarbageCollect() {
  RuntimeAssert(memoryState->toFree != nullptr, "GC must not be stopped");
  RuntimeAssert(!memoryState->gcInProgress, "Recursive GC is disallowed");
  memoryState->gcInProgress = true;
  // Traverse inner pointers in the closure of release candidates, and
  // temporary decrement refs on them. Set CONTAINER_TAG_SEEN while traversing.
#if TRACE_GC_PHASES
  dumpReachable("P0", memoryState->toFree);
#endif
  for (auto container : *memoryState->toFree) {
    phase1(container);
  }
#if TRACE_GC_PHASES
  dumpReachable("P1", memoryState->toFree);
#endif

  // Collect rootset from containers with non-zero reference counter. Those must
  // be referenced from outside of newly released object graph.
  // Clear CONTAINER_TAG_SEEN while traversing.
  ContainerHeaderSet rootset;
  for (auto container : *memoryState->toFree) {
    phase2(container, &rootset);
  }
#if TRACE_GC_PHASES
  dumpReachable("P2", memoryState->toFree);
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
  dumpReachable("P3", memoryState->toFree);
#endif

  // Traverse all elements, and collect those not having CONTAINER_TAG_SEEN and zero RC.
  // Clear CONTAINER_TAG_SEEN while traversing on live elements, set in on dead elements.
  ContainerHeaderSet toRemove;
  for (auto container : *memoryState->toFree) {
    phase4(container, &toRemove);
  }
#if TRACE_GC_PHASES
  dumpReachable("P4", memoryState->toFree);
#endif

  // Clear cycle candidates list.
  memoryState->toFree->clear();

  for (auto header : toRemove) {
    RuntimeAssert((header->refCount_ & CONTAINER_TAG_SEEN) != 0, "Must be not seen");
    FreeContainerNoRef(header);
  }

  memoryState->gcInProgress = false;
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
  if (memoryState->gcSuspendCount > 0) {
    memoryState->gcSuspendCount--;
    if (memoryState->toFree != nullptr &&
        memoryState->toFree->size() >= memoryState->gcThreshold) {
      GarbageCollect();
    }
  }
#endif
}

void Kotlin_konan_internal_GC_stop(KRef) {
#if USE_GC
  if (memoryState->toFree != nullptr) {
    GarbageCollect();
    delete memoryState->toFree;
    memoryState->toFree = nullptr;
  }
#endif
}

void Kotlin_konan_internal_GC_start(KRef) {
#if USE_GC
  if (memoryState->toFree == nullptr) {
    memoryState->toFree = new ContainerHeaderSet();
  }
#endif
}

void Kotlin_konan_internal_GC_setThreshold(KRef, KInt value) {
#if USE_GC
  if (value > 0) {
    memoryState->gcThreshold = value;
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


} // extern "C"
