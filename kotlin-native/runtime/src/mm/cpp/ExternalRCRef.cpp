/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExternalRCRef.hpp"

#include <optional>

#include "CompilerConstants.hpp"
#include "GC.hpp"
#include "Memory.h"
#include "ObjCBackRef.hpp"
#include "PointerBits.h"
#include "ReferenceOps.hpp"
#include "SpecialRefRegistry.hpp"

using namespace kotlin;

// Implementation of kotlin.native.internal.ref.ExternalRCRef

namespace {

constexpr unsigned kPermanentTag = 1;

}

RUNTIME_NOTHROW extern "C" void* Kotlin_native_internal_ref_createRetainedExternalRCRef(KRef obj) {
    AssertThreadState(ThreadState::kRunnable);
    RuntimeAssert(obj != nullptr, "Cannot handle nullptr");

    if (obj->permanent()) {
        return mm::permanentObjectAsExternalRCRef(obj);
    }
    return static_cast<mm::ExternalRCRefImpl*>(mm::ObjCBackRef::create(obj));
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_disposeExternalRCRef(void* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        // Nothing to do.
        return;
    }
    mm::ObjCBackRef(static_cast<mm::ExternalRCRefImpl*>(ref)).dispose();
}

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Kotlin_native_internal_ref_dereferenceExternalRCRef, void* ref) {
    AssertThreadState(ThreadState::kRunnable);
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        RETURN_OBJ(obj);
    }
    RETURN_OBJ(*mm::ObjCBackRef(static_cast<mm::ExternalRCRefImpl*>(ref)));
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_retainExternalRCRef(void* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        // Nothing to do.
        return;
    }
    mm::ObjCBackRef(static_cast<mm::ExternalRCRefImpl*>(ref)).retain();
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_releaseExternalRCRef(void* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        // Nothing to do.
        return;
    }
    mm::ObjCBackRef(static_cast<mm::ExternalRCRefImpl*>(ref)).release();
}

RUNTIME_NOTHROW extern "C" bool Kotlin_native_internal_ref_tryRetainExternalRCRef(void* ref) {
    AssertThreadState(ThreadState::kRunnable);
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (auto obj = mm::externalRCRefAsPermanentObject(ref)) {
        return true;
    }
    return mm::ObjCBackRef(static_cast<mm::ExternalRCRefImpl*>(ref)).tryRetain();
}

mm::ExternalRCRefImpl::ExternalRCRefImpl(mm::SpecialRefRegistry& registry, KRef obj, Rc rc) noexcept : obj_(obj), rc_(rc) {
    RuntimeAssert(obj != nullptr, "Creating ExternalRCRefImpl for null object");
    RuntimeAssert(rc >= 0, "Creating ExternalRCRefImpl with negative rc %d", rc);
    // Runtime tests occasionally use sentinel values under 8 for opaque objects
    RuntimeAssert(reinterpret_cast<uintptr_t>(obj) < 8u || !obj->local(), "Creating ExternalRCRefImpl to a stack-allocated object %p", obj);

    if (rc > 0) {
        registry.insertIntoRootsHead(*this);
    }
}

mm::ExternalRCRefImpl::~ExternalRCRefImpl() {
    if (compiler::runtimeAssertsEnabled()) {
        auto rc = rc_.load(std::memory_order_relaxed);
        RuntimeAssert(rc == disposedMarker, "Deleting ExternalRCRefImpl@%p with rc %d", this, rc);
    }
}

void mm::ExternalRCRefImpl::dispose() noexcept {
    // Synchronization with `SpecialRefRegistry::findAliveNode()`.
    // TODO: When assertions are disabled, exchange may pollute the
    //       generated assembly. Check if this a problem.
    auto rc = rc_.exchange(disposedMarker, std::memory_order_release);
    if (compiler::runtimeAssertsEnabled()) {
        if (rc > 0) {
            auto* obj = objAtomic().load(std::memory_order_relaxed);
            // In objc export if ObjCClass extends from KtClass
            // doing retain+autorelease inside [ObjCClass dealloc] will cause
            // this->dispose() be called after this->retain() but before
            // subsequent this->release().
            // However, since this happens in dealloc, the stored object must
            // have been cleared already.
            RuntimeAssert(obj == nullptr, "Disposing ExternalRCRefImpl@%p with rc %d and uncleaned object %p", this, rc, obj);
        }
        RuntimeAssert(rc >= 0, "Disposing ExternalRCRefImpl@%p with rc %d", this, rc);
    }
}

KRef mm::ExternalRCRefImpl::ref() const noexcept {
    if (compiler::runtimeAssertsEnabled()) {
        AssertThreadState(ThreadState::kRunnable);
        auto rc = rc_.load(std::memory_order_relaxed);
        RuntimeAssert(rc >= 0, "Dereferencing ExternalRCRefImpl@%p with rc %d", this, rc);
    }
    return objAtomic().load(std::memory_order_relaxed);
}

OBJ_GETTER0(mm::ExternalRCRefImpl::tryRef) noexcept {
    AssertThreadState(ThreadState::kRunnable);
    RETURN_RESULT_OF(mm::weakRefReadBarrier, objAtomic());
}

void mm::ExternalRCRefImpl::retainRef() noexcept {
    auto rc = rc_.fetch_add(1, std::memory_order_relaxed);
    RuntimeAssert(rc >= 0, "Retaining ExternalRCRefImpl@%p with rc %d", this, rc);
    if (rc == 0) {
        if (!objAtomic().load(std::memory_order_relaxed)) {
            // In objc export if ObjCClass extends from KtClass
            // calling retain inside [ObjCClass dealloc] will cause
            // node.retainRef() be called after node.obj_ was cleared but
            // before node.dispose().
            // We could place it into the root set, and it'll be removed
            // from it at some later point. But let's just skip it.
            return;
        }

        // With the current CMS implementation no barrier is required here.
        // The CMS builds Snapshot-at-the-beginning mark closure,
        // which means, it has to remember only the concurrent deletion of references, not creation.
        // TODO: A write-into-root-set barrier might be required here for other concurrent mark strategies.

        // In case of non-concurrent root set scanning, it is only required for the object to already be in roots.
        // If 0->1 happened from `[ObjCClass _tryRetain]`, it would first hold the object
        // on the stack via `tryRef`.
        // If 0->1 happened during construction:
        // * First of all, currently it's impossible because the `Node` is created with rc=1 and not inserted
        //   into the roots list until publishing.
        // * Even if the above changes, for the construction, the object must have been passed in from somewhere,
        //   so it must be reachable anyway.
        // If 0->1 happened because an object is passing through the interop border for the second time (or more)
        // (e.g. accessing a non-permanent global a couple of times). Follows the construction case above:
        // "the object must have been passed in from somewhere, so it must be reachable anyway".

        // 0->1 changes require putting this node into the root set.
        SpecialRefRegistry::instance().insertIntoRootsHead(*this);
    }
}

void mm::ExternalRCRefImpl::releaseRef() noexcept {
    if (gc::barriers::SpecialRefReleaseGuard::isNoop()) {
        auto rcBefore = rc_.fetch_sub(1, std::memory_order_relaxed);
        RuntimeAssert(
                rcBefore > 0, "Releasing ExternalRCRefImpl@%p(%p %s) with rc %d", this, obj_, obj_->type_info()->fqName().c_str(),
                rcBefore);
    } else {
        // A 1->0 release is potentially a removal from global root set.
        // The CMS GC scans global root set concurrently. A guard is required.
        auto rcBefore = rc_.load(std::memory_order_relaxed);
        while (true) {
            std::optional<gc::barriers::SpecialRefReleaseGuard> guard;
            if (rcBefore == 1) {
                // The guard is only required in case of the last reference release (0->1).
                // We avoid it in all other cases, as the guard can be quite an overhead: e.g. taking the GC lock.
                // We also drop the guard if CAS below fails and we retry. This way, the GC will be allowed to take the lock
                // sooner. This does, however, hurt a thread that failed to decrement, because it may have to wait for the GC.
                guard = gc::barriers::SpecialRefReleaseGuard{mm::DirectRefAccessor{obj_}};
            }
            if (rc_.compare_exchange_strong(rcBefore, rcBefore - 1, std::memory_order_relaxed)) break;
        }

        RuntimeAssert(
                rcBefore > 0, "Releasing ExternalRCRefImpl@%p(%p %s) with rc %d", this, obj_, obj_->type_info()->fqName().c_str(),
                rcBefore);
    }
}

KRef kotlin::mm::externalRCRefAsPermanentObject(void* ref) noexcept {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (hasPointerBits(ref, kPermanentTag)) {
        auto obj = clearPointerBits(static_cast<KRef>(ref), kPermanentTag);
        RuntimeAssert(obj->permanent(), "Permanent ExternalRCRef for non-permanent object %p", obj);
        return obj;
    }
    return nullptr;
}

void* kotlin::mm::permanentObjectAsExternalRCRef(KRef obj) noexcept {
    RuntimeAssert(obj != nullptr, "Cannot handle nullptr");
    RuntimeAssert(obj->permanent(), "Object %p must be permanent", obj);
    return setPointerBits(obj, kPermanentTag);
}

const TypeInfo* kotlin::mm::externalRCRefType(void* ref) noexcept {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    KRef obj = nullptr;
    if (hasPointerBits(ref, kPermanentTag)) {
        obj = clearPointerBits(static_cast<KRef>(ref), kPermanentTag);
        RuntimeAssert(obj->permanent(), "Permanent ExternalRCRef for non-permanent object %p", obj);
    } else {
        obj = *mm::ObjCBackRef(static_cast<mm::ExternalRCRefImpl*>(ref));
        RuntimeAssert(obj != nullptr, "ExternalRCRef %p points to null object", ref);
    }
    return obj->type_info();
}
