/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_SHAREDREF_H
#define RUNTIME_SHAREDREF_H

#include "Common.h"
#include "Types.h"

RUNTIME_NOTHROW void DisposeWorkerBoundReference(KRef thiz);

RUNTIME_NOTHROW void WorkerBoundReferenceFreezeHook(KRef thiz);

#endif  // RUNTIME_SHAREDREF_H
