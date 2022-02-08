/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_FINALIZER_HOOKS_TEST_SUPPORT_H
#define RUNTIME_MM_FINALIZER_HOOKS_TEST_SUPPORT_H

#include "gtest/gtest.h"
#include "gmock/gmock.h"

struct ObjHeader;

namespace kotlin {

class FinalizerHooksTestSupport {
public:
    FinalizerHooksTestSupport();
    ~FinalizerHooksTestSupport();

    testing::MockFunction<void(ObjHeader*)>& finalizerHook() { return finalizerHook_; }

private:
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> finalizerHook_;
};

} // namespace kotlin

#endif // RUNTIME_MM_FINALIZER_HOOKS_TEST_SUPPORT_H
