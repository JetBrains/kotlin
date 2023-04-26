/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SpecialRefRegistry.hpp"

#include "GlobalData.hpp"
#include "MemoryPrivate.hpp"
#include "ObjCBackRef.hpp"
#include "StableRef.hpp"
#include "ThreadData.hpp"
#include "ThreadState.hpp"
#include "WeakRef.hpp"

using namespace kotlin;

mm::StableRef mm::SpecialRefRegistry::ThreadQueue::createStableRef(ObjHeader* object) noexcept {
    return mm::StableRef(registerNode(object, 1, true).asRaw());
}

mm::WeakRef mm::SpecialRefRegistry::ThreadQueue::createWeakRef(ObjHeader* object) noexcept {
    return mm::WeakRef(registerNode(object, 0, false).asRaw());
}

mm::ObjCBackRef mm::SpecialRefRegistry::ThreadQueue::createObjCBackRef(ObjHeader* object) noexcept {
    return mm::ObjCBackRef(registerNode(object, 1, false).asRaw());
}

void mm::SpecialRefRegistry::ThreadQueue::deleteNodeIfLocal(Node& node) noexcept {
    // This is a very weird optimization.
    // * We're saving some time during root scanning and some memory by
    //   deleting some short-lived nodes without ever publishing them.
    // * But in order to do that we have to be in a runnable state, so
    //   we potentially force a native state thread to go wait for the GC.
    if (node.owner_ == this) {
        queue_.erase(node.position_);
    }
}

// static
mm::SpecialRefRegistry& mm::SpecialRefRegistry::instance() noexcept {
    return GlobalData::Instance().specialRefRegistry();
}

mm::SpecialRefRegistry::Node* mm::SpecialRefRegistry::nextRoot(Node* current) noexcept {
    RuntimeAssert(current != nullptr, "current cannot be null");
    RuntimeAssert(current != rootsTail(), "current cannot be tail");
    Node* candidate = current->nextRoot_.load(std::memory_order_relaxed);
    // Not an infinite loop, `candidate` always moves forward and since insertions can only
    // happen in the head, they will always happen before `candidate`.
    while (true) {
        RuntimeAssert(candidate != nullptr, "candidate cannot be null");
        if (candidate == rootsTail())
            // Reached tail, nothing to do anymore
            return candidate;
        if (candidate->rc_.load(std::memory_order_relaxed) > 0) {
            // Keeping acquire-release for nextRoot_.
            std::atomic_thread_fence(std::memory_order_acquire);
            // Perfectly good node. Stop right there.
            return candidate;
        }
        // Bad node. Let's remove it from the roots.
        // Racy if someone concurrently inserts in the middle. Or iterates.
        // But we don't have that here. Inserts are only in the beginning.
        // Iteration also happens only here.
        auto [candidatePrev, candidateNext] = eraseFromRoots(current, candidate);
        // We removed candidate. But should we have?
        if (candidate->rc_.load(std::memory_order_relaxed) > 0) {
            RuntimeAssert(candidate->obj_.load(std::memory_order_relaxed) != nullptr, "candidate cannot have a null obj_");
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
        // and our `current` then is our best guess at the previous node of the `candidate`.
        current = candidatePrev;
        candidate = candidateNext;
        // `current` has either moved forward or stayed where it is.
        // `candidate` has definitely moved forward.
        // `current` is only used in `eraseFromRoots` which itself ensures that no
        // infinite loop can happen.
        // So, this loop is also not infinite.
    }
}

std::pair<mm::SpecialRefRegistry::Node*, mm::SpecialRefRegistry::Node*> mm::SpecialRefRegistry::eraseFromRoots(
        Node* prev, Node* node) noexcept {
    RuntimeAssert(node != rootsHead(), "node cannot be head");
    RuntimeAssert(node != rootsTail(), "node cannot be tail");
    Node* next = node->nextRoot_.load(std::memory_order_acquire);
    RuntimeAssert(next != nullptr, "node@%p next cannot be null", node);
    do {
        Node* prevExpectedNext = node;
        bool removed =
                prev->nextRoot_.compare_exchange_strong(prevExpectedNext, next, std::memory_order_release, std::memory_order_acquire);
        if (removed) {
            auto* actualNext = node->nextRoot_.exchange(nullptr, std::memory_order_acq_rel);
            RuntimeAssert(actualNext == next, "node@%p next expected %p actual %p", node, next, actualNext);
            return {prev, next};
        }
        prev = prevExpectedNext;
        RuntimeAssert(prev != rootsHead(), "prev cannot be head");
        RuntimeAssert(prev != rootsTail(), "prev cannot be tail");
        // We moved `prev` forward, nothing can insert after `prev` anymore, this
        // cannot be an infinite loop, then.
    } while (true);
}

void mm::SpecialRefRegistry::insertIntoRootsHead(Node& node) noexcept {
    Node* next = rootsHead()->nextRoot_.load(std::memory_order_acquire);
    Node* nodeExpectedNext = nullptr;
    do {
        RuntimeAssert(next != nullptr, "head's next cannot be null");
        if (!node.nextRoot_.compare_exchange_strong(nodeExpectedNext, next, std::memory_order_release, std::memory_order_acquire)) {
            // So:
            // * `node` is already in the roots list
            // * some other thread is inserting it in the roots list
            // * GC thread may be removing it from the roots list, but
            //   will recheck rc afterwards and insert it back if needed
            // In either case, do not touch anything anymore here.
            return;
        }
        // CAS was successfull, so we need to update the expected value of node.nextRoot_
        nodeExpectedNext = next;
    } while (!rootsHead()->nextRoot_.compare_exchange_weak(next, &node, std::memory_order_release, std::memory_order_acquire));
}

std_support::list<mm::SpecialRefRegistry::Node>::iterator mm::SpecialRefRegistry::findAliveNode(
        std_support::list<Node>::iterator it) noexcept {
    while (it != all_.end() && it->rc_.load(std::memory_order_relaxed) == Node::disposedMarker) {
        // Synchronization with `Node::dispose()`
        std::atomic_thread_fence(std::memory_order_acquire);
        // Removing disposed nodes.
        if (it->nextRoot_.load(std::memory_order_relaxed) != nullptr) {
            // Wait, it's in the roots list. Lets wait until the next GC
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
