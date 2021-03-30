/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_SHADOW_STACK
#define RUNTIME_MM_SHADOW_STACK

#include "Memory.h"
#include "Utils.hpp"

struct FrameOverlay;
struct ObjHeader;

namespace kotlin {
namespace mm {

// Accessing current stack as provided by K/N compiler. The compiler calls `EnterFrame` when
// it has allocated and zeroed stack space in the function prologue. And it calls `LeaveFrame` in
// the function epilogue (both for regular return and for exception unwinding).
//
// Stack scanning does not lock anything and so must either be done while the mutator is stopped (or is
// running code outside Kotlin), or by the mutator itself. So, in concurrent collection case, make sure
// to do as little as possible while scanning the stack to free the mutator as soon as possible.
//
// TODO: This is currently incompatible with stack-allocated objects. Fix it.
class ShadowStack : private Pinned {
public:
    class Iterator {
    public:
        explicit Iterator(FrameOverlay* frame) noexcept : frame_(frame), object_(begin()), end_(end()) { Init(); }

        ObjHeader*& operator*() noexcept { return *object_; }
        Iterator& operator++() noexcept;

        bool operator==(const Iterator& rhs) const noexcept { return frame_ == rhs.frame_ && object_ == rhs.object_; }
        bool operator!=(const Iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        void Init() noexcept;

        // TODO: This copies the approach in the old MM. Do we need to also traverse function parameters in the new MM?
        ObjHeader** begin() noexcept { return frame_ ? reinterpret_cast<ObjHeader**>(frame_ + 1) + frame_->parameters : nullptr; }
        ObjHeader** end() noexcept {
            constexpr int kFrameOverlaySlots = sizeof(FrameOverlay) / sizeof(ObjHeader**);
            return frame_ ? begin() + frame_->count - kFrameOverlaySlots - frame_->parameters : nullptr;
        }

        FrameOverlay* frame_;
        ObjHeader** object_ = nullptr;
        ObjHeader** end_ = nullptr;
    };

    void EnterFrame(ObjHeader** start, int parameters, int count) noexcept;
    void LeaveFrame(ObjHeader** start, int parameters, int count) noexcept;

    Iterator begin() noexcept { return Iterator(currentFrame_); }
    Iterator end() noexcept { return Iterator(nullptr); }

private:
    FrameOverlay* currentFrame_ = nullptr;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_SHADOW_STACK
