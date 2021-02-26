/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "FreezeHooksTestSupport.hpp"

#include "FreezeHooks.hpp"

using namespace kotlin;

namespace {

testing::MockFunction<void(ObjHeader*)>* g_freezeHook = nullptr;

void freezeHook(ObjHeader* object) {
    g_freezeHook->Call(object);
}

} // namespace

kotlin::FreezeHooksTestSupport::FreezeHooksTestSupport() {
    g_freezeHook = &freezeHook_;
    SetFreezeHookForTesting(&::freezeHook);
}

kotlin::FreezeHooksTestSupport::~FreezeHooksTestSupport() {
    SetFreezeHookForTesting(nullptr);
    g_freezeHook = nullptr;
}
