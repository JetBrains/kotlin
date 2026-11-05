/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_SHADOW_STACK
#define RUNTIME_MM_SHADOW_STACK

#include <cstddef>
#include <cstdint>
#include <vector>

#include "Memory.h"
#include "Utils.hpp"

struct FrameOverlay;
struct ObjHeader;

namespace kotlin {
namespace mm {

// Accessing current stack as provided by K/N compiler.
// Supports both legacy FrameOverlay linked list (used by C++ ObjHolder and eager shadow stack)
// and frame-pointer stack walking using .llvm_stackmaps (used by late shadow stack).
//
// Stack scanning does not lock anything and so must either be done while the mutator is stopped (or is
// running code outside Kotlin), or by the mutator itself. So, in concurrent collection case, make sure
// to do as little as possible while scanning the stack to free the mutator as soon as possible.
class ShadowStack : private Pinned {
public:
    struct RunnableSegment {
        uintptr_t entryFp = 0;
        uintptr_t exitFp = 0;
        uintptr_t exitPc = 0;
        bool includeEntryFp = false;
    };

    class Iterator {
    public:
        using difference_type = ptrdiff_t;
        using value_type = ObjHeader*;
        using pointer = value_type*;
        using reference = value_type&;
        using iterator_category = std::forward_iterator_tag;

        Iterator(FrameOverlay* frame, const RunnableSegment* segments, size_t segmentCount) noexcept;

        ObjHeader*& operator*() noexcept { return *object_; }
        Iterator& operator++() noexcept;

        bool operator==(const Iterator& rhs) const noexcept {
            return frame_ == rhs.frame_ && segmentIndex_ == rhs.segmentIndex_ &&
                   walkFp_ == rhs.walkFp_ && object_ == rhs.object_;
        }
        bool operator!=(const Iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        void Init() noexcept;
        void AdvanceFrameWalk() noexcept;

        // TODO: This copies the approach in the old MM. Do we need to also traverse function parameters in the new MM?
        ObjHeader** begin() noexcept { return frame_ ? reinterpret_cast<ObjHeader**>(frame_ + 1) + frame_->parameters : nullptr; }
        ObjHeader** end() noexcept {
            constexpr int kFrameOverlaySlots = sizeof(FrameOverlay) / sizeof(ObjHeader**);
            return frame_ ? begin() + frame_->count - kFrameOverlaySlots - frame_->parameters : nullptr;
        }

        FrameOverlay* frame_ = nullptr;
        const RunnableSegment* segments_ = nullptr;
        size_t segmentCount_ = 0;
        size_t segmentIndex_ = 0;
        uintptr_t walkFp_ = 0;
        uintptr_t walkPc_ = 0;
        ObjHeader** object_ = nullptr;
        ObjHeader** end_ = nullptr;
    };

    void EnterFrame(ObjHeader** start, int parameters, int count) noexcept;
    void LeaveFrame(ObjHeader** start, int parameters, int count) noexcept;
    void SetCurrentFrame(ObjHeader** start) noexcept;
    FrameOverlay* getCurrentFrame() noexcept;
    void checkCurrentFrame(FrameOverlay* frame) noexcept;

    void onEnterRunnable(uintptr_t callerFp, bool includeEntryFp = false) noexcept;
    bool onLeaveRunnable(uintptr_t callerFp, uintptr_t callerPc = 0, bool popSegment = false) noexcept;

    Iterator begin() noexcept;
    Iterator end() noexcept { return Iterator(nullptr, nullptr, 0); }

private:
    FrameOverlay* currentFrame_ = nullptr;
    std::vector<RunnableSegment> segments_;
    RunnableSegment snapshotSegments_[64];
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_SHADOW_STACK
