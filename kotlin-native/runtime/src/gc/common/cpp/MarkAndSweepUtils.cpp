/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MarkAndSweepUtils.hpp"

uint64_t kotlin::gc::stopTheWorld(GCHandle gcHandle, const char* reason) noexcept {
    auto id = mm::RequestThreadsSuspension(reason);
    gcHandle.suspensionRequested();

    mm::WaitForThreadsSuspension(id);
    gcHandle.threadsAreSuspended();

    return id;
}

void kotlin::gc::resumeTheWorld(kotlin::gc::GCHandle gcHandle, uint64_t id) noexcept {
    mm::ResumeThreads(id);
    gcHandle.threadsAreResumed();
}
