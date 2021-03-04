/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_FINALIZER_HOOKS_H
#define RUNTIME_MM_FINALIZER_HOOKS_H

struct ObjHeader;

namespace kotlin {

bool HasFinalizers(ObjHeader* object) noexcept;
void RunFinalizers(ObjHeader* object) noexcept;

void SetFinalizerHookForTesting(void (*hook)(ObjHeader*)) noexcept;

} // namespace kotlin

#endif // RUNTIME_MM_FINALIZER_HOOKS_H
