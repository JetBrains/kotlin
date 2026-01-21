/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemoryUsageInfo.hpp"

#include <algorithm>
#include <limits>

#include "Memory.h"
#include "Types.h"

using namespace kotlin;

#if KONAN_WINDOWS

#include <windows.h>
#include <psapi.h>

size_t kotlin::peakResidentSetSizeBytes() noexcept {
    ::PROCESS_MEMORY_COUNTERS memoryCounters;
    auto succeeded = ::GetProcessMemoryInfo(::GetCurrentProcess(), &memoryCounters, sizeof(memoryCounters));
    if (!succeeded) {
        return 0;
    }
    return memoryCounters.PeakWorkingSetSize;
}

#elif KONAN_APPLE

#include <unistd.h>

#if KONAN_MACOSX
#include <libproc.h>
#else
// libproc.h is not shipped on non-macOS, but the function is still defined there.
extern "C" int proc_pid_rusage(int pid, int flavor, rusage_info_t* buffer)
	__OSX_AVAILABLE_STARTING(__MAC_10_9, __IPHONE_7_0);
#endif

size_t kotlin::peakResidentSetSizeBytes() noexcept {
    ::rusage_info_current info;
    auto failed = proc_pid_rusage(getpid(), RUSAGE_INFO_CURRENT, reinterpret_cast<rusage_info_t*>(&info));
    if (failed) {
        return 0;
    }
    // Max footprint can't be more than size_t.
    return static_cast<size_t>(info.ri_lifetime_max_phys_footprint);
}

#else

#include <sys/time.h>
#include <sys/resource.h>

size_t kotlin::peakResidentSetSizeBytes() noexcept {
    ::rusage usage;
    auto failed = ::getrusage(RUSAGE_SELF, &usage);
    if (failed) {
        return 0;
    }
    // ru_maxrss is in KiB on Linux.
    size_t maxrss = static_cast<size_t>(usage.ru_maxrss * 1024);
    return maxrss;
}

#endif

extern "C" RUNTIME_NOTHROW KLong Kotlin_MemoryUsageInfo_getPeakResidentSetSizeBytes() {
    ThreadStateGuard guard(ThreadState::kNative); // No need to take chances here.
    auto result = peakResidentSetSizeBytes();
    // TODO: Need a common implementation for such conversions.
    if constexpr (sizeof(decltype(result)) >= sizeof(KLong)) {
        return static_cast<KLong>(std::min<decltype(result)>(result, std::numeric_limits<KLong>::max()));
    } else {
        return std::min<KLong>(result, std::numeric_limits<KLong>::max());
    }
}
