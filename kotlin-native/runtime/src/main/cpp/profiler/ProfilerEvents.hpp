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
            if (typeInfo_->IsArray()) {
                return fqName + "[" + std::to_string(arrayLength_) +"]";
            }
            return fqName;
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

    struct SpecialRefCreation {
        auto operator==(const SpecialRefCreation& other) const noexcept { return kind_ == other.kind_; }
        auto operator!=(const SpecialRefCreation& other) const noexcept { return !operator==(other); }

        auto toString() const -> std::string {
            switch (kind_) {
                case SpecialRefKind::kStableRef: return "StableRef";
                case SpecialRefKind::kWeakRef: return "WeakRef";
                case SpecialRefKind::kBackRef: return "BackRef";
            }
        }

        SpecialRefKind kind_;
    };

    using Event = SpecialRefCreation;
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
        return static_cast<std::size_t>(e.kind_);
    }
};
