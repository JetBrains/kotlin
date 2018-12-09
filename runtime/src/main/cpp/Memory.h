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

#include "KAssert.h"
#include "Common.h"
#include "TypeInfo.h"

typedef enum {
  // Those bit masks are applied to refCount_ field.
  // Container is normal thread local container.
  CONTAINER_TAG_NORMAL = 0,
  // Container is frozen, could only refer to other frozen objects.
  // Refcounter update is atomics.
  CONTAINER_TAG_FROZEN = 1 | 1,  // shareable
  // Stack container, no need to free, children cleanup still shall be there.
  CONTAINER_TAG_STACK = 2,
  // Atomic container, reference counter is atomically updated.
  CONTAINER_TAG_ATOMIC = 3 | 1,  // shareable
  // Shift to get actual counter.
  CONTAINER_TAG_SHIFT = 2,
  // Actual value to increment/decrement container by. Tag is in lower bits.
  CONTAINER_TAG_INCREMENT = 1 << CONTAINER_TAG_SHIFT,
  // Mask for container type.
  CONTAINER_TAG_MASK = CONTAINER_TAG_INCREMENT - 1,

  // Shift to get actual object count.
  CONTAINER_TAG_GC_SHIFT     = 6,
  CONTAINER_TAG_GC_INCREMENT = 1 << CONTAINER_TAG_GC_SHIFT,
  // Color mask of a container.
  CONTAINER_TAG_COLOR_SHIFT   = 3,
  CONTAINER_TAG_GC_COLOR_MASK = (1 << CONTAINER_TAG_COLOR_SHIFT) - 1,
  // Colors.
  // In use or free.
  CONTAINER_TAG_GC_BLACK  = 0,
  // Possible member of garbage cycle.
  CONTAINER_TAG_GC_GRAY   = 1,
  // Member of garbage cycle.
  CONTAINER_TAG_GC_WHITE  = 2,
  // Possible root of cycle.
  CONTAINER_TAG_GC_PURPLE = 3,
  // Acyclic.
  CONTAINER_TAG_GC_GREEN  = 4,
  // Orange and red are currently unused.
  // Candidate cycle awaiting epoch.
  CONTAINER_TAG_GC_ORANGE = 5,
  // Candidate cycle awaiting sigma computation.
  CONTAINER_TAG_GC_RED    = 6,
  // Individual state bits used during GC and freezing.
  CONTAINER_TAG_GC_MARKED   = 1 << CONTAINER_TAG_COLOR_SHIFT,
  CONTAINER_TAG_GC_BUFFERED = 1 << (CONTAINER_TAG_COLOR_SHIFT + 1),
  CONTAINER_TAG_GC_SEEN     = 1 << (CONTAINER_TAG_COLOR_SHIFT + 2)
} ContainerTag;

typedef enum {
  // Must match to permTag() in Kotlin.
  OBJECT_TAG_PERMANENT_CONTAINER = 1 << 0,
  OBJECT_TAG_NONTRIVIAL_CONTAINER = 1 << 1,
  // Keep in sync with immTypeInfoMask in Kotlin.
  OBJECT_TAG_MASK = (1 << 2) - 1
} ObjectTag;

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

  inline bool frozen() const {
    return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_FROZEN;
  }

  inline void freeze() {
    refCount_ = (refCount_ & ~CONTAINER_TAG_MASK) | CONTAINER_TAG_FROZEN;
  }

  inline void makeShareable() {
      refCount_ = (refCount_ & ~CONTAINER_TAG_MASK) | CONTAINER_TAG_ATOMIC;
  }

  inline bool shareable() const {
      return (tag() & 1) != 0; // CONTAINER_TAG_FROZEN || CONTAINER_TAG_ATOMIC
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

  inline void setColorAssertIfGreen(unsigned color) {
    RuntimeAssert(this->color() != CONTAINER_TAG_GC_GREEN, "Must not be green");
    setColorEvenIfGreen(color);
  }

  inline void setColorEvenIfGreen(unsigned color) {
    // TODO: do we need atomic color update?
    objectCount_ = (objectCount_ & ~CONTAINER_TAG_GC_COLOR_MASK) | color;
  }

  inline void setColorUnlessGreen(unsigned color) {
    // TODO: do we need atomic color update?
    unsigned objectCount_ = objectCount_;
    if ((objectCount_ & CONTAINER_TAG_GC_COLOR_MASK) != CONTAINER_TAG_GC_GREEN)
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

  // We cannot use 'this' here, as it conflicts with aliasing analysis in clang.
  inline void setNextLink(ContainerHeader* next) {
    *reinterpret_cast<ContainerHeader**>(this + 1) = next;
  }

  inline ContainerHeader* nextLink() {
    return *reinterpret_cast<ContainerHeader**>(this + 1);
  }
};

inline bool PermanentOrFrozen(ContainerHeader* container) {
    return container == nullptr || container->frozen();
}

inline bool Shareable(ContainerHeader* container) {
    return container == nullptr || container->shareable();
}

struct ArrayHeader;
struct MetaObjHeader;

template <typename T>
ALWAYS_INLINE T* setPointerBits(T* ptr, unsigned bits) {
  return reinterpret_cast<T*>(reinterpret_cast<uintptr_t>(ptr) | bits);
}

template <typename T>
ALWAYS_INLINE T* clearPointerBits(T* ptr, unsigned bits) {
  return reinterpret_cast<T*>(reinterpret_cast<uintptr_t>(ptr) & ~static_cast<uintptr_t>(bits));
}

template <typename T>
ALWAYS_INLINE unsigned getPointerBits(T* ptr, unsigned bits) {
  return reinterpret_cast<uintptr_t>(ptr) & static_cast<uintptr_t>(bits);
}

template <typename T>
ALWAYS_INLINE bool hasPointerBits(T* ptr, unsigned bits) {
  return getPointerBits(ptr, bits) != 0;
}

// Header for the meta-object.
struct MetaObjHeader {
  // Pointer to the type info. Must be first, to match ArrayHeader and ObjHeader layout.
  const TypeInfo* typeInfo_;
  // Strong reference to the counter object.
  ObjHeader* counter_;
  // Container pointer.
  ContainerHeader* container_;
#ifdef KONAN_OBJC_INTEROP
  void* associatedObject_;
#endif

  // Flags for the object state.
  int32_t flags_;
};

// Header of every object.
struct ObjHeader {
  TypeInfo* typeInfoOrMeta_;

  const TypeInfo* type_info() const {
    return clearPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK)->typeInfo_;
  }

  bool has_meta_object() const {
    auto* typeInfoOrMeta = clearPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK);
    return (typeInfoOrMeta != typeInfoOrMeta->typeInfo_);
  }

  MetaObjHeader* meta_object() {
     return has_meta_object() ?
        reinterpret_cast<MetaObjHeader*>(clearPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK)) :
        createMetaObject(&typeInfoOrMeta_);
  }

  void setContainer(ContainerHeader* container) {
    meta_object()->container_ = container;
    typeInfoOrMeta_ = setPointerBits(typeInfoOrMeta_, OBJECT_TAG_NONTRIVIAL_CONTAINER);
  }

  ContainerHeader* container() const {
    unsigned bits = getPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK);
    if ((bits & OBJECT_TAG_PERMANENT_CONTAINER) != 0) return nullptr;
    return (bits & OBJECT_TAG_NONTRIVIAL_CONTAINER) != 0 ?
         (reinterpret_cast<MetaObjHeader*>(clearPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK)))->container_ :
         reinterpret_cast<ContainerHeader*>(const_cast<ObjHeader*>(this)) - 1;
  }

  // Unsafe cast to ArrayHeader. Use carefully!
  ArrayHeader* array() { return reinterpret_cast<ArrayHeader*>(this); }
  const ArrayHeader* array() const { return reinterpret_cast<const ArrayHeader*>(this); }

  inline bool permanent() const {
    return hasPointerBits(typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
  }

  static MetaObjHeader* createMetaObject(TypeInfo** location);
  static void destroyMetaObject(TypeInfo** location);
};

// Header of value type array objects. Keep layout in sync with that of object header.
struct ArrayHeader {
  TypeInfo* typeInfoOrMeta_;

  const TypeInfo* type_info() const {
    return clearPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK)->typeInfo_;
  }

  ObjHeader* obj() { return reinterpret_cast<ObjHeader*>(this); }
  const ObjHeader* obj() const { return reinterpret_cast<const ObjHeader*>(this); }

  // Elements count. Element size is stored in instanceSize_ field of TypeInfo, negated.
  uint32_t count_;
};

inline bool PermanentOrFrozen(ObjHeader* obj) {
    auto* container = obj->container();
    return container == nullptr || container->frozen();
}

// Class representing arbitrary placement container.
class Container {
 protected:
  // Data where everything is being stored.
  ContainerHeader* header_;

  void SetHeader(ObjHeader* obj, const TypeInfo* type_info) {
    obj->typeInfoOrMeta_ = const_cast<TypeInfo*>(type_info);
    // Take into account typeInfo's immutability for ARC strategy.
    if ((type_info->flags_ & TF_IMMUTABLE) != 0)
      header_->refCount_ |= CONTAINER_TAG_FROZEN;
    if ((type_info->flags_ & TF_ACYCLIC) != 0)
      header_->setColorEvenIfGreen(CONTAINER_TAG_GC_GREEN);
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
// Sets reference with taken lock.
void SetRefLocked(ObjHeader** location, ObjHeader* newValue, int32_t* spinlock) RUNTIME_NOTHROW;
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
void FreezeSubgraph(ObjHeader* obj);
// Ensure this object shall block freezing.
void EnsureNeverFrozen(ObjHeader* obj);
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
   void clear() { ::UpdateRef(&obj_, nullptr); }

  private:
   ObjHeader* obj_;
};

class KRefSharedHolder {
 public:
  inline ObjHeader** slotToInit() {
    initRefOwner();
    return &obj_;
  }

  inline void init(ObjHeader* obj) {
    SetRef(slotToInit(), obj);
  }

  inline ObjHeader* ref() const {
    verifyRefOwner();
    return obj_;
  }

  inline void dispose() {
    verifyRefOwner();
    UpdateRef(&obj_, nullptr);
  }

 private:
  typedef MemoryState* RefOwner;

  ObjHeader* obj_;
  RefOwner owner_;

  void initRefOwner();
  void verifyRefOwner() const;
};

#endif // RUNTIME_MEMORY_H
