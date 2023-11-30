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

#include <cstdint>
#include <cstdlib>
#include <deque>
#include <set>
#include <string.h>
#include <stdio.h>
#include <unordered_map>
#include <vector>

#include <pthread.h>
#include "PthreadUtils.h"

#include "Exceptions.h"
#include "KAssert.h"
#include "Memory.h"
#include "Natives.h"
#include "Runtime.h"
#include "Types.h"
#include "Worker.h"
#include "objc_support/AutoreleasePool.hpp"

using namespace kotlin;

extern "C" {

RUNTIME_NORETURN void ThrowWorkerAlreadyTerminated();
RUNTIME_NORETURN void ThrowWrongWorkerOrAlreadyTerminated();
RUNTIME_NORETURN void ThrowCannotTransferOwnership();
RUNTIME_NORETURN void ThrowFutureInvalidState();
OBJ_GETTER(WorkerLaunchpad, KRef);

}  // extern "C"

namespace {

enum class WorkerExceptionHandling {
    kDefault, // Perform the default processing of unhandled exception.
    kIgnore, // Do nothing on exception escaping job unit.
    kLog, // Deprecated.
};

WorkerExceptionHandling workerExceptionHandling() noexcept {
    switch (compiler::workerExceptionHandling()) {
        case compiler::WorkerExceptionHandling::kLegacy:
            return WorkerExceptionHandling::kLog;
        case compiler::WorkerExceptionHandling::kUseHook:
            return WorkerExceptionHandling::kDefault;
    }
}

} // namespace

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

// Using multiset instead of regular set, because we compare the jobs only by `whenExecute`.
// So if `whenExecute` of two different jobs is the same, the jobs are considered equivalent,
// and set would simply drop one of them.
typedef std::multiset<Job, JobCompare> DelayedJobSet;

}  // namespace

class Worker {
 public:
  Worker(KInt id, WorkerExceptionHandling exceptionHandling, KRef customName, WorkerKind kind)
      : id_(id),
        kind_(kind),
        exceptionHandling_(exceptionHandling) {
    name_ = customName != nullptr ? CreateStablePointer(customName) : nullptr;
    kotlin::ThreadStateGuard guard(ThreadState::kNative);
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

  RUNTIME_NODEBUG JobKind processQueueElement(bool blocking);

  bool park(KLong timeoutMicroseconds, bool process);

  KInt id() const { return id_; }

  WorkerExceptionHandling exceptionHandling() const { return exceptionHandling_; }

  KNativePtr name() const { return name_; }

  WorkerKind kind() const { return kind_; }

  pthread_t thread() const { return thread_; }

  MemoryState* memoryState() { return memoryState_; }

 private:
  void setThread(pthread_t thread) {
    // For workers started using the Worker API, we set thread_ in startEventLoop when calling pthread_create.
    // But we also set thread_ in WorkerInit to handle the main thread and threads calling Kotlin from native code.
    // In this case, thread id will be set twice for workers started by the Worker API.
    // This assert takes this into account.
    RuntimeAssert(thread_ == 0 || pthread_equal(thread_, thread),
                  "Cannot overwrite thread id for worker with id=%d", id());
    thread_ = thread;
  }

  void setMemoryState(MemoryState* state) {
    RuntimeAssert(memoryState_ == nullptr, "MemoryState is already set for the worker with id=%d", id());
    memoryState_ = state;
  }

  friend Worker* WorkerInit(MemoryState* memoryState);

  KInt id_;
  WorkerKind kind_;
  std::deque<Job> queue_;
  DelayedJobSet delayed_;
  // Stable pointer with worker's name.
  KNativePtr name_;
  // Lock and condition for waiting on the queue.
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
  WorkerExceptionHandling exceptionHandling_;
  bool terminated_ = false;
  pthread_t thread_ = 0;
  // MemoryState for worker's thread.
  // We set it in WorkerInit and use to correctly switch thread states in woker's destructor.
  MemoryState* memoryState_ = nullptr;
};

namespace {

THREAD_LOCAL_VARIABLE Worker* g_worker = nullptr;

KNativePtr transfer(ObjHolder* holder, KInt mode) {
  void* result = CreateStablePointer(holder->obj());
  if (!ClearSubgraphReferences(holder->obj(), mode == CHECKED)) {
    DisposeStablePointer(result);
    ThrowCannotTransferOwnership();
  }
  holder->clear();
  return result;
}

void waitInNativeState(pthread_cond_t* cond, pthread_mutex_t* mutex) {
    kotlin::compactObjectPoolInCurrentThread();
    CallWithThreadState<ThreadState::kNative>(pthread_cond_wait, cond, mutex);
}

void waitInNativeState(pthread_cond_t* cond,
          pthread_mutex_t* mutex,
          uint64_t timeoutNanoseconds,
          uint64_t* microsecondsPassed = nullptr) {
    kotlin::compactObjectPoolInCurrentThread();
    CallWithThreadState<ThreadState::kNative>(WaitOnCondVar, cond, mutex, timeoutNanoseconds, microsecondsPassed);
}

KULong pthreadToNumber(pthread_t thread) {
    static_assert(sizeof(pthread_t) <= sizeof(KULong), "Casting pthread_t to ULong will lose data");
    // That's almost std::bit_cast. The latter requires sizeof equality of types.
    KULong result = 0;
    memcpy(&result, &thread, sizeof(pthread_t));
    return result;
}

class Locker {
public:
    explicit Locker(pthread_mutex_t* lock, bool switchThreadState = true) : lock_(lock), switchThreadState_(switchThreadState) {
        if (switchThreadState) {
            kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative, true);
            pthread_mutex_lock(lock_);
        } else {
            // We may need to create a locker when the current thread is already unregistered in the memory subsystem.
            // For such cases we allow locking without switching or checking the thread state.
            pthread_mutex_lock(lock_);
        }
    }
    Locker(pthread_mutex_t* lock, MemoryState* memoryState) : lock_(lock), memoryState_(memoryState) {
        kotlin::ThreadStateGuard guard(memoryState, kotlin::ThreadState::kNative, true);
        pthread_mutex_lock(lock_);
    }

    ~Locker() {
        kotlin::ThreadStateGuard guard;
        if (switchThreadState_) {
            if (memoryState_ != nullptr) {
                guard = kotlin::ThreadStateGuard(memoryState_, ThreadState::kNative, true);
            } else {
                guard = kotlin::ThreadStateGuard(ThreadState::kNative, true);
            }
        }
        pthread_mutex_unlock(lock_);
    }

private:
    pthread_mutex_t* lock_;
    bool switchThreadState_ = true;
    MemoryState* memoryState_ = nullptr;
};

class Future {
 public:
  Future(KInt id) : state_(SCHEDULED), id_(id) {
    kotlin::ThreadStateGuard guard(ThreadState::kNative);
    pthread_mutex_init(&lock_, nullptr);
    pthread_cond_init(&cond_, nullptr);
  }

  ~Future() {
    clear();
    kotlin::ThreadStateGuard guard(ThreadState::kNative);
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
      waitInNativeState(&cond_, &lock_);
    }
    // TODO: maybe use message from exception?
    if (state_ == THROWN)
        ThrowIllegalStateException();
    auto result = AdoptStablePointer(result_, OBJ_RESULT);
    result_ = nullptr;
    return result;
  }

  void storeResultUnlocked(KNativePtr result, bool ok);

  void cancelUnlocked(MemoryState* memoryState);

  KInt stateUnlocked() const {
      Locker locker(&lock_);
      return state_;
  }
  // Those are called with the lock taken.
  KInt id() const { return id_; }

 private:
  // State of future execution.
  KInt state_;
  // Integer id of the future.
  KInt id_;
  // Stable pointer with future's result.
  KNativePtr result_;
  // Lock and condition for waiting on the future.
  mutable pthread_mutex_t lock_;
  mutable pthread_cond_t cond_;
};

class State {
 public:
  State() {
    kotlin::ThreadStateGuard guard(ThreadState::kNative);
    pthread_mutex_init(&lock_, nullptr);
    pthread_cond_init(&cond_, nullptr);

    currentWorkerId_ = 1;
    currentFutureId_ = 1;
    currentVersion_ = 0;
  }

  ~State() {
    kotlin::ThreadStateGuard guard(ThreadState::kNative);
    // TODO: some sanity check here?
    pthread_mutex_destroy(&lock_);
    pthread_cond_destroy(&cond_);
  }

  Worker* addWorkerUnlocked(WorkerExceptionHandling exceptionHandling, KRef customName, WorkerKind kind) {
    Worker* worker = nullptr;
    {
      Locker locker(&lock_);
      worker = new Worker(nextWorkerId(), exceptionHandling, customName, kind);
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
      // We call destroyWorkerUnlocked from runtimeDeinit when TLS may be already deallocated,
      // so we have to use the pointer to MemoryState saved in the worker instance.
      RuntimeAssert(pthread_equal(worker->thread(), pthread_self()),
                    "Worker destruction must be executed by the worker thread.");
      Locker locker(&lock_, worker->memoryState());
      auto id = worker->id();
      auto it = workers_.find(id);
      if (it != workers_.end()) {
        workers_.erase(it);
      }
    }
    GC_UnregisterWorker(worker);
    delete worker;
  }

  Future* addJobToWorkerUnlocked(
      KInt id, KNativePtr jobFunction, KNativePtr jobArgument, bool toFront, KInt transferMode) {
    Future* future = nullptr;
    Worker* worker = nullptr;
    Locker locker(&lock_);

    auto it = workers_.find(id);
    if (it == workers_.end()) return nullptr;
    worker = it->second;

    future = new Future(nextFutureId());
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
    if (::g_worker == nullptr || id != ::g_worker->id()) ThrowWrongWorkerOrAlreadyTerminated();
    JobKind kind = ::g_worker->processQueueElement(false);
    return kind != JOB_NONE && kind != JOB_TERMINATE;
  }

  bool parkUnlocked(KInt id, KLong timeoutMicroseconds, KBoolean process) {
      // Can only park current worker.
      if (::g_worker == nullptr || id != ::g_worker->id()) ThrowWrongWorkerOrAlreadyTerminated();
      return ::g_worker->park(timeoutMicroseconds, process);
  }

  KInt stateOfFutureUnlocked(KInt id) {
    Locker locker(&lock_);
    auto it = futures_.find(id);
    if (it == futures_.end()) return INVALID;
    return it->second->stateUnlocked();
  }

  OBJ_GETTER(consumeFutureUnlocked, KInt id) {
    Future* future = nullptr;
    {
      Locker locker(&lock_);
      auto it = futures_.find(id);
      if (it == futures_.end()) {
        // Caller checks [stateOfFutureUnlocked] first, so this code is reachable
        // only when trying to consume future twice concurrently.
        ThrowFutureInvalidState();
      }
      future = it->second;
    }

    KRef result = future->consumeResultUnlocked(OBJ_RESULT);

    {
       Locker locker(&lock_);
       auto it = futures_.find(id);
       if (it != futures_.end()) {
         futures_.erase(it);
         delete future;
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
            ThrowWorkerAlreadyTerminated();
        }
        DerefStablePointer(it->second->name(), nameHolder.slot());
    }
    RETURN_OBJ(nameHolder.obj());
  }

  KBoolean waitForAnyFuture(KInt version, KInt millis) {
    Locker locker(&lock_);
    if (version != currentVersion_) return false;

    if (millis < 0) {
      waitInNativeState(&cond_, &lock_);
      return true;
    }

    uint64_t nsDelta = millis * 1000000LL;
    waitInNativeState(&cond_, &lock_, nsDelta);
    return true;
  }

  void signalAnyFuture() {
    kotlin::AssertThreadState(ThreadState::kNative);
    {
      Locker locker(&lock_);
      currentVersion_++;
    }
    pthread_cond_broadcast(&cond_);
  }

  void signalAnyFuture(MemoryState* memoryState) {
    kotlin::AssertThreadState(memoryState, ThreadState::kNative);
    {
      Locker locker(&lock_, memoryState);
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
    // We destroy worker data when its thread is already unresigtered from the memory subsystem,
    // so there is no need to wrap the lock with a thread state switch.
    Locker locker(&lock_, false);
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

      ThreadStateGuard guard(ThreadState::kNative);
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
        "Unfinished workers detected, %zu workers leaked!\n"
        "Use `Platform.isMemoryLeakCheckerActive = false` to avoid this check.\n",
        remainingNativeWorkers);
      konan::consoleFlush();
      std::abort();
    }
  }

  KULong getWorkerPlatformThreadIdUnlocked(KInt id) {
      Locker locker(&lock_);
      auto it = workers_.find(id);
      if (it == workers_.end()) {
          ThrowWorkerAlreadyTerminated();
      }
      return pthreadToNumber(it->second->thread());
  }

  OBJ_GETTER0(getActiveWorkers) {
      std::vector<KInt> workers;
      {
          Locker locker(&lock_);

          workers.reserve(workers_.size());
          for (auto [id, worker] : workers_) {
              workers.push_back(id);
          }
      }
      ObjHolder arrayHolder;
      AllocArrayInstance(theIntArrayTypeInfo, workers.size(), arrayHolder.slot());
      std::copy(workers.begin(), workers.end(), IntArrayAddressOfElementAt(arrayHolder.obj()->array(), 0));
      RETURN_OBJ(arrayHolder.obj());
  }

 private:
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
  std::unordered_map<KInt, Future*> futures_;
  std::unordered_map<KInt, Worker*> workers_;
  std::unordered_map<KInt, pthread_t> terminating_native_workers_;
  KInt currentWorkerId_;
  KInt currentFutureId_;
  KInt currentVersion_;
};

State* theState() {
  static State* state = nullptr;

  if (state != nullptr) {
    return state;
  }

  State* result = new State();

  State* old = __sync_val_compare_and_swap(&state, nullptr, result);
  if (old != nullptr) {
    delete result;
    // Someone else inited this data.
    return old;
  }
  return state;
}

void Future::storeResultUnlocked(KNativePtr result, bool ok) {
  kotlin::ThreadStateGuard guard(ThreadState::kNative);
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

void Future::cancelUnlocked(MemoryState* memoryState) {
  kotlin::ThreadStateGuard guard(memoryState, ThreadState::kNative);
  {
    Locker locker(&lock_, memoryState);
    state_ = CANCELLED;
    result_ = nullptr;
    pthread_cond_broadcast(&cond_);
  }
  theState()->signalAnyFuture(memoryState);
}

// Defined in RuntimeUtils.kt.
extern "C" void ReportUnhandledException(KRef e);

KInt startWorker(WorkerExceptionHandling exceptionHandling, KRef customName) {
  Worker* worker = theState()->addWorkerUnlocked(exceptionHandling, customName, WorkerKind::kNative);
  if (worker == nullptr) return -1;
  worker->startEventLoop();
  return worker->id();
}

KInt currentWorker() {
  if (g_worker == nullptr) ThrowWorkerAlreadyTerminated();
  return ::g_worker->id();
}

KInt execute(KInt id, KInt transferMode, KRef producer, KNativePtr jobFunction) {
  ObjHolder holder;
  WorkerLaunchpad(producer, holder.slot());
  KNativePtr jobArgument = transfer(&holder, transferMode);
  Future* future = theState()->addJobToWorkerUnlocked(id, jobFunction, jobArgument, false, transferMode);
  if (future == nullptr) ThrowWorkerAlreadyTerminated();
  return future->id();
}

void executeAfter(KInt id, KRef job, KLong afterMicroseconds) {
  if (!theState()->executeJobAfterInWorkerUnlocked(id, job, afterMicroseconds))
    ThrowWorkerAlreadyTerminated();
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
  if (future == nullptr) ThrowWorkerAlreadyTerminated();
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

KULong platformThreadId(KInt id) {
    return theState()->getWorkerPlatformThreadIdUnlocked(id);
}

OBJ_GETTER0(activeWorkers) {
    RETURN_RESULT_OF0(theState()->getActiveWorkers);
}

}  // namespace

KInt GetWorkerId(Worker* worker) {
  return worker->id();
}

Worker* WorkerInit(MemoryState* memoryState) {
  Worker* worker;
  if (::g_worker != nullptr) {
      worker = ::g_worker;
  } else {
      worker = theState()->addWorkerUnlocked(workerExceptionHandling(), nullptr, WorkerKind::kOther);
      ::g_worker = worker;
  }
  worker->setThread(pthread_self());
  worker->setMemoryState(memoryState);
  return worker;
}

void WorkerDeinit(Worker* worker) {
  ::g_worker = nullptr;
  theState()->destroyWorkerUnlocked(worker);
}

void WorkerDestroyThreadDataIfNeeded(KInt id) {
  theState()->destroyWorkerThreadDataUnlocked(id);
}

void WaitNativeWorkersTermination() {
  theState()->waitNativeWorkersTerminationUnlocked(true, [](KInt worker) { return true; });
}

void WaitNativeWorkerTermination(KInt id) {
    theState()->waitNativeWorkersTerminationUnlocked(false, [id](KInt worker) { return worker == id; });
}

bool WorkerSchedule(KInt id, KNativePtr jobStablePtr) {
    return theState()->scheduleJobInWorkerUnlocked(id, jobStablePtr);
}

Worker::~Worker() {
  RuntimeAssert(pthread_equal(thread(), pthread_self()),
                "Worker destruction must be executed by the worker thread.");
  // Cleanup jobs in the queue.
  for (auto job : queue_) {
      switch (job.kind) {
          case JOB_REGULAR:
              DisposeStablePointerFor(memoryState_, job.regularJob.argument);
              job.regularJob.future->cancelUnlocked(memoryState_);
              break;
          case JOB_EXECUTE_AFTER: {
              // TODO: what do we do here? Shall we execute them?
              DisposeStablePointerFor(memoryState_, job.executeAfter.operation);
              break;
          }
          case JOB_TERMINATE: {
              // TODO: any more processing here?
              job.terminationRequest.future->cancelUnlocked(memoryState_);
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
      DisposeStablePointerFor(memoryState_, job.executeAfter.operation);
  }

  if (name_ != nullptr) {
      DisposeStablePointerFor(memoryState_, name_);
  }

  kotlin::ThreadStateGuard guard(memoryState_, ThreadState::kNative);
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

  // Only run this routine in the runnable state. The moment between this routine exiting and thread
  // destructors running will be spent in the native state. `Kotlin_deinitRuntimeCallback` ensures
  // that runtime deinitialization switches back to the runnable state.
  kotlin::ThreadStateGuard guard(worker->memoryState(), ThreadState::kRunnable);

  do {
    if (worker->processQueueElement(true) == JOB_TERMINATE) break;
  } while (true);

  return nullptr;
}

}  // namespace

void Worker::startEventLoop() {
  kotlin::ThreadStateGuard guard(ThreadState::kNative);
  pthread_create(&thread_, nullptr, workerRoutine, this);
}

void Worker::putJob(Job job, bool toFront) {
  kotlin::ThreadStateGuard guard(ThreadState::kNative);
  Locker locker(&lock_);
  if (toFront)
    queue_.push_front(job);
  else
    queue_.push_back(job);
  pthread_cond_signal(&cond_);
}

void Worker::putDelayedJob(Job job) {
  kotlin::ThreadStateGuard guard(ThreadState::kNative);
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
    // Note: `delayed_` is multiset sorted only by `whenExecute`.
    // So using erase(it) instead of erase(job) is crucial,
    // because the latter would remove all the jobs with the same `whenExecute`.
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
      waitInNativeState(&cond_, &lock_, nsDelta, remaining ? &microsecondsPassed : nullptr);
      if (remaining) {
        *remaining = timeoutMicroseconds - microsecondsPassed;
      }
    } else {
      waitInNativeState(&cond_, &lock_);
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
          objc_support::AutoreleasePool autoreleasePool;
          WorkerLaunchpad(obj, dummyHolder.slot());
      } catch(ExceptionObjHolder& e) {
        switch (exceptionHandling()) {
          case WorkerExceptionHandling::kIgnore: break;
          case WorkerExceptionHandling::kDefault:
              kotlin::ProcessUnhandledException(e.GetExceptionObject());
              break;
          case WorkerExceptionHandling::kLog:
              ReportUnhandledException(e.GetExceptionObject());
              break;
        }
      }

      DisposeStablePointer(job.executeAfter.operation);
      break;
    }
    case JOB_REGULAR: {
      KNativePtr result = nullptr;
      bool ok = true;
      ObjHolder argumentHolder;
      ObjHolder resultHolder;
      KRef argument = AdoptStablePointer(job.regularJob.argument, argumentHolder.slot());
      try {
          objc_support::AutoreleasePool autoreleasePool;
          {
              CurrentFrameGuard guard;
              job.regularJob.function(argument, resultHolder.slot());
          }
          argumentHolder.clear();
          // Transfer the result.
          result = transfer(&resultHolder, job.regularJob.transferMode);
      } catch (ExceptionObjHolder& e) {
        ok = false;
        switch (exceptionHandling()) {
            case WorkerExceptionHandling::kIgnore:
                break;
            case WorkerExceptionHandling::kDefault: // TODO: Pass exception object into the future and do nothing in the default case.
            case WorkerExceptionHandling::kLog:
                ReportUnhandledException(e.GetExceptionObject());
                break;
        }
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

extern "C" {

KInt Kotlin_Worker_startInternal(KBoolean errorReporting, KRef customName) {
    return startWorker(errorReporting ? workerExceptionHandling() : WorkerExceptionHandling::kIgnore, customName);
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
  if (object != nullptr && compiler::freezingEnabled())
    FreezeSubgraph(object);
}

KBoolean Kotlin_Worker_isFrozenInternal(KRef object) {
  if (!compiler::freezingChecksEnabled()) return false;
  return object == nullptr || isPermanentOrFrozen(object);
}

void Kotlin_Worker_ensureNeverFrozen(KRef object) {
  EnsureNeverFrozen(object);
}

void Kotlin_Worker_waitTermination(KInt id) {
    WaitNativeWorkerTermination(id);
}

KULong Kotlin_Worker_getPlatformThreadIdInternal(KInt id) {
    return platformThreadId(id);
}

OBJ_GETTER0(Kotlin_Worker_getActiveWorkersInternal) {
    RETURN_RESULT_OF0(activeWorkers);
}

}  // extern "C"
