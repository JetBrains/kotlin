/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <iterator>
#include <limits>
#include <optional>

#include "KAssert.h"
#include "Utils.hpp"

namespace kotlin {

template <typename T>
struct DefaultIntrusiveForwardListTraits {
    static T* next(const T& value) noexcept { return value.next(); }

    static void setNext(T& value, T* next) noexcept { value.setNext(next); }

    static bool trySetNext(T& value, T* next) noexcept { return value.trySetNext(next); }
};

// Intrusive variant of `std::forward_list`.
//
// The container does not own nodes. The list structure is maintained by `T` itself via
// `Traits`. `Traits` must provide 3 operations:
//
// static T* next(const T& value); // obtain the next pointer
// static void setNext(T& value, T* next); // set the next pointer
// static bool trySetNext(T& value, T* next); // try to set the next pointer or return `false` if it's not possible. Used by `try_push_front`.
// The default `Traits` implementation expects `T` to provide all operations as member functions.
//
// Notable differences from regular containers:
// * To put `T` into different intrusive lists simultaneously it should
//   provide custom `Traits` implementation to regulate which next pointer is used
//   by which list.
// * It's not possible to keep the same `T` twice in the same list.
// * The container is move-only and moving invalidates `before_begin` iterator.
// * `insert_after`, `erase_after` take `iterator` instead of `const_iterator`,
//   because they require mutability.
// * When the node is inserted into the container its next pointer is set
//   to something non-null, but when its removed from the container nothing
//   nulls the next pointer.
// * `Traits::trySetNext`, `Traits::setNext` and `Traits::next` must be callable even on uninitialized `T` (i.e. they should only access storage inside `T`).
template <typename T, typename Traits = DefaultIntrusiveForwardListTraits<T>>
class intrusive_forward_list : private MoveOnly {
public:
    using value_type = T;
    using size_type = size_t;
    using difference_type = ptrdiff_t;
    using reference = value_type&;
    using const_reference = const value_type&;
    using pointer = value_type*;
    using const_pointer = const value_type*;

    class iterator {
    public:
        using difference_type = intrusive_forward_list::difference_type;
        using value_type = intrusive_forward_list::value_type;
        using pointer = intrusive_forward_list::pointer;
        using reference = intrusive_forward_list::reference;
        using iterator_category = std::forward_iterator_tag;

        iterator() noexcept = default;
        iterator(const iterator&) noexcept = default;
        iterator& operator=(const iterator&) noexcept = default;

        reference operator*() noexcept { return *node_; }

        pointer operator->() noexcept { return node_; }

        iterator& operator++() noexcept {
            node_ = next(node_);
            return *this;
        }

        iterator operator++(int) noexcept {
            auto result = *this;
            ++(*this);
            return result;
        }

        bool operator==(const iterator& rhs) const noexcept { return node_ == rhs.node_; }
        bool operator!=(const iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        friend class intrusive_forward_list;

        explicit iterator(pointer node) noexcept : node_(node) {}

        intrusive_forward_list::pointer node_ = nullptr;
    };

    class const_iterator {
    public:
        using difference_type = intrusive_forward_list::difference_type;
        using value_type = const intrusive_forward_list::value_type;
        using pointer = intrusive_forward_list::const_pointer;
        using reference = intrusive_forward_list::const_reference;
        using iterator_category = std::forward_iterator_tag;

        const_iterator() noexcept = default;
        const_iterator(const const_iterator&) noexcept = default;
        const_iterator& operator=(const const_iterator&) noexcept = default;

        const_iterator(iterator it) noexcept : node_(it.node_) {}

        reference operator*() noexcept { return *node_; }

        pointer operator->() noexcept { return node_; }

        const_iterator& operator++() noexcept {
            node_ = next(node_);
            return *this;
        }

        const_iterator operator++(int) noexcept {
            auto result = *this;
            ++(*this);
            return result;
        }

        bool operator==(const const_iterator& rhs) const noexcept { return node_ == rhs.node_; }
        bool operator!=(const const_iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        friend class intrusive_forward_list;

        explicit const_iterator(pointer node) noexcept : node_(node) {}

        pointer node_ = nullptr;
    };

    // Complexity: O(1)
    intrusive_forward_list() noexcept { clear(); }

    // Complexity: O(1)
    intrusive_forward_list(intrusive_forward_list&& rhs) noexcept {
        // Since tail() is shared, there's no need to update the last node's next_.
        setNext(head(), next(rhs.head()));
        rhs.clear();
    }


    // `InputIt` should dereference into `T&`.
    // Complexity: O(last - first)
    template <typename InputIt>
    intrusive_forward_list(InputIt first, InputIt last) noexcept : intrusive_forward_list() {
        assign(std::move(first), std::move(last));
    }

    // Complexity: O(1)
    ~intrusive_forward_list() = default;

    // Complexity: O(1)
    intrusive_forward_list& operator=(intrusive_forward_list&& rhs) noexcept {
        intrusive_forward_list tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    // Complexity: O(1)
    void swap(intrusive_forward_list& rhs) noexcept {
        // Since tail() is shared, there's no need to swap the last nodes' next_.
        using std::swap;
        auto thisNext = next(head());
        auto rhsNext = next(rhs.head());
        swap(thisNext, rhsNext);
        setNext(head(), thisNext);
        setNext(rhs.head(), rhsNext);
    }

    // Rewrite the contents of `this` with nodes from range `[first, last)`.
    // `InputIt` should dereference into `T&`.
    // Complexity: O(last - first)
    template <typename InputIt>
    void assign(InputIt first, InputIt last) noexcept {
        clear();
        insert_after(before_begin(), std::move(first), std::move(last));
    }

    // Complexity: O(1)
    reference front() noexcept { return *next(head()); }
    // Complexity: O(1)
    const_reference front() const noexcept { return *next(head()); }

    // Iterator before the first node. Cannot be dereferenced.
    // Complexity: O(1)
    iterator before_begin() noexcept { return iterator(head()); }
    // Iterator before the first node. Cannot be dereferenced.
    // Complexity: O(1)
    const_iterator before_begin() const noexcept { return const_iterator(head()); }
    // Iterator before the first node. Cannot be dereferenced.
    // Complexity: O(1)
    const_iterator cbefore_begin() const noexcept { return const_iterator(head()); }

    // Complexity: O(1)
    iterator begin() noexcept { return iterator(next(head())); }
    // Complexity: O(1)
    const_iterator begin() const noexcept { return const_iterator(next(head())); }
    // Complexity: O(1)
    const_iterator cbegin() const noexcept { return const_iterator(next(head())); }

    // Complexity: O(1)
    iterator end() noexcept { return iterator(tail()); }
    // Complexity: O(1)
    const_iterator end() const noexcept { return const_iterator(tail()); }
    // Complexity: O(1)
    const_iterator cend() const noexcept { return const_iterator(tail()); }

    // Complexity: O(1)
    bool empty() const noexcept { return next(head()) == tail(); }

    // Complexity: O(1)
    size_type max_size() const noexcept { return std::numeric_limits<size_type>::max(); }

    // Complexity: O(1)
    void clear() noexcept { setNext(head(), tail()); }

    // Insert `value` after `pos`. `pos` can be in range `[before_begin(), end())`.
    // Returns iterator to the newly inserted element
    // Complexity: O(1)
    iterator insert_after(iterator pos, reference value) noexcept {
        RuntimeAssert(pos != end(), "Attempted to insert_after end()");
        RuntimeAssert(pos != iterator(), "Attempted to insert_after empty iterator");
        setNext(&value, next(pos.node_));
        setNext(pos.node_, &value);
        return iterator(&value);
    }

    // Insert `[first, last)` after `pos`. `pos` can be in range `[before_begin(), end())`.
    // `InputIt` should dereference into `T&`.
    // Returns iterator to the last inserted element.
    // Complexity: O(last - first)
    template <typename InputIt>
    iterator insert_after(iterator pos, InputIt first, InputIt last) noexcept {
        RuntimeAssert(pos != end(), "Attempted to insert_after end()");
        RuntimeAssert(pos != iterator(), "Attempted to insert_after empty iterator");
        pointer nextNode = next(pos.node_);
        pointer prevNode = pos.node_;
        for (auto it = first; it != last; ++it) {
            pointer newNode = &*it;
            setNext(prevNode, newNode);
            prevNode = newNode;
        }
        setNext(prevNode, nextNode);
        return iterator(prevNode);
    }

    // Erase a node after `pos`. `pos` can be in range `[begin(), end() - 1)`.
    // This does not destroy the erased element, and it does not change its next pointer.
    // Returns iterator to the next node of the erased one.
    // Complexity: O(1)
    iterator erase_after(iterator pos) noexcept {
        RuntimeAssert(pos != end(), "Attempted to erase_after end()");
        RuntimeAssert(pos != iterator(), "Attempted to erase_after empty iterator");
        RuntimeAssert(next(pos.node_) != tail(), "Attempted to erase_after the last node");
        pointer nextNode = next(next(pos.node_));
        setNext(pos.node_, nextNode);
        return iterator(nextNode);
    }

    // Erase all nodes in range `(first, last)`.
    // `first` can be in range `[before_begin(), last)`.
    // `last` can be in range `(first, end()]`.
    // This does not destroy erased elements, and it does not change their next pointers.
    // Returns iterator to the next node of the last erased (i.e. returns `last`).
    // Complexity: O(1)
    iterator erase_after(iterator first, iterator last) noexcept {
        RuntimeAssert(first != end(), "Attempted to erase_after starting at end()");
        RuntimeAssert(first != iterator(), "Attempted to erase_after starting at empty iterator");
        RuntimeAssert(next(first.node_) != tail(), "Attempted to erase_after starting at the last node");
        RuntimeAssert(last != iterator(), "Attempted to erase_after ending at empty iterator");
        setNext(first.node_, last.node_);
        return last;
    }

    // Insert a new node to the front.
    // Equivalent to `insert_after(before_begin(), value)`.
    // Complexity: O(1)
    void push_front(reference value) noexcept { insert_after(before_begin(), value); }

    // Try to insert a new node to the front.
    // When setting the next node of `value` uses `Traits::trySetNext`.
    // If `Traits::trySetNext` returns `true`, this operates like `push_front` and returns `true`.
    // If `Traits::trySetNext` returns `false`, this doesn't change anything else and returns `false`.
    // Complexity: O(1)
    bool try_push_front(reference value) noexcept { return try_insert_after(before_begin(), value) != std::nullopt; }

    // Erase a node at the front.
    // This does not destroy the erased node and does not change its next pointer.
    // Complexity: O(1)
    void pop_front() noexcept { erase_after(before_begin()); }

    // Try to erase node at the front.
    // If this list is empty, returns `nullptr`.
    // Otherwise returns pointer to the node at the front and erases it.
    // This does not destroy the erased node and does not change its next pointer.
    // Complexity: O(1)
    pointer try_pop_front() noexcept {
        pointer top = next(head());
        if (top == tail()) {
            return nullptr;
        }
        setNext(head(), next(top));
        return top;
    }

    // Erase node `value`.
    // If the `value` is not in the list, does nothing.
    // This does not destroy the erased node and does not change its next pointer.
    // Complexity: O(n)
    void remove(reference value) noexcept {
        // TODO: no need to move on after finding the first match.
        return remove_if([&value](const_reference x) noexcept { return &x == &value; });
    }

    // Erase all nodes satisfying predicate `P`.
    // This does not destroy erased nodes and does not change their next pointer.
    // Complexity: O(n)
    template <typename P>
    void remove_if(P p) noexcept(noexcept(p(std::declval<const_reference>()))) {
        pointer prev = head();
        pointer node = next(prev);
        while (node != tail()) {
            if (p(*node)) {
                // The node is being removed.
                node = next(node);
                setNext(prev, node);
            } else {
                // The node is staying.
                prev = node;
                node = next(node);
            }
        }
    }

    // Moves at most `maxCount` first elements from the range `(firstExcl, lastExcl)` after the element pointed by `insertAfter`.
    // No elements are copied or moved, only the internal pointers of the list nodes are re-pointed.
    // The behavior is undefined if `insertAfter` is an iterator in the range `[firstExcl, lastExcl)`.
    // Complexity: O(min(maxCount, std::distance(first, last)))
    size_type splice_after(iterator insertAfter, iterator firstExcl, iterator lastExcl, size_type maxCount) {
        auto firstIncl = std::next(firstExcl);
        if (firstIncl == lastExcl) return 0;
        auto lastIncl = firstExcl;
        size_type count = 0;
        while (std::next(lastIncl) != lastExcl && count < maxCount) {
            RuntimeAssert(lastIncl != insertAfter, "Position to splice after must not be in the spliced range");
            ++lastIncl;
            ++count;
        }
        lastExcl = std::next(lastIncl);
        setNext(firstExcl.node_, lastExcl.node_);
        setNext(lastIncl.node_, next(insertAfter.node_));
        setNext(insertAfter.node_, firstIncl.node_);
        return count;
    }

private:
    static pointer next(const_pointer node) noexcept { return Traits::next(*node); }

    static void setNext(pointer node, pointer next) noexcept { return Traits::setNext(*node, next); }

    static bool trySetNext(pointer node, pointer next) noexcept { return Traits::trySetNext(*node, next); }

    pointer head() noexcept { return &head_; }
    const_pointer head() const noexcept { return &head_; }

    static pointer tail() noexcept { return reinterpret_cast<pointer>(tailStorage_); }

    // TODO: Consider making public.
    std::optional<iterator> try_insert_after(iterator pos, reference value) noexcept {
        RuntimeAssert(pos != end(), "Attempted to try_insert_after end()");
        RuntimeAssert(pos != iterator(), "Attempted to try_insert_after empty iterator");
        if (!trySetNext(&value, next(pos.node_))) {
            return std::nullopt;
        }
        setNext(pos.node_, &value);
        return iterator(&value);
    }

    union {
        value_type head_; // for debugger
        alignas(value_type) char headStorage_[sizeof(value_type)] = {0};
    };
    alignas(value_type) static inline char tailStorage_[sizeof(value_type)] = {0};
};

template <typename InputIt>
intrusive_forward_list(InputIt, InputIt) -> intrusive_forward_list<typename std::iterator_traits<InputIt>::value_type>;

} // namespace kotlin
