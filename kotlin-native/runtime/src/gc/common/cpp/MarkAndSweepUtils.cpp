/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MarkAndSweepUtils.hpp"

void kotlin::gc::stopTheWorld(GCHandle gcHandle, const char* reason) noexcept {
    bool didSuspend = mm::RequestThreadsSuspension(reason);
    RuntimeAssert(didSuspend, "Only GC thread can request suspension");
    gcHandle.suspensionRequested();

    mm::WaitForThreadsSuspension();
    gcHandle.threadsAreSuspended();
}

void kotlin::gc::resumeTheWorld(kotlin::gc::GCHandle gcHandle) noexcept {
    mm::ResumeThreads();
    gcHandle.threadsAreResumed();
}
