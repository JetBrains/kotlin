#pragma once

#include "KAssert.h"
#include "Logging.hpp"

#include <cstdint>
#include <iostream>
#include <map>
#include <set>
#include <sstream>
#include <string>
#include <tuple>
#include <vector>

namespace kotlin::stackMap {

/// A single GC root location: either a callee-saved register or a stack
/// slot, as decoded from a delta-main stack map (see
/// DeltaMainStackMapEncoder::enumerate in the LLVM-side port for the
/// register numbering this decodes).
struct RootLocation {
    enum RootLocationType { Register, Direct, Indirect };

    RootLocationType Type;
    int32_t RegBit;
    int32_t Offset;

    static constexpr int32_t kRegMaskShift = 19;

    RootLocation() = default;
    RootLocation(RootLocationType type, int32_t regBit, int32_t offset)
        : Type(type), RegBit(regBit), Offset(offset) {}

    static RootLocation ConstructRegister(int32_t regBit) {
        return RootLocation(RootLocationType::Register, regBit, -1);
    }
    static RootLocation ConstructIndirect(int32_t offset) {
        return RootLocation(RootLocationType::Indirect, -1, offset);
    }

    // Traversal order: small register -> large register -> large slot
    // offset -> small slot offset.
    bool operator<(const RootLocation& other) const {
        auto t1 = std::tie(Type, RegBit);
        auto t2 = std::tie(other.Type, other.RegBit);
        if (t1 != t2) return t1 < t2;
        return Offset > other.Offset;
    }

    bool operator==(const RootLocation& other) const {
        return std::tie(Type, RegBit, Offset) == std::tie(other.Type, other.RegBit, other.Offset);
    }

    friend std::ostream& operator<<(std::ostream& os, const RootLocation& loc) {
        os << "[" << (loc.Type == RootLocation::Register ? "REG" :
                      loc.Type == RootLocation::Direct    ? "DIR" :
                      loc.Type == RootLocation::Indirect  ? "IND" : "UNDEF");

        if (loc.Type == RootLocation::Register || loc.Type == RootLocation::Direct) {
            os << " | Reg: x" << (loc.RegBit + kRegMaskShift);
        }
        if (loc.Type == RootLocation::Direct || loc.Type == RootLocation::Indirect) {
            os << " | Off: " << loc.Offset;
        }
        return os << "]";
    }
};

/// The set of live GC roots at one call site, as a map from each base
/// pointer location to the (possibly empty) set of derived-pointer
/// locations computed from it.
class RootsInfo {
public:
    RootsInfo() = default;

    bool operator==(const RootsInfo& other) const { return base2Derived_ == other.base2Derived_; }

    /// Records that `base` is live, with the given (possibly empty) set of
    /// derived pointers computed from it. Each `base` may only be added
    /// once.
    void addLink(const RootLocation& base, const std::vector<RootLocation>& derived) {
        RuntimeAssert(base2Derived_.find(base) == base2Derived_.end(), "Expected that base will be used once");
        base2Derived_[base] = std::set<RootLocation>(derived.begin(), derived.end());
    }

    /// Iterates every root location: each base, followed by its derived
    /// pointers, in `base2Derived_` key order.
    class AllLocationsIterator {
        using MapIt = std::map<RootLocation, std::set<RootLocation>>::const_iterator;
        using SetIt = std::set<RootLocation>::const_iterator;

        MapIt mapIt_;
        MapIt mapEnd_;
        SetIt setIt_;
        bool onBase_;

    public:
        AllLocationsIterator(MapIt begin, MapIt end) : mapIt_(begin), mapEnd_(end), onBase_(true) {
            if (mapIt_ != mapEnd_) {
                setIt_ = mapIt_->second.begin();
            }
        }

        const RootLocation& operator*() const { return onBase_ ? mapIt_->first : *setIt_; }

        AllLocationsIterator& operator++() {
            if (onBase_) {
                if (setIt_ != mapIt_->second.end()) {
                    onBase_ = false;
                } else {
                    ++mapIt_;
                    if (mapIt_ != mapEnd_) setIt_ = mapIt_->second.begin();
                }
            } else {
                ++setIt_;
                if (setIt_ == mapIt_->second.end()) {
                    onBase_ = true;
                    ++mapIt_;
                    if (mapIt_ != mapEnd_) setIt_ = mapIt_->second.begin();
                }
            }
            return *this;
        }

        bool operator!=(const AllLocationsIterator& other) const {
            if (mapIt_ != other.mapIt_) return true;
            if (mapIt_ == mapEnd_) return false;
            return onBase_ != other.onBase_ || (!onBase_ && setIt_ != other.setIt_);
        }
    };

    AllLocationsIterator begin() const { return AllLocationsIterator(base2Derived_.begin(), base2Derived_.end()); }
    AllLocationsIterator end() const { return AllLocationsIterator(base2Derived_.end(), base2Derived_.end()); }

    size_t totalCount() const {
        size_t count = 0;
        for (const auto& [base, deriveds] : base2Derived_) {
            count += 1 + deriveds.size();
        }
        return count;
    }

    void log() const {
        for (const auto& [base, deriveds] : base2Derived_) {
            std::ostringstream ossBase;
            ossBase << "    | " << base;
            RuntimeLogDebug({kTagGC}, "%s", ossBase.str().c_str());

            for (const auto& derived : deriveds) {
                std::ostringstream ossDerived;
                ossDerived << "    | ---> " << derived;
                RuntimeLogDebug({kTagGC}, "%s", ossDerived.str().c_str());
            }
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

    std::map<RootLocation, std::set<RootLocation>> base2Derived_;
};

} // namespace kotlin::stackMap
