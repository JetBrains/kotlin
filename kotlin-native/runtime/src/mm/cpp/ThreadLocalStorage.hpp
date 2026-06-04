/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_THREAD_LOCAL_STORAGE_H
#define RUNTIME_MM_THREAD_LOCAL_STORAGE_H

#include <unordered_map>
#include <utility>
#include <vector>

#include "FlatteningIterator.hpp"
#include "Memory.h"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

class ThreadLocalStorage : Pinned {
public:
    using Key = const void*;

private:
    // Per-key block of object slots. The backing buffer is heap-allocated once and
    // never resized, so the slot addresses handed out by `LookupOrRegister` stay
    // valid for the whole lifetime of the storage (until `Clear`).
    using Record = std::vector<ObjHeader*>;
    using Storage = std::unordered_map<Key, Record>;

public:
    // Iterates over every object slot in every registered block (used for GC roots),
    // flattening the per-key blocks into a single sequence. Dereferencing yields the
    // slot by reference (`ObjHeader*&`).
    using Iterator = FlatteningIterator<Storage::iterator, SecondOfPair>;

    // Returns the address of object slot `index` for `key`, lazily registering a block
    // of `size` slots on the first access for `key`. Slot addresses are stable until `Clear`.
    ObjHeader** LookupOrRegister(Key key, int size, int index) noexcept;
    // Free all storage. Subsequent `LookupOrRegister` calls re-register lazily.
    void Clear() noexcept;

    Iterator begin() noexcept { return Iterator(storage_.begin(), storage_.end()); }
    Iterator end() noexcept { return Iterator(storage_.end(), storage_.end()); }

private:
    Storage storage_;
    // Cache the most recently used block for fast repeated lookups.
    Key lastKey_ = nullptr;
    Record* lastRecord_ = nullptr;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_THREAD_LOCAL_STORAGE_H
