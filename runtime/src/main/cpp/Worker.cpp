/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef KONAN_NO_THREADS
# define WITH_WORKERS 1
#endif

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#if WITH_WORKERS
#include <pthread.h>
#include <sys/time.h>
#endif

#include "Alloc.h"
#include "Exceptions.h"
#include "KAssert.h"
#include "Memory.h"
#include "Runtime.h"
#include "Types.h"

extern "C" {

RUNTIME_NORETURN void ThrowWorkerInvalidState();
RUNTIME_NORETURN void ThrowWorkerUnsupported();
OBJ_GETTER(WorkerLaunchpad, KRef);

}  // extern "C"

namespace {

#if WITH_WORKERS

enum {
  INVALID = 0,
  SCHEDULED = 1,
  COMPUTED = 2,
  CANCELLED = 3,
  THROWN = 4
};

enum {
  CHECKED = 0,
  UNCHECKED = 1
};

THREAD_LOCAL_VARIABLE KInt g_currentWorkerId = 0;

KNativePtr transfer(KRef object, KInt mode) {
  switch (mode) {
    case CHECKED:
    case UNCHECKED:
      if (!ClearSubgraphReferences(object, mode == CHECKED)) {
        // Release reference to the object, as it is not being managed by ObjHolder.
        UpdateRef(&object, nullptr);
        ThrowWorkerInvalidState();
        return nullptr;
      }
      return object;
  }
  return nullptr;
}

class Locker {
 public:
  explicit Locker(pthread_mutex_t* lock) : lock_(lock) {
    pthread_mutex_lock(lock_);
  }
  ~Locker() {
     pthread_mutex_unlock(lock_);
  }

 private:
  pthread_mutex_t* lock_;
};

class Future {
 public:
  Future(KInt id) : state_(SCHEDULED), id_(id) {
    pthread_mutex_init(&lock_, nullptr);
    pthread_cond_init(&cond_, nullptr);
  }

  ~Future() {
    pthread_mutex_destroy(&lock_);
    pthread_cond_destroy(&cond_);
  }

  OBJ_GETTER0(consumeResultUnlocked) {
    Locker locker(&lock_);
    while (state_ == SCHEDULED) {
      pthread_cond_wait(&cond_, &lock_);
    }
    // TODO: maybe use message from exception?
    if (state_ == THROWN)
        ThrowIllegalStateException();
    auto result = AdoptStablePointer(result_, OBJ_RESULT);
    result_ = nullptr;
    return result;
  }

  void storeResultUnlocked(KNativePtr result, bool ok);

  void cancelUnlocked();

  // Those are called with the lock taken.
  KInt state() const { return state_; }
  KInt id() const { return id_; }

 private:
  // State of future execution.
  KInt state_;
  // Integer id of the future.
  KInt id_;
  // Stable pointer with future's result.
  KNativePtr result_;
  // Lock and condition for waiting on the future.
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
};

struct Job {
  KRef (*function)(KRef, ObjHeader**);
  KNativePtr argument;
  Future* future;
  KInt transferMode;
};

class Worker {
 public:
  Worker(KInt id, bool errorReporting) : id_(id), errorReporting_(errorReporting) {
    pthread_mutex_init(&lock_, nullptr);
    pthread_cond_init(&cond_, nullptr);
  }

  ~Worker() {
    // Cleanup jobs in queue.
    for (auto job : queue_) {
      DisposeStablePointer(job.argument);
      job.future->cancelUnlocked();
    }

    pthread_mutex_destroy(&lock_);
    pthread_cond_destroy(&cond_);
  }

  void putJob(Job job, bool toFront) {
    Locker locker(&lock_);
    if (toFront)
       queue_.push_front(job);
    else
       queue_.push_back(job);
    pthread_cond_signal(&cond_);
  }

  Job getJob() {
    Locker locker(&lock_);
    while (queue_.size() == 0) {
      pthread_cond_wait(&cond_, &lock_);
    }
    auto result = queue_.front();
    queue_.pop_front();
    return result;
  }

  KInt id() const { return id_; }

  bool errorReporting() const { return errorReporting_; }

 private:
  KInt id_;
  KStdDeque<Job> queue_;
  // Lock and condition for waiting on the queue.
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
  bool errorReporting_;
};

class State {
 public:
  State() {
    pthread_mutex_init(&lock_, nullptr);
    pthread_cond_init(&cond_, nullptr);

    currentWorkerId_ = 1;
    currentFutureId_ = 1;
    currentVersion_ = 0;
  }

  ~State() {
    // TODO: some sanity check here?
    pthread_mutex_destroy(&lock_);
    pthread_cond_destroy(&cond_);
  }

  Worker* addWorkerUnlocked(bool errorReporting) {
    Locker locker(&lock_);
    Worker* worker = konanConstructInstance<Worker>(nextWorkerId(), errorReporting);
    if (worker == nullptr) return nullptr;
    workers_[worker->id()] = worker;
    return worker;
  }

  void removeWorkerUnlocked(KInt id) {
    Locker locker(&lock_);
    auto it = workers_.find(id);
    if (it == workers_.end()) return;
    workers_.erase(it);
  }

  Future* addJobToWorkerUnlocked(
      KInt id, KNativePtr jobFunction, KNativePtr jobArgument, bool toFront, KInt transferMode) {
    Future* future = nullptr;
    Worker* worker = nullptr;
    {
      Locker locker(&lock_);

      auto it = workers_.find(id);
      if (it == workers_.end()) return nullptr;
      worker = it->second;

      future = konanConstructInstance<Future>(nextFutureId());
      futures_[future->id()] = future;
    }

    Job job;
    job.function = reinterpret_cast<KRef (*)(KRef, ObjHeader**)>(jobFunction);
    job.argument = jobArgument;
    job.future = future;
    job.transferMode = transferMode;

    worker->putJob(job, toFront);

    return future;
  }

  KInt stateOfFutureUnlocked(KInt id) {
    Locker locker(&lock_);
    auto it = futures_.find(id);
    if (it == futures_.end()) return INVALID;
    return it->second->state();
  }

  OBJ_GETTER(consumeFutureUnlocked, KInt id) {
    Future* future = nullptr;
    {
      Locker locker(&lock_);
      auto it = futures_.find(id);
      if (it == futures_.end()) ThrowWorkerInvalidState();
      future = it->second;
    }

    KRef result = future->consumeResultUnlocked(OBJ_RESULT);

    {
       Locker locker(&lock_);
       auto it = futures_.find(id);
       if (it != futures_.end()) {
         futures_.erase(it);
         konanDestructInstance(future);
       }
    }

    return result;
  }

  KBoolean waitForAnyFuture(KInt version, KInt millis) {
    Locker locker(&lock_);
    if (version != currentVersion_) return false;

    if (millis < 0) {
      pthread_cond_wait(&cond_, &lock_);
      return true;
    }
    struct timeval tv;
    struct timespec ts;
    gettimeofday(&tv, nullptr);
    KLong nsDelta = millis * 1000000LL;
    ts.tv_nsec = (tv.tv_usec * 1000LL + nsDelta) % 1000000000LL;
    ts.tv_sec =  (tv.tv_sec * 1000000000LL + nsDelta) / 1000000000LL;
    pthread_cond_timedwait(&cond_, &lock_, &ts);
    return true;
  }

  void signalAnyFuture() {
    {
      Locker locker(&lock_);
      currentVersion_++;
    }
    pthread_cond_broadcast(&cond_);
  }

  KInt versionToken() {
    Locker locker(&lock_);
    return currentVersion_;
  }

  // All those called with lock taken.
  KInt nextWorkerId() { return currentWorkerId_++; }
  KInt nextFutureId() { return currentFutureId_++; }

 private:
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
  KStdUnorderedMap<KInt, Future*> futures_;
  KStdUnorderedMap<KInt, Worker*> workers_;
  KInt currentWorkerId_;
  KInt currentFutureId_;
  KInt currentVersion_;
};

State* theState() {
  static State* state = nullptr;

  if (state != nullptr) {
    return state;
  }

  State* result = konanConstructInstance<State>();

  State* old = __sync_val_compare_and_swap(&state, nullptr, result);
  if (old != nullptr) {
    konanDestructInstance(result);
    // Someone else inited this data.
    return old;
  }
  return state;
}

void Future::storeResultUnlocked(KNativePtr result, bool ok) {
  {
    Locker locker(&lock_);
    state_ = ok ? COMPUTED : THROWN;
    result_ = result;
    // Beware here: although manual clearly says that pthread_cond_signal() could be called outside
    // of the taken lock, it's not on macOS (as of 10.13.1). If moved outside of the lock,
    // some notifications are missing.
    pthread_cond_signal(&cond_);
  }
  theState()->signalAnyFuture();
}

void Future::cancelUnlocked() {
  {
    Locker locker(&lock_);
    state_ = CANCELLED;
    result_ = nullptr;
    pthread_cond_signal(&cond_);
  }
  theState()->signalAnyFuture();
}

// Defined in RuntimeUtils.kt.
extern "C" void ReportUnhandledException(KRef e);

void* workerRoutine(void* argument) {
  Worker* worker = reinterpret_cast<Worker*>(argument);

  g_currentWorkerId = worker->id();
  Kotlin_initRuntimeIfNeeded();

  while (true) {
    Job job = worker->getJob();
    if (job.function == nullptr) {
       // Termination request, notify the future.
      job.future->storeResultUnlocked(nullptr, true);
      theState()->removeWorkerUnlocked(worker->id());
      break;
    }
    ObjHolder argumentHolder;
    KRef argument = AdoptStablePointer(job.argument, argumentHolder.slot());
    // Note that this is a bit hacky, as we must not auto-release resultRef,
    // so we don't use ObjHolder.
    // It is so, as ownership is transferred.
    KRef resultRef = nullptr;
    KNativePtr result = nullptr;
    bool ok = true;
    try {
        job.function(argument, &resultRef);
        argumentHolder.clear();
        // Transfer the result.
        result = transfer(resultRef, job.transferMode);
    } catch (ObjHolder& e) {
        ok = false;
        if (worker->errorReporting())
            ReportUnhandledException(e.obj());
    }
    // Notify the future.
    job.future->storeResultUnlocked(result, ok);
  }

  Kotlin_deinitRuntimeIfNeeded();

  konanDestructInstance(worker);

  return nullptr;
}

KInt startWorker(KBoolean errorReporting) {
  Worker* worker = theState()->addWorkerUnlocked(errorReporting != 0);
  if (worker == nullptr) return -1;
  pthread_t thread = 0;
  pthread_create(&thread, nullptr, workerRoutine, worker);
  return worker->id();
}

KInt currentWorker() {
  return g_currentWorkerId;
}

KInt schedule(KInt id, KInt transferMode, KRef producer, KNativePtr jobFunction) {
  Job job;
  // Note that this is a bit hacky, as we must not auto-release jobArgumentRef,
  // so we don't use ObjHolder.
  KRef jobArgumentRef = nullptr;
  WorkerLaunchpad(producer, &jobArgumentRef);
  KNativePtr jobArgument = transfer(jobArgumentRef, transferMode);
  Future* future = theState()->addJobToWorkerUnlocked(id, jobFunction, jobArgument, false, transferMode);
  if (future == nullptr) ThrowWorkerInvalidState();
  return future->id();
}

KInt stateOfFuture(KInt id) {
  return theState()->stateOfFutureUnlocked(id);
}

OBJ_GETTER(consumeFuture, KInt id) {
  RETURN_RESULT_OF(theState()->consumeFutureUnlocked, id);
}

KInt requestTermination(KInt id, KBoolean processScheduledJobs) {
  Future* future = theState()->addJobToWorkerUnlocked(
      id, nullptr, nullptr, /* toFront = */ !processScheduledJobs, UNCHECKED);
  if (future == nullptr) ThrowWorkerInvalidState();
  return future->id();
}

KBoolean waitForAnyFuture(KInt version, KInt millis) {
  return theState()->waitForAnyFuture(version, millis);
}

KInt versionToken() {
  return theState()->versionToken();
}

OBJ_GETTER(attachObjectGraphInternal, KNativePtr stable) {
  RETURN_RESULT_OF(AdoptStablePointer, stable);
}

KNativePtr detachObjectGraphInternal(KInt transferMode, KRef producer) {
   KRef ref = nullptr;
   WorkerLaunchpad(producer, &ref);
   if (ref != nullptr) {
     return transfer(ref, transferMode);
   } else
     return nullptr;
}

#else

KInt startWorker(KBoolean errorReporting) {
  ThrowWorkerUnsupported();
  return -1;
}

KInt stateOfFuture(KInt id) {
  ThrowWorkerUnsupported();
  return 0;
}

KInt schedule(KInt id, KInt transferMode, KRef producer, KNativePtr jobFunction) {
  ThrowWorkerUnsupported();
  return 0;
}

KInt currentWorker() {
  ThrowWorkerUnsupported();
  return 0;
}

OBJ_GETTER(consumeFuture, KInt id) {
  ThrowWorkerUnsupported();
  RETURN_OBJ(nullptr);
}

KInt requestTermination(KInt id, KBoolean processScheduledJobs) {
  ThrowWorkerUnsupported();
  return -1;
}

KBoolean waitForAnyFuture(KInt versionToken, KInt millis) {
  ThrowWorkerUnsupported();
  return false;
}

KInt versionToken() {
  ThrowWorkerUnsupported();
  return 0;
}

OBJ_GETTER(attachObjectGraphInternal, KNativePtr stable) {
  ThrowWorkerUnsupported();
  return nullptr;
}

KNativePtr detachObjectGraphInternal(KInt transferMode, KRef producer) {
   ThrowWorkerUnsupported();
   return nullptr;
}

#endif  // WITH_WORKERS

}  // namespace

extern "C" {

KInt Kotlin_Worker_startInternal(KBoolean noErrorReporting) {
  return startWorker(noErrorReporting);
}

KInt Kotlin_Worker_currentInternal() {
  return currentWorker();
}

KInt Kotlin_Worker_requestTerminationWorkerInternal(KInt id, KBoolean processScheduledJobs) {
    return requestTermination(id, processScheduledJobs);
}

KInt Kotlin_Worker_executeInternal(KInt id, KInt transferMode, KRef producer, KNativePtr job) {
  return schedule(id, transferMode, producer, job);
}

KInt Kotlin_Worker_stateOfFuture(KInt id) {
  return stateOfFuture(id);
}

OBJ_GETTER(Kotlin_Worker_consumeFuture, KInt id) {
  RETURN_RESULT_OF(consumeFuture, id);
}

KBoolean Kotlin_Worker_waitForAnyFuture(KInt versionToken, KInt millis) {
  return waitForAnyFuture(versionToken, millis);
}

KInt Kotlin_Worker_versionToken() {
  return versionToken();
}

OBJ_GETTER(Kotlin_Worker_attachObjectGraphInternal, KNativePtr stable) {
  RETURN_RESULT_OF(attachObjectGraphInternal, stable);
}

KNativePtr Kotlin_Worker_detachObjectGraphInternal(KInt transferMode, KRef producer) {
  return detachObjectGraphInternal(transferMode, producer);
}

void Kotlin_Worker_freezeInternal(KRef object) {
  if (object != nullptr)
    FreezeSubgraph(object);
}

KBoolean Kotlin_Worker_isFrozenInternal(KRef object) {
  return object == nullptr || PermanentOrFrozen(object);
}

void Kotlin_Worker_ensureNeverFrozen(KRef object) {
  EnsureNeverFrozen(object);
}

}  // extern "C"
