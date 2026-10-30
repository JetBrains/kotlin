/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <cstdint>
#include <map>
#include <mutex>

#include "Utils.hpp"

namespace kotlin::alloc {

/**
 * The address range for the heap under compressed references.
 *
 * Every object a compressed reference may point to must lie inside the compression window:
 * heap objects and permanent objects, which the compiler places into a dedicated section of the binary.
 * So the heap address range is reserved close to that section. Heap pages are taken from it:
 * the memory of a page is committed when the page is allocated and decommitted when it is freed.
 */
class HeapReservation : private Pinned {
public:
    // The first call reserves the heap and sets the window base.
    static HeapReservation& instance() noexcept;

    void* allocate(size_t size) noexcept;
    void free(void* ptr, size_t size) noexcept;

    // Bump allocator for permanent objects created by the runtime.
    void* allocateRuntimePermanent(size_t size) noexcept;

private:
    HeapReservation() noexcept;
    ~HeapReservation() = delete;

    bool tryReserve(uintptr_t start, size_t size) noexcept;

    size_t pageSize_ = 0;
    uintptr_t start_ = 0;
    size_t size_ = 0;

    std::mutex mutex_;
    std::map<uintptr_t, size_t> freeRanges_;

    std::mutex runtimePermanentMutex_;
    uintptr_t runtimePermanentNext_ = 0;
    uintptr_t runtimePermanentEnd_ = 0;
};

}
