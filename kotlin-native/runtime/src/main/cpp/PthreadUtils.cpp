/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "PthreadUtils.h"

#include <pthread.h>
#include <sys/time.h>

namespace {

constexpr int64_t kNanosecondsInASecond = 1000000000LL;

}  // namespace

int WaitOnCondVar(
    pthread_cond_t* cond,
    pthread_mutex_t* mutex,
    uint64_t timeoutNanoseconds,
    uint64_t* microsecondsPassed) {
  struct timeval tvBefore;
  // TODO: Error reporting?
  gettimeofday(&tvBefore, nullptr);

  struct timespec ts;
  const uint64_t nanoseconds = tvBefore.tv_usec * 1000LL + timeoutNanoseconds;
  ts.tv_sec = tvBefore.tv_sec + nanoseconds / kNanosecondsInASecond;
  ts.tv_nsec = nanoseconds % kNanosecondsInASecond;
  auto result = pthread_cond_timedwait(cond, mutex, &ts);

  if (microsecondsPassed) {
    struct timeval tvAfter;
    // TODO: Error reporting?
    gettimeofday(&tvAfter, nullptr);

    *microsecondsPassed = (tvAfter.tv_sec - tvBefore.tv_sec) * 1000000LL +
        tvAfter.tv_usec - tvBefore.tv_usec;
  }

  return result;
}
