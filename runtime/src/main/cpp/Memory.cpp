#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include <cstddef> // for offsetof
#include <set> // only for memory tracing.
#include <vector>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"

// Define to 1 to see all memory operations.
#define TRACE_MEMORY 0
// Define to 1 to use in multithreaded environment.
#define CONCURRENT 0

ContainerHeader ObjHeader::theStaticObjectsContainer = { CONTAINER_TAG_NOCOUNT };

namespace {

// Current number of allocated containers.
int allocCount = 0;
#if TRACE_MEMORY
// List of all global objects addresses.
std::vector<KRef*>* globalObjects = nullptr;
// Set of all containers.
std::set<ContainerHeader*>* containers = nullptr;
#endif

}  // namespace

ContainerHeader* AllocContainer(size_t size) {
  ContainerHeader* result = reinterpret_cast<ContainerHeader*>(calloc(1, size));
#if TRACE_MEMORY
  printf(">>> alloc %d -> %p\n", (int)size, result);
  containers->insert(result);
#endif
  // TODO: atomic increment in concurrent case.
  allocCount++;
  return result;
}

void FreeContainer(ContainerHeader* header) {
#if TRACE_MEMORY
  printf("<<< free %p\n", header);
  containers->erase(header);
#endif
  header->ref_count_ = CONTAINER_TAG_INVALID;

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
  allocCount--;
  free(header);
}

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
    printf("object at %p\n", GetPlace());
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
    printf("array at %p\n", GetPlace());
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
  printf("AddRef on %p in %p\n", object, object->container());
#endif
  AddRef(object->container());
}

inline void ReleaseRef(const ObjHeader* object) {
#if TRACE_MEMORY
  printf("ReleaseRef on %p in %p\n", object, object->container());
#endif
  Release(object->container());
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
  // TODO: initialize heap here.
  allocCount = 0;
#if TRACE_MEMORY
  globalObjects = new std::vector<KRef*>();
  containers = new std::set<ContainerHeader*>();
#endif
}

void DeinitMemory() {
#if TRACE_MEMORY
  // Free all global objects, to ensure no memory leaks happens.
  for (auto location: *globalObjects) {
    printf("Release global in *%p: %p\n", location, *location);
    UpdateGlobalRef(location, nullptr);
  }
  delete globalObjects;
  globalObjects = nullptr;
#endif

  if (allocCount > 0) {
#if TRACE_MEMORY
    // TODO: move out of TRACE_MEMORY, once exceptions free memory.
    printf("*** Memory leaks, leaked %d containers ***\n", allocCount);
    for (auto container: *containers) {
      printf("Unfreed container %p, count = %d\n", container, container->ref_count_);
    }
    delete containers;
    containers = nullptr;
#endif
  }
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
    globalObjects->push_back(location);
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
  printf("SetLocalRef *%p: %p\n", location, object);
#endif
  *const_cast<const ObjHeader**>(location) = object;
  if (object != nullptr) {
    AddRef(object);
  }
}

void SetGlobalRef(ObjHeader** location, const ObjHeader* object) {
#if TRACE_MEMORY
  printf("SetGlobalRef *%p: %p\n", location, object);
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
  printf("UpdateLocalRef *%p: %p -> %p\n", location, old, object);
#endif
  if (old != object) {
    *const_cast<const ObjHeader**>(location) = object;
    if (old > reinterpret_cast<ObjHeader*>(1)) {
      ReleaseRef(old);
    }
    if (object != nullptr) {
      AddRef(object);
    }
  }
}

void UpdateGlobalRef(ObjHeader** location, const ObjHeader* object) {
#if CONCURRENT
  ObjHeader* old = *location;
#if TRACE_MEMORY
  printf("UpdateGlobalRef *%p: %p -> %p\n", location, old, object);
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
  printf("ReleaseLocalRefs %p .. %p\n", start, start + count);
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
  printf("ReleaseGlobalRefs %p .. %p\n", start, start + count);
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

} // extern "C"
