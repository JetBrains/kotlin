/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "mm/ShadowStack.hpp"

#include <algorithm>
#include <cstdint>
#include <mutex>
#include <vector>

#include "CallsChecker.hpp"

extern "C" {
extern const uint8_t __start_llvm_stackmaps[] __attribute__((weak));
extern const uint8_t __stop_llvm_stackmaps[] __attribute__((weak));
void* _Unwind_FindEnclosingFunction(void* pc) __attribute__((weak));
}

using namespace kotlin;

namespace {

struct FunctionStackMap {
    uintptr_t funcStart;
    int32_t frameOffset;
    uint32_t numSlots;
};

class StackMapTable {
public:
    static StackMapTable& instance() noexcept {
        static StackMapTable table;
        return table;
    }

    NO_EXTERNAL_CALLS_CHECK const FunctionStackMap* lookupFunction(uintptr_t pc) noexcept {
        ensureInitialized();
        if (functions_.empty() || &_Unwind_FindEnclosingFunction == nullptr) return nullptr;
        void* enclosing = _Unwind_FindEnclosingFunction(reinterpret_cast<void*>(pc - 1));
        if (enclosing == nullptr) return nullptr;
        uintptr_t funcStart = reinterpret_cast<uintptr_t>(enclosing);
        auto it = std::lower_bound(
                functions_.begin(), functions_.end(), funcStart,
                [](const FunctionStackMap& entry, uintptr_t addr) { return entry.funcStart < addr; });
        if (it == functions_.end() || it->funcStart != funcStart) return nullptr;
        return &(*it);
    }

    bool isBridgeExitPc(uintptr_t pc) noexcept {
        ensureInitialized();
        return std::binary_search(bridgeExitPcs_.begin(), bridgeExitPcs_.end(), pc);
    }

private:
    struct Header {
        uint8_t version;
        uint8_t reserved1;
        uint16_t reserved2;
        uint32_t numFunctions;
        uint32_t numConstants;
        uint32_t numRecords;
    };

    struct StkSizeRecord {
        uint64_t functionAddress;
        uint64_t stackSize;
        uint64_t recordCount;
    };

    struct Location {
        uint8_t type;
        uint8_t reserved1;
        uint16_t size;
        uint16_t dwarfRegNum;
        uint16_t reserved2;
        int32_t offset;
    };

    void ensureInitialized() noexcept {
        std::call_once(initFlag_, [this]() {
            CallsCheckerIgnoreGuard guard;
            const uint8_t* p = &__start_llvm_stackmaps[0];
            const uint8_t* end = &__stop_llvm_stackmaps[0];
            if (!p || !end || p >= end) return;

            while (p + sizeof(Header) <= end) {
                auto* hdr = reinterpret_cast<const Header*>(p);
                if (hdr->version != 3) break;
                p += sizeof(Header);

                auto* funcs = reinterpret_cast<const StkSizeRecord*>(p);
                p += hdr->numFunctions * sizeof(StkSizeRecord);
                p += hdr->numConstants * sizeof(uint64_t);

                for (uint32_t f = 0; f < hdr->numFunctions; ++f) {
                    uintptr_t funcAddr = static_cast<uintptr_t>(funcs[f].functionAddress);
                    uint64_t recCount = funcs[f].recordCount;
                    int32_t frameOffset = 0;
                    uint32_t numSlots = 0;

                    for (uint64_t r = 0; r < recCount; ++r) {
                        uint64_t patchPointId = *reinterpret_cast<const uint64_t*>(p);
                        uint32_t instructionOffset = *reinterpret_cast<const uint32_t*>(p + 8);
                        uint16_t numLocations = *reinterpret_cast<const uint16_t*>(p + 14);
                        p += 16;

                        auto* locs = reinterpret_cast<const Location*>(p);
                        if (patchPointId == UINT64_MAX) {
                            if (funcAddr != 0) {
                                bridgeExitPcs_.push_back(funcAddr + instructionOffset);
                            }
                        } else if (patchPointId > 0 && numLocations >= 1 && locs[0].type == 2) {
                            frameOffset = locs[0].offset;
                            numSlots = static_cast<uint32_t>(patchPointId);
                        }

                        p += numLocations * sizeof(Location);
                        if ((reinterpret_cast<uintptr_t>(p) & 7) != 0) {
                            p += 4;
                        }
                        uint16_t numLiveOuts = *reinterpret_cast<const uint16_t*>(p + 2);
                        p += 4 + numLiveOuts * 4;
                        if ((reinterpret_cast<uintptr_t>(p) & 7) != 0) {
                            p += 4;
                        }
                    }

                    if (funcAddr != 0 && numSlots > 0) {
                        functions_.push_back({funcAddr, frameOffset, numSlots});
                    }
                }
            }

            std::sort(functions_.begin(), functions_.end(), [](const FunctionStackMap& a, const FunctionStackMap& b) {
                return a.funcStart < b.funcStart;
            });
            std::sort(bridgeExitPcs_.begin(), bridgeExitPcs_.end());
        });
    }

    std::once_flag initFlag_;
    std::vector<FunctionStackMap> functions_;
    std::vector<uintptr_t> bridgeExitPcs_;
};

} // namespace

mm::ShadowStack::Iterator::Iterator(FrameOverlay* frame, const RunnableSegment* segments, size_t segmentCount) noexcept :
    frame_(frame), segments_(segments), segmentCount_(segmentCount), object_(begin()), end_(end()) {
    Init();
}

mm::ShadowStack::Iterator& mm::ShadowStack::Iterator::operator++() noexcept {
    ++object_;
    Init();
    return *this;
}

void mm::ShadowStack::Iterator::Init() noexcept {
    while (frame_) {
        if (object_ < end_) return;
        frame_ = frame_->previous;
        object_ = begin();
        end_ = end();
    }
    if (!compiler::stackwalkShadowStack() || segmentCount_ == 0) return;
    if (object_ < end_) return;
    AdvanceFrameWalk();
}

void mm::ShadowStack::Iterator::AdvanceFrameWalk() noexcept {
    while (segmentIndex_ < segmentCount_) {
        const auto& seg = segments_[segmentIndex_];
        if (walkFp_ == 0) {
            walkFp_ = seg.exitFp;
            walkPc_ = seg.exitPc;
            if (walkFp_ == 0) {
                ++segmentIndex_;
                continue;
            }
        } else {
            uintptr_t nextFp = *reinterpret_cast<uintptr_t*>(walkFp_);
            uintptr_t nextPc = *reinterpret_cast<uintptr_t*>(walkFp_ + sizeof(uintptr_t));
            if (nextFp <= walkFp_ || (nextFp & (sizeof(uintptr_t) - 1)) != 0) {
                walkFp_ = 0;
                walkPc_ = 0;
                ++segmentIndex_;
                continue;
            }
            walkFp_ = nextFp;
            walkPc_ = nextPc;
        }

        bool inSegment = seg.includeEntryFp ? (walkFp_ <= seg.entryFp) : (walkFp_ < seg.entryFp);
        if (!inSegment) {
            walkFp_ = 0;
            walkPc_ = 0;
            ++segmentIndex_;
            continue;
        }

        if (walkPc_ != 0) {
            if (const auto* func = StackMapTable::instance().lookupFunction(walkPc_)) {
                if (func->numSlots > 0) {
                    object_ = reinterpret_cast<ObjHeader**>(walkFp_ + func->frameOffset);
                    end_ = object_ + func->numSlots;
                    return;
                }
            }
        }
    }
    walkFp_ = 0;
    walkPc_ = 0;
    segmentIndex_ = 0;
    segmentCount_ = 0;
    object_ = nullptr;
    end_ = nullptr;
}

void mm::ShadowStack::EnterFrame(ObjHeader** start, int parameters, int count) noexcept {
    FrameOverlay* frame = reinterpret_cast<FrameOverlay*>(start);
    frame->previous = currentFrame_;
    currentFrame_ = frame;
    // TODO: maybe compress in single value somehow.
    frame->parameters = parameters;
    frame->count = count;
}

void mm::ShadowStack::LeaveFrame(ObjHeader** start, int parameters, int count) noexcept {
    FrameOverlay* frame = reinterpret_cast<FrameOverlay*>(start);
    RuntimeAssert(currentFrame_ == frame, "Frame to leave is expected to be %p, but current frame is %p", frame, currentFrame_);
    currentFrame_ = frame->previous;
}

void mm::ShadowStack::SetCurrentFrame(ObjHeader** start) noexcept {
    currentFrame_ = reinterpret_cast<FrameOverlay*>(start);
}

FrameOverlay* mm::ShadowStack::getCurrentFrame() noexcept {
    return currentFrame_;
}

ALWAYS_INLINE void mm::ShadowStack::checkCurrentFrame(FrameOverlay* frame) noexcept {
    RuntimeAssert(currentFrame_ == frame, "Frame is expected to be %p, but current frame is %p", frame, currentFrame_);
}

void mm::ShadowStack::onEnterRunnable(uintptr_t callerFp, bool includeEntryFp) noexcept {
    if (!segments_.empty() && segments_.back().exitFp != 0 &&
        callerFp >= segments_.back().exitFp && callerFp <= segments_.back().entryFp) {
        segments_.back().exitFp = 0;
        segments_.back().exitPc = 0;
    } else if (callerFp != 0) {
        segments_.push_back({callerFp, 0, 0, includeEntryFp});
    }
}

bool mm::ShadowStack::onLeaveRunnable(uintptr_t callerFp, uintptr_t callerPc, bool popSegment) noexcept {
    if (segments_.empty()) return false;
    if (popSegment || (callerPc != 0 && StackMapTable::instance().isBridgeExitPc(callerPc))) {
        segments_.pop_back();
        return false;
    }
    if (segments_.back().exitFp == 0) {
        segments_.back().exitFp = callerFp;
        segments_.back().exitPc = callerPc;
        return true;
    }
    return false;
}

mm::ShadowStack::Iterator mm::ShadowStack::begin() noexcept {
    if (!compiler::stackwalkShadowStack()) {
        return Iterator(currentFrame_, nullptr, 0);
    }
    size_t count = std::min(segments_.size(), std::size(snapshotSegments_));
    for (size_t i = 0; i < count; ++i) {
        snapshotSegments_[i] = segments_[i];
    }
    if (count > 0 && snapshotSegments_[count - 1].exitFp == 0) {
        snapshotSegments_[count - 1].exitFp = reinterpret_cast<uintptr_t>(__builtin_frame_address(0));
        snapshotSegments_[count - 1].exitPc = 0;
    }
    return Iterator(currentFrame_, snapshotSegments_, count);
}