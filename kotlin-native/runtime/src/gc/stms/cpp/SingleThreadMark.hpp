/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "MarkTraits.hpp"

namespace kotlin::gc::internal {

class SingleThreadMark {
public:
    void setupBeforeSTW(GCHandle gcHandle) {
        gcHandle_ = gcHandle;
    }

    void markInSTW() {
        gc::collectRootSet<internal::MarkTraits>(gcHandle(), markQueue_, [](mm::ThreadData &) { return true; });
        gc::Mark<internal::MarkTraits>(gcHandle(), markQueue_);
        gc::processWeaks<DefaultProcessWeaksTraits>(gcHandle(), mm::ExternalRCRefRegistry::instance());
    }

    void requestShutdown() { /* no-op */ }

private:
    GCHandle& gcHandle() {
        RuntimeAssert(gcHandle_.isValid(), "GCHandle must be initialized");
        return gcHandle_;
    }

    GCHandle gcHandle_ = GCHandle::invalid();
    MarkQueue markQueue_{};
};

}