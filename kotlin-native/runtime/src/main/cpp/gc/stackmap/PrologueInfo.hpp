#pragma once

#include "Logging.hpp"

#include <cstdint>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

namespace kotlin::stackMap {

/// Callee-saved-register spill info for one function's prologue: which
/// registers were saved (as a bitmask) and, for each set bit in mask order,
/// the frame offset it was spilled to. Currently always empty/zero: see
/// StackMapConfig.hpp's ENABLE_REGISTERS.
struct PrologueInfo {
    uint32_t calleeSaved = 0;
    std::vector<uint32_t> offsets;

    PrologueInfo() = default;
    PrologueInfo(uint32_t calleeSaved, std::vector<uint32_t> offsets) :
        calleeSaved(calleeSaved), offsets(std::move(offsets)) {}

    bool operator==(const PrologueInfo& other) const {
        return calleeSaved == other.calleeSaved && offsets == other.offsets;
    }

    void log() const {
        std::ostringstream oss;
        oss << "    | CalleeSaved: 0x" << std::hex << calleeSaved << std::dec << ", offsets: [ ";
        for (uint32_t offset : offsets) {
            oss << offset << " ";
        }
        oss << "]";
        RuntimeLogDebug({kTagGC}, "%s", oss.str().c_str());
    }
};

} // namespace kotlin::stackMap
