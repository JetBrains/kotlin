/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MULTI_SOURCE_QUEUE_H
#define RUNTIME_MULTI_SOURCE_QUEUE_H

#include <list>
#include <mutex>

#include "Mutex.hpp"

namespace kotlin {

// A queue that is constructed by collecting subqueues from several `Producer`s.
template <typename T>
class MultiSourceQueue {
public:
    class Producer {
    public:
        explicit Producer(MultiSourceQueue& owner) noexcept : owner_(owner) {}

        ~Producer() { Publish(); }

        void Insert(const T& value) noexcept { queue_.push_back(value); }

        // Merge `this` queue with owning `MultiSourceQueue`. `this` will have empty queue after the call.
        // This call is performed without heap allocations. TODO: Test that no allocations are happening.
        void Publish() noexcept { owner_.Collect(*this); }

    private:
        friend class MultiSourceQueue;

        MultiSourceQueue& owner_; // weak
        std::list<T> queue_;
    };

    using Iterator = typename std::list<T>::iterator;

    class Iterable : MoveOnly {
    public:
        explicit Iterable(MultiSourceQueue& owner) noexcept : owner_(owner), guard_(owner_.mutex_) {}

        Iterator begin() noexcept { return owner_.commonQueue_.begin(); }
        Iterator end() noexcept { return owner_.commonQueue_.end(); }

    private:
        MultiSourceQueue& owner_; // weak
        std::unique_lock<SimpleMutex> guard_;
    };

    // Lock MultiSourceQueue for safe iteration.
    Iterable Iter() noexcept { return Iterable(*this); }

private:
    void Collect(Producer& producer) noexcept {
        std::lock_guard<SimpleMutex> guard(mutex_);
        commonQueue_.splice(commonQueue_.end(), producer.queue_);
    }

    // Using `std::list` as it allows to implement `Collect` without memory allocations,
    // which is important for GC mark phase.
    std::list<T> commonQueue_;
    SimpleMutex mutex_;
};

} // namespace kotlin

#endif // RUNTIME_MULTI_SOURCE_QUEUE_H
