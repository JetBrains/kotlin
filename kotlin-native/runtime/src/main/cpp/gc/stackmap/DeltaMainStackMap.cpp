#include "DeltaMainStackMap.hpp"

#include "KAssert.h"
#include "Logging.hpp"

#include <algorithm>
#include <cinttypes>
#include <set>
#include <utility>

namespace kotlin::stackMap {

Delta DeltaMainStackMapBuilder::Reader::getNextDelta() {
    Delta delta;

#if ENABLE_REGISTERS
    delta.regs = bytes_.nextULEB128BitVector();
#endif

    delta.slots = bytes_.nextULEB128BitVector();

    uint64_t size = bytes_.nextULEB128();
    for (uint64_t i = 0; i < size; i++) {
        int64_t location = bytes_.nextSLEB128();
        int64_t base = bytes_.nextSLEB128();
        delta.derives.emplace_back(location, base);
    }
    return delta;
}

PrologueInfo DeltaMainStackMapBuilder::Reader::getPrologueInfo() {
#if ENABLE_REGISTERS
    uint32_t calleeSavedMask = bytes_.nextULEB128();
    uint32_t remainingMask = calleeSavedMask;
    std::vector<uint32_t> calleeSavedOffsets;

    while (remainingMask != 0) {
        if (remainingMask & 1) {
            calleeSavedOffsets.push_back(bytes_.nextULEB128());
        }
        remainingMask >>= 1;
    }
    return {calleeSavedMask, std::move(calleeSavedOffsets)};
#else
    return {};
#endif
}

void DeltaMainStackMapBuilder::Reader::getRootsInfo(
        uint64_t baseOffset, FunctionAddress functionAddress, std::unordered_map<FunctionAddress, RootsInfo>& pc2RootsInfo) {
    Delta base = getNextDelta();

    std::vector<std::pair<uint64_t, uint64_t>> pc2delta;
    std::set<uint64_t> deltaIndices;
    uint64_t pc2deltaSize = bytes_.nextULEB128();
    for (uint64_t i = 0; i < pc2deltaSize; i++) {
        uint64_t pc = bytes_.nextULEB128();
        uint64_t deltaIdx = bytes_.nextULEB128();
        deltaIndices.insert(deltaIdx);
        pc2delta.emplace_back(pc, deltaIdx);
    }

    std::vector<Delta> deltas;
    deltas.reserve(deltaIndices.size());
    for (uint64_t i = 0; i < deltaIndices.size(); i++) {
        deltas.push_back(getNextDelta());
    }

    for (const auto& [pc, deltaIdx] : pc2delta) {
        Delta callsiteDelta = base ^ deltas[deltaIdx];
        RootsInfo rootsInfo = callsiteDelta.toRootInfo(baseOffset);
        rootsInfo.log(functionAddress, functionAddress + pc);
        pc2RootsInfo[functionAddress + pc] = std::move(rootsInfo);
    }
}

void DeltaMainStackMapBuilder::verifyMagic(uint8_t magic) {
    uint8_t expected = (DELTA_MAIN_VERSION << 4) | REGISTER_IN_STACKMAP_MAGIC | LAZY_ENABLED_MAGIC;
    if (magic != expected) {
        RuntimeLogDebug({kTagGC}, "MAGIC IS WRONG ASSERT WOULD FAIL");
    }

    RuntimeAssert(magic == expected,
                 "Delta-main stack map magic mismatch: section was built with incompatible "
                 "ENABLE_REGISTERS/ENABLE_LAZY_STACKMAP/DELTA_MAIN_VERSION settings "
                 "(expected 0x%x, got 0x%x)",
                 expected, magic);
}

void DeltaMainStackMapBuilder::collect() {
    uint64_t sectionStart = reinterpret_cast<uint64_t>(sectionStart_);
    uint64_t functionCount = reader_.getNextULEB128();
    RuntimeLogDebug({kTagGC}, "DeltaMainStackMap: collecting %" PRIu64 " functions", functionCount);

    for (uint64_t i = 0; i < functionCount; i++) {
        uint64_t funcOffset = static_cast<uint64_t>(reader_.getNextInt(4));
        FunctionAddress funcAddress = funcOffset + sectionStart;

        int64_t baseOffset = reader_.getNextSLEB128();
        [[maybe_unused]] uint64_t stackSize = reader_.getNextULEB128();

        PrologueInfo prologue = reader_.getPrologueInfo();
        prologue.log();
        funcAddr2PrologueInfo_[funcAddress] = prologue;

        reader_.getRootsInfo(baseOffset, funcAddress, pc2RootsInfo_);
    }
}

void DeltaMainStackMapBuilder::collectLazy(uintptr_t functionPC, uintptr_t currentPC, uint64_t* funcStackMapAddr, RootsInfo& rootsInfo) {
    reader_.setP(reinterpret_cast<uint8_t*>(funcStackMapAddr));

    // Function offset and stack size are only meaningful when scanning the
    // section sequentially (collect()); when jumping directly to a known
    // function's record, only baseOffset is needed to decode its deltas.
    reader_.getNextInt(4);
    int64_t baseOffset = reader_.getNextSLEB128();
    reader_.getNextULEB128(); // stack size

    reader_.getPrologueInfo(); // Not needed for a single-callsite lookup.

    Delta base = reader_.getNextDelta();

    std::vector<std::pair<uint64_t, uint64_t>> pc2delta;
    std::set<uint64_t> deltaIndices;
    uint64_t pc2deltaSize = reader_.getNextULEB128();
    for (uint64_t i = 0; i < pc2deltaSize; i++) {
        uint64_t pc = reader_.getNextULEB128();
        uint64_t deltaIdx = reader_.getNextULEB128();
        deltaIndices.insert(deltaIdx);
        pc2delta.emplace_back(pc, deltaIdx);
    }

    std::vector<Delta> deltas;
    deltas.reserve(deltaIndices.size());
    for (uint64_t i = 0; i < deltaIndices.size(); i++) {
        deltas.push_back(reader_.getNextDelta());
    }

    uint64_t targetPC = currentPC - functionPC;

    // pc2delta is sorted by pc (DeltaMainStackMapEncoder::emit writes
    // PcToDelta in call-recording order, which is program order within a
    // function, so PC offsets are monotonically increasing).
    auto it = std::lower_bound(
            pc2delta.begin(), pc2delta.end(), targetPC, [](const auto& pair, uint64_t val) { return pair.first < val; });

    RuntimeAssert(it != pc2delta.end() && it->first == targetPC,
                 "collectLazy: no delta-main record for PC offset %" PRIu64 " within function at %p", targetPC,
                 reinterpret_cast<void*>(functionPC));

    Delta callsiteDelta = base ^ deltas[it->second];
    rootsInfo = callsiteDelta.toRootInfo(baseOffset);
    rootsInfo.log();
}

} // namespace kotlin::stackMap
