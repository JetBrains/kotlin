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

#include <utility>

#include "KAssert.h"
#include "Common.h"
#include "TypeInfo.h"
#include "Atomic.h"
#include "PointerBits.h"
#include "Utils.hpp"

typedef enum {
  // Must match to permTag() in Kotlin.
  OBJECT_TAG_PERMANENT_CONTAINER = 1 << 0,
  OBJECT_TAG_NONTRIVIAL_CONTAINER = 1 << 1,
  // Keep in sync with immTypeInfoMask in Kotlin.
  OBJECT_TAG_MASK = (1 << 2) - 1
} ObjectTag;

struct ArrayHeader;
struct MetaObjHeader;

// Header of every object.
struct ObjHeader {
  TypeInfo* typeInfoOrMeta_;

  // Returns `nullptr` if it's not a meta object.
  static MetaObjHeader* AsMetaObject(TypeInfo* typeInfo) noexcept {
      auto* typeInfoOrMeta = clearPointerBits(typeInfo, OBJECT_TAG_MASK);
      if (typeInfoOrMeta != typeInfoOrMeta->typeInfo_) {
          return reinterpret_cast<MetaObjHeader*>(typeInfoOrMeta);
      } else {
          return nullptr;
      }
  }

  const TypeInfo* type_info() const {
    return clearPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK)->typeInfo_;
  }

  bool has_meta_object() const {
      return AsMetaObject(typeInfoOrMeta_) != nullptr;
  }

  MetaObjHeader* meta_object() {
      if (auto* metaObject = AsMetaObject(typeInfoOrMeta_)) {
          return metaObject;
      }
      return createMetaObject(this);
  }

  ALWAYS_INLINE ObjHeader** GetWeakCounterLocation();

#ifdef KONAN_OBJC_INTEROP
  ALWAYS_INLINE void* GetAssociatedObject();
  ALWAYS_INLINE void** GetAssociatedObjectLocation();
  ALWAYS_INLINE void SetAssociatedObject(void* obj);
#endif

  inline bool local() const {
    unsigned bits = getPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK);
    return (bits & (OBJECT_TAG_PERMANENT_CONTAINER | OBJECT_TAG_NONTRIVIAL_CONTAINER)) ==
        (OBJECT_TAG_PERMANENT_CONTAINER | OBJECT_TAG_NONTRIVIAL_CONTAINER);
  }

  // Unsafe cast to ArrayHeader. Use carefully!
  ArrayHeader* array() { return reinterpret_cast<ArrayHeader*>(this); }
  const ArrayHeader* array() const { return reinterpret_cast<const ArrayHeader*>(this); }

  inline bool permanent() const {
    return hasPointerBits(typeInfoOrMeta_, OBJECT_TAG_PERMANENT_CONTAINER);
  }

  inline bool heap() const { return getPointerBits(typeInfoOrMeta_, OBJECT_TAG_MASK) == 0; }

  static MetaObjHeader* createMetaObject(ObjHeader* object);
  static void destroyMetaObject(ObjHeader* object);
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

ALWAYS_INLINE bool isFrozen(const ObjHeader* obj);
ALWAYS_INLINE bool isPermanentOrFrozen(const ObjHeader* obj);
ALWAYS_INLINE bool isShareable(const ObjHeader* obj);

class ForeignRefManager;
typedef ForeignRefManager* ForeignRefContext;

#ifdef __cplusplus
extern "C" {
#endif

#define OBJ_RESULT __result__
#define OBJ_GETTER0(name) ObjHeader* name(ObjHeader** OBJ_RESULT)
#define OBJ_GETTER(name, ...) ObjHeader* name(__VA_ARGS__, ObjHeader** OBJ_RESULT)
#define RETURN_OBJ(value) { ObjHeader* __obj = value; \
    UpdateReturnRef(OBJ_RESULT, __obj);               \
    return __obj; }
#define RETURN_RESULT_OF0(name) {       \
    ObjHeader* __obj = name(OBJ_RESULT);  \
    return __obj;                         \
  }
#define RETURN_RESULT_OF(name, ...) {                   \
    ObjHeader* __result = name(__VA_ARGS__, OBJ_RESULT);  \
    return __result;                                      \
  }

struct MemoryState;

MemoryState* InitMemory(bool firstRuntime);
void DeinitMemory(MemoryState*, bool destroyRuntime);
void RestoreMemory(MemoryState*);

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

OBJ_GETTER(AllocArrayInstance, const TypeInfo* type_info, int32_t elements);

OBJ_GETTER(InitThreadLocalSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));

OBJ_GETTER(InitSingleton, ObjHeader** location, const TypeInfo* typeInfo, void (*ctor)(ObjHeader*));

// `initialValue` may be `nullptr`, which signifies that the appropriate initial value was already
// set by static initialization.
// TODO: When global initialization becomes lazy, this signature won't do.
void InitAndRegisterGlobal(ObjHeader** location, const ObjHeader* initialValue) RUNTIME_NOTHROW;

//
// Object reference management.
//
// Reference management scheme we use assumes significant degree of flexibility, so that
// one could implement either pure reference counting scheme, or tracing collector without
// much ado.
// Most important primitive is Update*Ref() API, which modifies location to use new
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

// NOTE: Must match `MemoryModel` in `Platform.kt`
enum class MemoryModel {
    kStrict = 0,
    kRelaxed = 1,
    kExperimental = 2,
};

// Controls the current memory model, is compile-time constant.
extern const MemoryModel CurrentMemoryModel;

// Sets stack location.
void SetStackRef(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
// Sets heap location.
void SetHeapRef(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
// Zeroes heap location.
void ZeroHeapRef(ObjHeader** location) RUNTIME_NOTHROW;
// Zeroes an array.
void ZeroArrayRefs(ArrayHeader* array) RUNTIME_NOTHROW;
// Zeroes stack location.
void ZeroStackRef(ObjHeader** location) RUNTIME_NOTHROW;
// Updates stack location.
void UpdateStackRef(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
// Updates heap/static data location.
void UpdateHeapRef(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
// Updates heap/static data in one array.
void UpdateHeapRefsInsideOneArray(const ArrayHeader* array, int fromIndex, int toIndex, int count) RUNTIME_NOTHROW;
// Updates location if it is null, atomically.
void UpdateHeapRefIfNull(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
// Updates reference in return slot.
void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) RUNTIME_NOTHROW;
// Compares and swaps reference with taken lock.
OBJ_GETTER(SwapHeapRefLocked,
    ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue, int32_t* spinlock,
    int32_t* cookie) RUNTIME_NOTHROW;
// Sets reference with taken lock.
void SetHeapRefLocked(ObjHeader** location, ObjHeader* newValue, int32_t* spinlock,
    int32_t* cookie) RUNTIME_NOTHROW;
// Reads reference with taken lock.
OBJ_GETTER(ReadHeapRefLocked, ObjHeader** location, int32_t* spinlock, int32_t* cookie) RUNTIME_NOTHROW;
OBJ_GETTER(ReadHeapRefNoLock, ObjHeader* object, int32_t index);
// Called on frame enter, if it has object slots.
void EnterFrame(ObjHeader** start, int parameters, int count) RUNTIME_NOTHROW;
// Called on frame leave, if it has object slots.
void LeaveFrame(ObjHeader** start, int parameters, int count) RUNTIME_NOTHROW;
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
void CheckLifetimesConstraint(ObjHeader* obj, ObjHeader* pointee) RUNTIME_NOTHROW;
// Freeze object subgraph.
void FreezeSubgraph(ObjHeader* obj);
// Ensure this object shall block freezing.
void EnsureNeverFrozen(ObjHeader* obj);
// Add TLS object storage, called by the generated code.
void AddTLSRecord(MemoryState* memory, void** key, int size) RUNTIME_NOTHROW;
// Allocate storage for TLS. `AddTLSRecord` cannot be called after this.
void CommitTLSStorage(MemoryState* memory) RUNTIME_NOTHROW;
// Clear TLS object storage.
void ClearTLS(MemoryState* memory) RUNTIME_NOTHROW;
// Lookup element in TLS object storage.
ObjHeader** LookupTLS(void** key, int index) RUNTIME_NOTHROW;

// APIs for the async GC.
void GC_RegisterWorker(void* worker) RUNTIME_NOTHROW;
void GC_UnregisterWorker(void* worker) RUNTIME_NOTHROW;
void GC_CollectorCallback(void* worker) RUNTIME_NOTHROW;

void Kotlin_native_internal_GC_collect(ObjHeader*);
void Kotlin_native_internal_GC_collectCyclic(ObjHeader*);
void Kotlin_native_internal_GC_suspend(ObjHeader*);
void Kotlin_native_internal_GC_resume(ObjHeader*);
void Kotlin_native_internal_GC_stop(ObjHeader*);
void Kotlin_native_internal_GC_start(ObjHeader*);
void Kotlin_native_internal_GC_setThreshold(ObjHeader*, int32_t value);
int32_t Kotlin_native_internal_GC_getThreshold(ObjHeader*);
void Kotlin_native_internal_GC_setCollectCyclesThreshold(ObjHeader*, int64_t value);
int64_t Kotlin_native_internal_GC_getCollectCyclesThreshold(ObjHeader*);
void Kotlin_native_internal_GC_setThresholdAllocations(ObjHeader*, int64_t value);
int64_t Kotlin_native_internal_GC_getThresholdAllocations(ObjHeader*);
void Kotlin_native_internal_GC_setTuneThreshold(ObjHeader*, int32_t value);
bool Kotlin_native_internal_GC_getTuneThreshold(ObjHeader*);
OBJ_GETTER(Kotlin_native_internal_GC_detectCycles, ObjHeader*);
OBJ_GETTER(Kotlin_native_internal_GC_findCycle, ObjHeader*, ObjHeader* root);
bool Kotlin_native_internal_GC_getCyclicCollector(ObjHeader* gc);
void Kotlin_native_internal_GC_setCyclicCollector(ObjHeader* gc, bool value);

bool Kotlin_Any_isShareable(ObjHeader* thiz);
void Kotlin_Any_share(ObjHeader* thiz);
void PerformFullGC(MemoryState* memory) RUNTIME_NOTHROW;

bool TryAddHeapRef(const ObjHeader* object);

void ReleaseHeapRef(const ObjHeader* object) RUNTIME_NOTHROW;
void ReleaseHeapRefNoCollect(const ObjHeader* object) RUNTIME_NOTHROW;

ForeignRefContext InitLocalForeignRef(ObjHeader* object);

ForeignRefContext InitForeignRef(ObjHeader* object);
void DeinitForeignRef(ObjHeader* object, ForeignRefContext context);

bool IsForeignRefAccessible(ObjHeader* object, ForeignRefContext context);

// Should be used when reference is read from a possibly shared variable,
// and there's nothing else keeping the object alive.
void AdoptReferenceFromSharedVariable(ObjHeader* object);

void CheckGlobalsAccessible();

// Sets state of the current thread to NATIVE (used by the new MM).
ALWAYS_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateNative();
// Sets state of the current thread to RUNNABLE (used by the new MM).
ALWAYS_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateRunnable();

// Safe point callbacks from Kotlin code generator.
void Kotlin_mm_safePointFunctionEpilogue() RUNTIME_NOTHROW;
void Kotlin_mm_safePointWhileLoopBody() RUNTIME_NOTHROW;
void Kotlin_mm_safePointExceptionUnwind() RUNTIME_NOTHROW;

#ifdef __cplusplus
}
#endif

struct FrameOverlay {
  void* arena;
  FrameOverlay* previous;
  // As they go in pair, sizeof(FrameOverlay) % sizeof(void*) == 0 is always held.
  int32_t parameters;
  int32_t count;
};

// Class holding reference to an object, holding object during C++ scope.
class ObjHolder {
 public:
   ObjHolder() : obj_(nullptr) {
     EnterFrame(frame(), 0, sizeof(*this)/sizeof(void*));
   }

   explicit ObjHolder(const ObjHeader* obj) {
     EnterFrame(frame(), 0, sizeof(*this)/sizeof(void*));
     ::SetStackRef(slot(), obj);
   }

   ~ObjHolder() {
     LeaveFrame(frame(), 0, sizeof(*this)/sizeof(void*));
   }

   ObjHeader* obj() { return obj_; }

   const ObjHeader* obj() const { return obj_; }

   ObjHeader** slot() {
     return &obj_;
   }

   void clear() { ::ZeroStackRef(&obj_); }

 private:
   ObjHeader** frame() { return reinterpret_cast<ObjHeader**>(&frame_); }

   FrameOverlay frame_;
   ObjHeader* obj_;
};

class ExceptionObjHolder {
public:
#if !KONAN_NO_EXCEPTIONS
    static void Throw(ObjHeader* exception) RUNTIME_NORETURN;

    ObjHeader* GetExceptionObject() noexcept;
#endif

    // Exceptions are not on a hot path, so having virtual dispatch is fine.
    virtual ~ExceptionObjHolder() = default;
};

namespace kotlin {
namespace mm {

// Returns the MemoryState for the current thread. The runtime must be initialized.
// Try not to use it very often, as (1) thread local access can be slow on some platforms,
// (2) TLS gets deallocated before our thread destruction hooks run.
MemoryState* GetMemoryState();

} // namespace mm

enum class ThreadState {
    kRunnable, kNative
};

// Switches the state of the given thread to `newState` and returns the previous thread state.
ALWAYS_INLINE ThreadState SwitchThreadState(MemoryState* thread, ThreadState newState) noexcept;

// Asserts that the given thread is in the given state.
ALWAYS_INLINE void AssertThreadState(MemoryState* thread, ThreadState expected) noexcept;

// Asserts that the current thread is in the the given state.
ALWAYS_INLINE inline void AssertThreadState(ThreadState expected) noexcept {
    AssertThreadState(mm::GetMemoryState(), expected);
}

// Scopely sets the given thread state for the given thread.
class ThreadStateGuard final : private Pinned {
public:
    // Set the state for the given thread.
    ThreadStateGuard(MemoryState* thread, ThreadState state) noexcept : thread_(thread) {
        oldState_ = SwitchThreadState(thread_, state);
    }

    // Sets the state for the current thread.
    explicit ThreadStateGuard(ThreadState state) noexcept
        : ThreadStateGuard(mm::GetMemoryState(), state) {};

    ~ThreadStateGuard() noexcept {
        SwitchThreadState(thread_, oldState_);
    }
private:
    MemoryState* thread_;
    ThreadState oldState_;
};

// Calls the given function in the `Runnable` thread state.
template <typename R, typename... Args>
ALWAYS_INLINE inline R CallKotlin(R(*kotlinFunction)(Args...), Args... args) {
    ThreadStateGuard guard(ThreadState::kRunnable);
    return kotlinFunction(std::forward<Args>(args)...);
}

// Calls the given function in the `Runnable` thread state. The function must be marked as RUNTIME_NORETURN.
// If the function returns, behaviour is undefined.
template <typename... Args>
ALWAYS_INLINE RUNTIME_NORETURN inline void CallKotlinNoReturn(void(*noreturnKotlinFunction)(Args...), Args... args) {
    CallKotlin(noreturnKotlinFunction, std::forward<Args>(args)...);
    RuntimeFail("The function must not return");
}

} // namespace kotlin

#endif // RUNTIME_MEMORY_H
