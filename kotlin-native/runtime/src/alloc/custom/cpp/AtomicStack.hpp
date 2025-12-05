/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_ATOMICSTACK_HPP_
#define CUSTOM_ALLOC_CPP_ATOMICSTACK_HPP_

#include <atomic>
#include <vector>

#include "KAssert.h"
#include "std_support/Atomic.hpp"
#include "Utils.hpp"

namespace kotlin::alloc {

template <class T>
class AtomicStack : MoveOnly {
public:
    AtomicStack() noexcept = default;

    AtomicStack(AtomicStack&& other) noexcept : head_(other.atomicHead().exchange(nullptr, std::memory_order_acq_rel)) {}

    AtomicStack& operator=(AtomicStack&& other) noexcept {
        // Not using swap idiom, because implementing swap of two atomics requires DCAS or locks.
        auto newHead = other.atomicHead().exchange(nullptr, std::memory_order_acq_rel);
        atomicHead().store(newHead, std::memory_order_release);
        return *this;
    }

    // Pop() is not fully thread-safe, in that the returned page must not be
    // immediately freed, if another thread might be simultaneously Popping
    // from the same stack. As of writing this comment, this is handled by only
    // freeing pages during STW.
    T* Pop() noexcept {
        T* elm = atomicHead().load(std::memory_order_acquire);
        while (true) {
            if (!elm) {
                return nullptr;
            }
            auto* elmNext = elm->atomicNext().load(std::memory_order_relaxed);
            if (atomicHead().compare_exchange_weak(elm, elmNext, std::memory_order_acq_rel)) {
                return elm;
            }
        }
    }

    void Push(T* elm) noexcept {
        T* head = atomicHead().load(std::memory_order_acquire);
        do {
            elm->atomicNext().store(head, std::memory_order_relaxed);
        } while (!atomicHead().compare_exchange_weak(head, elm, std::memory_order_acq_rel));
    }

    T* PopNonAtomic() noexcept {
        if (T* elm = head_) {
            head_ = elm->next_;
            return elm;
        }
        return nullptr;
    }

    void PushNonAtomic(T* elm) noexcept {
        elm->next_ = head_;
        head_ = elm;
    }

    // This will put the contents of the other stack on top of this stack
    void TransferAllFrom(AtomicStack<T> other) noexcept {
        T* otherHead = other.atomicHead().exchange(nullptr, std::memory_order_relaxed);
        // If the `other` stack was empty, do nothing.
        if (!otherHead) return;
        // If `this` stack is empty, just copy the `other` stack over
        T* thisHead = nullptr;
        if (atomicHead().compare_exchange_strong(thisHead, otherHead, std::memory_order_acq_rel)) {
            return;
        }
        // `this` stack is not empty. Find the tail of `other`. If no deletions are performed, this is safe.
        T* otherTail = otherHead;
        while (auto* next = otherTail->next_) otherTail = next;
        // can't be because of the loop above
        RuntimeAssert(otherTail->next_ == nullptr, "otherTail->next_ must be a tail");
        // Now make `otherTail->next_` point to the current head of `this` and
        // simultaneously make `otherHead` the new current head.
        do {
            otherTail->atomicNext().store(thisHead, std::memory_order_relaxed);
        } while (!atomicHead().compare_exchange_weak(thisHead, otherHead, std::memory_order_release, std::memory_order_relaxed));
    }

    bool isEmpty() const noexcept { return atomicHead().load(std::memory_order_relaxed) == nullptr; }

    // Not thread-safe. Named like this to make AtomicStack compatible with FinalizerQueue
    size_t size() {
        size_t size = 0;
        for (T* elm = head_; elm != nullptr; elm = elm->next_) {
            ++size;
        }
        return size;
    }

    ~AtomicStack() { RuntimeAssert(isEmpty(), "AtomicStack must be empty on destruction"); }

    // Test method
    std::vector<T*> GetElements() {
        std::vector<T*> elements;
        TraverseElements([&](T* element) { elements.push_back(element); });
        return elements;
    }

    template <typename F>
    void TraverseElements(F process) noexcept(noexcept(process(std::declval<T*>()))) {
        T* elm = atomicHead().load();
        while (elm) {
            process(elm);
            elm = elm->atomicNext().load();
        }
    }

private:
    auto atomicHead() noexcept { return std_support::atomic_ref{head_}; }
    auto atomicHead() const noexcept { return std_support::atomic_ref{head_}; }

    T* head_ = nullptr;
};

} // namespace kotlin::alloc

#endif
