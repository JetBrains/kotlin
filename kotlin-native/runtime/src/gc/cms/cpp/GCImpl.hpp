/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Barriers.hpp"
#include "CompilerConstants.hpp"
#include "ConcurrentMark.hpp"
#include "CmsGCTraits.hpp"
#if defined(__aarch64__)
#include "DeltaMainStackMap.hpp"
#endif
#include "gc/GC.hpp"
#include "gc/GCState.hpp"
#include "gc/MainGCThread.hpp"

namespace kotlin {
namespace gc {

// Concurrent mark & sweep. The GC runs in a separate thread, finalizers run in another thread of their own.
class GC::Impl : private Pinned {
public:
    Impl(alloc::Allocator& allocator, gcScheduler::GCScheduler& gcScheduler, bool mutatorsCooperate, size_t auxGCThreads) noexcept :
#if defined(__aarch64__)
        gcThread_(state_, allocator, gcScheduler, mark_), stackMapBuilder_() {
        if (compiler::gcStackMapScheme() == compiler::GCStackMapScheme::kDeltaMain) {
            stackMapBuilder_.collect();
        }
#else
        gcThread_(state_, allocator, gcScheduler, mark_) {
#endif
        RuntimeAssert(!mutatorsCooperate, "Cooperative mutators aren't supported yet");
        RuntimeAssert(auxGCThreads == 0, "Auxiliary GC threads aren't supported yet");
    }

    GCStateHolder state_;
    mark::ConcurrentMark mark_{};
    internal::MainGCThread<internal::CmsGCTraits> gcThread_;
#if defined(__aarch64__)
    stackMap::DeltaMainStackMapBuilder stackMapBuilder_;
#endif
};

class GC::ThreadData::Impl : private Pinned {
public:
#if defined(__aarch64__)
    Impl(mark::ConcurrentMark& mark, mm::ThreadData& threadData, stackMap::DeltaMainStackMapBuilder& stackMapBuilder) noexcept : mark_(mark, threadData),
        stackMapBuilder_(stackMapBuilder){}
#else
    Impl(mark::ConcurrentMark& mark, mm::ThreadData& threadData) noexcept : mark_(mark, threadData) {}
#endif

    barriers::BarriersThreadData barriers_;
    mark::ConcurrentMark::ThreadData mark_;
#if defined(__aarch64__)
    stackMap::DeltaMainStackMapBuilder& stackMapBuilder_;
#endif
};

} // namespace gc
} // namespace kotlin
