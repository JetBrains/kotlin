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

#ifndef RUNTIME_MEMORY_H
#define RUNTIME_MEMORY_H

#include "Assert.h"
#include "Common.h"
#include "TypeInfo.h"

// Must fit in two bits.
typedef enum {
  // Container is normal thread local container.
  CONTAINER_TAG_NORMAL = 0,
  // Container shall be atomically refcounted, currently disabled.
  // CONTAINER_TAG_SHARED = 1,
  // Those container tags shall not be refcounted.
  // Permanent object, cannot refer to non-permanent objects, so no need to cleanup those.
  CONTAINER_TAG_PERMANENT = 2,
  // Stack objects, no need to free, children cleanup still shall be there.
  CONTAINER_TAG_STACK = 3,
  // Container was seen during GC.
  CONTAINER_TAG_SEEN = 4,
  // Shift to get actual counter.
  CONTAINER_TAG_SHIFT = 3,
  // Actual value to increment/decrement conatiner by. Tag is in lower bits.
  CONTAINER_TAG_INCREMENT = 1 << CONTAINER_TAG_SHIFT,
  // Mask for container type, disregard seen bit.
  CONTAINER_TAG_MASK = ((CONTAINER_TAG_INCREMENT >> 1) - 1)
} ContainerTag;

typedef uint32_t container_offset_t;
typedef uint32_t container_size_t;


// Header of all container objects. Contains reference counter.
struct ContainerHeader {
  // Reference counter of container. Uses two lower bits of counter for
  // container type (for polymorphism in ::Release()).
  volatile uint32_t refCount_;
  // Number of objects in the container.
  uint32_t objectCount_;
};

struct ArrayHeader;

// Header of every object.
struct ObjHeader {
  const TypeInfo* type_info_;
  container_offset_t container_offset_negative_;

  const TypeInfo* type_info() const {
    // TODO: for moving collectors use meta-objects approach:
    //  - store tag in lower bit TypeInfo, which marks if meta-object is in place
    //  - when reading type_info_ check if it is unaligned
    //  - if it is, pointer points to the MetaObject
    //  - otherwise this is direct pointer to TypeInfo
    // Meta-object allows storing additional data associated with some objects,
    // such as stable hash code.
    return type_info_;
  }

  void set_type_info(const TypeInfo* type_info) {
    type_info_ = type_info;
  }

  static ContainerHeader theStaticObjectsContainer;

  ContainerHeader* container() const {
    if (container_offset_negative_ == 0) {
      return &theStaticObjectsContainer;
    } else {
      return reinterpret_cast<ContainerHeader*>(
          reinterpret_cast<uintptr_t>(this) - container_offset_negative_);
    }
  }

  // Unsafe cast to ArrayHeader. Use carefully!
  ArrayHeader* array() { return reinterpret_cast<ArrayHeader*>(this); }
  const ArrayHeader* array() const { return reinterpret_cast<const ArrayHeader*>(this); }
};

// Header of value type array objects. Keep layout in sync with that of object header.
struct ArrayHeader {
  const TypeInfo* type_info_;
  container_offset_t container_offset_negative_;

  const TypeInfo* type_info() const {
    // TODO: for moving collectors use meta-objects approach:
    //  - store tag in lower bit TypeInfo, which marks if meta-object is in place
    //  - when reading type_info_ check if it is unaligned
    //  - if it is, pointer points to the MetaObject
    //  - otherwise this is direct pointer to TypeInfo
    // Meta-object allows storing additional data associated with some objects,
    // such as stable hash code.
    return type_info_;
  }

  void set_type_info(const TypeInfo* type_info) {
    type_info_ = type_info;
  }

  ContainerHeader* container() const {
    return reinterpret_cast<ContainerHeader*>(
        reinterpret_cast<uintptr_t>(this) - container_offset_negative_);
  }

  ObjHeader* obj() { return reinterpret_cast<ObjHeader*>(this); }
  const ObjHeader* obj() const { return reinterpret_cast<const ObjHeader*>(this); }

  // Elements count. Element size is stored in instanceSize_ field of TypeInfo, negated.
  uint32_t count_;
};

inline uint32_t ArrayDataSizeBytes(const ArrayHeader* obj) {
  // Instance size is negative.
  return -obj->type_info()->instanceSize_ * obj->count_;
}

// TODO: those two operations can be implemented by translator when storing
// reference to an object.
inline void AddRef(ContainerHeader* header) {
  // Looking at container type we may want to skip AddRef() totally
  // (non-escaping stack objects, constant objects).
  switch (header->refCount_ & CONTAINER_TAG_MASK) {
    case CONTAINER_TAG_STACK:
    case CONTAINER_TAG_PERMANENT:
      break;
    case CONTAINER_TAG_NORMAL:
      header->refCount_ += CONTAINER_TAG_INCREMENT;
      break;
    default:
      RuntimeAssert(false, "unknown container type");
      break;
  }
}

void FreeContainer(ContainerHeader* header);

// Release() returns 'true' iff container cannot be part of cycle (either NOCOUNT
// object or container was fully released and will be collected).
inline bool Release(ContainerHeader* header) {
  switch (header->refCount_ & CONTAINER_TAG_MASK) {
      case CONTAINER_TAG_PERMANENT:
      case CONTAINER_TAG_STACK:
        // permanent/stack containers aren't loop candidates.
        return true;
    case CONTAINER_TAG_NORMAL:
      if ((header->refCount_ -= CONTAINER_TAG_INCREMENT) == CONTAINER_TAG_NORMAL) {
        FreeContainer(header);
        return true;
      }
      break;
    default:
      RuntimeAssert(false, "unknown container type");
      break;
  }
  // Object with non-zero counter after release are loop candidates.
  return false;
}

// Class representing arbitrary placement container.
class Container {
 protected:
  // Data where everything is being stored.
  ContainerHeader* header_;

  void SetMeta(ObjHeader* obj, const TypeInfo* type_info) {
    obj->container_offset_negative_ =
        reinterpret_cast<uintptr_t>(obj) - reinterpret_cast<uintptr_t>(header_);
    obj->set_type_info(type_info);
    RuntimeAssert(obj->container() == header_, "Placement must match");
  }

 public:
  // Increment reference counter associated with container.
  void AddRef() {
    if (header_) ::AddRef(header_);
  }

  // Decrement reference counter associated with container.
  // For objects whith tricky lifetime (such as ones shared between threads objects)
  // individual container per object (ObjectContainer) shall be created.
  // As an alternative, such objects could be evacuated from short-lived containers.
  void Release() {
    if (header_) ::Release(header_);
  }
};

// Container for a single object.
class ObjectContainer : public Container {
 public:
  // Single instance.
  explicit ObjectContainer(const TypeInfo* type_info) {
    Init(type_info);
  }

  // Object container shalln't have any dtor, as it's being freed by
  // ::Release().

  ObjHeader* GetPlace() const {
    return reinterpret_cast<ObjHeader*>(header_ + 1);
  }

 private:
  void Init(const TypeInfo* type_info);
};


class ArrayContainer : public Container {
 public:
  ArrayContainer(const TypeInfo* type_info, uint32_t elements) {
    Init(type_info, elements);
  }

  // Array container shalln't have any dtor, as it's being freed by ::Release().

  ArrayHeader* GetPlace() const {
    return reinterpret_cast<ArrayHeader*>(header_ + 1);
  }

 private:
  void Init(const TypeInfo* type_info, uint32_t elements);
};

// Class representing arena-style placement container.
// Container is used for reference counting, and it is assumed that objects
// with related placement will share container. Only
// whole container can be freed, individual objects are not taken into account.
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

 private:
  struct ContainerChunk {
    ContainerChunk* next;
    // Then we have ContainerHeader here.
    ContainerHeader* asHeader() {
      return reinterpret_cast<ContainerHeader*>(this + 1);
    }
  };

  void* place(container_size_t size);
  bool allocContainer(container_size_t minSize);
  void setMeta(ObjHeader* obj, const TypeInfo* type_info) {
    obj->container_offset_negative_ =
        reinterpret_cast<uintptr_t>(obj) - reinterpret_cast<uintptr_t>(currentChunk_->asHeader());
    obj->set_type_info(type_info);
    RuntimeAssert(obj->container() == currentChunk_->asHeader(), "Placement must match");
  }
  ContainerChunk* currentChunk_;
  uint8_t* current_;
  uint8_t* end_;
};

#ifdef __cplusplus
extern "C" {
#endif

// Bit or'ed to slot pointer, marking the fact that allocation shall happen
// in arena pointed by the slot.
#define ARENA_BIT 1
#define OBJ_RESULT __result__
#define OBJ_GETTER0(name) ObjHeader* name(ObjHeader** OBJ_RESULT)
#define OBJ_GETTER(name, ...) ObjHeader* name(__VA_ARGS__, ObjHeader** OBJ_RESULT)
#define RETURN_OBJ(value) { ObjHeader* obj = value; \
    UpdateReturnRef(OBJ_RESULT, obj);               \
    return obj; }
#define RETURN_RESULT_OF0(name) {       \
    ObjHeader* obj = name(OBJ_RESULT);  \
    return obj;                         \
  }
#define RETURN_RESULT_OF(name, ...) {                   \
    ObjHeader* result = name(__VA_ARGS__, OBJ_RESULT);  \
    return result;                                      \
  }

struct MemoryState;

MemoryState* InitMemory();
void DeinitMemory(MemoryState*);

//
// Object allocation.
//
// Allocation can happen in either GLOBAL, FRAME or ARENA scope. Depending on that,
// Alloc* or ArenaAlloc* is called. Regular alloc means allocation happens in the heap,
// and each object gets its individual container. Otherwise, allocator uses aux slot in
// an implementation-defined manner, current behavior is to keep arena pointer there.
// Arena containers are not reference counted, and is explicitly freed when leaving
// its owner frame.
// Escape analysis algorithm is the provider of information for decision on exact aux slot
// selection, and comes from upper bound esteemation of object lifetime.
//
OBJ_GETTER(AllocInstance, const TypeInfo* type_info) RUNTIME_NOTHROW;
OBJ_GETTER(AllocArrayInstance, const TypeInfo* type_info, uint32_t elements) RUNTIME_NOTHROW;
OBJ_GETTER(InitInstance, ObjHeader** location, const TypeInfo* type_info,
           void (*ctor)(ObjHeader*));

//
// Object reference management.
//
// Reference management scheme we use assumes significant degree of flexibility, so that
// one could implement either pure reference counting scheme, or tracing collector without
// much ado.
// Most important primitive is UpdateRef() API, which modifies location to use new
// object reference. In pure reference counted scheme it will check old value,
// decrement reference, increment counter on the new value, and store it into the field.
// In tracing collector-like scheme, only field updates counts, and all other operations are
// essentially no-ops.
//
// On codegeneration phase we adopt following approaches:
//  - every stack frame has several slots, holding object references (allRefs)
//  - those are known by compiler (and shall be grouped together)
//  - it keeps all locally allocated objects in such slot
//  - all local variables keeping an object also allocate a slot
//  - most manipulations on objects happens in SSA variables and do no affect slots
//  - exception handlers knowns slot locations for every function, and can update references
//    in intermediate frames when throwing
//

// Sets location.
void SetRef(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
// Updates location.
void UpdateRef(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
// Updates reference in return slot.
void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) RUNTIME_NOTHROW;
// Optimization: release all references in range.
void ReleaseRefs(ObjHeader** start, int count) RUNTIME_NOTHROW;
// Called on frame leave, if it has object slots.
void LeaveFrame(ObjHeader** start, int count) RUNTIME_NOTHROW;
// Collect garbage, which cannot be found by reference counting (cycles).
void GarbageCollect() RUNTIME_NOTHROW;

#ifdef __cplusplus
}
#endif

// Class holding reference to an object, holding object during C++ scope.
class ObjHolder {
 public:
   ObjHolder() : obj_(nullptr) {}

   explicit ObjHolder(const ObjHeader* obj) {
     ::SetRef(&obj_, obj);
   }
   ~ObjHolder() {
     ::UpdateRef(&obj_, nullptr);
   }

   ObjHeader* obj() { return obj_; }
   const ObjHeader* obj() const { return obj_; }
   ObjHeader** slot() { return &obj_; }

  private:
   ObjHeader* obj_;
};

#endif // RUNTIME_MEMORY_H
