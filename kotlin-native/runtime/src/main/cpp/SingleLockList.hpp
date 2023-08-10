/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_SINGLE_LOCK_LIST_H
#define RUNTIME_SINGLE_LOCK_LIST_H

#include <cstddef>
#include <iterator>
#include <memory>
#include <mutex>
#include <type_traits>

#include "Mutex.hpp"
#include "Utils.hpp"
#include "std_support/Memory.hpp"

namespace kotlin {

// TODO: Consider different locking mechanisms.
template <typename Value, typename Mutex, typename Allocator = std::allocator<Value>>
class SingleLockList : private Pinned {
public:
    class Node;

private:
    using NodeAllocator = typename std::allocator_traits<Allocator>::template rebind_alloc<Node>;
    using NodeOwner = std::unique_ptr<Node, std_support::allocator_deleter<Node, NodeAllocator>>;

public:
    // TODO: Maybe just hide `Node` altogether?
    class Node : private Pinned {
    public:
        template <typename... Args>
        explicit Node(const Allocator& allocator, Args&&... args) noexcept :
            value_(std::forward<Args>(args)...), next_(std_support::nullptr_unique<Node>(allocator)) {}

        Value* Get() noexcept { return &value_; }

    private:
        friend class SingleLockList;

        Value value_;
        NodeOwner next_;
        Node* previous_ = nullptr; // weak
    };

    class Iterator {
    public:
        using difference_type = std::ptrdiff_t;
        using value_type = Value;
        using pointer = Value*;
        using reference = Value&;
        using iterator_category = std::forward_iterator_tag;

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
        explicit Iterable(SingleLockList* list) noexcept : list_(list), guard_(list->Lock()) {}

        Iterator begin() noexcept { return Iterator(list_->root_.get()); }

        Iterator end() noexcept { return Iterator(nullptr); }

    private:
        SingleLockList* list_;
        std::unique_lock<Mutex> guard_;
    };

    SingleLockList() noexcept = default;

    explicit SingleLockList(const Allocator& allocator) noexcept :
        allocator_(allocator), root_(std_support::nullptr_unique<Node, NodeAllocator>(allocator)) {}

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
        auto node = std_support::allocate_unique<Node>(allocator_, allocator_, std::forward<Args>(args)...);
        auto* nodePtr = node.get();
        std::lock_guard<Mutex> guard(mutex_);
        AssertCorrectUnsafe();
        if (root_) {
            root_->previous_ = nodePtr;
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
            auto next = std::move(node->next_);
            root_ = std::move(next);
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
    // for (auto& value: list.LockForIter()) {
    //    // Do something with `value`, there's a guarantee that it'll not be
    //    // destroyed mid-iteration.
    // }
    // // At this point `list` is unlocked.
    Iterable LockForIter() noexcept { return Iterable(this); }

    std::unique_lock<Mutex> Lock() noexcept { return std::unique_lock(mutex_); }

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

    [[no_unique_address]] NodeAllocator allocator_;
    NodeOwner root_;
    Node* last_ = nullptr;
    Mutex mutex_;
};

} // namespace kotlin

#endif // RUNTIME_SINGLE_LOCK_LIST_H
