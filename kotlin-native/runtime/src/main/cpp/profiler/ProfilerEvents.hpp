/*
* Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the LICENSE file.
*/

#pragma once

#include "KString.h"

namespace kotlin::profiler {

enum class EventKind : int32_t {
    kAllocation = 0,
    kSafePoint = 1,
    kSpecialRef = 2,
    kAllocSize = 3,
};

namespace internal {
template<EventKind kEventKind>
class EventTraits {
public:
    ALWAYS_INLINE static bool enabled() noexcept {
        return samplingFrequencyPeriod() != 0;
    }

    ALWAYS_INLINE static std::size_t samplingFrequencyPeriod() noexcept {
        return compiler::eventTrackerFrequency()[static_cast<std::size_t>(kEventKind)];
    }

    ALWAYS_INLINE static std::size_t backtraceDepth() {
        return compiler::eventTrackerBacktraceDepth()[static_cast<std::size_t>(kEventKind)];
    }
};
}

class AllocationEventTraits : public internal::EventTraits<EventKind::kAllocation> {
public:
    static constexpr auto kName = "Allocation";

    struct AllocationRecord {
        auto operator==(const AllocationRecord& other) const noexcept {
            return typeInfo_ == other.typeInfo_ && arrayLength_ == other.arrayLength_;
        }
        auto operator!=(const AllocationRecord& other) const noexcept { return !operator==(other); }

        auto toString() const -> std::string {
            auto pkg = to_string(typeInfo_->packageName_);
            auto cls = to_string(typeInfo_->relativeName_);
            auto fqName = pkg.empty() ? cls : pkg + "." + cls;
            auto res = fqName;
            if (typeInfo_->IsArray()) {
                res += "[" + std::to_string(arrayLength_) +"]";
            }
            res += " (" + std::to_string(typeInfo_->instanceSize_) + "bytes)";
            return res;
        }

        const TypeInfo* typeInfo_;
        std::size_t arrayLength_ = 0;
    };

    using Event = AllocationRecord;
};

class SafePointEventTraits : public internal::EventTraits<EventKind::kSafePoint> {
public:
    static constexpr auto kName = "SafePoint";

    struct SafePointHit {
        auto operator==(const SafePointHit&) const noexcept { return true; }
        auto operator!=(const SafePointHit&) const noexcept { return false; }

        auto toString() const -> std::string {
            return "Safe point";
        }
    };

    using Event = SafePointHit;
};

class SpecialRefEventTraits : public internal::EventTraits<EventKind::kSpecialRef> {
public:
    static constexpr auto kName = "SpecialRef";

    enum class SpecialRefKind {
        kStableRef, kWeakRef, kBackRef
    };
    enum class OpKind {
        kCreated, kDisposed
    };

    struct SpecialRefOp {
        auto operator==(const SpecialRefOp& other) const noexcept { return kind_ == other.kind_ && op_ == other.op_; }
        auto operator!=(const SpecialRefOp& other) const noexcept { return !operator==(other); }

        auto toString() const -> std::string {
            std::string kind;
            switch (kind_) {
                case SpecialRefKind::kStableRef: kind = "StableRef"; break;
                case SpecialRefKind::kWeakRef: kind = "WeakRef"; break;
                case SpecialRefKind::kBackRef: kind = "BackRef"; break;
            }
            std::string op;
            switch (op_) {
                case OpKind::kCreated: op = "created"; break;
                case OpKind::kDisposed: op = "disposed"; break;
            }
            return kind + " " + op;
        }

        SpecialRefKind kind_;
        OpKind op_;
    };

    using Event = SpecialRefOp;
};

class AllocSizeEventTraits : public internal::EventTraits<EventKind::kAllocSize> {
public:
    static constexpr auto kName = "AllocSize";

    struct Alloc {
        auto operator==(const Alloc& other) const noexcept { return size_ == other.size_; }
        auto operator!=(const Alloc& other) const noexcept { return !operator==(other); }

        auto toString() const -> std::string {
            return std::to_string(size_) + " bytes";
        }

        std::size_t size_;
    };

    using Event = Alloc;
};

}

template<>
struct std::hash<kotlin::profiler::AllocationEventTraits::Event> {
    std::size_t operator()(const kotlin::profiler::AllocationEventTraits::Event& alloc) const noexcept {
        return kotlin::CombineHash(kotlin::hashOf(alloc.typeInfo_), kotlin::hashOf(alloc.arrayLength_));
    }
};

template<>
struct std::hash<kotlin::profiler::SafePointEventTraits::Event> {
    std::size_t operator()(const kotlin::profiler::SafePointEventTraits::Event&) const noexcept {
        return 0;
    }
};

template<>
struct std::hash<kotlin::profiler::SpecialRefEventTraits::Event> {
    std::size_t operator()(const kotlin::profiler::SpecialRefEventTraits::Event& e) const noexcept {
        return kotlin::CombineHash(static_cast<std::size_t>(e.kind_), static_cast<std::size_t>(e.op_));
    }
};

template<>
struct std::hash<kotlin::profiler::AllocSizeEventTraits::Event> {
    std::size_t operator()(const kotlin::profiler::AllocSizeEventTraits::Event& e) const noexcept {
        return e.size_;
    }
};
