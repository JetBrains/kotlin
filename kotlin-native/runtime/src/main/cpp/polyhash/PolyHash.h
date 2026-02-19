/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_POLYHASH_H
#define RUNTIME_POLYHASH_H

// Computes polynomial hash with base = 31.
template <typename UnitType>
int polyHash(int length, UnitType const* str);

#endif  // RUNTIME_POLYHASH_H
