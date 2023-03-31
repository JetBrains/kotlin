/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

#include "MemoryPrivate.hpp"

using namespace kotlin;

void KRefSharedHolder::initLocal(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    context_ = nullptr;
    obj_ = obj;
}

void KRefSharedHolder::init(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    context_ = InitForeignRef(obj);
    obj_ = obj;
}

template <ErrorPolicy errorPolicy>
ObjHeader* KRefSharedHolder::ref() const {
    AssertThreadState(ThreadState::kRunnable);
    // ref_ may be null if created with initLocal.
    return obj_;
}

template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kDefaultValue>() const;
template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kThrow>() const;
template ObjHeader* KRefSharedHolder::ref<ErrorPolicy::kTerminate>() const;

void KRefSharedHolder::dispose() {
    if (obj_ == nullptr) {
        // To handle the case when it is not initialized. See [KotlinMutableSet/Dictionary dealloc].
        return;
    }

    DeinitForeignRef(obj_, context_);
}

void BackRefFromAssociatedObject::initForPermanentObject(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(obj->permanent(), "Can only be called with permanent object");
    obj_ = obj;
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    RuntimeAssert(!obj->permanent(), "Can only be called with non-permanent object");
    obj_ = obj;

    // Generally a specialized addRef below:
    context_ = InitForeignRef(obj);
    refCount = 1;
}

template <ErrorPolicy errorPolicy>
void BackRefFromAssociatedObject::addRef() {
    static_assert(errorPolicy != ErrorPolicy::kDefaultValue, "Cannot use default return value here");

    // Can be called both from Native state (if ObjC or Swift code adds RC)
    // and from Runnable state (Kotlin_ObjCExport_refToObjC).

    if (atomicAdd(&refCount, 1) == 1) {
        if (obj_ == nullptr) return; // E.g. after [detach].

        kotlin::CalledFromNativeGuard guard(/* reentrant */ true);

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
    kotlin::CalledFromNativeGuard guard;

    if (obj_ == nullptr) return false; // E.g. after [detach].

    ObjHolder holder;
    ObjHeader* obj = TryRef(obj_, holder.slot());
    // Failed to lock weak reference.
    if (obj == nullptr) return false;
    RuntimeAssert(obj == obj_, "Mismatched locked weak. obj=%p obj_=%p", obj, obj_);
    // TODO: This is a very weird way to ask for "unsafe" addRef.
    addRef<ErrorPolicy::kIgnore>();
    return true;
}

template bool BackRefFromAssociatedObject::tryAddRef<ErrorPolicy::kThrow>();
template bool BackRefFromAssociatedObject::tryAddRef<ErrorPolicy::kTerminate>();

void BackRefFromAssociatedObject::releaseRef() {
    ForeignRefContext context = context_;
    if (atomicAdd(&refCount, -1) == 0) {
        if (obj_ == nullptr) return; // E.g. after [detach].

        kotlin::CalledFromNativeGuard guard;

        // Note: by this moment "subsequent" addRef may have already happened and patched context_.
        // So use the value loaded before refCount update:
        DeinitForeignRef(obj_, context);
        // From this moment [context] is generally a dangling pointer.
        // This is handled in [IsForeignRefAccessible] and [addRef].
        // TODO: This probably isn't fine in new MM. Make sure it works.
    }
}

void BackRefFromAssociatedObject::detach() {
    RuntimeAssert(atomicGet(&refCount) == 0, "unexpected refCount");
    obj_ = nullptr; // Handled in addRef/tryAddRef/releaseRef/ref.
}

ALWAYS_INLINE void BackRefFromAssociatedObject::assertDetached() {
    RuntimeAssert(obj_ == nullptr, "Expecting this=%p to be detached, but found obj_=%p", this, obj_);
}

template <ErrorPolicy errorPolicy>
ObjHeader* BackRefFromAssociatedObject::ref() const {
    kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);
    RuntimeAssert(obj_ != nullptr, "no valid Kotlin object found");
    return obj_;
}

template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kDefaultValue>() const;
template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kThrow>() const;
template ObjHeader* BackRefFromAssociatedObject::ref<ErrorPolicy::kTerminate>() const;

ObjHeader* BackRefFromAssociatedObject::refPermanent() const {
  return obj_;
}
