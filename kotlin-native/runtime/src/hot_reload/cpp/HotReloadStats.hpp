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
    void RegisterStart(long start) noexcept;
    void RegisterEnd(long end) noexcept;
    void RegisterLoadedObject(const std::string& loadedLibrary) noexcept;
    void RegisterReboundSymbols(int reboundSymbols) noexcept;
    void RegisterSuccessful(bool wasSuccessful) noexcept;

    const Stats& GetCurrent() const noexcept { return kCurrent; }

private:
    Stats kCurrent = {};
};
}

#endif

#endif //HOTRELOADSTATS_HPP
