/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadLocalStorage.hpp"

using namespace kotlin;

void mm::ThreadLocalStorage::AddRecord(Key key, int size) noexcept {
    RuntimeAssert(state_ == State::kBuilding, "Storage must be in the building state");
    RuntimeAssert(size >= 0, "Size cannot be negative");
    auto it = map_.find(key);
    if (it != map_.end()) {
        RuntimeAssert(it->second.size == size, "Attempt to add TLS record with the same key, but different size");
        return;
    }
    map_.emplace(key, Entry{size_, size});
    size_ += size;
}

void mm::ThreadLocalStorage::Commit() noexcept {
    RuntimeAssert(state_ == State::kBuilding, "Storage must be in the building state");
    storage_.resize(size_);
    state_ = State::kCommitted;
}

void mm::ThreadLocalStorage::Clear() noexcept {
    RuntimeAssert(state_ == State::kCommitted, "Storage must be in the committed state");
    // Just free the storage.
    storage_.clear();
    state_ = State::kCleared;
}

ObjHeader** mm::ThreadLocalStorage::Lookup(Key key, int index) noexcept {
    RuntimeAssert(state_ == State::kCommitted, "Storage must be in the committed state");
    if (lastKeyAndEntry_.first == key) {
        return Lookup(lastKeyAndEntry_.second, index);
    }
    auto it = map_.find(key);
    RuntimeAssert(it != map_.end(), "Unknown TLS key");
    lastKeyAndEntry_ = *it;
    return Lookup(it->second, index);
}

ObjHeader** mm::ThreadLocalStorage::Lookup(Entry entry, int index) noexcept {
    RuntimeAssert(index < entry.size, "Out of bounds TLS access");
    return &storage_[entry.offset + index];
}
