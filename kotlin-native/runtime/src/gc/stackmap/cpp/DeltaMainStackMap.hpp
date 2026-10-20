#pragma once

#include "RootsInfo.hpp"

#include <cstdint>
#include <unordered_map>

namespace kotlin::stackMap {

class DeltaMainStackMap {
public:
    DeltaMainStackMap() noexcept;
    DeltaMainStackMap(uint8_t* begin, uint8_t* end, bool lazy) noexcept : begin_(begin), end_(end), lazy_(lazy) {}

    void collect();

    bool tryGetRootsInfo(const uint64_t* fp, uintptr_t pc, RootsInfo& rootsInfo) const;

private:
    uint8_t* begin_;
    uint8_t* end_;
    bool lazy_;
    std::unordered_map<uintptr_t, RootsInfo> pc2RootsInfo_;
};

} // namespace kotlin::stackMap
