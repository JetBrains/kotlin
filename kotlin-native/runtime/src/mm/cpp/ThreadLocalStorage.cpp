/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadLocalStorage.hpp"
#include "Logging.hpp"

using namespace kotlin;

ObjHeader** mm::ThreadLocalStorage::LookupOrRegister(Key key, int size, int index) noexcept {
    RuntimeLogDebug({kTagTLS}, "Lookup key = %p, size = %d, index = %d", key, size, index);
    RuntimeAssert(size > 0, "TLS record size must be positive");
    RuntimeAssert(index < size, "Out of bounds TLS access");
    if (key == lastKey_) {
        return &(*lastRecord_)[index];
    }
    auto it = storage_.find(key);
    if (it == storage_.end()) {
        RuntimeLogDebug({kTagTLS}, "Register key = %p, size = %d", key, size);
        it = storage_.emplace(key, Record(size, nullptr)).first;
    } else {
        RuntimeAssert(static_cast<int>(it->second.size()) == size, "Attempt to look up a TLS record with a different size");
    }
    lastKey_ = key;
    lastRecord_ = &it->second;
    return &it->second[index];
}

void mm::ThreadLocalStorage::Clear() noexcept {
    RuntimeLogDebug({kTagTLS}, "Cleared");
    storage_.clear();
    lastKey_ = nullptr;
    lastRecord_ = nullptr;
}
