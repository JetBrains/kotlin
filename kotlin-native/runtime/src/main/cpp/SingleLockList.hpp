/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_SINGLE_LOCK_LIST_H
#define RUNTIME_SINGLE_LOCK_LIST_H

#include <cstddef>
#include <memory>
#include <mutex>

#include "Alloc.h"
#include "Mutex.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {

// TODO: Consider different locking mechanisms.
template <typename Value, typename Mutex = SpinLock>
class SingleLockList : private Pinned {
public:
    class Node;

private:
    class NodeDeleter {
    public:
        void operator()(Node* node) const { delete node; }
    };

    using NodeOwner = std::unique_ptr<Node, NodeDeleter>;

public:
    class Node : private Pinned, public KonanAllocatorAware {
    public:
        Value* Get() noexcept { return &value_; }

    private:
        friend class SingleLockList;

        template <typename... Args>
        Node(Args&&... args) noexcept : value_(std::forward<Args>(args)...) {}

        // Make sure `Node` can only be deleted by `SingleLockList` itself.
        ~Node() = default;

        Value value_;
        NodeOwner next_;
        Node* previous_ = nullptr; // weak
    };

    class Iterator {
    public:
        explicit Iterator(Node* node) noexcept : node_(node) {}

        Value& operator*() noexcept { return node_->value_; }

        Iterator& operator++() noexcept {
            node_ = node_->next_.get();
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return node_ == rhs.node_; }

        bool operator!=(const Iterator& rhs) const noexcept { return node_ != rhs.node_; }

    private:
        Node* node_;
    };

    class Iterable : private MoveOnly {
    public:
        explicit Iterable(SingleLockList* list) noexcept : list_(list), guard_(list->mutex_) {}

        Iterator begin() noexcept { return Iterator(list_->root_.get()); }

        Iterator end() noexcept { return Iterator(nullptr); }

    private:
        SingleLockList* list_;
        std::unique_lock<Mutex> guard_;
    };

    ~SingleLockList() {
        AssertCorrectUnsafe();
        // Make sure not to blow up the stack by nested `~Node` calls.
        for (auto node = std::move(root_); node != nullptr; node = std::move(node->next_)) {
        }
        last_ = nullptr;
        AssertCorrectUnsafe();
    }

    // TODO: Consider making `Emplace` append to `last_`.
    template <typename... Args>
    Node* Emplace(Args&&... args) noexcept {
        auto* nodePtr = new Node(std::forward<Args>(args)...);
        NodeOwner node(nodePtr);
        std::lock_guard<Mutex> guard(mutex_);
        AssertCorrectUnsafe();
        if (root_) {
            root_->previous_ = node.get();
        } else {
            last_ = nodePtr;
        }
        node->next_ = std::move(root_);
        root_ = std::move(node);
        AssertCorrectUnsafe();
        return nodePtr;
    }

    // Using `node` including its referred `Value` after `Erase` is undefined behaviour.
    void Erase(Node* node) noexcept {
        std::lock_guard<Mutex> guard(mutex_);
        AssertCorrectUnsafe();
        if (last_ == node) {
            last_ = node->previous_;
        }
        if (root_.get() == node) {
            root_ = std::move(node->next_);
            if (root_) {
                root_->previous_ = nullptr;
            }
            AssertCorrectUnsafe();
            return;
        }
        auto* previous = node->previous_;
        RuntimeAssert(previous != nullptr, "Only the root node doesn't have the previous node");
        auto ownedNode = std::move(previous->next_);
        previous->next_ = std::move(node->next_);
        if (auto& next = previous->next_) {
            next->previous_ = previous;
        }
        AssertCorrectUnsafe();
    }

    // Returned value locks `this` to perform safe iteration. `this` unlocks when
    // `Iterable` gets out of scope. Example usage:
    // for (auto& value: list.Iter()) {
    //    // Do something with `value`, there's a guarantee that it'll not be
    //    // destroyed mid-iteration.
    // }
    // // At this point `list` is unlocked.
    Iterable Iter() noexcept { return Iterable(this); }

private:
    // Expects `mutex_` to be held by the current thread.
    ALWAYS_INLINE void AssertCorrectUnsafe() const noexcept {
        if (root_ == nullptr) {
            RuntimeAssert(last_ == nullptr, "last_ must be null");
        } else {
            RuntimeAssert(root_->previous_ == nullptr, "root_ must not have previous_");
            RuntimeAssert(last_ != nullptr, "last_ must not be null");
            RuntimeAssert(last_->next_ == nullptr, "last_ must not have next_");
        }
    }

    NodeOwner root_;
    Node* last_ = nullptr;
    Mutex mutex_;
};

} // namespace kotlin

#endif // RUNTIME_SINGLE_LOCK_LIST_H
