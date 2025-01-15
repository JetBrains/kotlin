/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExternalRCRef.hpp"

#include <atomic>
#include <optional>

#include "CompilerConstants.hpp"
#include "ExternalRCRefRegistry.hpp"
#include "GC.hpp"
#include "KAssert.h"
#include "Memory.h"
#include "PointerBits.h"
#include "ReferenceOps.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

// Implementation of kotlin.native.internal.ref.ExternalRCRef

namespace {

constexpr unsigned kPermanentTag = 1;

}

RUNTIME_NOTHROW extern "C" mm::RawExternalRCRef* Kotlin_native_internal_ref_createRetainedExternalRCRef(KRef obj) {
    RuntimeAssert(obj != nullptr, "Cannot handle nullptr");
    return mm::createRetainedExternalRCRef(obj);
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_disposeExternalRCRef(mm::RawExternalRCRef* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    return mm::disposeExternalRCRef(ref);
}

RUNTIME_NOTHROW extern "C" OBJ_GETTER(Kotlin_native_internal_ref_dereferenceExternalRCRef, mm::RawExternalRCRef* ref) {
    AssertThreadState(ThreadState::kRunnable);
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    RETURN_OBJ(mm::dereferenceExternalRCRef(ref));
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_retainExternalRCRef(mm::RawExternalRCRef* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    mm::retainExternalRCRef(ref);
}

RUNTIME_NOTHROW extern "C" void Kotlin_native_internal_ref_releaseExternalRCRef(mm::RawExternalRCRef* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    mm::releaseExternalRCRef(ref);
}

RUNTIME_NOTHROW extern "C" bool Kotlin_native_internal_ref_tryRetainExternalRCRef(mm::RawExternalRCRef* ref) {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    AssertThreadState(ThreadState::kRunnable);
    if (externalRCRefAsPermanentObject(ref)) return true;
    auto refImpl = mm::ExternalRCRefImpl::fromRaw(ref);
    ObjHolder holder;
    if (refImpl->tryRef(holder.slot())) {
        refImpl->retainRef();
        return true;
    }
    return false;
}

mm::ExternalRCRefImpl::ExternalRCRefImpl(mm::ExternalRCRefRegistry& registry, KRef obj, Rc rc) noexcept : obj_(obj), rc_(rc) {
    RuntimeAssert(obj != nullptr, "Creating ExternalRCRefImpl for null object");
    RuntimeAssert(rc >= 0, "Creating ExternalRCRefImpl with negative rc %d", rc);
    // Runtime tests occasionally use sentinel values under 8 for opaque objects
    RuntimeAssert(reinterpret_cast<uintptr_t>(obj) < 8u || !obj->stack(), "Creating ExternalRCRefImpl to a stack-allocated object %p", obj);

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

// static
mm::ExternalRCRefImpl& mm::ExternalRCRefImpl::create(KRef obj, Rc rc) noexcept {
    return mm::ThreadRegistry::Instance().CurrentThreadData()->externalRCRefRegistry().createExternalRCRefImpl(obj, rc);
}

void mm::ExternalRCRefImpl::dispose() noexcept {
    // Synchronization with `ExternalRCRefRegistry::findAliveNode()`.
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

const TypeInfo* mm::ExternalRCRefImpl::typeInfo() const noexcept {
    if (compiler::runtimeAssertsEnabled()) {
        auto rc = rc_.load(std::memory_order_relaxed);
        RuntimeAssert(rc >= 0, "Getting TypeInfo from ExternalRCRefImpl@%p with rc %d", this, rc);
    }
    auto* obj = objAtomic().load(std::memory_order_relaxed);
    RuntimeAssert(obj, "Getting TypeInfo from ExternalRCRefImpl@%p that already has nulled-out object", this);
    return obj->type_info();
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
        ExternalRCRefRegistry::instance().insertIntoRootsHead(*this);
    }
}

void mm::ExternalRCRefImpl::releaseRef() noexcept {
    if (gc::barriers::ExternalRCRefReleaseGuard::isNoop()) {
        auto rcBefore = rc_.fetch_sub(1, std::memory_order_relaxed);
        RuntimeAssert(
                rcBefore > 0, "Releasing ExternalRCRefImpl@%p(%p %s) with rc %d", this, obj_, obj_->type_info()->fqName().c_str(),
                rcBefore);
    } else {
        // A 1->0 release is potentially a removal from global root set.
        // The CMS GC scans global root set concurrently. A guard is required.
        auto rcBefore = rc_.load(std::memory_order_relaxed);
        while (true) {
            std::optional<gc::barriers::ExternalRCRefReleaseGuard> guard;
            if (rcBefore == 1) {
                // The guard is only required in case of the last reference release (0->1).
                // We avoid it in all other cases, as the guard can be quite an overhead: e.g. taking the GC lock.
                // We also drop the guard if CAS below fails and we retry. This way, the GC will be allowed to take the lock
                // sooner. This does, however, hurt a thread that failed to decrement, because it may have to wait for the GC.
                guard = gc::barriers::ExternalRCRefReleaseGuard{mm::DirectRefAccessor{obj_}};
            }
            if (rc_.compare_exchange_strong(rcBefore, rcBefore - 1, std::memory_order_relaxed)) break;
        }

        RuntimeAssert(
                rcBefore > 0, "Releasing ExternalRCRefImpl@%p(%p %s) with rc %d", this, obj_, obj_->type_info()->fqName().c_str(),
                rcBefore);
    }
}

// static
const mm::ExternalRCRefImpl* mm::ExternalRCRefImpl::fromRaw(const RawExternalRCRef* ref) noexcept {
    auto* obj = mm::externalRCRefAsPermanentObject(ref);
    RuntimeAssert(obj == nullptr, "Trying to convert permanent ExternalRCRef (for %p) into ExternalRCRefImpl", obj);
    return reinterpret_cast<const mm::ExternalRCRefImpl*>(ref);
}

KRef kotlin::mm::externalRCRefAsPermanentObject(const RawExternalRCRef* ref) noexcept {
    RuntimeAssert(ref != nullptr, "Cannot handle nullptr");
    if (hasPointerBits(ref, kPermanentTag)) {
        auto obj = clearPointerBits(reinterpret_cast<KConstRef>(ref), kPermanentTag);
        RuntimeAssert(obj->permanent(), "Permanent ExternalRCRef for non-permanent object %p", obj);
        return const_cast<KRef>(obj); // RawExternalRCRef* for permanent objects is always created from KRef, not KConstRef.
    }
    return nullptr;
}

mm::RawExternalRCRef* kotlin::mm::permanentObjectAsExternalRCRef(KRef obj) noexcept {
    RuntimeAssert(obj != nullptr, "Cannot handle nullptr");
    RuntimeAssert(obj->permanent(), "Object %p must be permanent", obj);
    return reinterpret_cast<mm::RawExternalRCRef*>(setPointerBits(obj, kPermanentTag));
}
