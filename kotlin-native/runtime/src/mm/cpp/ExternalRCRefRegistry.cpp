/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ExternalRCRefRegistry.hpp"

#include "GlobalData.hpp"

using namespace kotlin;

// static
mm::ExternalRCRefRegistry& mm::ExternalRCRefRegistry::instance() noexcept {
    return GlobalData::Instance().externalRCRefRegistry();
}

mm::ExternalRCRefImpl* mm::ExternalRCRefRegistry::nextRoot(mm::ExternalRCRefImpl* current) noexcept {
    RuntimeAssert(current != nullptr, "current cannot be null");
    RuntimeAssert(current != &rootsTail_, "current cannot be tail");
    mm::ExternalRCRefImpl* candidate = current->nextRoot_.load(std::memory_order_relaxed);
    // Not an infinite loop, `candidate` always moves forward and since insertions can only
    // happen in the head, they will always happen before `candidate`.
    while (true) {
        RuntimeAssert(candidate != nullptr, "candidate cannot be null");
        if (candidate == &rootsTail_)
            // Reached tail, nothing to do anymore
            return candidate;
        if (candidate->rc_.load(std::memory_order_relaxed) > 0) {
            // Keeping acquire-release for nextRoot_.
            std::atomic_thread_fence(std::memory_order_acquire);
            // Perfectly good ref. Stop right there.
            return candidate;
        }
        // Bad ref. Let's remove it from the roots.
        // Racy if someone concurrently inserts in the middle. Or iterates.
        // But we don't have that here. Inserts are only in the beginning.
        // Iteration also happens only here.
        auto [candidatePrev, candidateNext] = eraseFromRoots(current, candidate);
        // We removed candidate. But should we have?
        if (candidate->rc_.load(std::memory_order_relaxed) > 0) {
            RuntimeAssert(candidate->objAtomic().load(std::memory_order_relaxed) != nullptr, "candidate cannot have a null obj_");
            // Ooops. Let's put it back. Okay to put into the head.
            insertIntoRootsHead(*candidate);
        }
        // eraseFromRoots and insertIntoRootsHead are both acquire-release fences.
        // This means they play nice with each other and we don't need an extra fence
        // here to ensure synchronization with 0->1 rc_ change:
        // * We read rc_ after eraseFromRoots.
        // * retainRef writes rc_ before insertIntoRootsHead.
        // So the write to rc_ in retainRef happens before the read here.
        //
        // Okay, properly deleted. Our new `candidate` is the next of previous candidate,
        // and our `current` then is our best guess at the previous ref of the `candidate`.
        current = candidatePrev;
        candidate = candidateNext;
        // `current` has either moved forward or stayed where it is.
        // `candidate` has definitely moved forward.
        // `current` is only used in `eraseFromRoots` which itself ensures that no
        // infinite loop can happen.
        // So, this loop is also not infinite.
    }
}

std::pair<mm::ExternalRCRefImpl*, mm::ExternalRCRefImpl*> mm::ExternalRCRefRegistry::eraseFromRoots(
        mm::ExternalRCRefImpl* prev, mm::ExternalRCRefImpl* ref) noexcept {
    RuntimeAssert(ref != &rootsHead_, "ref cannot be head");
    RuntimeAssert(ref != &rootsTail_, "ref cannot be tail");
    mm::ExternalRCRefImpl* next = ref->nextRoot_.load(std::memory_order_acquire);
    RuntimeAssert(next != nullptr, "ref@%p next cannot be null", ref);
    do {
        mm::ExternalRCRefImpl* prevExpectedNext = ref;
        bool removed =
                prev->nextRoot_.compare_exchange_strong(prevExpectedNext, next, std::memory_order_release, std::memory_order_acquire);
        if (removed) {
            auto* actualNext = ref->nextRoot_.exchange(nullptr, std::memory_order_acq_rel);
            RuntimeAssert(actualNext == next, "ref@%p next expected %p actual %p", ref, next, actualNext);
            return {prev, next};
        }
        prev = prevExpectedNext;
        RuntimeAssert(prev != &rootsHead_, "prev cannot be head");
        RuntimeAssert(prev != &rootsTail_, "prev cannot be tail");
        // We moved `prev` forward, nothing can insert after `prev` anymore, this
        // cannot be an infinite loop, then.
    } while (true);
}

void mm::ExternalRCRefRegistry::insertIntoRootsHead(mm::ExternalRCRefImpl& ref) noexcept {
    mm::ExternalRCRefImpl* next = rootsHead_.nextRoot_.load(std::memory_order_acquire);
    mm::ExternalRCRefImpl* refExpectedNext = nullptr;
    do {
        RuntimeAssert(next != nullptr, "head's next cannot be null");
        if (!ref.nextRoot_.compare_exchange_strong(refExpectedNext, next, std::memory_order_release, std::memory_order_acquire)) {
            // So:
            // * `ref` is already in the roots list
            // * some other thread is inserting it in the roots list
            // * GC thread may be removing it from the roots list, but
            //   will recheck rc afterward and insert it back if needed
            // In either case, do not touch anything anymore here.
            return;
        }
        // CAS was successful, so we need to update the expected value of ref.nextRoot_
        refExpectedNext = next;
    } while (!rootsHead_.nextRoot_.compare_exchange_weak(next, &ref, std::memory_order_release, std::memory_order_acquire));
}

std::list<mm::ExternalRCRefImpl>::iterator mm::ExternalRCRefRegistry::nextAlive(std::list<mm::ExternalRCRefImpl>::iterator it) noexcept {
    while (it != all_.end() && it->rc_.load(std::memory_order_relaxed) == mm::ExternalRCRefImpl::disposedMarker) {
        // Synchronization with `ExternalRCRefImpl::dispose()`
        std::atomic_thread_fence(std::memory_order_acquire);
        // Removing disposed refs.
        if (it->nextRoot_.load(std::memory_order_relaxed) != nullptr) {
            // Wait, it's in the roots list. Let's wait until the next GC
            // for it to get cleaned up from there.
            ++it;
            continue;
        }
        // If we observe both `nextRoot_ == nullptr` and `rc_ == disposedMarker`, we
        // can be sure, that no mutator can change `nextRoot_` later.
        // Proof:
        // For the mutator to change `nextRoot_`, it would need to perform 0->1 `rc_` transition.
        // But `rc_` can only be set to `disposedMarker` during `dispose()` call,
        // which can only happen after any `retainRef()` call. So, it's impossible
        // to observe any changes in `nextRoot_` after we observe `disposedMarker` in `nextRoot_`.
        it = all_.erase(it);
    }
    return it;
}
