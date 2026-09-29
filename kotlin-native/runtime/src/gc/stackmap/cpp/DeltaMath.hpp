#pragma once

#include "RootsInfo.hpp"

#include <cstdint>
#include <utility>
#include <vector>

namespace kotlin::stackMap {

/// The live-location set of one call site, encoded relative to a function's
/// base state: stack-slot liveness as a 64-bit-word bit vector. This is the
/// runtime-side mirror of llvm::deltamain::Delta (see
/// llvm/include/llvm/CodeGen/DeltaMainStackMap.h in the LLVM-side port) --
/// the two must agree on encoding, since one is produced by the other.
struct Delta {
    std::vector<uint64_t> slots;

    /// Converts this Delta's live-location set to root locations relative
    /// to `baseOffset` (the function's highest live stack offset -- see
    /// deltamain::FunctionState::assignSlotIndices on the emitting side).
    RootsInfo toRootInfo(uint64_t baseOffset) const;

    void log(std::vector<uint64_t>& vec) const;
    void logSlots() const;
};

/// The symmetric difference of two Deltas: bit vectors are XORed word by
/// word (treating a missing trailing word as all-zero). This must invert
/// cleanly: for any Delta D, `(D ^ D)` decodes to no live locations, and
/// `(base ^ (base ^ other))` decodes to the same locations as `other` (the
/// actual reconstruction, `base ^ delta`, is how
/// DeltaMainStackMapBuilder::Reader recovers a callsite's live-location set
/// from its function's base Delta plus its stored per-callsite Delta).
Delta operator^(const Delta& lhs, const Delta& rhs);

} // namespace kotlin::stackMap
