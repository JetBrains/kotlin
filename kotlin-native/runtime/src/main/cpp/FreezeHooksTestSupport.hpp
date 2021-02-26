/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_FREEZE_HOOKS_TEST_SUPPORT_H
#define RUNTIME_MM_FREEZE_HOOKS_TEST_SUPPORT_H

#include "gtest/gtest.h"
#include "gmock/gmock.h"

struct ObjHeader;

namespace kotlin {

class FreezeHooksTestSupport {
public:
    FreezeHooksTestSupport();
    ~FreezeHooksTestSupport();

    testing::MockFunction<void(ObjHeader*)>& freezeHook() { return freezeHook_; }

private:
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> freezeHook_;
};

} // namespace kotlin

#endif // RUNTIME_MM_FREEZE_HOOKS_TEST_SUPPORT_H
