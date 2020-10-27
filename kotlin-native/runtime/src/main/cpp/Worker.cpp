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
#define WITH_WORKERS 1
#endif

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#if WITH_WORKERS
#include <pthread.h>
#include "PthreadUtils.h"
#endif

#include "Alloc.h"
#include "Exceptions.h"
#include "KAssert.h"
#include "Memory.h"
#include "Runtime.h"
#include "Types.h"
#include "Worker.h"

extern "C" {

RUNTIME_NORETURN void ThrowWorkerInvalidState();
RUNTIME_NORETURN void ThrowWorkerUnsupported();
OBJ_GETTER(WorkerLaunchpad, KRef);

}  // extern "C"

#if WITH_WORKERS

namespace {

class Future;

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

enum JobKind {
  JOB_NONE = 0,
  JOB_TERMINATE = 1,
  // Order is important in sense that all job kinds after this one is considered
  // processed for APIs returning request process status.
  JOB_REGULAR = 2,
  JOB_EXECUTE_AFTER = 3,
};

enum class WorkerKind {
  kNative,  // Workers created using Worker.start public API.
  kOther,   // Any other kind of workers.
};

struct Job {
  enum JobKind kind;
  union {
    struct {
      KRef (*function)(KRef, ObjHeader**);
      KNativePtr argument;
      Future* future;
      KInt transferMode;
    } regularJob;

    struct {
      Future* future;
      bool waitDelayed;
    } terminationRequest;

    struct {
      KNativePtr operation;
      uint64_t whenExecute;
    } executeAfter;
  };
};

struct JobCompare {
  bool operator() (const Job& lhs, const Job& rhs) const {
    RuntimeAssert(lhs.kind == JOB_EXECUTE_AFTER && rhs.kind == JOB_EXECUTE_AFTER, "Must be delayed jobs");
    return lhs.executeAfter.whenExecute < rhs.executeAfter.whenExecute;
  }
};

typedef KStdOrderedSet<Job, JobCompare> DelayedJobSet;

}  // namespace

class Worker {
 public:
  Worker(KInt id, bool errorReporting, KRef customName, WorkerKind kind)
      : id_(id),
        kind_(kind),
        errorReporting_(errorReporting) {
    name_ = customName != nullptr ? CreateStablePointer(customName) : nullptr;
    pthread_mutex_init(&lock_, nullptr);
    pthread_cond_init(&cond_, nullptr);
  }

  ~Worker();

  void startEventLoop();

  void putJob(Job job, bool toFront);
  void putDelayedJob(Job job);

  bool waitDelayed(bool blocking);

  Job getJob(bool blocking);

  KLong checkDelayedLocked();

  bool waitForQueueLocked(KLong timeoutMicroseconds, KLong* remaining);

  JobKind processQueueElement(bool blocking);

  bool park(KLong timeoutMicroseconds, bool process);

  KInt id() const { return id_; }

  bool errorReporting() const { return errorReporting_; }

  KNativePtr name() const { return name_; }

  WorkerKind kind() const { return kind_; }

  pthread_t thread() const { return thread_; }

 private:
  KInt id_;
  WorkerKind kind_;
  KStdDeque<Job> queue_;
  DelayedJobSet delayed_;
  // Stable pointer with worker's name.
  KNativePtr name_;
  // Lock and condition for waiting on the queue.
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
  // If errors to be reported on console.
  bool errorReporting_;
  bool terminated_ = false;
  pthread_t thread_ = 0;
};

#endif  // WITH_WORKERS

namespace {

#if WITH_WORKERS

THREAD_LOCAL_VARIABLE Worker* g_worker = nullptr;

KNativePtr transfer(ObjHolder* holder, KInt mode) {
  void* result = CreateStablePointer(holder->obj());
  if (!ClearSubgraphReferences(holder->obj(), mode == CHECKED)) {
    DisposeStablePointer(result);
    ThrowWorkerInvalidState();
  }
  holder->clear();
  return result;
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
    clear();
    pthread_mutex_destroy(&lock_);
    pthread_cond_destroy(&cond_);
  }

  void clear() {
    Locker locker(&lock_);
    if (result_ != nullptr) {
      // No one cared to consume result - dispose it.
      DisposeStablePointer(result_);
      result_ = nullptr;
    }
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

  Worker* addWorkerUnlocked(bool errorReporting, KRef customName, WorkerKind kind) {
    Worker* worker = nullptr;
    {
      Locker locker(&lock_);
      worker = konanConstructInstance<Worker>(nextWorkerId(), errorReporting, customName, kind);
      if (worker == nullptr) return nullptr;
      workers_[worker->id()] = worker;
    }
    GC_RegisterWorker(worker);
    return worker;
  }

  void removeWorkerUnlocked(KInt id) {
    Locker locker(&lock_);
    auto it = workers_.find(id);
    if (it == workers_.end()) return;
    Worker* worker = it->second;
    if (worker->kind() == WorkerKind::kNative) {
      terminating_native_workers_[id] = worker->thread();
    }
    workers_.erase(it);
  }

  void destroyWorkerUnlocked(Worker* worker) {
    {
      Locker locker(&lock_);
      auto id = worker->id();
      auto it = workers_.find(id);
      if (it != workers_.end()) {
        workers_.erase(it);
      }
    }
    GC_UnregisterWorker(worker);
    konanDestructInstance(worker);
  }

  Future* addJobToWorkerUnlocked(
      KInt id, KNativePtr jobFunction, KNativePtr jobArgument, bool toFront, KInt transferMode) {
    Future* future = nullptr;
    Worker* worker = nullptr;
    Locker locker(&lock_);

    auto it = workers_.find(id);
    if (it == workers_.end()) return nullptr;
    worker = it->second;

    future = konanConstructInstance<Future>(nextFutureId());
    futures_[future->id()] = future;

    Job job;
    if (jobFunction == nullptr) {
      job.kind = JOB_TERMINATE;
      job.terminationRequest.future = future;
      job.terminationRequest.waitDelayed = !toFront;
    } else {
      job.kind = JOB_REGULAR;
      job.regularJob.function = reinterpret_cast<KRef (*)(KRef, ObjHeader**)>(jobFunction);
      job.regularJob.argument = jobArgument;
      job.regularJob.future = future;
      job.regularJob.transferMode = transferMode;
    }

    worker->putJob(job, toFront);

    return future;
  }

  bool executeJobAfterInWorkerUnlocked(KInt id, KRef operation, KLong afterMicroseconds) {
    Worker* worker = nullptr;
    Locker locker(&lock_);

    RuntimeAssert(afterMicroseconds >= 0, "afterMicroseconds cannot be negative");

    auto it = workers_.find(id);
    if (it == workers_.end()) {
      return false;
    }
    worker = it->second;
    Job job;
    job.kind = JOB_EXECUTE_AFTER;
    job.executeAfter.operation = CreateStablePointer(operation);
    if (afterMicroseconds == 0) {
      worker->putJob(job, false);
    } else {
      job.executeAfter.whenExecute = konan::getTimeMicros() + afterMicroseconds;
      worker->putDelayedJob(job);
    }
    return true;
  }

  bool scheduleJobInWorkerUnlocked(KInt id, KNativePtr operationStablePtr) {
      Worker* worker = nullptr;
      Locker locker(&lock_);

      auto it = workers_.find(id);
      if (it == workers_.end()) {
          return false;
      }
      worker = it->second;

      Job job;
      job.kind = JOB_EXECUTE_AFTER;
      job.executeAfter.operation = operationStablePtr;
      worker->putJob(job, false);
      return true;
  }

  // Returns `true` if something was indeed processed.
  bool processQueueUnlocked(KInt id) {
    // Can only process queue of the current worker.
    if (::g_worker == nullptr || id != ::g_worker->id()) ThrowWorkerInvalidState();
    JobKind kind = ::g_worker->processQueueElement(false);
    return kind != JOB_NONE && kind != JOB_TERMINATE;
  }

  bool parkUnlocked(KInt id, KLong timeoutMicroseconds, KBoolean process) {
      // Can only park current worker.
      if (::g_worker == nullptr || id != ::g_worker->id()) ThrowWorkerInvalidState();
      return ::g_worker->park(timeoutMicroseconds, process);
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

  OBJ_GETTER(getWorkerNameUnlocked, KInt id) {
    ObjHolder nameHolder;
    {
      Locker locker(&lock_);
      auto it = workers_.find(id);
      if (it == workers_.end()) {
        ThrowWorkerInvalidState();
      }
      DerefStablePointer(it->second->name(), nameHolder.slot());
    }
    RETURN_OBJ(nameHolder.obj());
  }

  KBoolean waitForAnyFuture(KInt version, KInt millis) {
    Locker locker(&lock_);
    if (version != currentVersion_) return false;

    if (millis < 0) {
      pthread_cond_wait(&cond_, &lock_);
      return true;
    }

    uint64_t nsDelta = millis * 1000000LL;
    WaitOnCondVar(&cond_, &lock_, nsDelta);
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

  void destroyWorkerThreadDataUnlocked(KInt id) {
    Locker locker(&lock_);
    auto it = terminating_native_workers_.find(id);
    if (it == terminating_native_workers_.end()) return;
    // If this worker was not joined, detach it to free resources.
    pthread_detach(it->second);
    terminating_native_workers_.erase(it);
  }

  template <typename F>
  void waitNativeWorkersTerminationUnlocked(bool checkLeaks, F waitForWorker) {
      std::vector<std::pair<KInt, pthread_t>> workersToWait;
      {
          Locker locker(&lock_);

          if (checkLeaks) {
              checkNativeWorkersLeakLocked();
          }

          for (auto& kvp : terminating_native_workers_) {
              RuntimeAssert(!pthread_equal(kvp.second, pthread_self()), "Native worker is joining with itself");
              if (waitForWorker(kvp.first)) {
                  workersToWait.push_back(kvp);
              }
          }
          for (auto worker : workersToWait) {
              terminating_native_workers_.erase(worker.first);
          }
      }

      for (auto worker : workersToWait) {
          pthread_join(worker.second, nullptr);
      }
  }

  void checkNativeWorkersLeakLocked() {
    size_t remainingNativeWorkers = 0;
    for (const auto& kvp : workers_) {
      Worker* worker = kvp.second;
      if (worker->kind() == WorkerKind::kNative) {
        ++remainingNativeWorkers;
      }
    }

    if (remainingNativeWorkers != 0) {
      konan::consoleErrorf(
        "Unfinished workers detected, %lu workers leaked!\n"
        "Use `Platform.isMemoryLeakCheckerActive = false` to avoid this check.\n",
        remainingNativeWorkers);
      konan::consoleFlush();
      konan::abort();
    }
  }

 private:
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
  KStdUnorderedMap<KInt, Future*> futures_;
  KStdUnorderedMap<KInt, Worker*> workers_;
  KStdUnorderedMap<KInt, pthread_t> terminating_native_workers_;
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
    // Beware here: although manual clearly says that pthread_cond_broadcast() could be called outside
    // of the taken lock, it's not on macOS (as of 10.13.1). If moved outside of the lock,
    // some notifications are missing.
    pthread_cond_broadcast(&cond_);
  }
  theState()->signalAnyFuture();
}

void Future::cancelUnlocked() {
  {
    Locker locker(&lock_);
    state_ = CANCELLED;
    result_ = nullptr;
    pthread_cond_broadcast(&cond_);
  }
  theState()->signalAnyFuture();
}

// Defined in RuntimeUtils.kt.
extern "C" void ReportUnhandledException(KRef e);

KInt startWorker(KBoolean errorReporting, KRef customName) {
  Worker* worker = theState()->addWorkerUnlocked(errorReporting != 0, customName, WorkerKind::kNative);
  if (worker == nullptr) return -1;
  worker->startEventLoop();
  return worker->id();
}

KInt currentWorker() {
  if (g_worker == nullptr) ThrowWorkerInvalidState();
  return ::g_worker->id();
}

KInt execute(KInt id, KInt transferMode, KRef producer, KNativePtr jobFunction) {
  ObjHolder holder;
  WorkerLaunchpad(producer, holder.slot());
  KNativePtr jobArgument = transfer(&holder, transferMode);
  Future* future = theState()->addJobToWorkerUnlocked(id, jobFunction, jobArgument, false, transferMode);
  if (future == nullptr) ThrowWorkerInvalidState();
  return future->id();
}

void executeAfter(KInt id, KRef job, KLong afterMicroseconds) {
  if (!theState()->executeJobAfterInWorkerUnlocked(id, job, afterMicroseconds))
    ThrowWorkerInvalidState();
}

KBoolean processQueue(KInt id) {
   return theState()->processQueueUnlocked(id);
}

KBoolean park(KInt id, KLong timeoutMicroseconds, KBoolean process) {
   return theState()->parkUnlocked(id, timeoutMicroseconds, process);
}

KInt stateOfFuture(KInt id) {
  return theState()->stateOfFutureUnlocked(id);
}

OBJ_GETTER(consumeFuture, KInt id) {
  RETURN_RESULT_OF(theState()->consumeFutureUnlocked, id);
}

OBJ_GETTER(getWorkerName, KInt id) {
  RETURN_RESULT_OF(theState()->getWorkerNameUnlocked, id);
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
   ObjHolder result;
   WorkerLaunchpad(producer, result.slot());
   if (result.obj() != nullptr) {
     return transfer(&result, transferMode);
   } else {
     return nullptr;
   }
}

#else

KInt startWorker(KBoolean errorReporting, KRef customName) {
  ThrowWorkerUnsupported();
}

KInt stateOfFuture(KInt id) {
  ThrowWorkerUnsupported();
}

KInt execute(KInt id, KInt transferMode, KRef producer, KNativePtr jobFunction) {
  ThrowWorkerUnsupported();
}

void executeAfter(KInt id, KRef job, KLong afterMicroseconds) {
  ThrowWorkerUnsupported();
}

KBoolean processQueue(KInt id) {
  ThrowWorkerUnsupported();
}

KBoolean park(KInt id, KLong timeoutMicroseconds, KBoolean process) {
   ThrowWorkerUnsupported();
}

KInt currentWorker() {
  ThrowWorkerUnsupported();
}

OBJ_GETTER(consumeFuture, KInt id) {
  ThrowWorkerUnsupported();
}

OBJ_GETTER(getWorkerName, KInt id) {
  ThrowWorkerUnsupported();
}

KInt requestTermination(KInt id, KBoolean processScheduledJobs) {
  ThrowWorkerUnsupported();
}

KBoolean waitForAnyFuture(KInt versionToken, KInt millis) {
  ThrowWorkerUnsupported();
}

KInt versionToken() {
  ThrowWorkerUnsupported();
}

OBJ_GETTER(attachObjectGraphInternal, KNativePtr stable) {
  ThrowWorkerUnsupported();
}

KNativePtr detachObjectGraphInternal(KInt transferMode, KRef producer) {
   ThrowWorkerUnsupported();
}

#endif  // WITH_WORKERS

}  // namespace

KInt GetWorkerId(Worker* worker) {
#if WITH_WORKERS
  return worker->id();
#else
  return 0;
#endif  // WITH_WORKERS
}

Worker* WorkerInit(KBoolean errorReporting) {
#if WITH_WORKERS
  if (::g_worker != nullptr) return ::g_worker;
  Worker* worker = theState()->addWorkerUnlocked(errorReporting != 0, nullptr, WorkerKind::kOther);
  ::g_worker = worker;
  return worker;
#else
  return nullptr;
#endif  // WITH_WORKERS
}

void WorkerDeinit(Worker* worker) {
#if WITH_WORKERS
  ::g_worker = nullptr;
  theState()->destroyWorkerUnlocked(worker);
#endif  // WITH_WORKERS
}

void WorkerDestroyThreadDataIfNeeded(KInt id) {
#if WITH_WORKERS
  theState()->destroyWorkerThreadDataUnlocked(id);
#endif
}

void WaitNativeWorkersTermination() {
#if WITH_WORKERS
    theState()->waitNativeWorkersTerminationUnlocked(true, [](KInt worker) { return true; });
#endif
}

void WaitNativeWorkerTermination(KInt id) {
#if WITH_WORKERS
    theState()->waitNativeWorkersTerminationUnlocked(false, [id](KInt worker) { return worker == id; });
#endif
}

bool WorkerSchedule(KInt id, KNativePtr jobStablePtr) {
#if WITH_WORKERS
    return theState()->scheduleJobInWorkerUnlocked(id, jobStablePtr);
#else
    return false;
#endif // WITH_WORKERS
}

#if WITH_WORKERS

Worker::~Worker() {
  // Cleanup jobs in the queue.
  for (auto job : queue_) {
    switch (job.kind) {
      case JOB_REGULAR:
        DisposeStablePointer(job.regularJob.argument);
        job.regularJob.future->cancelUnlocked();
        break;
      case JOB_EXECUTE_AFTER: {
        // TODO: what do we do here? Shall we execute them?
        DisposeStablePointer(job.executeAfter.operation);
        break;
      }
      case JOB_TERMINATE: {
        // TODO: any more processing here?
        job.terminationRequest.future->cancelUnlocked();
        break;
      }
      case JOB_NONE: {
        RuntimeCheck(false, "Cannot be in queue");
        break;
      }
    }
  }

  for (auto job : delayed_) {
    RuntimeAssert(job.kind == JOB_EXECUTE_AFTER, "Must be delayed");
    DisposeStablePointer(job.executeAfter.operation);
  }

  if (name_ != nullptr) DisposeStablePointer(name_);

  pthread_mutex_destroy(&lock_);
  pthread_cond_destroy(&cond_);
}

namespace {

void* workerRoutine(void* argument) {
  Worker* worker = reinterpret_cast<Worker*>(argument);

  // Kotlin_initRuntimeIfNeeded calls WorkerInit that needs
  // to see there's already a worker created for this thread.
  ::g_worker = worker;
  Kotlin_initRuntimeIfNeeded();

  do {
    if (worker->processQueueElement(true) == JOB_TERMINATE) break;
  } while (true);

  // Runtime deinit callback could be called when TLS is already zeroed out, so clear memory
  // here explicitly. to make sure leak detector properly works.
  Kotlin_zeroOutTLSGlobals();

  return nullptr;
}

}  // namespace

void Worker::startEventLoop() {
  pthread_create(&thread_, nullptr, workerRoutine, this);
}

void Worker::putJob(Job job, bool toFront) {
  Locker locker(&lock_);
  if (toFront)
    queue_.push_front(job);
  else
    queue_.push_back(job);
  pthread_cond_signal(&cond_);
}

void Worker::putDelayedJob(Job job) {
  Locker locker(&lock_);
  delayed_.insert(job);
  pthread_cond_signal(&cond_);
}

bool Worker::waitDelayed(bool blocking) {
  Locker locker(&lock_);
  if (delayed_.size() == 0) return false;
  if (blocking) waitForQueueLocked(-1, nullptr);
  return true;
}

Job Worker::getJob(bool blocking) {
  Locker locker(&lock_);
  RuntimeAssert(!terminated_, "Must not be terminated");
  if (queue_.size() == 0 && !blocking) return Job { .kind = JOB_NONE };
  waitForQueueLocked(-1, nullptr);
  auto result = queue_.front();
  queue_.pop_front();
  return result;
}

KLong Worker::checkDelayedLocked() {
  if (delayed_.size() == 0) {
    return -1;
  }
  auto it = delayed_.begin();
  auto job = *it;
  RuntimeAssert(job.kind == JOB_EXECUTE_AFTER, "Must be delayed job");
  auto now = konan::getTimeMicros();
  if (job.executeAfter.whenExecute <= now) {
    delayed_.erase(it);
    queue_.push_back(job);
    return 0;
  } else {
    return job.executeAfter.whenExecute - now;
  }
}

bool Worker::waitForQueueLocked(KLong timeoutMicroseconds, KLong* remaining) {
  while (queue_.size() == 0) {
    KLong closestToRunMicroseconds = checkDelayedLocked();
    if (closestToRunMicroseconds == 0) {
        continue;
    }
    if (timeoutMicroseconds >= 0) {
        closestToRunMicroseconds = (timeoutMicroseconds < closestToRunMicroseconds || closestToRunMicroseconds < 0)
          ? timeoutMicroseconds
          : closestToRunMicroseconds;
    }
    if (closestToRunMicroseconds == 0) {
      // Just no wait at all here.
    } else if (closestToRunMicroseconds > 0) {
      // Protect from potential overflow, cutting at 10_000_000 seconds, aka 115 days.
      if (closestToRunMicroseconds > 10LL * 1000 * 1000 * 1000 * 1000)
        closestToRunMicroseconds = 10LL * 1000 * 1000 * 1000 * 1000;
      uint64_t nsDelta = closestToRunMicroseconds * 1000LL;
      uint64_t microsecondsPassed = 0;
      WaitOnCondVar(&cond_, &lock_, nsDelta, remaining ? &microsecondsPassed : nullptr);
      if (remaining) {
        *remaining = timeoutMicroseconds - microsecondsPassed;
      }
    } else {
      pthread_cond_wait(&cond_, &lock_);
      if (remaining) *remaining = 0;
    }
    if (timeoutMicroseconds >= 0) return queue_.size() != 0;
  }
  return true;
}

bool Worker::park(KLong timeoutMicroseconds, bool process) {
  {
    Locker locker(&lock_);
    if (terminated_) {
      return false;
    }
    auto arrived = false;
    KLong remaining = timeoutMicroseconds;
    do {
      arrived = waitForQueueLocked(remaining, &remaining);
    } while (remaining > 0 && !arrived);
    if (!process) {
      return arrived;
    }
    if (!arrived) {
      return false;
    }
  }
  return processQueueElement(false) >= JOB_REGULAR;
}

JobKind Worker::processQueueElement(bool blocking) {
  GC_CollectorCallback(this);
  ObjHolder argumentHolder;
  ObjHolder resultHolder;
  if (terminated_) return JOB_TERMINATE;
  Job job = getJob(blocking);
  switch (job.kind) {
    case JOB_NONE: {
      break;
    }
    case JOB_TERMINATE: {
      if (job.terminationRequest.waitDelayed) {
        if (waitDelayed(blocking)) {
          putJob(job, false);
          return JOB_NONE;
        }
      }
      terminated_ = true;
      // Termination request, remove the worker and notify the future.
      theState()->removeWorkerUnlocked(id());
      job.terminationRequest.future->storeResultUnlocked(nullptr, true);
      break;
    }
    case JOB_EXECUTE_AFTER: {
      ObjHolder operationHolder, dummyHolder;
      KRef obj = DerefStablePointer(job.executeAfter.operation, operationHolder.slot());
      try {
        WorkerLaunchpad(obj, dummyHolder.slot());
      } catch (ExceptionObjHolder& e) {
        if (errorReporting())
          ReportUnhandledException(e.obj());
      }
      DisposeStablePointer(job.executeAfter.operation);
      break;
    }
    case JOB_REGULAR: {
      KRef argument = AdoptStablePointer(job.regularJob.argument, argumentHolder.slot());
      KNativePtr result = nullptr;
      bool ok = true;
      try {
        job.regularJob.function(argument, resultHolder.slot());
        argumentHolder.clear();
        // Transfer the result.
        result = transfer(&resultHolder, job.regularJob.transferMode);
       } catch (ExceptionObjHolder& e) {
         ok = false;
         if (errorReporting())
           ReportUnhandledException(e.obj());
       }
       // Notify the future.
       job.regularJob.future->storeResultUnlocked(result, ok);
       break;
    }
    default: {
      RuntimeCheck(false, "Must be exhaustive");
    }
  }
  return job.kind;
}

#endif  // WITH_WORKERS

extern "C" {

KInt Kotlin_Worker_startInternal(KBoolean noErrorReporting, KRef customName) {
  return startWorker(noErrorReporting, customName);
}

KInt Kotlin_Worker_currentInternal() {
  return currentWorker();
}

KInt Kotlin_Worker_requestTerminationWorkerInternal(KInt id, KBoolean processScheduledJobs) {
  return requestTermination(id, processScheduledJobs);
}

KInt Kotlin_Worker_executeInternal(KInt id, KInt transferMode, KRef producer, KNativePtr job) {
  return execute(id, transferMode, producer, job);
}

void Kotlin_Worker_executeAfterInternal(KInt id, KRef job, KLong afterMicroseconds) {
  executeAfter(id, job, afterMicroseconds);
}

KBoolean Kotlin_Worker_processQueueInternal(KInt id) {
  return processQueue(id);
}

KBoolean Kotlin_Worker_parkInternal(KInt id, KLong timeoutMicroseconds, KBoolean process) {
  return park(id, timeoutMicroseconds, process);
}

OBJ_GETTER(Kotlin_Worker_getNameInternal, KInt id) {
  RETURN_RESULT_OF(getWorkerName, id);
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
  return object == nullptr || isPermanentOrFrozen(object);
}

void Kotlin_Worker_ensureNeverFrozen(KRef object) {
  EnsureNeverFrozen(object);
}

void Kotlin_Worker_waitTermination(KInt id) {
    WaitNativeWorkerTermination(id);
}

}  // extern "C"
