/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ShadowStack.hpp"

using namespace kotlin;

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