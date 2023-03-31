/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_WEAK_H
#define RUNTIME_WEAK_H

#include "Memory.h"

extern "C" {

// Atomically clears counter object reference.
void WeakReferenceCounterClear(ObjHeader* counter);

} // extern "C"

#endif // RUNTIME_WEAK_H
