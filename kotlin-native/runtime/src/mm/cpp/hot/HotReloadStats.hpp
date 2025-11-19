//
// Created by Gabriele.Pappalardo on 19/11/2025.
//

#ifndef HOTRELOADSTATS_HPP
#define HOTRELOADSTATS_HPP

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

#endif //HOTRELOADSTATS_HPP
