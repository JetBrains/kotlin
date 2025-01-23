/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <atomic>
#include <list>
#include <mutex>

#include "ExternalRCRef.hpp"
#include "Memory.h"
#include "RawPtr.hpp"
#include "concurrent/Mutex.hpp"

namespace kotlin::mm {

// Registry for all externally-reference-counted references to objects. Examples:
// * stable references (i.e. always part of the root set)
// * weak references
// * ObjC back references. A mix between stable and weak references
//   created for ObjC part of Kotlin objects. Have a count of external
//   references. When > 0 - stable reference. When = 0 - weak reference.
//
// Consists of 2 global lists and 1 thread local list of `ExternalRCRefImpl`s.
// Each `ExternalRCRefImpl` has a reference `obj_` to an object and an external references counter `rc_`.
// Invariants:
// * `rc_ > 0` -> alive externally referenced object, must be in the root set.
// * `rc_ == 0` -> alive externally unreferenced object, must eventually be out of the root set.
// * `rc_ == disposedMarker` -> the `ExternalRCRefImpl` itself is no longer externally referenced and can be eventually deleted.
// * `rc_` can be increased and decreased by any thread in any state.
//   * In practice 0 -> 1 only happens in mutator threads in runnable state, but the implementation of `SpecialRefRegistry`
//     does not depend on it.
// * `ExternalRCRefImpl`s are owned either by a global `std::list` or by thread local `std::list`s. Global list is protected by a mutex.
// * Insertion into the global list happens by moving from thread local list during STW and when a thread gets destroyed.
// * Only the GC thread traverses and removes elements from the global list. Removal happens only if `ExternalRCRefImpl`
//   has `rc_ == disposedMarker` and it's not in the roots list.
// * During global list traversal `ExternalRCRefImpl`s `obj_` referenced may get nulled out by the GC.
// * Insertion into thread local lists happens in runnable state.
// * Removal from thread local list happens during STW, thread destruction, or in the runnable state for `ExternalRCRefImpl`s that can never
//   go through 0 -> 1 rc transition (created via mm::StableRef).
// * `ExternalRCRefImpl`s are additionally linked into an intrusive global roots list.
// * Any thread in any state can insert into the roots list. Insertion only happens into the head.
// * Only the GC thread can remove from the roots list during root scanning. If after removal
//   the `rc_` of the `ExternalRCRefImpl` is `> 0`, the GC thread will make sure the ref is inserted
//   into the head
// * During roots list traversal all refs to the left are either marked or inserted into the mark queue.
class SpecialRefRegistry : private Pinned {
    // TODO: Consider using a real mutex.
    using Mutex = SpinLock;

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

        [[nodiscard("must be manually disposed")]] ExternalRCRefImpl& createExternalRCRefImpl(KRef obj, ExternalRCRefImpl::Rc rc) noexcept {
            AssertThreadState(ThreadState::kRunnable);
            queue_.emplace_back(owner_, obj, rc);
            return queue_.back();
        }

    private:
        SpecialRefRegistry& owner_;
        std::list<ExternalRCRefImpl> queue_;
    };

    class RootsIterator {
    public:
        KRef operator*() const noexcept {
            // Ignoring rc here. If someone nulls out rc during root
            // scanning, it's okay to be conservative and still make it a root.
            return ref_->objAtomic().load(std::memory_order_relaxed);
        }

        RootsIterator& operator++() noexcept {
            ref_ = owner_->nextRoot(ref_);
            return *this;
        }

        bool operator==(const RootsIterator& rhs) const noexcept { return ref_ == rhs.ref_; }

        bool operator!=(const RootsIterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        friend class SpecialRefRegistry;

        RootsIterator(SpecialRefRegistry& owner, ExternalRCRefImpl* ref) noexcept : owner_(&owner), ref_(ref) {}

        SpecialRefRegistry* owner_;
        ExternalRCRefImpl* ref_;
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
        std_support::atomic_ref<KRef> operator*() noexcept { return iterator_->objAtomic(); }

        Iterator& operator++() noexcept {
            iterator_ = owner_->nextAlive(std::next(iterator_));
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return iterator_ == rhs.iterator_; }

        bool operator!=(const Iterator& rhs) const noexcept { return iterator_ != rhs.iterator_; }

    private:
        friend class SpecialRefRegistry;
        friend class SpecialRefRegistryTest;

        Iterator(SpecialRefRegistry& owner, std::list<ExternalRCRefImpl>::iterator iterator) noexcept : owner_(&owner), iterator_(iterator) {}

        SpecialRefRegistry* owner_;
        std::list<ExternalRCRefImpl>::iterator iterator_;
    };

    class Iterable : private MoveOnly {
    public:
        Iterator begin() noexcept { return Iterator(owner_, owner_.nextAlive(owner_.all_.begin())); }
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
        for (auto& ref : all_) {
            // Allow the tests not to run the finalizers for weaks.
            ref.rc_ = ExternalRCRefImpl::disposedMarker;
        }
        all_.clear();
    }

    // Should be called on the GC thread after all threads have published.
    RootsIterable roots() noexcept { return RootsIterable(*this); }

    // Should be called on the GC thread after marking is complete.
    // Locks the registry and allows safe iteration over it.
    Iterable lockForIter() noexcept { return Iterable(*this); }

private:
    friend class ExternalRCRefImpl;
    friend class SpecialRefRegistryTest;

    ExternalRCRefImpl* nextRoot(ExternalRCRefImpl* current) noexcept;
    // Erase `ref` from the roots list. `prev` is the current guess of the ref
    // previous to `ref`. Returns two refs between which `ref` was deleted.
    std::pair<ExternalRCRefImpl*, ExternalRCRefImpl*> eraseFromRoots(ExternalRCRefImpl* prev, ExternalRCRefImpl* ref) noexcept;
    void insertIntoRootsHead(ExternalRCRefImpl& ref) noexcept;
    // Return the next undisposed ref, cleaning up the disposed in the process.
    std::list<ExternalRCRefImpl>::iterator nextAlive(std::list<ExternalRCRefImpl>::iterator it) noexcept;

    // TODO: Iteration over `all_` will be slow, because it's `std::list`
    //       collected at different times from different threads, and so the refs
    //       are all over the memory. Consider using custom allocator for that.
    std::list<ExternalRCRefImpl> all_;
    Mutex mutex_;
    ExternalRCRefImpl rootsHead_{};
    static inline ExternalRCRefImpl rootsTail_{};
};

} // namespace kotlin::mm
