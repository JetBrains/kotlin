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

#ifndef RUNTIME_MEMORYPRIVATE_HPP
#define RUNTIME_MEMORYPRIVATE_HPP

#include "Memory.h"

typedef enum {
  // Those bit masks are applied to refCount_ field.
  // Container is normal thread-local container.
  CONTAINER_TAG_LOCAL = 0,
  // Container is frozen, could only refer to other frozen objects.
  // Refcounter update is atomics.
  CONTAINER_TAG_FROZEN = 1 | 1,  // shareable
  // Stack container, no need to free, children cleanup still shall be there.
  CONTAINER_TAG_STACK = 2,
  // Atomic container, reference counter is atomically updated.
  CONTAINER_TAG_SHARED = 3 | 1,  // shareable
  // Shift to get actual counter.
  CONTAINER_TAG_SHIFT = 2,
  // Actual value to increment/decrement container by. Tag is in lower bits.
  CONTAINER_TAG_INCREMENT = 1 << CONTAINER_TAG_SHIFT,
  // Mask for container type.
  CONTAINER_TAG_MASK = CONTAINER_TAG_INCREMENT - 1,

  // Shift to get actual object count, if has it.
  CONTAINER_TAG_GC_SHIFT     = 7,
  CONTAINER_TAG_GC_MASK      = (1 << CONTAINER_TAG_GC_SHIFT) - 1,
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
  CONTAINER_TAG_GC_SEEN     = 1 << (CONTAINER_TAG_COLOR_SHIFT + 2),
  // If indeed has more that one object.
  CONTAINER_TAG_GC_HAS_OBJECT_COUNT = 1 << (CONTAINER_TAG_COLOR_SHIFT + 3)
} ContainerTag;

// Header of all container objects. Contains reference counter.
struct ContainerHeader {
  // Reference counter of container. Uses CONTAINER_TAG_SHIFT, lower bits of counter
  // for container type (for polymorphism in ::Release()).
  uint32_t refCount_;
  // Number of objects in the container.
  uint32_t objectCount_;

  inline bool local() const {
      return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_LOCAL;
  }

  inline bool frozen() const {
    return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_FROZEN;
  }

  inline void freeze() {
    refCount_ = (refCount_ & ~CONTAINER_TAG_MASK) | CONTAINER_TAG_FROZEN;
  }

  inline void makeShared() {
      refCount_ = (refCount_ & ~CONTAINER_TAG_MASK) | CONTAINER_TAG_SHARED;
  }

  inline bool shared() const {
    return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_SHARED;
  }

  inline bool shareable() const {
      return (tag() & 1) != 0; // CONTAINER_TAG_FROZEN || CONTAINER_TAG_SHARED
  }

  inline bool stack() const {
    return (refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_STACK;
  }

  inline int refCount() const {
    return (int)refCount_ >> CONTAINER_TAG_SHIFT;
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
  inline bool tryIncRefCount() {
    if (Atomic) {
      while (true) {
        uint32_t currentRefCount_ = refCount_;
        if (((int)currentRefCount_ >> CONTAINER_TAG_SHIFT) > 0) {
          if (compareAndSet(&refCount_, currentRefCount_, currentRefCount_ + CONTAINER_TAG_INCREMENT)) {
            return true;
          }
        } else {
          return false;
        }
      }
    } else {
      // Note: tricky case here is doing this during cycle collection.
      // This can actually happen due to deallocation hooks.
      // Fortunately by this point reference counts have been made precise again.
      if (refCount() > 0) {
        incRefCount</* Atomic = */ false>();
        return true;
      } else {
        return false;
      }
    }
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

  inline int decRefCount() {
  #ifdef KONAN_NO_THREADS
      int value = refCount_ -= CONTAINER_TAG_INCREMENT;
  #else
      int value = shareable() ?
         __sync_sub_and_fetch(&refCount_, CONTAINER_TAG_INCREMENT) : refCount_ -= CONTAINER_TAG_INCREMENT;
  #endif
      return value >> CONTAINER_TAG_SHIFT;
  }

  inline unsigned tag() const {
    return refCount_ & CONTAINER_TAG_MASK;
  }

  inline unsigned objectCount() const {
    return (objectCount_ & CONTAINER_TAG_GC_HAS_OBJECT_COUNT) != 0 ?
        (objectCount_ >> CONTAINER_TAG_GC_SHIFT) : 1;
  }

  inline void incObjectCount() {
    RuntimeAssert((objectCount_ & CONTAINER_TAG_GC_HAS_OBJECT_COUNT) != 0, "Must have object count");
    objectCount_ += CONTAINER_TAG_GC_INCREMENT;
  }

  inline void setObjectCount(int count) {
    if (count == 1) {
      objectCount_ &= ~CONTAINER_TAG_GC_HAS_OBJECT_COUNT;
    } else {
      objectCount_ = (count << CONTAINER_TAG_GC_SHIFT) | CONTAINER_TAG_GC_HAS_OBJECT_COUNT;
    }
  }

  inline unsigned containerSize() const {
    RuntimeAssert((objectCount_ & CONTAINER_TAG_GC_HAS_OBJECT_COUNT) == 0, "Must be single-object");
    return (objectCount_ >> CONTAINER_TAG_GC_SHIFT);
  }

  inline void setContainerSize(unsigned size) {
    RuntimeAssert((objectCount_ & CONTAINER_TAG_GC_HAS_OBJECT_COUNT) == 0, "Must not have object count");
    objectCount_ = (objectCount_ & CONTAINER_TAG_GC_MASK) | (size << CONTAINER_TAG_GC_SHIFT);
  }

  inline bool hasContainerSize() {
    return (objectCount_ & CONTAINER_TAG_GC_HAS_OBJECT_COUNT) == 0;
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
    unsigned objectCount = objectCount_;
    if ((objectCount & CONTAINER_TAG_GC_COLOR_MASK) != CONTAINER_TAG_GC_GREEN)
        objectCount_ = (objectCount & ~CONTAINER_TAG_GC_COLOR_MASK) | color;
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

  // Following operations only work on freed container which is in finalization queue.
  // We cannot use 'this' here, as it conflicts with aliasing analysis in clang.
  inline void setNextLink(ContainerHeader* next) {
    *reinterpret_cast<ContainerHeader**>(this + 1) = next;
  }

  inline ContainerHeader* nextLink() {
    return *reinterpret_cast<ContainerHeader**>(this + 1);
  }
};

ALWAYS_INLINE ContainerHeader* containerFor(const ObjHeader* obj);

// Header for the meta-object.
struct MetaObjHeader {
  // Pointer to the type info. Must be first, to match ArrayHeader and ObjHeader layout.
  const TypeInfo* typeInfo_;
  // Container pointer.
  ContainerHeader* container_;

#ifdef KONAN_OBJC_INTEROP
  void* associatedObject_;
#endif

  // Flags for the object state.
  int32_t flags_;

  struct {
    // Strong reference to the counter object.
    ObjHeader* counter_;
  } WeakReference;
};

extern "C" {

#define MODEL_VARIANTS(returnType, name, ...)            \
   returnType name##Strict(__VA_ARGS__) RUNTIME_NOTHROW; \
   returnType name##Relaxed(__VA_ARGS__) RUNTIME_NOTHROW;

OBJ_GETTER(AllocInstanceStrict, const TypeInfo* type_info) RUNTIME_NOTHROW;
OBJ_GETTER(AllocInstanceRelaxed, const TypeInfo* type_info) RUNTIME_NOTHROW;

OBJ_GETTER(AllocArrayInstanceStrict, const TypeInfo* type_info, int32_t elements);
OBJ_GETTER(AllocArrayInstanceRelaxed, const TypeInfo* type_info, int32_t elements);

OBJ_GETTER(InitThreadLocalSingletonStrict, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));
OBJ_GETTER(InitThreadLocalSingletonRelaxed, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));

OBJ_GETTER(InitSingletonStrict, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));
OBJ_GETTER(InitSingletonRelaxed, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));

MODEL_VARIANTS(void, SetStackRef, ObjHeader** location, const ObjHeader* object);
MODEL_VARIANTS(void, SetHeapRef, ObjHeader** location, const ObjHeader* object);
MODEL_VARIANTS(void, ZeroStackRef, ObjHeader** location);
MODEL_VARIANTS(void, UpdateStackRef, ObjHeader** location, const ObjHeader* object);
MODEL_VARIANTS(void, UpdateHeapRef, ObjHeader** location, const ObjHeader* object);
MODEL_VARIANTS(void, UpdateHeapRefIfNull, ObjHeader** location, const ObjHeader* object);
MODEL_VARIANTS(void, UpdateReturnRef, ObjHeader** returnSlot, const ObjHeader* object);
MODEL_VARIANTS(void, UpdateHeapRefsInsideOneArray, const ArrayHeader* array, int fromIndex, int toIndex,
               int count);
MODEL_VARIANTS(void, EnterFrame, ObjHeader** start, int parameters, int count);
MODEL_VARIANTS(void, LeaveFrame, ObjHeader** start, int parameters, int count);

MODEL_VARIANTS(void, ReleaseHeapRef, const ObjHeader* object);
MODEL_VARIANTS(void, ReleaseHeapRefNoCollect, const ObjHeader* object);

}  // extern "C"

#endif // RUNTIME_MEMORYPRIVATE_HPP
