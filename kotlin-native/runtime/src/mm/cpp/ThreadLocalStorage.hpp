/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_LOCAL_STORAGE_H
#define RUNTIME_MM_THREAD_LOCAL_STORAGE_H

#include <unordered_map>
#include <utility>
#include <vector>

#include "Memory.h"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

class ThreadLocalStorage : Pinned {
public:
    using Key = void*;

    class Iterator {
    public:
        explicit Iterator(KStdVector<ObjHeader*>::iterator iterator) : iterator_(iterator) {}

        ObjHeader** operator*() noexcept { return &*iterator_; }

        Iterator& operator++() noexcept {
            ++iterator_;
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return iterator_ == rhs.iterator_; }
        bool operator!=(const Iterator& rhs) const noexcept { return iterator_ != rhs.iterator_; }

    private:
        KStdVector<ObjHeader*>::iterator iterator_;
    };

    // Add TLS record. Can only be called before `Commit`.
    void AddRecord(Key key, int size) noexcept;
    // Prepare storage for records added by `AddRecord`.
    void Commit() noexcept;
    // Clear storage. Can only be called after `Commit`.
    void Clear() noexcept;
    // Lookup value in storage. Can only be called after `Commit`.
    ObjHeader** Lookup(Key key, int index) noexcept;

    Iterator begin() noexcept { return Iterator(storage_.begin()); }
    Iterator end() noexcept { return Iterator(storage_.end()); }

private:
    enum class State {
        kBuilding,
        kCommitted,
        kCleared,
    };

    struct Entry {
        int offset;
        int size;
    };

    ObjHeader** Lookup(Entry entry, int index) noexcept;

    KStdVector<ObjHeader*> storage_;
    // TODO: `KStdUnorderedMap` is probably the wrong container here.
    KStdUnorderedMap<Key, Entry> map_;
    State state_ = State::kBuilding;
    int size_ = 0; // Only used in `State::kBuilding`
    std::pair<Key, Entry> lastKeyAndEntry_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_LOCAL_STORAGE_H
