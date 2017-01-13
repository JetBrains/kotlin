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
  CONTAINER_TAG_NOCOUNT | CONTAINER_TAG_INCREMENT
};

namespace {

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

#if USE_GC
bool isPermanent(const ContainerHeader* header) {
  return (header->ref_count_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_NOCOUNT;
}

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
          header, header->ref_count_, header->ref_count_ >> CONTAINER_TAG_SHIFT,
          (header->ref_count_ & CONTAINER_TAG_SEEN) != 0 ? "X" : "-");
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
  if ((header->ref_count_ & CONTAINER_TAG_SEEN) != 0)
    return;
  header->ref_count_ |= CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
      container->ref_count_ -= CONTAINER_TAG_INCREMENT;
      phase1(container);
  }
}

void phase2(ContainerHeader* header, ContainerHeaderSet* rootset) {
  if ((header->ref_count_ & CONTAINER_TAG_SEEN) == 0)
    return;
  if ((header->ref_count_ >> CONTAINER_TAG_SHIFT) != 0)
    rootset->insert(header);
  header->ref_count_ &= ~CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
    phase2(container, rootset);
  }
}

void phase3(ContainerHeader* header) {
  if ((header->ref_count_ & CONTAINER_TAG_SEEN) != 0) {
    return;
  }
  header->ref_count_ |= CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
    container->ref_count_ += CONTAINER_TAG_INCREMENT;
    phase3(container);
  }
}

void phase4(ContainerHeader* header, ContainerHeaderSet* toRemove) {
  auto ref_count = header->ref_count_ >> CONTAINER_TAG_SHIFT;
  bool seen = (ref_count > 0 && (header->ref_count_ & CONTAINER_TAG_SEEN) == 0) ||
      (ref_count == 0 && (header->ref_count_ & CONTAINER_TAG_SEEN) != 0);
  if (seen) return;

  // Add to toRemove set.
  if (ref_count == 0)
    toRemove->insert(header);

  // Update seen bit.
  if (ref_count == 0)
    header->ref_count_ |= CONTAINER_TAG_SEEN;
  else
    header->ref_count_ &= ~CONTAINER_TAG_SEEN;
  auto containers = collectMutableReferred(header);
  for (auto container : containers) {
    phase4(container, toRemove);
  }
}

#endif // USE_GC

}  // namespace

ContainerHeader* AllocContainer(size_t size) {
  ContainerHeader* result = reinterpret_cast<ContainerHeader*>(calloc(1, size));
#if TRACE_MEMORY
  fprintf(stderr, ">>> alloc %d -> %p\n", (int)size, result);
   memoryState->containers->insert(result);
#endif
  // TODO: atomic increment in concurrent case.
  memoryState->allocCount++;
  return result;
}

void FreeContainer(ContainerHeader* header) {
  RuntimeAssert(!isPermanent(header), "this kind of container shalln't be freed");
#if TRACE_MEMORY
  fprintf(stderr, "<<< free %p\n", header);
  memoryState->containers->erase(header);
#endif
  header->ref_count_ = CONTAINER_TAG_INVALID;
#if USE_GC
  if (memoryState->toFree)
    memoryState->toFree->erase(header);
#endif
  // Now let's clean all object's fields in this container.
  // TODO: this is gross hack, relying on the fact that we now only alloc
  // ArenaContainer and ObjectContainer, which both have single element.
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(header + 1);
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

  // And release underlying memory.
  // TODO: atomic decrement in concurrent case.
#if CONCURRENT
  #error "Atomic update of allocCount"
#endif
  memoryState->allocCount--;
  free(header);
}

#if USE_GC
void FreeContainerNoRef(ContainerHeader* header) {
  RuntimeAssert(!isPermanent(header), "this kind of container shalln't be freed");
#if TRACE_MEMORY
  fprintf(stderr, "<<< free %p\n", header);
  memoryState->containers->erase(header);
#endif
  header->ref_count_ = CONTAINER_TAG_INVALID;
#if USE_GC
  if (memoryState->toFree)
    memoryState->toFree->erase(header);
#endif
  memoryState->allocCount--;
  free(header);
}
#endif

ArenaContainer::ArenaContainer(uint32_t size) {
    ArenaContainerHeader* header =
        static_cast<ArenaContainerHeader*>(AllocContainer(size + sizeof(ArenaContainerHeader)));
    header_ = header;
    // header->ref_count_ is zero initialized by AllocContainer().
    header->current_ =
        reinterpret_cast<uint8_t*>(header_) + sizeof(ArenaContainerHeader);
    header->end_ = header->current_ + size;
}

void ObjectContainer::Init(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "Must be an object");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ObjHeader) + type_info->instanceSize_;
  header_ = AllocContainer(alloc_size);
  if (header_) {
     // header->ref_count_ is zero initialized by AllocContainer().
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
    // header->ref_count_ is zero initialized by AllocContainer().
    GetPlace()->count_ = elements;
    SetMeta(GetPlace()->obj(), type_info);
#if TRACE_MEMORY
    fprintf(stderr, "array at %p\n", GetPlace());
#endif
  }
}

ObjHeader* ArenaContainer::PlaceObject(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  uint32_t size = type_info->instanceSize_ + sizeof(ObjHeader);
  ObjHeader* result = reinterpret_cast<ObjHeader*>(Place(size));
  if (!result) {
      return nullptr;
  }
  SetMeta(result, type_info);
  return result;
}

ArrayHeader* ArenaContainer::PlaceArray(const TypeInfo* type_info, int count) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  uint32_t size = sizeof(ArrayHeader) - type_info->instanceSize_ * count;
  ArrayHeader* result = reinterpret_cast<ArrayHeader*>(Place(size));
  if (!result) {
    return nullptr;
  }
  SetMeta(result->obj(), type_info);
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

// Now we ignore all placement hints and always allocate heap space for new object.
OBJ_GETTER(AllocInstance, const TypeInfo* type_info, PlacementHint hint) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  RETURN_OBJ(ObjectContainer(type_info).GetPlace());
}

OBJ_GETTER(AllocArrayInstance,
           const TypeInfo* type_info, PlacementHint hint, uint32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  RETURN_OBJ(ArrayContainer(type_info, elements).GetPlace()->obj());
}

OBJ_GETTER(AllocStringInstance,
  PlacementHint hint, const char* data, uint32_t length) {
  ArrayHeader* array = ArrayContainer(theStringTypeInfo, length).GetPlace();
  memcpy(
      ByteArrayAddressOfElementAt(array, 0),
      data,
      length);
  RETURN_OBJ(array->obj());
}

OBJ_GETTER(InitInstance,
    ObjHeader** location, const TypeInfo* type_info, PlacementHint hint,
    void (*ctor)(ObjHeader*)) {
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

  AllocInstance(type_info, hint, OBJ_RESULT);
  ObjHeader* object = *OBJ_RESULT;
  try {
    ctor(object);
    UpdateGlobalRef(location, object);
#if CONCURRENT
    // TODO: locking or smth lock-free in MT case?
#endif
#if TRACE_MEMORY
    memoryState->globalObjects->push_back(location);
#endif
    RETURN_OBJ_RESULT();
  } catch (...) {
    UpdateLocalRef(OBJ_RESULT, nullptr);
    UpdateGlobalRef(location, nullptr);
    RETURN_OBJ(nullptr);
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

void UpdateLocalRef(ObjHeader** location, const ObjHeader* object) {
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
    RuntimeAssert((header->ref_count_ & CONTAINER_TAG_SEEN) != 0, "Must be not seen");
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
