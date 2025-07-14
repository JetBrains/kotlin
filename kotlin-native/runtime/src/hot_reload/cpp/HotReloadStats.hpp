/**
* Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
*/

#ifndef HOTRELOADSTATS_HPP
#define HOTRELOADSTATS_HPP

#ifdef KONAN_HOT_RELOAD

#include "Types.h"

namespace kotlin::hot {
struct Stats {
    long start;
    long end;
    std::string loadedLibrary;
    int reboundSymbols;
    bool wasSuccessful;

    Stats() = default;

    Stats(const long start, const long end, const std::string& loaded_library, const int rebound_symbols, const bool was_successful) :
        start(start), end(end), loadedLibrary(loaded_library), reboundSymbols(rebound_symbols), wasSuccessful(was_successful) {}

    void build(KRef builder) const noexcept;
};

class StatsCollector {
public:
    void registerStart(long start) noexcept;
    void registerEnd(long end) noexcept;
    void registerLoadedLibrary(const std::string& loadedLibrary) noexcept;
    void registerReboundSymbols(int reboundSymbols) noexcept;
    void registerSuccessful(bool wasSuccessful) noexcept;

    const Stats& getCurrent() const noexcept { return kCurrent; }

private:
    Stats kCurrent = {};
};
}

#endif

#endif //HOTRELOADSTATS_HPP
