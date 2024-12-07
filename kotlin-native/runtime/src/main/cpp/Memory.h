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
#include <std_support/Atomic.hpp>

#include "Alignment.hpp"
#include "KAssert.h"
#include "Common.h"
#include "TypeInfo.h"
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

  TypeInfo* typeInfoOrMetaRelaxed() const { return kotlin::std_support::atomic_ref{typeInfoOrMeta_}.load(std::memory_order_relaxed);}
  TypeInfo* typeInfoOrMetaAcquire() const { return kotlin::std_support::atomic_ref{typeInfoOrMeta_}.load(std::memory_order_acquire);}

  /**
   * Formally, this code data races with installing ExtraObject. Even though, we are okey, with reading
   * both typeInfo and meta-object pointer, llvm memory model doesn't guarantee, that if we are able to
   * see metaObject, written by other thread, we would be able to see metaObject->typeInfo.
   *
   * To make this correct with llvm memory model we need to use [LLVMAtomicOrdering.LLVMAtomicOrderingAcquire] here.
   * Unfortunately, this is dramatically harmful for performance on arm architecture. So, we are using
   * [LLVMAtomicOrdering.LLVMAtomicOrderingMonotonic] for both this read and following load of metaObject->typeInfo.
   * At this point, we have no data race, but llvm memory model allows uninitialized value to be read from metaObject->typeInfo.
   *
   * Hardware guaranties on many supported platforms doesn't allow this to happen.
   */
  const TypeInfo* type_info() const {
      auto atomicTypeInfoPtr = kotlin::std_support::atomic_ref{clearPointerBits(typeInfoOrMetaRelaxed(), OBJECT_TAG_MASK)->typeInfo_};
      const TypeInfo* typeInfo = atomicTypeInfoPtr.load(std::memory_order_relaxed);
      RuntimeAssert(typeInfo != nullptr, "TypeInfo ptr in object %p in null", this);
      return typeInfo;
  }

  bool has_meta_object() const {
      return meta_object_or_null() != nullptr;
  }

  MetaObjHeader* meta_object() {
      if (auto* metaObject = AsMetaObject(typeInfoOrMetaAcquire())) {
          return metaObject;
      }
      return createMetaObject(this);
  }

  MetaObjHeader* meta_object_or_null() const noexcept { return AsMetaObject(typeInfoOrMetaAcquire()); }

#ifdef KONAN_OBJC_INTEROP
  void* GetAssociatedObject() const;
  void SetAssociatedObject(void* obj);
  void* CasAssociatedObject(void* expectedObj, void* obj);
#endif

  inline bool local() const {
    unsigned bits = getPointerBits(typeInfoOrMetaRelaxed(), OBJECT_TAG_MASK);
    return (bits & (OBJECT_TAG_PERMANENT_CONTAINER | OBJECT_TAG_NONTRIVIAL_CONTAINER)) ==
        (OBJECT_TAG_PERMANENT_CONTAINER | OBJECT_TAG_NONTRIVIAL_CONTAINER);
  }

  // Unsafe cast to ArrayHeader. Use carefully!
  // TODO: RuntimeAssert on type_info()->IsArray()?
  ArrayHeader* array() { return reinterpret_cast<ArrayHeader*>(this); }
  const ArrayHeader* array() const { return reinterpret_cast<const ArrayHeader*>(this); }

  inline bool permanent() const {
    return hasPointerBits(typeInfoOrMetaRelaxed(), OBJECT_TAG_PERMANENT_CONTAINER);
  }

  inline bool heap() const { return getPointerBits(typeInfoOrMetaRelaxed(), OBJECT_TAG_MASK) == 0; }

  static MetaObjHeader* createMetaObject(ObjHeader* object);
  static void destroyMetaObject(ObjHeader* object);
};
static_assert(alignof(ObjHeader) <= kotlin::kObjectAlignment);

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
static_assert(alignof(ArrayHeader) <= kotlin::kObjectAlignment);

static inline ObjHeader* const kInitializingSingleton = reinterpret_cast<ObjHeader*>(1);
ALWAYS_INLINE inline bool isNullOrMarker(const ObjHeader* obj) noexcept {
    return reinterpret_cast<uintptr_t>(obj) <= 1;
}

struct FrameOverlay;

namespace kotlin::mm {
struct RawSpecialRef;
} // namespace kotlin::mm

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

MemoryState* InitMemory();
void DeinitMemory(MemoryState*, bool destroyRuntime);
void ClearMemoryForTests(MemoryState*);

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
// selection, and comes from upper bound estimation of object lifetime.
//
OBJ_GETTER(AllocInstance, const TypeInfo* type_info) RUNTIME_NOTHROW;

OBJ_GETTER(AllocArrayInstance, const TypeInfo* type_info, int32_t elements);


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
// Updates volatile heap/static data location.
void UpdateVolatileHeapRef(ObjHeader** location, const ObjHeader* object) RUNTIME_NOTHROW;
OBJ_GETTER(CompareAndSwapVolatileHeapRef, ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue) RUNTIME_NOTHROW;
bool CompareAndSetVolatileHeapRef(ObjHeader** location, ObjHeader* expectedValue, ObjHeader* newValue) RUNTIME_NOTHROW;
OBJ_GETTER(GetAndSetVolatileHeapRef, ObjHeader** location, ObjHeader* newValue) RUNTIME_NOTHROW;

// Updates location if it is null, atomically.
// Updates reference in return slot.
void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) RUNTIME_NOTHROW;
// Called on frame enter, if it has object slots.
void EnterFrame(ObjHeader** start, int parameters, int count) RUNTIME_NOTHROW;
// Called on frame leave, if it has object slots.
void LeaveFrame(ObjHeader** start, int parameters, int count) RUNTIME_NOTHROW;
// Set current frame in case if exception caught.
void SetCurrentFrame(ObjHeader** start) RUNTIME_NOTHROW;
FrameOverlay* getCurrentFrame() RUNTIME_NOTHROW;
void CheckCurrentFrame(ObjHeader** frame) RUNTIME_NOTHROW;

// Creates a stable pointer out of the object.
void* CreateStablePointer(ObjHeader* obj) RUNTIME_NOTHROW;
// Disposes a stable pointer to the object.
void DisposeStablePointer(void* pointer) RUNTIME_NOTHROW;
// Translate stable pointer to object reference.
OBJ_GETTER(DerefStablePointer, void*) RUNTIME_NOTHROW;
// Move stable pointer ownership.
OBJ_GETTER(AdoptStablePointer, void*) RUNTIME_NOTHROW;
// Add TLS object storage, called by the generated code.
void AddTLSRecord(MemoryState* memory, void** key, int size) RUNTIME_NOTHROW;
// Allocate storage for TLS. `AddTLSRecord` cannot be called after this.
void CommitTLSStorage(MemoryState* memory) RUNTIME_NOTHROW;
// Clear TLS object storage.
void ClearTLS(MemoryState* memory) RUNTIME_NOTHROW;
// Lookup element in TLS object storage.
ObjHeader** LookupTLS(void** key, int index) RUNTIME_NOTHROW;

void Kotlin_native_internal_GC_collect(ObjHeader*);
void Kotlin_native_internal_GC_setTuneThreshold(ObjHeader*, bool value);
bool Kotlin_native_internal_GC_getTuneThreshold(ObjHeader*);
RUNTIME_NOTHROW bool Kotlin_native_runtime_Debugging_dumpMemory(ObjHeader*, int fd);

void PerformFullGC(MemoryState* memory) RUNTIME_NOTHROW;

// Sets state of the current thread to NATIVE (used by the new MM).
RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateNative();
// Sets state of the current thread to RUNNABLE (used by the new MM).
RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateRunnable();
// No-inline versions of the functions above are used in debug mode to workaround KT-67567 
// by outlining certain CAS instructions from user code:
NO_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateNative_debug();
NO_INLINE RUNTIME_NOTHROW void Kotlin_mm_switchThreadStateRunnable_debug();

// Safe point callbacks from Kotlin code generator.
void Kotlin_mm_safePointFunctionPrologue() RUNTIME_NOTHROW;
void Kotlin_mm_safePointWhileLoopBody() RUNTIME_NOTHROW;

RUNTIME_NOTHROW void DisposeRegularWeakReferenceImpl(ObjHeader* counter);

#ifdef __cplusplus
}
#endif

struct FrameOverlay {
  FrameOverlay* previous;
  // As they go in pair, sizeof(FrameOverlay) % sizeof(void*) == 0 is always held.
  int32_t parameters;
  int32_t count;
};

// Class holding reference to an object, holding object during C++ scope.
// TODO adopt ref accessors
class ObjHolder {
 public:
   ObjHolder() : obj_(nullptr) {
     EnterFrame(frame(), 0, sizeof(*this)/sizeof(void*));
   }

   explicit ObjHolder(const ObjHeader* obj) {
     EnterFrame(frame(), 0, sizeof(*this)/sizeof(void*));
     ::UpdateStackRef(slot(), obj);
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
    static void Throw(ObjHeader* exception) RUNTIME_NORETURN;

    ObjHeader* GetExceptionObject() noexcept;

    // Exceptions are not on a hot path, so having virtual dispatch is fine.
    virtual ~ExceptionObjHolder() = default;
};

namespace kotlin {
namespace mm {

// Returns the MemoryState for the current thread.
// The current thread must be attached to the runtime.
// Try not to use it very often, as (1) thread local access can be slow on some platforms,
// (2) TLS gets deallocated before our thread destruction hooks run.
MemoryState* GetMemoryState() noexcept;

// TODO: Replace with direct access to ThreadRegistry.
// Checks if the current thread is attached to the runtime.
// This function accesses a TLS variable, so it must not be called from a thread destructor.
bool IsCurrentThreadRegistered() noexcept;

} // namespace mm

enum class ThreadState {
    kRunnable, kNative
};

ThreadState GetThreadState(MemoryState* thread) noexcept;

inline ThreadState GetThreadState() noexcept {
    return GetThreadState(mm::GetMemoryState());
}

// Switches the state of the given thread to `newState` and returns the previous thread state.
ThreadState SwitchThreadState(MemoryState* thread, ThreadState newState, bool reentrant = false) noexcept;

// Asserts that the given thread is in the given state.
void AssertThreadState(MemoryState* thread, ThreadState expected) noexcept;
void AssertThreadState(MemoryState* thread, std::initializer_list<ThreadState> expected) noexcept;

// Asserts that the current thread is in the the given state.
ALWAYS_INLINE inline void AssertThreadState(ThreadState expected) noexcept {
    // Avoid redundant TLS access in GetMemoryState if runtime asserts are disabled.
    if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
        AssertThreadState(mm::GetMemoryState(), expected);
    }
}

ALWAYS_INLINE inline void AssertThreadState(std::initializer_list<ThreadState> expected) noexcept {
    // Avoid redundant TLS access in GetMemoryState if runtime asserts are disabled.
    if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
        AssertThreadState(mm::GetMemoryState(), expected);
    }
}

// Scopely sets the given thread state for the given thread.
class ThreadStateGuard final : private MoveOnly {
public:
    // Do not set any state. Useful to create a variable to move another guard into.
    ThreadStateGuard() : thread_(nullptr), oldState_(ThreadState::kNative), reentrant_(false) {}

    // Set the state for the given thread.
    ThreadStateGuard(MemoryState* thread, ThreadState state, bool reentrant = false) noexcept : thread_(thread), reentrant_(reentrant) {
        oldState_ = SwitchThreadState(thread_, state, reentrant_);
    }

    // Sets the state for the current thread.
    explicit ThreadStateGuard(ThreadState state, bool reentrant = false) noexcept
        : ThreadStateGuard(mm::GetMemoryState(), state, reentrant) {};

    ThreadStateGuard(ThreadStateGuard&& other) noexcept
        : thread_(other.thread_), oldState_(other.oldState_), reentrant_(other.reentrant_) {
        other.thread_ = nullptr;
    }

    ~ThreadStateGuard() noexcept {
        if (thread_ != nullptr) {
            SwitchThreadState(thread_, oldState_, reentrant_);
        }
    }

    ThreadStateGuard& operator=(ThreadStateGuard&& other) noexcept {
        thread_ = other.thread_;
        oldState_ = other.oldState_;
        reentrant_ = other.reentrant_;
        other.thread_ = nullptr;
        return *this;
    }

private:
    MemoryState* thread_;
    ThreadState oldState_;
    bool reentrant_;
};

// Scopely sets the kRunnable thread state for the current thread,
// and initializes runtime if needed for new MM.
// No-op for old GC.
class CalledFromNativeGuard final : private Pinned {
public:
    CalledFromNativeGuard(bool reentrant = false) noexcept;

    ~CalledFromNativeGuard() noexcept {
        SwitchThreadState(thread_, oldState_, reentrant_);
    }
private:
    MemoryState* thread_;
    ThreadState oldState_;
    bool reentrant_;
};

class CurrentFrameGuard : Pinned {
public:
    CurrentFrameGuard() : frame_(getCurrentFrame()) {}
    ~CurrentFrameGuard() { SetCurrentFrame(reinterpret_cast<ObjHeader**>(frame_)); }
private:
    FrameOverlay* frame_;
};

template <ThreadState state, typename R, typename... Args>
ALWAYS_INLINE inline R CallWithThreadState(R(*function)(Args...), Args... args) {
    ThreadStateGuard guard(state);
    return function(std::forward<Args>(args)...);
}

class NativeOrUnregisteredThreadGuard final : private MoveOnly {
public:
    explicit NativeOrUnregisteredThreadGuard(bool reentrant = false) noexcept {
        // The default ctor of ThreadStateGuard doesn't set the state.
        // So the actual state switching is performed only if the thread is registered.
        if (kotlin::mm::IsCurrentThreadRegistered()) {
            backingGuard_ = kotlin::ThreadStateGuard(kotlin::ThreadState::kNative, reentrant);
        }
    }

private:
    ThreadStateGuard backingGuard_;
};

void initGlobalMemory() noexcept;

void StartFinalizerThreadIfNeeded() noexcept;
bool FinalizersThreadIsRunning() noexcept;

void OnMemoryAllocation(size_t totalAllocatedBytes) noexcept;

void initObjectPool() noexcept;
void compactObjectPoolInCurrentThread() noexcept;

} // namespace kotlin

RUNTIME_NOTHROW extern "C" void Kotlin_processObjectInMark(void* state, ObjHeader* object);
RUNTIME_NOTHROW extern "C" void Kotlin_processArrayInMark(void* state, ObjHeader* object);
RUNTIME_NOTHROW extern "C" void Kotlin_processEmptyObjectInMark(void* state, ObjHeader* object);

#endif // RUNTIME_MEMORY_H
