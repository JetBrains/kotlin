/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "HeapReservation.hpp"

#ifndef KONAN_WINDOWS

#include <algorithm>
#include <cerrno>
#include <cinttypes>
#include <cstdio>
#include <cstring>
#include <vector>

#include <sys/mman.h>
#include <unistd.h>

#if KONAN_APPLE
#include <mach/mach.h>
#include <mach/vm_statistics.h>
#endif

#include "Alignment.hpp"
#include "CompilerConstants.hpp"
#include "CompressedRefs.hpp"
#include "Constants.hpp"
#include "CustomLogging.hpp"
#include "KAssert.h"
#include "Porting.h"

using namespace kotlin;

// Bounds of the section with permanent objects, see `KotlinStaticData.placeInPermanentsSection` in the compiler.
#if KONAN_APPLE
extern "C" char Kotlin_permanentsStart[] __asm("section$start$__DATA_CONST$__kperm");
extern "C" char Kotlin_permanentsEnd[] __asm("section$end$__DATA_CONST$__kperm");
#else
extern "C" char Kotlin_permanentsStart[] __asm("__start_kperm") __attribute__((weak));
extern "C" char Kotlin_permanentsEnd[] __asm("__stop_kperm") __attribute__((weak));
#endif

namespace {

constexpr size_t kMinReservationSize = 256 * KiB * KiB;

struct Range {
    uintptr_t start;
    uintptr_t end;
};

// Address ranges mapped in the process, sorted by address.
std::vector<Range> mappedRanges() noexcept {
    std::vector<Range> result;
#if KONAN_APPLE
    vm_address_t address = 0;
    while (true) {
        vm_size_t size = 0;
        vm_region_basic_info_data_64_t info;
        mach_msg_type_number_t count = VM_REGION_BASIC_INFO_COUNT_64;
        mach_port_t objectName;
        auto status = vm_region_64(mach_task_self(), &address, &size, VM_REGION_BASIC_INFO_64,
                reinterpret_cast<vm_region_info_t>(&info), &count, &objectName);
        if (status != KERN_SUCCESS) break;
        result.push_back({address, address + size});
        address += size;
    }
#else
    if (auto* maps = std::fopen("/proc/self/maps", "r")) {
        char line[256];
        bool lineStart = true;
        while (std::fgets(line, sizeof(line), maps)) {
            uintptr_t start, end;
            if (lineStart && std::sscanf(line, "%" SCNxPTR "-%" SCNxPTR, &start, &end) == 2) {
                result.push_back({start, end});
            }
            lineStart = std::strchr(line, '\n') != nullptr;
        }
        std::fclose(maps);
    }
#endif
    std::sort(result.begin(), result.end(), [](const Range& a, const Range& b) { return a.start < b.start; });
    return result;
}

// Free address ranges of the process, sorted by address.
std::vector<Range> freeRanges(uintptr_t pageSize) noexcept {
    std::vector<Range> result;
    uintptr_t cursor = pageSize;
    for (const auto& range : mappedRanges()) {
        if (range.start > cursor) result.push_back({cursor, range.start});
        cursor = std::max(cursor, range.end);
    }
    result.push_back({cursor, UINTPTR_MAX});
    return result;
}

int mmapFileDescriptor() noexcept {
#if KONAN_APPLE
    auto tag = compiler::mmapTag();
    return tag ? VM_MAKE_TAG(tag) : -1;
#else
    return -1;
#endif
}

constexpr int kMmapFlags = MAP_PRIVATE | MAP_ANONYMOUS | MAP_NORESERVE;

// Makes a reserved range accessible. The memory is zero-filled.
void commit(uintptr_t address, size_t size) noexcept {
    auto* ptr = reinterpret_cast<void*>(address);
    if (mprotect(ptr, size, PROT_READ | PROT_WRITE) != 0) {
        konan::consoleErrorf("Out of memory trying to commit %zu bytes at %p: %s. Aborting.\n", size, ptr, strerror(errno));
        std::abort();
    }
}

// Returns the memory of a range to the OS, keeping the address space reserved.
void decommit(uintptr_t address, size_t size) noexcept {
    auto* ptr = reinterpret_cast<void*>(address);
    void* result = mmap(ptr, size, PROT_NONE, kMmapFlags | MAP_FIXED, mmapFileDescriptor(), 0);
    RuntimeCheck(result == ptr, "Failed to decommit %zu bytes at %p: %s", size, ptr, strerror(errno));
}

} // namespace

alloc::HeapReservation& alloc::HeapReservation::instance() noexcept {
    [[clang::no_destroy]] static HeapReservation instance;
    return instance;
}

bool alloc::HeapReservation::tryReserve(uintptr_t start, size_t size) noexcept {
    auto* wanted = reinterpret_cast<void*>(start);
    int flags = kMmapFlags;
#ifdef MAP_FIXED_NOREPLACE
    flags |= MAP_FIXED_NOREPLACE;
#endif
    void* result = mmap(wanted, size, PROT_NONE, flags, mmapFileDescriptor(), 0);
    if (result == MAP_FAILED) return false;
    if (result != wanted) {
        munmap(result, size);
        return false;
    }
    start_ = start;
    size_ = size;
    return true;
}

alloc::HeapReservation::HeapReservation() noexcept {
    pageSize_ = static_cast<size_t>(sysconf(_SC_PAGESIZE));

    auto permanentsStart = reinterpret_cast<uintptr_t>(Kotlin_permanentsStart);
    auto permanentsEnd = reinterpret_cast<uintptr_t>(Kotlin_permanentsEnd);
    RuntimeCheck(permanentsStart != 0 && permanentsStart <= permanentsEnd, "No section with permanent objects found");

    // The base is placed one page below the lowest object, so that offset 0 always means null.
    uint64_t windowSize = mm::kCompressedRefsWindowSize - pageSize_;
    uintptr_t aboveLimit = permanentsStart + windowSize;
    uintptr_t belowLimit = permanentsEnd > windowSize ? permanentsEnd - windowSize : 0;

    // Free ranges within the window, above or below the image.
    std::vector<Range> candidates;
    for (const auto& range : freeRanges(pageSize_)) {
        uintptr_t aboveStart = AlignUp(std::max(range.start, permanentsEnd), pageSize_);
        uintptr_t aboveEnd = AlignDown(std::min(range.end, aboveLimit), pageSize_);
        if (aboveStart + kMinReservationSize <= aboveEnd) candidates.push_back({aboveStart, aboveEnd});

        uintptr_t belowStart = AlignUp(std::max(range.start, belowLimit), pageSize_);
        uintptr_t belowEnd = AlignDown(std::min(range.end, permanentsStart), pageSize_);
        if (belowStart + kMinReservationSize <= belowEnd) candidates.push_back({belowStart, belowEnd});
    }
    // The reservation size limits the heap, so bigger ones first.
    std::sort(candidates.begin(), candidates.end(), [](const Range& a, const Range& b) { return a.end - a.start > b.end - b.start; });

    bool reserved = false;
    for (size_t size = candidates.empty() ? 0 : candidates.front().end - candidates.front().start; size >= kMinReservationSize && !reserved;
            size = AlignDown(size / 2, pageSize_)) {
        for (auto candidate = candidates.begin(); candidate != candidates.end() && !reserved; ++candidate) {
            reserved = tryReserve(candidate->start, std::min(size, candidate->end - candidate->start));
        }
    }
    if (!reserved) {
        konan::consoleErrorf(
                "Failed to reserve the heap within %" PRIu64 " GB of the permanent objects at [%p, %p). Aborting.\n",
                mm::kCompressedRefsWindowSize >> 30, reinterpret_cast<void*>(permanentsStart), reinterpret_cast<void*>(permanentsEnd));
        std::abort();
    }

    Kotlin_mm_compressedRefsBase = std::min(permanentsStart, start_) - pageSize_;
    RuntimeCheck(
            std::max(permanentsEnd, start_ + size_) - mm::compressedRefsBase() <= mm::kCompressedRefsWindowSize,
            "The heap and the permanent objects do not fit into the compressed references window");
    freeRanges_.emplace(start_, size_);

    CustomAllocInfo(
            "Reserved %zu MB for the heap at [%p, %p), permanent objects at [%p, %p), window base %p", size_ >> 20,
            reinterpret_cast<void*>(start_), reinterpret_cast<void*>(start_ + size_), reinterpret_cast<void*>(permanentsStart),
            reinterpret_cast<void*>(permanentsEnd), reinterpret_cast<void*>(mm::compressedRefsBase()));
}

void* alloc::HeapReservation::allocate(size_t size) noexcept {
    size = AlignUp(size, pageSize_);

    uintptr_t result = 0;
    {
        std::lock_guard guard(mutex_);
        for (auto range = freeRanges_.begin(); range != freeRanges_.end(); ++range) {
            if (range->second < size) continue;
            result = range->first;
            auto remaining = range->second - size;
            freeRanges_.erase(range);
            if (remaining != 0) freeRanges_.emplace(result + size, remaining);
            break;
        }
    }
    if (result == 0) {
        konan::consoleErrorf(
                "Out of memory trying to allocate %zu bytes: the heap reservation of %zu MB is exhausted. Aborting.\n",
                size, size_ >> 20);
        std::abort();
    }

    commit(result, size);
    return reinterpret_cast<void*>(result);
}

void alloc::HeapReservation::free(void* ptr, size_t size) noexcept {
    auto address = reinterpret_cast<uintptr_t>(ptr);
    size = AlignUp(size, pageSize_);
    RuntimeAssert(
            address >= start_ && address + size <= start_ + size_, "[%p, %p) is not in the heap reservation",
            ptr, reinterpret_cast<void*>(address + size));

    decommit(address, size);

    std::lock_guard guard(mutex_);
    auto [range, inserted] = freeRanges_.emplace(address, size);
    RuntimeAssert(inserted, "%p is freed twice", ptr);
    if (auto next = std::next(range); next != freeRanges_.end() && range->first + range->second == next->first) {
        range->second += next->second;
        freeRanges_.erase(next);
    }
    if (range != freeRanges_.begin()) {
        if (auto previous = std::prev(range); previous->first + previous->second == range->first) {
            previous->second += range->second;
            freeRanges_.erase(range);
        }
    }
}

void* alloc::HeapReservation::allocateRuntimePermanent(size_t size) noexcept {
    size = AlignUp(size, kObjectAlignment);
    std::lock_guard guard(runtimePermanentMutex_);
    if (runtimePermanentEnd_ - runtimePermanentNext_ < size) {
        auto chunkSize = AlignUp(size, pageSize_);
        runtimePermanentNext_ = reinterpret_cast<uintptr_t>(allocate(chunkSize));
        runtimePermanentEnd_ = runtimePermanentNext_ + chunkSize;
    }
    auto result = runtimePermanentNext_;
    runtimePermanentNext_ += size;
    return reinterpret_cast<void*>(result);
}

#endif // !KONAN_WINDOWS
