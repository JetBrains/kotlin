#include "DeltaMainStackMap.hpp"

#include "CompilerConstants.hpp"
#include "DeltaMath.hpp"
#include "KAssert.h"
#include "VarIntCodec.hpp"

#include <algorithm>
#include <cinttypes>
#include <set>
#include <utility>
#include <vector>

#if defined(__APPLE__)
#include <dlfcn.h>
#include <mach-o/getsect.h>
#endif

namespace kotlin::stackMap {

namespace {

constexpr size_t kRecordHeaderSize = 8;

std::pair<uint8_t*, uint8_t*> findSection() noexcept {
#if defined(__APPLE__)
    Dl_info info;
    if (dladdr(reinterpret_cast<const void*>(&findSection), &info) == 0) return {nullptr, nullptr};
    unsigned long size = 0;
    uint8_t* begin = getsectiondata(
            reinterpret_cast<const mach_header_64*>(info.dli_fbase), "__LLVM_STACKMAPS", "__llvm_stackmaps", &size);
    if (begin == nullptr) return {nullptr, nullptr};
    return {begin, begin + size};
#else
    return {nullptr, nullptr};
#endif
}

Delta readDelta(ByteReader& reader) {
    Delta delta;
    delta.slots = reader.nextULEB128BitVector();
    return delta;
}

struct Record {
    uintptr_t functionStart;
    int64_t baseOffset;
    Delta base;
    std::vector<std::pair<uint64_t, uint64_t>> pc2delta;
    std::vector<Delta> deltas;
    uint8_t* end;

    RootsInfo rootsInfo(uint64_t deltaIdx) const {
        RuntimeCheck(deltaIdx < deltas.size(), "Delta index is out of range");
        return (base ^ deltas[deltaIdx]).toRootInfo(baseOffset);
    }
};

Record readRecord(uint8_t* record) {
    ByteReader reader(record);
    Record result;
    result.functionStart = reinterpret_cast<uintptr_t>(record) + static_cast<uintptr_t>(reader.nextInt(4));
    reader.nextInt(4);
    result.baseOffset = reader.nextSLEB128();
    reader.nextULEB128();
    result.base = readDelta(reader);

    std::set<uint64_t> deltaIndices;
    uint64_t pc2deltaSize = reader.nextULEB128();
    for (uint64_t i = 0; i < pc2deltaSize; i++) {
        uint64_t pc = reader.nextULEB128();
        uint64_t deltaIdx = reader.nextULEB128();
        deltaIndices.insert(deltaIdx);
        result.pc2delta.emplace_back(pc, deltaIdx);
    }

    result.deltas.reserve(deltaIndices.size());
    for (uint64_t i = 0; i < deltaIndices.size(); i++) {
        result.deltas.push_back(readDelta(reader));
    }
    result.end = reader.pos();
    return result;
}

} // namespace

DeltaMainStackMap::DeltaMainStackMap() noexcept : lazy_(compiler::gcStackMapLazy()) {
    std::tie(begin_, end_) = findSection();
    if (compiler::gcStackMapScheme() == compiler::GCStackMapScheme::kDeltaMain) {
        RuntimeCheck(begin_ != nullptr && begin_ < end_, "Delta-main stack map section is not found or corrupted");
        if (!lazy_) {
            collect();
        }
    }
}

void DeltaMainStackMap::collect() {
    uint8_t* position = begin_;
    while (position < end_) {
        Record record = readRecord(position);
        for (const auto& [pc, deltaIdx] : record.pc2delta) {
            RootsInfo info = record.rootsInfo(deltaIdx);
            info.log(record.functionStart, record.functionStart + pc);
            pc2RootsInfo_[record.functionStart + pc] = std::move(info);
        }
        position = record.end;
    }
    RuntimeCheck(position == end_, "Stack map records do not fill the section");
}

bool DeltaMainStackMap::tryGetRootsInfo(const uint64_t* fp, uintptr_t pc, RootsInfo& rootsInfo) const {
    if (!lazy_) {
        auto it = pc2RootsInfo_.find(pc);
        if (it == pc2RootsInfo_.end()) return false;
        rootsInfo = it->second;
        return true;
    }

    auto address = static_cast<uintptr_t>(fp[-2]);
    auto begin = reinterpret_cast<uintptr_t>(begin_);
    auto end = reinterpret_cast<uintptr_t>(end_);
    if (address < begin || address > end - kRecordHeaderSize) return false;

    ByteReader header(reinterpret_cast<uint8_t*>(address));
    uintptr_t functionStart = address + static_cast<uintptr_t>(header.nextInt(4));
    auto functionSize = static_cast<uint32_t>(header.nextInt(4));
    if (pc - functionStart >= functionSize) return false;

    Record record = readRecord(reinterpret_cast<uint8_t*>(address));
    uint64_t targetPc = pc - functionStart;
    auto it = std::lower_bound(
            record.pc2delta.begin(), record.pc2delta.end(), targetPc, [](const auto& pair, uint64_t val) { return pair.first < val; });
    RuntimeCheck(it != record.pc2delta.end() && it->first == targetPc,
            "No stack map for pc offset %" PRIu64 " in function at %p", targetPc, reinterpret_cast<void*>(functionStart));

    rootsInfo = record.rootsInfo(it->second);
    rootsInfo.log(functionStart, pc);
    return true;
}

} // namespace kotlin::stackMap
