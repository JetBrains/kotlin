/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Exceptions.h"
#include "MemoryPrivate.hpp"
#include "MemorySharedRefs.hpp"
#include "Runtime.h"

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

ObjHeader* KRefSharedHolder::ref() const {
  ensureRefAccessible();
  return obj_;
}

static inline void ensureForeignRefAccessible(ObjHeader* object, ForeignRefContext context) {
  if (!Kotlin_hasRuntime()) {
    // So the object is either unowned or shared.
    // In the former case initialized runtime is required to throw the exception below,
    // in the latter case -- to provide proper execution context for caller.
    Kotlin_initRuntimeIfNeeded();
  }

  if (!IsForeignRefAccessible(object, context)) {
    // TODO: add some info about the context.
    // Note: retrieving 'type_info()' is supposed to be correct even for unowned object.
    ThrowIllegalObjectSharingException(object->type_info(), object);
  }
}

void KRefSharedHolder::dispose() const {
  if (obj_ == nullptr) {
    // To handle the case when it is not initialized. See [KotlinMutableSet/Dictionary dealloc].
    return;
  }

  DeinitForeignRef(obj_, context_);
}

void KRefSharedHolder::ensureRefAccessible() const {
  ensureForeignRefAccessible(obj_, context_);
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  obj_ = obj;

  // Generally a specialized addRef below:
  context_ = InitForeignRef(obj);
  refCount = 1;
}

void BackRefFromAssociatedObject::addRef() {
  if (atomicAdd(&refCount, 1) == 1) {
    // There are no references to the associated object itself, so Kotlin object is being passed from Kotlin,
    // and it is owned therefore.
    ensureRefAccessible(); // TODO: consider removing explicit verification.

    // Foreign reference has already been deinitialized (see [releaseRef]).
    // Create a new one:
    context_ = InitForeignRef(obj_);
  }
}

bool BackRefFromAssociatedObject::tryAddRef() {
  // Suboptimal but simple:
  ObjHeader* obj = this->ref();

  if (!TryAddHeapRef(obj)) return false;
  this->addRef();
  ReleaseHeapRef(obj); // Balance TryAddHeapRef.
  // TODO: consider optimizing for non-shared objects.

  return true;
}

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

void BackRefFromAssociatedObject::ensureRefAccessible() const {
  ensureForeignRefAccessible(obj_, context_);
}

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

ObjHeader* KRefSharedHolder_ref(const KRefSharedHolder* holder) {
  return holder->ref();
}
} // extern "C"