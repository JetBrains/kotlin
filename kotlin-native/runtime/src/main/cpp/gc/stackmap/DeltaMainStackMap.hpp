#pragma once

#include "CompilerConstants.hpp"
#include "DeltaMath.hpp"
#include "PrologueInfo.hpp"
#include "RootsInfo.hpp"
#include "StackMapConfig.hpp"
#include "VarIntCodec.hpp"

#include <cstdint>
#include <unordered_map>

namespace kotlin::stackMap {

using FunctionAddress = uintptr_t;

/// Decodes the delta-main-encoded `__LLVM_StackMaps` section written by
/// DeltaMainStackMapEncoder (llvm/lib/CodeGen/DeltaMainStackMapEncoder.h)
/// into per-callsite RootsInfo, for GC root scanning.
///
/// Two collection modes are supported:
///  - collect(): eagerly decodes every function's every callsite up front,
///    populating pc2RootsInfo()/funcAddr2PrologueInfo() for the whole
///    binary. Used when ENABLE_LAZY_STACKMAP is off.
///  - collectLazy(): decodes a single function's record on demand, given a
///    pointer directly to that function's stack map (obtained out of band,
///    e.g. from a per-function offset table). Not yet wired up to an
///    emitting side in this port -- see StackMapConfig.hpp.
struct DeltaMainStackMapBuilder {
    /// Walks one function's delta-main record: base state, PC-to-delta
    /// index, and the deduplicated deltas themselves. Pure decoding, with
    /// no notion of "which function" or "where in the section" -- that
    /// bookkeeping lives in DeltaMainStackMapBuilder::collect/collectLazy.
    class Reader {
        ByteReader bytes_;

    public:
        explicit Reader(uint8_t* p) : bytes_(p) {}

        uint8_t* getP() const { return bytes_.pos(); }
        void setP(uint8_t* newP) { bytes_.setPos(newP); }

        int64_t getNextInt(uint64_t size) { return bytes_.nextInt(size); }
        uint64_t getNextULEB128() { return bytes_.nextULEB128(); }
        int64_t getNextSLEB128() { return bytes_.nextSLEB128(); }

        /// Reads one Delta: (if ENABLE_REGISTERS) a register bit vector,
        /// then a stack-slot bit vector, then the derived-pointer link
        /// list. Mirrors llvm::deltamain::Delta::emit.
        Delta getNextDelta();

        /// Decodes the callee-saved-register spill info written for one
        /// function (empty when ENABLE_REGISTERS is off).
        PrologueInfo getPrologueInfo();

        /// Reads one function's complete delta-main record starting at the
        /// current position, and records a RootsInfo for each of its
        /// callsites into `pc2RootsInfo`, keyed by `functionAddress + pc`.
        void getRootsInfo(uint64_t baseOffset, FunctionAddress functionAddress,
                          std::unordered_map<FunctionAddress, RootsInfo>& pc2RootsInfo);
    };

    DeltaMainStackMapBuilder() : reader_(stackMap::deltaMainStackMapsSymbol()), sectionStart_(stackMap::deltaMainStackMapsSymbol()) {
        if (std::strcmp(Kotlin_gcStackMapScheme, compiler::DELTA_MAIN) == 0) {
            verifyMagic(reader_.getNextInt(1));
        }
    }

    /// Decodes every function's every callsite in the section, populating
    /// pc2RootsInfo()/funcAddr2PrologueInfo() for the whole binary.
    void collect();

    /// Decodes a single function's record, given a direct pointer to it
    /// (`funcStackMapAddr`), and writes the RootsInfo for `currentPC` (a
    /// PC within that function, identified by its entry point
    /// `functionPC`) into `rootsInfo`. See the class comment: not yet
    /// wired up to an emitting side.
    void collectLazy(uintptr_t functionPC, uintptr_t currentPC, uint64_t* funcStackMapAddr, RootsInfo& rootsInfo);

    std::unordered_map<uintptr_t, RootsInfo>& pc2RootsInfo() { return pc2RootsInfo_; }
    std::unordered_map<uintptr_t, PrologueInfo>& funcAddr2PrologueInfo() { return funcAddr2PrologueInfo_; }

private:
    void verifyMagic(uint8_t magic);

    Reader reader_;
    uint8_t* sectionStart_;
    std::unordered_map<uintptr_t, RootsInfo> pc2RootsInfo_;
    std::unordered_map<uintptr_t, PrologueInfo> funcAddr2PrologueInfo_;
};

} // namespace kotlin::stackMap
