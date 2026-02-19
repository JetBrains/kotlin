/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstdint>

#include "AuxiliaryGCThreads.hpp"

#include "Allocator.hpp"
#include "Barriers.hpp"
#include "GCScheduler.hpp"
#include "GCState.hpp"
#include "MarkAndSweepUtils.hpp"
#include "ParallelMark.hpp"

using namespace kotlin;

namespace {

template <typename Body>
UtilityThread createGCThread(const char* name, Body&& body) {
    return UtilityThread(std::string_view(name), [name, body] {
        RuntimeLogDebug({kTagGC}, "%s %" PRIuPTR " starts execution", name, konan::currentThreadId());
        body();
        RuntimeLogDebug({kTagGC}, "%s %" PRIuPTR " finishes execution", name, konan::currentThreadId());
    });
}

} // namespace

gc::internal::AuxiliaryGCThreads::AuxiliaryGCThreads(mark::ParallelMark& markDispatcher, size_t count) noexcept :
    markDispatcher_(markDispatcher) {
    startThreads(count);
}

void gc::internal::AuxiliaryGCThreads::stopThreads() noexcept {
    threads_.clear();
}

void gc::internal::AuxiliaryGCThreads::startThreads(size_t count) noexcept {
    RuntimeAssert(threads_.empty(), "Auxiliary threads must have been cleared");
    for (size_t i = 0; i < count; ++i) {
        threads_.emplace_back(createGCThread("Auxiliary GC thread", [this] { body(); }));
    }
}

void gc::internal::AuxiliaryGCThreads::body() noexcept {
    RuntimeAssert(!compiler::gcMarkSingleThreaded(), "Should not reach here during single threaded mark");
    while (!markDispatcher_.shutdownRequested()) {
        markDispatcher_.runAuxiliary();
    }
}
