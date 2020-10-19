/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_CLEANER_H
#define RUNTIME_CLEANER_H

#include "Common.h"
#include "Types.h"

RUNTIME_NOTHROW void DisposeCleaner(KRef thiz);

void ShutdownCleaners(bool executeScheduledCleaners);

extern "C" KInt Kotlin_CleanerImpl_getCleanerWorker();

void ResetCleanerWorkerForTests();

#endif // RUNTIME_CLEANER_H
