/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_PTHREAD_UTILS_H
#define RUNTIME_PTHREAD_UTILS_H

#include <cstdint>
#include <pthread.h>

// Releases mutex and waits on cond for timeoutNanoseconds.
// Returns ETIMEDOUT if timeoutNanoseconds has passed.
int WaitOnCondVar(
    pthread_cond_t* cond,
    pthread_mutex_t* mutex,
    uint64_t timeoutNanoseconds,
    uint64_t* microsecondsPassed = nullptr);

#endif  // RUNTIME_PTHREAD_UTILS_H
