/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

#include "ExternalRCRef.hpp"

using namespace kotlin;

extern "C" RUNTIME_NOTHROW void KRefSharedHolder_initLocal(KRefSharedHolder* holder, ObjHeader* obj) {
    holder->ref_ = nullptr;
    holder->obj_ = obj;
}

extern "C" RUNTIME_NOTHROW void KRefSharedHolder_init(KRefSharedHolder* holder, ObjHeader* obj) {
    holder->ref_ = mm::createRetainedExternalRCRef(obj);
    holder->obj_ = obj;
}

extern "C" RUNTIME_NOTHROW void KRefSharedHolder_dispose(KRefSharedHolder* holder) {
    auto ref = std::move(holder->ref_);
    mm::releaseAndDisposeExternalRCRef(static_cast<mm::RawExternalRCRef*>(ref));
    // obj_ is dangling now.
}

extern "C" RUNTIME_NOTHROW ObjHeader* KRefSharedHolder_ref(const KRefSharedHolder* holder) {
    AssertThreadState(ThreadState::kRunnable);
    // ref_ may be null if created with initLocal.
    return holder->obj_;
}
