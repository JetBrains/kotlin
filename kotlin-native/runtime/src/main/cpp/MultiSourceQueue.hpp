/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MULTI_SOURCE_QUEUE_H
#define RUNTIME_MULTI_SOURCE_QUEUE_H

#include <list>

namespace kotlin {

// A queue that is constructed by collecting subqueues from several `Producer`s.
template <typename T>
class MultiSourceQueue {
public:
    class Producer {
    public:
        void Insert(const T& value) noexcept { queue_.push_back(value); }

    private:
        friend class MultiSourceQueue;

        std::list<T> queue_;
    };

    using Iterator = typename std::list<T>::iterator;

    Iterator begin() noexcept { return commonQueue_.begin(); }
    Iterator end() noexcept { return commonQueue_.end(); }

    // Merge `producer`s queue with `this`. `producer` will have empty queue after the call.
    // This call is performed without heap allocations. TODO: Test that no allocations are happening.
    void Collect(Producer* producer) noexcept { commonQueue_.splice(commonQueue_.end(), producer->queue_); }

private:
    // Using `std::list` as it allows to implement `Collect` without memory allocations,
    // which is important for GC mark phase.
    std::list<T> commonQueue_;
};

} // namespace kotlin

#endif // RUNTIME_MULTI_SOURCE_QUEUE_H
