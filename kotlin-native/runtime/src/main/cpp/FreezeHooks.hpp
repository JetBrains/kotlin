/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_FREEZE_HOOKS_H
#define RUNTIME_MM_FREEZE_HOOKS_H

struct ObjHeader;
struct TypeInfo;

namespace kotlin {

// These hooks are only allowed to modify `object` subgraph.
void RunFreezeHooks(ObjHeader* object) noexcept;

void SetFreezeHookForTesting(void (*hook)(ObjHeader*)) noexcept;

} // namespace kotlin

#endif // RUNTIME_MM_FREEZE_HOOKS_H
