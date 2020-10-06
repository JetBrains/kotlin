/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Exceptions.h"
#include "MemoryPrivate.hpp"
#include "MemorySharedRefs.hpp"
#include "Runtime.h"
#include "Types.h"

extern "C" {
// Returns a string describing object at `address` of type `typeInfo`.
OBJ_GETTER(DescribeObjectForDebugging, KConstNativePtr typeInfo, KConstNativePtr address);
}  // extern "C"

namespace {

inline bool isForeignRefAccessible(ObjHeader* object, ForeignRefContext context) {
  if (!Kotlin_hasRuntime()) {
    // So the object is either unowned or shared.
    // In the former case initialized runtime is required to throw exceptions
    // in the latter case -- to provide proper execution context for caller.
    Kotlin_initRuntimeIfNeeded();
  }

  return IsForeignRefAccessible(object, context);
}

RUNTIME_NORETURN inline void throwIllegalSharingException(ObjHeader* object) {
  // TODO: add some info about the context.
  // Note: retrieving 'type_info()' is supposed to be correct even for unowned object.
  ThrowIllegalObjectSharingException(object->type_info(), object);
}

RUNTIME_NORETURN inline void terminateWithIllegalSharingException(ObjHeader* object) {
#if KONAN_NO_EXCEPTIONS
  // This will terminate.
  throwIllegalSharingException(object);
#else
  try {
    throwIllegalSharingException(object);
  } catch (...) {
    // A trick to terminate with unhandled exception. This will print a stack trace
    // and write to iOS crash log.
    std::terminate();
  }
#endif
}

template <ErrorPolicy errorPolicy>
bool ensureRefAccessible(ObjHeader* object, ForeignRefContext context) {
  static_assert(errorPolicy != ErrorPolicy::kIgnore, "Must've been handled by specialization");

  if (isForeignRefAccessible(object, context)) {
    return true;
  }

  switch (errorPolicy) {
    case ErrorPolicy::kDefaultValue:
      return false;
    case ErrorPolicy::kThrow:
      throwIllegalSharingException(object);
    case ErrorPolicy::kTerminate:
      terminateWithIllegalSharingException(object);
  }
}

template <>
bool ensureRefAccessible<ErrorPolicy::kIgnore>(ObjHeader* object, ForeignRefContext context) {
  return true;
}

}  // namespace

void KRefSharedHolder::initLocal(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  context_ = InitLocalForeignRef(obj);
  obj_ = obj;
}

void KRefSharedHolder::init(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  context_ = InitForeignRef(obj);
  obj_ = obj;
}

template <ErrorPolicy errorPolicy>
ObjHeader* KRefSharedHolder::ref() const {
  if (!ensureRefAccessible<errorPolicy>(obj_, context_)) {
    return nullptr;
  }

  AdoptReferenceFromSharedVariable(obj_);
  return obj_;
}

template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kDefaultValue>() const;
template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kThrow>() const;
template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kTerminate>() const;

void KRefSharedHolder::dispose() const {
  if (obj_ == nullptr) {
    // To handle the case when it is not initialized. See [KotlinMutableSet/Dictionary dealloc].
    return;
  }

  DeinitForeignRef(obj_, context_);
}

OBJ_GETTER0(KRefSharedHolder::describe) const {
  // Note: retrieving 'type_info()' is supposed to be correct even for unowned object.
  RETURN_RESULT_OF(DescribeObjectForDebugging, obj_->type_info(), obj_);
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  obj_ = obj;

  // Generally a specialized addRef below:
  context_ = InitForeignRef(obj);
  refCount = 1;
}

template <ErrorPolicy errorPolicy>
void BackRefFromAssociatedObject::addRef() {
  static_assert(errorPolicy != ErrorPolicy::kDefaultValue, "Cannot use default return value here");

  if (atomicAdd(&refCount, 1) == 1) {
    // There are no references to the associated object itself, so Kotlin object is being passed from Kotlin,
    // and it is owned therefore.
    ensureRefAccessible<errorPolicy>(obj_, context_); // TODO: consider removing explicit verification.

    // Foreign reference has already been deinitialized (see [releaseRef]).
    // Create a new one:
    context_ = InitForeignRef(obj_);
  }
}

template void BackRefFromAssociatedObject::addRef<ErrorPolicy::kThrow>();
template void BackRefFromAssociatedObject::addRef<ErrorPolicy::kTerminate>();

template <ErrorPolicy errorPolicy>
bool BackRefFromAssociatedObject::tryAddRef() {
  static_assert(errorPolicy != ErrorPolicy::kDefaultValue, "Cannot use default return value here");

  // Suboptimal but simple:
  ensureRefAccessible<errorPolicy>(obj_, context_);

  ObjHeader* obj = obj_;

  if (!TryAddHeapRef(obj)) return false;
  RuntimeAssert(isForeignRefAccessible(obj_, context_), "Cannot be inaccessible because of the check above");
  // TODO: This is a very weird way to ask for "unsafe" addRef.
  addRef<ErrorPolicy::kIgnore>();
  ReleaseHeapRefNoCollect(obj); // Balance TryAddHeapRef.
  // TODO: consider optimizing for non-shared objects.

  return true;
}

template bool BackRefFromAssociatedObject::tryAddRef<ErrorPolicy::kThrow>();
template bool BackRefFromAssociatedObject::tryAddRef<ErrorPolicy::kTerminate>();

void BackRefFromAssociatedObject::releaseRef() {
  ForeignRefContext context = context_;
  if (atomicAdd(&refCount, -1) == 0) {
    // Note: by this moment "subsequent" addRef may have already happened and patched context_.
    // So use the value loaded before refCount update:
    DeinitForeignRef(obj_, context);
    // From this moment [context] is generally a dangling pointer.
    // This is handled in [IsForeignRefAccessible] and [addRef].
  }
}

template <ErrorPolicy errorPolicy>
ObjHeader* BackRefFromAssociatedObject::ref() const {
  if (!ensureRefAccessible<errorPolicy>(obj_, context_)) {
    return nullptr;
  }

  AdoptReferenceFromSharedVariable(obj_);
  return obj_;
}

template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kDefaultValue>() const;
template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kThrow>() const;
template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kTerminate>() const;

extern "C" {
RUNTIME_NOTHROW void KRefSharedHolder_initLocal(KRefSharedHolder* holder, ObjHeader* obj) {
  holder->initLocal(obj);
}

RUNTIME_NOTHROW void KRefSharedHolder_init(KRefSharedHolder* holder, ObjHeader* obj) {
  holder->init(obj);
}

RUNTIME_NOTHROW void KRefSharedHolder_dispose(const KRefSharedHolder* holder) {
  holder->dispose();
}

RUNTIME_NOTHROW ObjHeader* KRefSharedHolder_ref(const KRefSharedHolder* holder) {
  return holder->ref<ErrorPolicy::kTerminate>();
}
} // extern "C"
