/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MULTI_SOURCE_QUEUE_H
#define RUNTIME_MULTI_SOURCE_QUEUE_H

#include <atomic>
#include <list>
#include <mutex>

#include "Mutex.hpp"

namespace kotlin {

// A queue that is constructed by collecting subqueues from several `Producer`s.
template <typename T>
class MultiSourceQueue {
public:
    class Producer;

    // TODO: Consider switching from `std::list` to `SingleLockList` to hide the constructor
    // and to not store the iterator.
    class Node : private Pinned {
    public:
        Node(const T& value, Producer* owner) noexcept : value_(value), owner_(owner) {}

        T& operator*() noexcept { return value_; }

    private:
        friend class MultiSourceQueue;

        T value_;
        std::atomic<Producer*> owner_; // `nullptr` signifies that `MultiSourceQueue` owns it.
        typename std::list<Node>::iterator position_;
    };

    class Producer {
    public:
        explicit Producer(MultiSourceQueue& owner) noexcept : owner_(owner) {}

        ~Producer() { Publish(); }

        Node* Insert(const T& value) noexcept {
            queue_.emplace_back(value, this);
            auto& node = queue_.back();
            node.position_ = std::prev(queue_.end());
            return &node;
        }

        void Erase(Node* node) noexcept {
            if (node->owner_ == this) {
                // If we own it, delete it immediately.
                queue_.erase(node->position_);
                return;
            }
            // If it's owned by the global queue or some other `Producer`, queue it.
            deletionQueue_.push_back(node);
        }

        // Merge `this` queue with owning `MultiSourceQueue`. `this` will have empty queue after the call.
        // This call is performed without heap allocations. TODO: Test that no allocations are happening.
        void Publish() noexcept {
            for (auto& node : queue_) {
                node.owner_ = nullptr;
            }
            std::lock_guard<SpinLock> guard(owner_.mutex_);
            owner_.queue_.splice(owner_.queue_.end(), queue_);
            owner_.deletionQueue_.splice(owner_.deletionQueue_.end(), deletionQueue_);
        }

    private:
        MultiSourceQueue& owner_; // weak
        std::list<Node> queue_;
        std::list<Node*> deletionQueue_;
    };

    class Iterator {
    public:
        T& operator*() noexcept { return **position_; }

        Iterator& operator++() noexcept {
            ++position_;
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return position_ == rhs.position_; }

        bool operator!=(const Iterator& rhs) const noexcept { return position_ != rhs.position_; }

    private:
        friend class MultiSourceQueue;

        explicit Iterator(const typename std::list<Node>::iterator& position) noexcept : position_(position) {}

        typename std::list<Node>::iterator position_;
    };

    class Iterable : MoveOnly {
    public:
        Iterator begin() noexcept { return Iterator(owner_.queue_.begin()); }
        Iterator end() noexcept { return Iterator(owner_.queue_.end()); }

    private:
        friend class MultiSourceQueue;

        explicit Iterable(MultiSourceQueue& owner) noexcept : owner_(owner), guard_(owner_.mutex_) {}

        MultiSourceQueue& owner_; // weak
        std::unique_lock<SpinLock> guard_;
    };

    // Lock `MultiSourceQueue` for safe iteration. If element was scheduled for deletion,
    // it'll still be iterated. Use `ApplyDeletions` to remove those elements.
    Iterable Iter() noexcept { return Iterable(*this); }

    // Lock `MultiSourceQueue` and apply deletions. Only deletes elements that were published.
    void ApplyDeletions() noexcept {
        std::lock_guard<SpinLock> guard(mutex_);
        std::list<Node*> remainingDeletions;

        auto it = deletionQueue_.begin();
        while (it != deletionQueue_.end()) {
            auto next = std::next(it);
            Node* node = *it;

            if (node->owner_ != nullptr) {
                // If the `Node` is still owned by some `Producer`, skip it.
                remainingDeletions.splice(remainingDeletions.end(), deletionQueue_, it);
            } else {
                queue_.erase(node->position_);
                // `node` is invalid after this
            }

            it = next;
        }
        deletionQueue_ = std::move(remainingDeletions);
    }

private:
    // Using `std::list` as it allows to implement `Collect` without memory allocations,
    // which is important for GC mark phase.
    std::list<Node> queue_;
    std::list<Node*> deletionQueue_;
    SpinLock mutex_;
};

} // namespace kotlin

#endif // RUNTIME_MULTI_SOURCE_QUEUE_H
