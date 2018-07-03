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

#ifndef RUNTIME_MEMORY_H
#define RUNTIME_MEMORY_H

#include "Assert.h"
#include "Common.h"
#include "TypeInfo.h"

typedef enum {
  // Those bit masks are applied to refCount_ field.
  // Container is normal thread local container.
  CONTAINER_TAG_NORMAL = 0,
  // Container is frozen, could only refer to other frozen objects.
  // Refcounter update is atomics.
  CONTAINER_TAG_FROZEN = 1,
  // Those container tags shall not be refcounted.
  // Permanent container, cannot refer to non-permanent containers, so no need to cleanup those.
  CONTAINER_TAG_PERMANENT = 2,
  // Stack container, no need to free, children cleanup still shall be there.
  CONTAINER_TAG_STACK = 3,
  // Shift to get actual counter.
  CONTAINER_TAG_SHIFT = 2,
  // Actual value to increment/decrement container by. Tag is in lower bits.
  CONTAINER_TAG_INCREMENT = 1 << CONTAINER_TAG_SHIFT,
  // Mask for container type.
  CONTAINER_TAG_MASK = CONTAINER_TAG_INCREMENT - 1,

  // Those bit masks are applied to objectCount_ field.
  // Shift to get actual object count.
  CONTAINER_TAG_GC_SHIFT = 5,
  CONTAINER_TAG_GC_INCREMENT = 1 << CONTAINER_TAG_GC_SHIFT,
  // Color mask of a container.
  CONTAINER_TAG_GC_COLOR_MASK = (1 << 2) - 1,
  // Colors.
  CONTAINER_TAG_GC_BLACK  = 0,
  CONTAINER_TAG_GC_GRAY   = 1,
  CONTAINER_TAG_GC_WHITE  = 2,
  CONTAINER_TAG_GC_PURPLE = 3,
  // Individual state bits used during GC and freezing.
  CONTAINER_TAG_GC_MARKED = 1 << 2,
  CONTAINER_TAG_GC_BUFFERED = 1 << 3,
  CONTAINER_TAG_GC_SEEN = 1 << 4
} ContainerTag;

typedef uint32_t container_size_t;

// Header of all container objects. Contains reference counter.
struct ContainerHeader {
  // Reference counter of container. Uses CONTAINER_TAG_SHIFT, lower bits of counter
  // for container type (for polymorphism in ::Release()).
  uint32_t refCount_;
  // Number of objects in the container.
  uint32_t objectCount_;

  inline bool normal() const {
      return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_NORMAL;
  }

  inline bool permanent() const {
    return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_PERMANENT;
  }

  inline bool frozen() const {
    return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_FROZEN;
  }

  inline void freeze() {
    refCount_ = (refCount_ & ~CONTAINER_TAG_MASK) | CONTAINER_TAG_FROZEN;
  }

  inline bool permanentOrFrozen() const {
    return tag() == CONTAINER_TAG_PERMANENT || tag() == CONTAINER_TAG_FROZEN;
  }

  inline bool stack() const {
    return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_STACK;
  }

  inline unsigned refCount() const {
    return refCount_ >> CONTAINER_TAG_SHIFT;
  }

  inline void setRefCount(unsigned refCount) {
    refCount_ = tag() | (refCount << CONTAINER_TAG_SHIFT);
  }

  template <bool Atomic>
  inline void incRefCount() {
#ifdef KONAN_NO_THREADS
    refCount_ += CONTAINER_TAG_INCREMENT;
#else
    if (Atomic)
      __sync_add_and_fetch(&refCount_, CONTAINER_TAG_INCREMENT);
    else
      refCount_ += CONTAINER_TAG_INCREMENT;
#endif
  }

  template <bool Atomic>
  inline int decRefCount() {
#ifdef KONAN_NO_THREADS
    int value = refCount_ -= CONTAINER_TAG_INCREMENT;
#else
    int value = Atomic ?
       __sync_sub_and_fetch(&refCount_, CONTAINER_TAG_INCREMENT) : refCount_ -= CONTAINER_TAG_INCREMENT;
#endif
    return value >> CONTAINER_TAG_SHIFT;
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

  inline bool seen() const {
    return (objectCount_ & CONTAINER_TAG_GC_SEEN) != 0;
  }

  inline void setSeen() {
    objectCount_ |= CONTAINER_TAG_GC_SEEN;
  }

  inline void resetSeen() {
    objectCount_ &= ~CONTAINER_TAG_GC_SEEN;
  }
};

struct ArrayHeader;
struct MetaObjHeader;

// Header of every object.
struct ObjHeader {
  TypeInfo* typeInfoOrMeta_;
  ContainerHeader* container_;

  const TypeInfo* type_info() const {
    return typeInfoOrMeta_->typeInfo_;
  }

  bool has_meta_object() const {
    return typeInfoOrMeta_ != typeInfoOrMeta_->typeInfo_;
  }

  MetaObjHeader* meta_object() {
     return has_meta_object() ?
        reinterpret_cast<MetaObjHeader*>(typeInfoOrMeta_) : createMetaObject(&typeInfoOrMeta_);
  }

  ContainerHeader* container() const {
    return container_;
  }

  // Unsafe cast to ArrayHeader. Use carefully!
  ArrayHeader* array() { return reinterpret_cast<ArrayHeader*>(this); }
  const ArrayHeader* array() const { return reinterpret_cast<const ArrayHeader*>(this); }

  inline bool permanent() const {
    return container()->permanent();
  }

  static MetaObjHeader* createMetaObject(TypeInfo** location);
  static void destroyMetaObject(TypeInfo** location);
};

// Header of value type array objects. Keep layout in sync with that of object header.
struct ArrayHeader {
  TypeInfo* typeInfoOrMeta_;
  ContainerHeader* container_;

  const TypeInfo* type_info() const {
    return typeInfoOrMeta_->typeInfo_;
  }

  ContainerHeader* container() const {
    return container_;
  }

  ObjHeader* obj() { return reinterpret_cast<ObjHeader*>(this); }
  const ObjHeader* obj() const { return reinterpret_cast<const ObjHeader*>(this); }

  // Elements count. Element size is stored in instanceSize_ field of TypeInfo, negated.
  uint32_t count_;
};

// Header for the meta-object.
struct MetaObjHeader {
  // Pointer to the type info. Must be first, to match ArrayHeader and ObjHeader layout.
  const TypeInfo* typeInfo_;
  // Strong reference to counter object.
  ObjHeader* counter_;

#ifdef KONAN_OBJC_INTEROP
  void* associatedObject_;
#endif
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

  void SetHeader(ObjHeader* obj, const TypeInfo* type_info) {
    obj->container_ = header_;
    obj->typeInfoOrMeta_ = const_cast<TypeInfo*>(type_info);
    // Take into account typeInfo's immutability for ARC strategy.
    if ((type_info->flags_ & TF_IMMUTABLE) != 0)
      header_->refCount_ |= CONTAINER_TAG_FROZEN;
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

  void setHeader(ObjHeader* obj, const TypeInfo* typeInfo) {
    obj->container_ = currentChunk_->asHeader();
    obj->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
    // Here we do not take into account typeInfo's immutability for ARC strategy, as there's no ARC.
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

MemoryState* SuspendMemory();
void ResumeMemory(MemoryState* state);

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

// Weak reference operations.
// Atomically clears counter object reference.
void WeakReferenceCounterClear(ObjHeader* counter);

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
// Updates location if it is null, atomically.
void UpdateRefIfNull(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
// Updates reference in return slot.
void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) RUNTIME_NOTHROW;
// Compares and swaps reference with taken lock.
OBJ_GETTER(SwapRefLocked,
    ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue, int32_t* spinlock) RUNTIME_NOTHROW;
// Reads reference with taken lock.
OBJ_GETTER(ReadRefLocked, ObjHeader** location, int32_t* spinlock) RUNTIME_NOTHROW;
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
void DisposeStablePointer(void* pointer) RUNTIME_NOTHROW;
// Translate stable pointer to object reference.
OBJ_GETTER(DerefStablePointer, void*) RUNTIME_NOTHROW;
// Move stable pointer ownership.
OBJ_GETTER(AdoptStablePointer, void*) RUNTIME_NOTHROW;
// Check mutability state.
void MutationCheck(ObjHeader* obj);
// Freeze object subgraph.
void FreezeSubgraph(ObjHeader* root);
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
