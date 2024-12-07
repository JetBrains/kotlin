/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <list>
#include <optional>

#include "GC.hpp"
#include "Memory.h"
#include "ReferenceOps.hpp"
#include "RawPtr.hpp"
#include "ReferenceOps.hpp"
#include "ThreadRegistry.hpp"

namespace kotlin::mm {

class ObjCBackRef;
class StableRef;
class WeakRef;

// Registry for all special references to objects:
// * stable references (i.e. always part of the root set)
// * weak references
// * ObjC back references. A mix between stable and weak references
//   created for ObjC part of Kotlin objects. Have a count of external
//   references. When > 0 - stable reference. When = 0 - weak reference.
//
// Consists of 2 global lists and 1 thread local list of `Node`s.
// Each `Node` has a reference `obj_` to an object and an external references counter `rc_`.
// Invariants:
// * `rc_ > 0` -> alive externally referenced object, must be in the root set.
// * `rc_ == 0` -> alive externally unreferenced object, must eventually be out of the root set.
// * `rc_ == disposedMarker` -> the `Node` itself is no longer externally referenced and can be eventually deleted.
// * `rc_` can be increased and decreased by any thread in any state.
//   * In practice 0 -> 1 only happens in mutator threads in runnable state, but the implementation of `SpecialRefRegistry`
//     does not depend on it.
// * `Node`s are owned either by a global `std::list` or by thread local `std::list`s. Global list is protected by a mutex.
// * Insertion into the global list happens by moving from thread local list during STW and when a thread gets destroyed.
// * Only the GC thread traverses and removes elements from the global list. Removal happens only if `Node` has `rc_ == disposedMarker`
//   and it's not in the roots list.
// * During global list traversal `Node`s `obj_` referenced may get nulled out by the GC.
// * Insertion into thread local lists happens in runnable state.
// * Removal from thread local list happens during STW, thread destruction, or in the runnable state for `Node`s that can never
//   go through 0 -> 1 rc transition (created via mm::StableRef).
// * `Node`s are additionally linked into an intrusive global roots list.
// * Any thread in any state can insert into the roots list. Insertion only happens into the head.
// * Only the GC thread can remove from the roots list during root scanning. If after removal
//   the `rc_` of the `Node` is `> 0`, the GC thread will make sure the node is inserted
//   into the head
// * During roots list traversal all nodes to the left are either marked or inserted into the mark queue.
class SpecialRefRegistry : private Pinned {
    // TODO: Consider using a real mutex.
    using Mutex = SpinLock;

    class Node : private Pinned {
    public:
        using Rc = int32_t;
        inline static constexpr Rc disposedMarker = std::numeric_limits<Rc>::min();
        static_assert(disposedMarker < 0, "disposedMarker must be an impossible Rc value");

        Node(SpecialRefRegistry& registry, ObjHeader* obj, Rc rc) noexcept : obj_(obj), rc_(rc) {
            RuntimeAssert(obj != nullptr, "Creating StableRef for null object");
            RuntimeAssert(rc >= 0, "Creating StableRef with negative rc %d", rc);
            // Runtime tests occasionally use sentinel values under 8 for opaque objects
            RuntimeAssert(reinterpret_cast<uintptr_t>(obj) < 8u || !obj->local(), "Creating StableRef to a stack-allocated object %p", obj);

            if (rc > 0) {
                registry.insertIntoRootsHead(*this);
            }
        }

        Node() noexcept : obj_(nullptr), rc_(disposedMarker) {}

        ~Node() {
            if (compiler::runtimeAssertsEnabled()) {
                auto rc = rc_.load(std::memory_order_relaxed);
                RuntimeAssert(rc == disposedMarker, "Deleting StableRef@%p with rc %d", this, rc);
            }
        }

        void dispose() noexcept {
            // Synchronization with `findAliveNode()`.
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
                    RuntimeAssert(obj == nullptr, "Disposing StableRef@%p with rc %d and uncleaned object %p", this, rc, obj);
                }
                RuntimeAssert(rc >= 0, "Disposing StableRef@%p with rc %d", this, rc);
            }
        }

        [[nodiscard("expensive pure function")]] ObjHeader* ref() const noexcept {
            if (compiler::runtimeAssertsEnabled()) {
                AssertThreadState(ThreadState::kRunnable);
                auto rc = rc_.load(std::memory_order_relaxed);
                RuntimeAssert(rc >= 0, "Dereferencing StableRef@%p with rc %d", this, rc);
            }
            return objAtomic().load(std::memory_order_relaxed);
        }

        [[nodiscard("expensive pure function")]] const TypeInfo* typeInfo() const noexcept {
            if (compiler::runtimeAssertsEnabled()) {
                auto rc = rc_.load(std::memory_order_relaxed);
                RuntimeAssert(rc > 0, "Getting typeInfo of StableRef@%p with rc %d", this, rc);
            }
            return objAtomic().load(std::memory_order_relaxed)->type_info();
        }

        OBJ_GETTER0(tryRef) noexcept {
            AssertThreadState(ThreadState::kRunnable);
            RETURN_RESULT_OF(mm::weakRefReadBarrier, objAtomic());
        }

        void retainRef() noexcept {
            auto rc = rc_.fetch_add(1, std::memory_order_relaxed);
            RuntimeAssert(rc >= 0, "Retaining StableRef@%p with rc %d", this, rc);
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

        void releaseRef() noexcept {
            if (gc::barriers::SpecialRefReleaseGuard::isNoop()) {
                auto rcBefore = rc_.fetch_sub(1, std::memory_order_relaxed);
                RuntimeAssert(rcBefore > 0, "Releasing StableRef@%p(%p %s) with rc %d", this, obj_, obj_->type_info()->fqName().c_str(), rcBefore);
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

                RuntimeAssert(rcBefore > 0, "Releasing StableRef@%p(%p %s) with rc %d", this, obj_, obj_->type_info()->fqName().c_str(), rcBefore);
            }
        }

        RawSpecialRef* asRaw() noexcept { return reinterpret_cast<RawSpecialRef*>(this); }
        static Node* fromRaw(RawSpecialRef* ref) noexcept { return reinterpret_cast<Node*>(ref); }

    private:
        friend class SpecialRefRegistry;
        friend class SpecialRefRegistryTest;

        // obj_ is set in the constructor and can be nulled out only by the
        // GC thread when processing weaks. It's the responsibility of the
        // GC to make sure nulling out obj_ is synchronized with mutators:
        // * via STW: nulling obj_ only happens when mutators are paused.
        // * via weak read barriers: when GC enters a weak processing phase,
        //   it enables weak read barriers which do not read obj_ if obj_ will
        //   be nulled, and disable the barriers when the phase is completed.
        //   Synchronization between GC and mutators happens via enabling/disabling
        //   the barriers.
        // TODO: Try to handle it atomically only when the GC is in progress.
        std_support::atomic_ref<ObjHeader*> objAtomic() noexcept  { return std_support::atomic_ref{obj_}; }
        std_support::atomic_ref<ObjHeader* const> objAtomic() const noexcept { return std_support::atomic_ref{obj_}; }
        ObjHeader* obj_;

        // Only ever updated using relaxed memory ordering. Any synchronization
        // with nextRoot_ is achieved via acquire-release of nextRoot_.
        std::atomic<Rc> rc_; // After dispose() will be disposedMarker.
        // Singly linked lock free list. Using acquire-release throughout.
        std::atomic<Node*> nextRoot_ = nullptr;
    };

public:
    class ThreadQueue : private Pinned {
    public:
        explicit ThreadQueue(SpecialRefRegistry& registry) : owner_(registry) {}

        ~ThreadQueue() { publish(); }

        void publish() noexcept {
            std::unique_lock guard(owner_.mutex_);
            RuntimeAssert(owner_.all_.get_allocator() == queue_.get_allocator(), "allocators must match");
            owner_.all_.splice(owner_.all_.end(), std::move(queue_));
        }

        void clearForTests() noexcept {
            for (auto& specialRef: queue_) {
                specialRef.dispose();
            }
            queue_.clear();
        }

        [[nodiscard("must be manually disposed")]] StableRef createStableRef(ObjHeader* object) noexcept;
        [[nodiscard("must be manually disposed")]] WeakRef createWeakRef(ObjHeader* object) noexcept;
        [[nodiscard("must be manually disposed")]] ObjCBackRef createObjCBackRef(ObjHeader* object) noexcept;

    private:
        friend class StableRef;
        friend class SpecialRefRegistryTest;

        [[nodiscard("must be manually disposed")]] Node& registerNode(ObjHeader* obj, Node::Rc rc, bool allowFastDeletion) noexcept {
            RuntimeAssert(obj != nullptr, "Creating node for null object");
            queue_.emplace_back(owner_, obj, rc);
            auto& node = queue_.back();
            return node;
        }

        SpecialRefRegistry& owner_;
        std::list<Node> queue_;
    };

    class RootsIterator {
    public:
        ObjHeader* operator*() const noexcept {
            // Ignoring rc here. If someone nulls out rc during root
            // scanning, it's okay to be conservative and still make it a root.
            return node_->objAtomic().load(std::memory_order_relaxed);
        }

        RootsIterator& operator++() noexcept {
            node_ = owner_->nextRoot(node_);
            return *this;
        }

        bool operator==(const RootsIterator& rhs) const noexcept { return node_ == rhs.node_; }

        bool operator!=(const RootsIterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        friend class SpecialRefRegistry;

        RootsIterator(SpecialRefRegistry& owner, Node* node) noexcept : owner_(&owner), node_(node) {}

        SpecialRefRegistry* owner_;
        Node* node_;
    };

    class RootsIterable : private MoveOnly {
    public:
        RootsIterator begin() const noexcept { return RootsIterator(*owner_, owner_->nextRoot(&owner_->rootsHead_)); }

        RootsIterator end() const noexcept { return RootsIterator(*owner_, &owner_->rootsTail_); }

    private:
        friend class SpecialRefRegistry;

        explicit RootsIterable(SpecialRefRegistry& owner) noexcept : owner_(&owner) {}

        raw_ptr<SpecialRefRegistry> owner_;
    };

    class Iterator {
    public:
        std_support::atomic_ref<ObjHeader*> operator*() noexcept { return iterator_->objAtomic(); }

        Iterator& operator++() noexcept {
            iterator_ = owner_->findAliveNode(std::next(iterator_));
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return iterator_ == rhs.iterator_; }

        bool operator!=(const Iterator& rhs) const noexcept { return iterator_ != rhs.iterator_; }

    private:
        friend class SpecialRefRegistry;
        friend class SpecialRefRegistryTest;

        Iterator(SpecialRefRegistry& owner, std::list<Node>::iterator iterator) noexcept : owner_(&owner), iterator_(iterator) {}

        SpecialRefRegistry* owner_;
        std::list<Node>::iterator iterator_;
    };

    class Iterable : private MoveOnly {
    public:
        Iterator begin() noexcept { return Iterator(owner_, owner_.findAliveNode(owner_.all_.begin())); }
        Iterator end() noexcept { return Iterator(owner_, owner_.all_.end()); }

    private:
        friend class SpecialRefRegistry;

        Iterable(SpecialRefRegistry& owner) noexcept : owner_(owner), guard_(owner_.mutex_) {}

        SpecialRefRegistry& owner_;
        std::unique_lock<Mutex> guard_;
    };

    SpecialRefRegistry() noexcept { rootsHead_.nextRoot_.store(&rootsTail_, std::memory_order_relaxed); }

    ~SpecialRefRegistry() = default;

    static SpecialRefRegistry& instance() noexcept;

    void clearForTests() noexcept {
        rootsHead_.nextRoot_ = &rootsTail_;
        for (auto& node : all_) {
            // Allow the tests not to run the finalizers for weaks.
            node.rc_ = Node::disposedMarker;
        }
        all_.clear();
    }

    // Should be called on the GC thread after all threads have published.
    RootsIterable roots() noexcept { return RootsIterable(*this); }

    // Should be called on the GC thread after marking is complete.
    // Locks the registry and allows safe iteration over it.
    Iterable lockForIter() noexcept { return Iterable(*this); }

private:
    friend class ObjCBackRef;
    friend class StableRef;
    friend class WeakRef;
    friend class SpecialRefRegistryTest;

    Node* nextRoot(Node* current) noexcept;
    // Erase `node` from the roots list. `prev` is the current guess of the node
    // previous to `node`. Returns two nodes between which `node` was deleted.
    std::pair<Node*, Node*> eraseFromRoots(Node* prev, Node* node) noexcept;
    void insertIntoRootsHead(Node& node) noexcept;
    std::list<Node>::iterator findAliveNode(std::list<Node>::iterator it) noexcept;

    // TODO: Iteration over `all_` will be slow, because it's `std::list`
    //       collected at different times from different threads, and so the nodes
    //       are all over the memory. Consider using custom allocator for that.
    std::list<Node> all_;
    Mutex mutex_;
    Node rootsHead_{};
    static inline Node rootsTail_{};
};

} // namespace kotlin::mm
