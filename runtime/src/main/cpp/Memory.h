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
  // Those bit masks are applied to refCount_ field.

  // Container is normal thread local container.
  CONTAINER_TAG_NORMAL = 0,
  // Container shall be atomically refcounted, currently disabled.
  // CONTAINER_TAG_SHARED = 1,
  // Those container tags shall not be refcounted.
  // Permanent object, cannot refer to non-permanent objects, so no need to cleanup those.
  CONTAINER_TAG_PERMANENT = 2,
  // Stack objects, no need to free, children cleanup still shall be there.
  CONTAINER_TAG_STACK = 3,
  // Shift to get actual counter.
  CONTAINER_TAG_SHIFT = 2,
  // Actual value to increment/decrement container by. Tag is in lower bits.
  CONTAINER_TAG_INCREMENT = 1 << CONTAINER_TAG_SHIFT,
  // Mask for container type.
  CONTAINER_TAG_MASK = CONTAINER_TAG_INCREMENT - 1,

  // Those bit masks are applied to objectCount_ field.
  // Shift to get actual object count.
  CONTAINER_TAG_GC_SHIFT = 4,
  CONTAINER_TAG_GC_INCREMENT = 1 << CONTAINER_TAG_GC_SHIFT,
  // Color of a container.
  CONTAINER_TAG_GC_COLOR_MASK = ((CONTAINER_TAG_GC_INCREMENT >> 2) - 1),
  // Colors.
  CONTAINER_TAG_GC_BLACK  = 0,
  CONTAINER_TAG_GC_GRAY   = 1,
  CONTAINER_TAG_GC_WHITE  = 2,
  CONTAINER_TAG_GC_PURPLE = 3,
  CONTAINER_TAG_GC_MARKED = 4,
  CONTAINER_TAG_GC_BUFFERED = 8
} ContainerTag;

typedef uint32_t container_offset_t;
typedef uint32_t container_size_t;

// Header of all container objects. Contains reference counter.
struct ContainerHeader {
  // Reference counter of container. Uses CONTAINER_TAG_SHIFT,lower bits of counter
  // for container type (for polymorphism in ::Release()).
  uint32_t refCount_;
  // Number of objects in the container.
  uint32_t objectCount_;

  inline unsigned refCount() const {
    return refCount_ >> CONTAINER_TAG_SHIFT;
  }
  inline void incRefCount() {
    refCount_ += CONTAINER_TAG_INCREMENT;
  }
  inline int decRefCount() {
    refCount_ -= CONTAINER_TAG_INCREMENT;
    return refCount_ >> CONTAINER_TAG_SHIFT;
  }
  inline unsigned tag() const {
    return refCount_ & CONTAINER_TAG_MASK;
  }
  inline unsigned objectCount() const {
    return objectCount_ >> CONTAINER_TAG_GC_SHIFT;
  }
  inline void incObjectCount() {
    objectCount_ += CONTAINER_TAG_GC_INCREMENT;
  }
  inline void setObjectCount(int count) {
    objectCount_ = count << CONTAINER_TAG_GC_SHIFT;
  }
  inline unsigned color() const {
    return objectCount_ & CONTAINER_TAG_GC_COLOR_MASK;
  }
  inline void setColor(unsigned color) {
    objectCount_ = (objectCount_ & ~CONTAINER_TAG_GC_COLOR_MASK) | color;
  }
  inline bool buffered() const {
    return (objectCount_ & CONTAINER_TAG_GC_BUFFERED) != 0;
  }
  inline void setBuffered() {
    objectCount_ |= CONTAINER_TAG_GC_BUFFERED;
  }
  inline void resetBuffered() {
    objectCount_ &= ~CONTAINER_TAG_GC_BUFFERED;
  }
  inline bool marked() const {
    return (objectCount_ & CONTAINER_TAG_GC_MARKED) != 0;
  }
  inline void mark() {
    objectCount_ |= CONTAINER_TAG_GC_MARKED;
  }
  inline void unMark() {
    objectCount_ &= ~CONTAINER_TAG_GC_MARKED;
  }
};

inline bool isPermanent(const ContainerHeader* header) {
  return (header->refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_PERMANENT;
}

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

inline bool isPermanent(const ObjHeader* obj) {
  return isPermanent(obj->container());
}

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
  void setMeta(ObjHeader* obj, const TypeInfo* typeInfo) {
    obj->container_offset_negative_ =
        reinterpret_cast<uintptr_t>(obj) - reinterpret_cast<uintptr_t>(currentChunk_->asHeader());
    obj->set_type_info(typeInfo);
    RuntimeAssert(obj->container() == currentChunk_->asHeader(), "Placement must match");
  }
  ContainerChunk* currentChunk_;
  uint8_t* current_;
  uint8_t* end_;
  ArrayHeader* slots_;
  uint32_t slotsCount_;
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
void DeinitInstanceBody(const TypeInfo* typeInfo, void* body);
OBJ_GETTER(InitInstance, ObjHeader** location, const TypeInfo* type_info,
           void (*ctor)(ObjHeader*));

// Returns true iff the object has space reserved in its tail for special purposes.
bool HasReservedObjectTail(ObjHeader* obj) RUNTIME_NOTHROW;
// Returns the pointer to the reserved space, `HasReservedObjectTail(obj)` must be true.
void* GetReservedObjectTail(ObjHeader* obj) RUNTIME_NOTHROW;

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
// Called on frame enter, if it has object slots.
void EnterFrame(ObjHeader** start, int parameters, int count) RUNTIME_NOTHROW;
// Called on frame leave, if it has object slots.
void LeaveFrame(ObjHeader** start, int parameters, int count) RUNTIME_NOTHROW;
// Tries to use returnSlot's arena for allocation.
ObjHeader** GetReturnSlotIfArena(ObjHeader** returnSlot, ObjHeader** localSlot) RUNTIME_NOTHROW;
// Tries to use param's arena for allocation.
ObjHeader** GetParamSlotIfArena(ObjHeader* param, ObjHeader** localSlot) RUNTIME_NOTHROW;
// Collect garbage, which cannot be found by reference counting (cycles).
void GarbageCollect() RUNTIME_NOTHROW;
// Clears object subgraph references from memory subsystem, and optionally
// checks if subgraph referenced by given root is disjoint from the rest of
// object graph, i.e. no external references exists.
bool ClearSubgraphReferences(ObjHeader* root, bool checked) RUNTIME_NOTHROW;
// Creates stable pointer out of the object.
void* CreateStablePointer(ObjHeader* obj) RUNTIME_NOTHROW;
// Disposes stable pointer to the object.
void DisposeStablePointer(void* pointer) RUNTIME_NOTHROW;;
// Translate stable pointer to object reference.
OBJ_GETTER(DerefStablePointer, void*) RUNTIME_NOTHROW;
// Move stable pointer ownership.
OBJ_GETTER(AdoptStablePointer, void*) RUNTIME_NOTHROW;

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
