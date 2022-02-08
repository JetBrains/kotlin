/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_FREEZING_H
#define RUNTIME_MM_FREEZING_H

struct ObjHeader;

namespace kotlin {
namespace mm {

bool IsFrozen(const ObjHeader* object) noexcept;

// If some object in the `root` subgraph is marked with `EnsureNeverFrozen` only
// freeze hooks will be executed, and the offending object will be return. Otherwise,
// all objects in the subgraph will be frozen and `nullptr` will be returned.
// Note: not thread safe.
ObjHeader* FreezeSubgraph(ObjHeader* root) noexcept;

// If `object` is already frozen returns false.
// Note: not thread safe.
bool EnsureNeverFrozen(ObjHeader* object) noexcept;

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_FREEZING_H
