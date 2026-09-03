#include "DeltaMath.hpp"

#include "Logging.hpp"

#include <algorithm>
#include <map>
#include <set>
#include <sstream>

namespace kotlin::stackMap {

namespace {

// The AArch64 callee-saved general-purpose registers x19-x27 (9 of them)
// are renumbered to 0-8 on the LLVM emitting side (see
// DeltaMainStackMapEncoder::enumerate); denumerate() below inverts that.
constexpr uint32_t kAmountCalleeSaved = 9;
constexpr uint32_t kFirstCalleeSaved = 19;

/// Inverts DeltaMainStackMapEncoder::enumerate/signedEnumerate:
///  - negative values are stack-slot indices: `-enumeration - 1`.
///  - non-negative values are register indices, remapped back from the
///    dense 0-8/9-27/28 packing to real AArch64 register numbers:
///      0..8   -> x19..x27
///      9..27  -> x0..x18
///      28     -> x28
int32_t denumerate(int64_t enumeration) {
    if (enumeration < 0) {
        return static_cast<int32_t>(-enumeration - 1);
    }
    if (enumeration < kAmountCalleeSaved) {
        return static_cast<int32_t>(enumeration + kFirstCalleeSaved);
    }
    if (enumeration < kAmountCalleeSaved + kFirstCalleeSaved) {
        return static_cast<int32_t>(enumeration - kAmountCalleeSaved);
    }
    return static_cast<int32_t>(enumeration);
}

/// Builds a RootLocation from a signed-enumerated value (see
/// DeltaMainStackMapEncoder::signedEnumerate): non-negative is a register,
/// negative is a stack slot at `baseOffset - denumerate(enumeration) * 8`.
RootLocation locationFromSignedEnum(int64_t enumeration, uint64_t baseOffset) {
    if (enumeration >= 0) {
        return RootLocation::ConstructRegister(denumerate(enumeration));
    }
    return RootLocation::ConstructIndirect(static_cast<int32_t>(baseOffset - denumerate(enumeration) * 8));
}

/// Builds a RootLocation from an (unsigned) bit-vector index -- used for
/// the register and stack-slot bit vectors, where the sign convention of
/// signedEnumerate does not apply (the bit vector's identity already tells
/// us which kind of location it is).
RootLocation locationFromBitIndex(RootLocation::RootLocationType type, int32_t idx, uint64_t baseOffset) {
    switch (type) {
        case RootLocation::Register:
            return RootLocation::ConstructRegister(denumerate(idx));
        case RootLocation::Indirect:
            return RootLocation::ConstructIndirect(static_cast<int32_t>(baseOffset - idx * 8));
        case RootLocation::Direct:
            RuntimeFail("locationFromBitIndex: Direct locations are not stored in bit vectors");
    }
}

std::vector<uint64_t> xorWords(const std::vector<uint64_t>& lhs, const std::vector<uint64_t>& rhs) {
    std::vector<uint64_t> result;
    size_t common = std::min(lhs.size(), rhs.size());

    for (size_t i = 0; i < common; ++i) {
        result.push_back(lhs[i] ^ rhs[i]);
    }
    for (size_t i = common; i < lhs.size(); ++i) {
        result.push_back(lhs[i]);
    }
    for (size_t i = common; i < rhs.size(); ++i) {
        result.push_back(rhs[i]);
    }
    return result;
}

} // namespace

Delta operator^(const Delta& lhs, const Delta& rhs) {
    Delta result;
    result.regs = xorWords(lhs.regs, rhs.regs);
    result.slots = xorWords(lhs.slots, rhs.slots);

    std::set<std::pair<int64_t, int64_t>> lhsSet(lhs.derives.begin(), lhs.derives.end());
    std::set<std::pair<int64_t, int64_t>> rhsSet(rhs.derives.begin(), rhs.derives.end());

    for (const auto& derive : lhs.derives) {
        if (rhsSet.find(derive) == rhsSet.end()) {
            result.derives.push_back(derive);
        }
    }
    for (const auto& derive : rhs.derives) {
        if (lhsSet.find(derive) == lhsSet.end()) {
            result.derives.push_back(derive);
        }
    }
    return result;
}

void Delta::log(std::vector<uint64_t>& vec) const {
    std::ostringstream oss;
    oss << "    [ ";
    for (auto elem : vec) {
        oss << elem << " ";
    }
    oss << "]";
    RuntimeLogDebug({kTagGC}, "%s", oss.str().c_str());
}

void Delta::logRegs() const {
    RuntimeLogDebug({kTagGC}, "    Delta.regs");
    log(const_cast<std::vector<uint64_t>&>(regs));
}

void Delta::logSlots() const {
    RuntimeLogDebug({kTagGC}, "    Delta.slots");
    log(const_cast<std::vector<uint64_t>&>(slots));
}

RootsInfo Delta::toRootInfo(uint64_t baseOffset) const {
    std::map<RootLocation, std::vector<RootLocation>> locations;

    auto traverseBitVector = [&locations, baseOffset](const std::vector<uint64_t>& words, RootLocation::RootLocationType type) {
        for (size_t wordIdx = 0; wordIdx < words.size(); ++wordIdx) {
            uint64_t word = words[wordIdx];
            int32_t bitIdx = static_cast<int32_t>(wordIdx * 64);

            while (word != 0) {
                if (word & 1) {
                    locations[locationFromBitIndex(type, bitIdx, baseOffset)];
                }
                bitIdx++;
                word >>= 1;
            }
        }
    };

    traverseBitVector(regs, RootLocation::RootLocationType::Register);
    traverseBitVector(slots, RootLocation::RootLocationType::Indirect);

    for (const auto& [locationEnum, baseEnum] : derives) {
        RootLocation location = locationFromSignedEnum(locationEnum, baseOffset);
        RootLocation base = locationFromSignedEnum(baseEnum, baseOffset);
        locations[base].push_back(location);
    }

    RootsInfo result;
    for (auto& [base, derivedLocations] : locations) {
        result.addLink(base, derivedLocations);
    }
    return result;
}

} // namespace kotlin::stackMap
