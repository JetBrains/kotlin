/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCImplTestSupport.hpp"

using namespace kotlin;

void gc::test_support::reconfigureGCParallelism(
        gc::GC::Impl& gc, size_t maxParallelism, bool mutatorsCooperate, size_t auxGCThreads) noexcept {
    if (compiler::gcMarkSingleThreaded()) {
        RuntimeCheck(auxGCThreads == 0, "Auxiliary GC threads must not be created with gcMarkSingleThread");
        return;
    }
    auto mainGCLock = mm::GlobalData::Instance().gc().gcLock();
    gc.markDispatcher_.reset(maxParallelism, mutatorsCooperate, [&gc] { gc.auxThreads_.stopThreads(); });
    gc.auxThreads_.startThreads(auxGCThreads);
}
