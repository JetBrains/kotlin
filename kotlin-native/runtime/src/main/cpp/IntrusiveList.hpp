/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <iterator>
#include <limits>

#include "KAssert.h"
#include "Utils.hpp"

namespace kotlin {

template <typename T>
struct DefaultIntrusiveForwardListTraits {
    static T* next(const T& value) noexcept { return value.next_; }

    static void setNext(T& value, T* next) noexcept { value.next_ = next; }
};

// Intrusive variant of `std::forward_list`. Notable differences:
// * The container does not own nodes. Care must be taken not to allow a node
//   to be in two containers at once, or twice into the same container.
// * The container is move-only, and moving invalidates `before_begin` iterator.
// * insert_after, erase_after take `iterator` instead of `const_iterator`, because
//   they do in fact require mutability via `Traits::setNext`.
// * When the node leaves the container, nothing clears `next` pointer inside it.
//
// `Traits` must have 2 methods:
// static T* next(const T& value);
// static void setNext(T& value, T* next);
// NOTE: `setNext` and `next` must be callable even on uninitialized `T` (i.e. they
//       should only access storage inside `T`).
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

    intrusive_forward_list() noexcept {
        setNext(head(), nullptr);
    }

    intrusive_forward_list(intrusive_forward_list&& rhs) noexcept : size_(rhs.size_) {
        setNext(head(), next(rhs.head()));
        setNext(rhs.head(), nullptr);
        rhs.size_ = 0;
    }

    template <typename InputIt>
    intrusive_forward_list(InputIt first, InputIt last) noexcept {
        setNext(head(), nullptr);
        assign(std::move(first), std::move(last));
    }

    ~intrusive_forward_list() = default;

    intrusive_forward_list& operator=(intrusive_forward_list&& rhs) noexcept {
        intrusive_forward_list tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    void swap(intrusive_forward_list& rhs) noexcept {
        using std::swap;
        auto thisNext = next(head());
        auto rhsNext = next(rhs.head());
        swap(thisNext, rhsNext);
        setNext(head(), thisNext);
        setNext(rhs.head(), rhsNext);
        swap(size_, rhs.size_);
    }

    template <typename InputIt>
    void assign(InputIt first, InputIt last) noexcept {
        clear();
        insert_after(before_begin(), std::move(first), std::move(last));
    }

    reference front() noexcept { return *next(head()); }
    const_reference front() const noexcept { return *next(head()); }

    iterator before_begin() noexcept { return iterator(head()); }
    const_iterator before_begin() const noexcept { return const_iterator(head()); }
    const_iterator cbefore_begin() const noexcept { return const_iterator(head()); }

    iterator begin() noexcept { return iterator(next(head())); }
    const_iterator begin() const noexcept { return const_iterator(next(head())); }
    const_iterator cbegin() const noexcept { return const_iterator(next(head())); }

    iterator end() noexcept { return iterator(); }
    const_iterator end() const noexcept { return const_iterator(); }
    const_iterator cend() const noexcept { return const_iterator(); }

    bool empty() const noexcept { return size_ == 0; }

    size_type max_size() const noexcept { return std::numeric_limits<size_type>::max(); }

    void clear() noexcept { setNext(head(), nullptr); size_ = 0; }

    iterator insert_after(iterator pos, reference value) noexcept {
        pointer nextNode = next(pos.node_);
        setNext(pos.node_, &value);
        setNext(&value, nextNode);
        ++size_;
        return iterator(&value);
    }

    template <typename InputIt>
    iterator insert_after(iterator pos, InputIt first, InputIt last) noexcept {
        pointer nextNode = next(pos.node_);
        pointer prevNode = pos.node_;
        size_t newSize = size_;
        for (auto it = first; it != last; ++it) {
            setNext(prevNode, &*it);
            prevNode = &*it;
            ++newSize;
        }
        setNext(prevNode, nextNode);
        size_ = newSize;
        return iterator(prevNode);
    }

    iterator erase_after(iterator pos) noexcept {
        pointer prevNode = pos.node_;
        pointer nodeToErase = next(pos.node_);
        if (!nodeToErase) {
            return end();
        }
        pointer nextNode = next(nodeToErase);
        setNext(prevNode, nextNode);
        setNext(nodeToErase, nullptr);
        --size_;
        return iterator(nextNode);
    }

    iterator erase_after(iterator first, iterator last) noexcept {
        size_ -= std::distance(first, last) - 1;
        setNext(first.node_, last.node_);
        return last;
    }

    void push_front(reference value) noexcept { insert_after(before_begin(), value); }

    void pop_front() noexcept { erase_after(before_begin()); }

    void remove(reference value) noexcept {
        // TODO: no need to move on after finding the first match.
        return remove_if([&value](const_reference x) { return &x == &value; });
    }

    template <typename P>
    void remove_if(P p) noexcept {
        size_t newSize = size_;
        pointer prev = head();
        pointer node = next(prev);
        while (node) {
            if (p(*node)) {
                // The node is being removed.
                node = next(node);
                setNext(prev, node);
                --newSize;
            } else {
                // The node is staying.
                prev = node;
                node = next(node);
            }
        }
        size_ = newSize;
    }

    // TODO: Implement splice_after.

    size_type size() const noexcept {
        return size_;
    }

private:
    static pointer next(const_pointer node) noexcept { return Traits::next(*node); }

    static void setNext(pointer node, pointer next) noexcept { return Traits::setNext(*node, next); }

    pointer head() noexcept {
        return reinterpret_cast<pointer>(headStorage_);
    }

    const_pointer head() const noexcept {
        return reinterpret_cast<const_pointer>(headStorage_);
    }

    alignas(value_type) char headStorage_[sizeof(value_type)] = { 0 };
    size_t size_ = 0;
};

template <typename InputIt>
intrusive_forward_list(InputIt, InputIt) -> intrusive_forward_list<typename std::iterator_traits<InputIt>::value_type>;

} // namespace kotlin
