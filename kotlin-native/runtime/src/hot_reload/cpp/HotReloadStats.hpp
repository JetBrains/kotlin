/**
* Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
*/

#ifndef HOTRELOADSTATS_HPP
#define HOTRELOADSTATS_HPP

#include "Types.h"
#include <vector>

namespace kotlin::hot {
struct Stats {
    long start_;
    long end_;
    std::vector<std::string> loadedObjects_;
    int reboundSymbols_;
    bool wasSuccessful_;

    Stats() = default;

    Stats(const long start, const long end, const std::vector<std::string>& loaded_objects, const int rebound_symbols, const bool was_successful) :
        start_(start), end_(end), loadedObjects_(loaded_objects), reboundSymbols_(rebound_symbols), wasSuccessful_(was_successful) {}

    void build(KRef builder) const noexcept;
};

class StatsCollector {
public:
    void RegisterStart(long start) noexcept;
    void RegisterEnd(long end) noexcept;
    void RegisterLoadedObject(const std::vector<std::string>& loadedObjects) noexcept;
    void RegisterReboundSymbols(int reboundSymbols) noexcept;
    void RegisterSuccessful(bool wasSuccessful) noexcept;

    const Stats& GetCurrent() const noexcept { return currentStats_; }

private:
    Stats currentStats_ = {};
};
}

#endif //HOTRELOADSTATS_HPP
