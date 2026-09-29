#pragma once

#include "KAssert.h"
#include "Logging.hpp"

#include <algorithm>
#include <cstdint>
#include <iostream>
#include <sstream>
#include <string>
#include <tuple>
#include <vector>

namespace kotlin::stackMap {

/// A single GC root location: a stack slot (or, for the vestigial Direct
/// case, a register), as decoded from a delta-main stack map (see
/// DeltaMainStackMapEncoder::enumerate in the LLVM-side port for the
/// register numbering this decodes).
struct RootLocation {
    enum RootLocationType { Direct, Indirect };

    RootLocationType Type;
    int32_t RegBit;
    int32_t Offset;

    RootLocation() = default;
    RootLocation(RootLocationType type, int32_t regBit, int32_t offset)
        : Type(type), RegBit(regBit), Offset(offset) {}

    static RootLocation ConstructIndirect(int32_t offset) {
        return RootLocation(RootLocationType::Indirect, -1, offset);
    }

    bool operator==(const RootLocation& other) const {
        return std::tie(Type, RegBit, Offset) == std::tie(other.Type, other.RegBit, other.Offset);
    }

    friend std::ostream& operator<<(std::ostream& os, const RootLocation& loc) {
        os << "[" << (loc.Type == RootLocation::Direct    ? "DIR" :
                      loc.Type == RootLocation::Indirect  ? "IND" : "UNDEF");

        if (loc.Type == RootLocation::Direct) {
            os << " | Reg: x" << loc.RegBit;
        }
        if (loc.Type == RootLocation::Direct || loc.Type == RootLocation::Indirect) {
            os << " | Off: " << loc.Offset;
        }
        return os << "]";
    }
};

/// The set of live GC roots at one call site: the base pointer locations
/// live at that point (derived pointers are not tracked -- this port's GC
/// is entirely non-moving, so only base-pointer locations matter for
/// marking).
class RootsInfo {
public:
    RootsInfo() = default;

    bool operator==(const RootsInfo& other) const { return bases_ == other.bases_; }

    /// Records that `base` is live. Each `base` may only be added once.
    void addBase(const RootLocation& base) {
        RuntimeAssert(std::find(bases_.begin(), bases_.end(), base) == bases_.end(), "Expected that base will be used once");
        bases_.push_back(base);
    }

    std::vector<RootLocation>::const_iterator begin() const { return bases_.begin(); }
    std::vector<RootLocation>::const_iterator end() const { return bases_.end(); }

    const std::vector<RootLocation>& bases() const { return bases_; }

    size_t totalCount() const { return bases_.size(); }

    void log() const {
        for (const auto& base : bases_) {
            std::ostringstream ossBase;
            ossBase << "    | " << base;
            RuntimeLogDebug({kTagGC}, "%s", ossBase.str().c_str());
        }
    }

    void log(uintptr_t funcAddr, uintptr_t curPC) const {
        RuntimeLogDebug({kTagGC}, "    funcAddr: %p, pc: %p", reinterpret_cast<void*>(funcAddr), reinterpret_cast<void*>(curPC));
        RuntimeLogDebug({kTagGC}, "    | RootsInfo:");
        if (totalCount() == 0) {
            RuntimeLogDebug({kTagGC}, "    |      empty");
            return;
        }
        log();
    }

    std::vector<RootLocation> bases_;
};

} // namespace kotlin::stackMap
