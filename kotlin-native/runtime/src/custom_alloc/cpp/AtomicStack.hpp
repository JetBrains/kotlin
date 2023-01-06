/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_ATOMICSTACK_HPP_
#define CUSTOM_ALLOC_CPP_ATOMICSTACK_HPP_

#include <atomic>

#include "CustomLogging.hpp"
#include "KAssert.h"

namespace kotlin::alloc {

template <class T>
class AtomicStack {
public:
    // Pop() is not fully thread-safe, in that the returned page must not be
    // immediately freed, if another thread might be simultaneously Popping
    // from the same stack. As of writing this comment, this is handled by only
    // freeing pages during STW.
    T* Pop() noexcept {
        T* elm = stack_.load(std::memory_order_acquire);
        while (elm && !stack_.compare_exchange_weak(elm, elm->next_, std::memory_order_acq_rel)) {}
        CustomAllocDebug("AtomicStack(%p)::Pop() = %p", this, elm);
        return elm;
    }

    void Push(T* elm) noexcept {
        T* head = nullptr;
        do {
            elm->next_ = head;
        } while (!stack_.compare_exchange_weak(head, elm, std::memory_order_acq_rel));
    }

    void TransferAllFrom(AtomicStack<T>& other) noexcept {
        // Clear out the `other` stack.
        T* otherHead = nullptr;
        while (!other.stack_.compare_exchange_weak(otherHead, nullptr, std::memory_order_acq_rel)) {}
        // If the `other` stack was empty, do nothing.
        if (!otherHead) return;
        // If `this` stack is empty, just copy the `other` stack over 
        T* thisHead = nullptr;
        if (stack_.compare_exchange_strong(thisHead, otherHead, std::memory_order_acq_rel)) {
            return;
        }
        // `this` stack is not empty. Find the tail of `other`. If no deletions are performed, this is safe.
        T* otherTail = otherHead;
        while (otherTail->next_) otherTail = otherTail->next_;
        // can't be because of the loop above
        RuntimeAssert(otherTail->next_ == nullptr, "otherTail->next_ must be a tail");
        // Now make `otherTail->next_` point to the current head of `this` and
        // simultaneously make `otherHead` the new current head.
        do {
            otherTail->next_ = thisHead;
        } while (!stack_.compare_exchange_weak(thisHead, otherHead, std::memory_order_acq_rel));
    }

    bool isEmpty() noexcept { return stack_.load(std::memory_order_relaxed) == nullptr; }

    ~AtomicStack() noexcept {
        RuntimeAssert(isEmpty(), "AtomicStack must be empty when destroyed");
    }

private:
    std::atomic<T*> stack_{nullptr};
};

} // namespace kotlin::alloc

#endif
