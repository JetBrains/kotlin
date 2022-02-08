/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FinalizerHooksTestSupport.hpp"

#include "FinalizerHooks.hpp"

using namespace kotlin;

namespace {

testing::MockFunction<void(ObjHeader*)>* g_finalizerHook = nullptr;

void finalizerHook(ObjHeader* object) {
    g_finalizerHook->Call(object);
}

} // namespace

kotlin::FinalizerHooksTestSupport::FinalizerHooksTestSupport() {
    g_finalizerHook = &finalizerHook_;
    SetFinalizerHookForTesting(&::finalizerHook);
}

kotlin::FinalizerHooksTestSupport::~FinalizerHooksTestSupport() {
    SetFinalizerHookForTesting(nullptr);
    g_finalizerHook = nullptr;
}
