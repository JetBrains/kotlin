/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

using namespace kotlin;

void KRefSharedHolder::initLocal(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    ref_ = nullptr;
    obj_ = obj;
}

void KRefSharedHolder::init(ObjHeader* obj) {
    RuntimeAssert(obj != nullptr, "must not be null");
    ref_ = mm::createRetainedExternalRCRef(obj);
    obj_ = obj;
}

ObjHeader* KRefSharedHolder::ref() const {
    AssertThreadState(ThreadState::kRunnable);
    // ref_ may be null if created with initLocal.
    return obj_;
}

void KRefSharedHolder::dispose() {
    // Handles the case when it is not initialized. See [KotlinMutableSet/Dictionary dealloc].
    if (!ref_) {
        return;
    }
    auto ref = std::move(ref_);
    mm::releaseAndDisposeExternalRCRef(static_cast<mm::RawExternalRCRef*>(ref));
    // obj_ is dangling now.
}
