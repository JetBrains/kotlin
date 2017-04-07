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

#include <pthread.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

#include <cassert>
#include <deque>
#include <unordered_map>
#include <unordered_set>

#include "MessageChannel.h"

namespace {

// Konan entry point.
extern "C" int Konan_main(int argc, const char** argv);

struct WorkerState {
  WorkerState(worker_id_t id, int argc, const char** argv)
      : id_(id), argc_(argc + 1), argv_(nullptr) {
    printf("create with %d\n", id);

    argv_ = reinterpret_cast<char**>(malloc(sizeof(char*) * argc_));
    argv_[0] = strdup("worker");
    for (auto i = 0; i < argc; i++) {
      argv_[i + 1] = strdup(argv[i]);
    }
    pthread_mutex_init(&mutex_, nullptr);
    pthread_cond_init(&cond_, nullptr);
    blocked_on_message_ = nullptr;
    blocked_message_used_ = false;
  }

  ~WorkerState() {
    for (auto i = 0; i < argc_; i++) {
      free(argv_[i]);
    }
    if (argv_) free(argv_);

    pthread_mutex_destroy(&mutex_);
    pthread_cond_destroy(&cond_);
  }

  int GetMessageNotEmptyLocked(Message* message);

  bool HasMessageLocked() const {
    return (blocked_on_message_ != nullptr && blocked_message_used_) ||
        !queue_.empty();
  }

  std::deque<Message*> queue_;
  Message* blocked_on_message_;
  bool blocked_message_used_;
  pthread_mutex_t mutex_;
  pthread_cond_t cond_;
  worker_id_t id_;
  pthread_t tid_;
  int argc_;
  char** argv_;
};

__thread WorkerState* current_worker = nullptr;

struct WorkersState {
  WorkersState() : current_worker_id_(1) {
    pthread_mutex_init(&mutex_, nullptr);
    auto thisWorker = new WorkerState(current_worker_id_++, 0, nullptr);
    AddWorker(pthread_self(), thisWorker);
    current_worker = thisWorker;
  }

  ~WorkersState() {
    for (auto worker : workers_) delete worker;
    pthread_mutex_destroy(&mutex_);
  }

  void AddWorkerLocked(pthread_t tid, WorkerState* state) {
    workers_.insert(state);
    state->tid_ = tid;
    id_map_[state->id_] = state;
    thread_map_[state->tid_] = state;
  }

  void AddWorker(pthread_t tid, WorkerState* state) {
    pthread_mutex_lock(&mutex_);
    AddWorkerLocked(tid, state);
    pthread_mutex_unlock(&mutex_);
  }

  void RemoveWorkerLocked(WorkerState* worker) {
    workers_.erase(worker);
    id_map_.erase(worker->id_);
    thread_map_.erase(worker->tid_);
    delete worker;
  }

  void RemoveWorker(WorkerState* worker) {
    pthread_mutex_lock(&mutex_);
    RemoveWorkerLocked(worker);
    pthread_mutex_unlock(&mutex_);
  }

  pthread_mutex_t mutex_;
  worker_id_t current_worker_id_;
  std::unordered_map<worker_id_t, WorkerState*> id_map_;
  std::unordered_map<pthread_t, WorkerState*> thread_map_;
  std::unordered_set<WorkerState*> workers_;
};

WorkersState* getWorkers() {
 static WorkersState* instance = new WorkersState();
  return instance;
}

void copyMessageSteal(Message* destination, Message* source) {
  destination->kind_ = source->kind_;
  if (source->data_size_ > 0) {
    free(destination->data_);
    destination->data_ = source->data_;
    destination->data_capacity_ = source->data_capacity_;
    source->data_ = nullptr;
  }
  destination->data_size_ = source->data_size_;
}

void copyMessageNoSteal(Message* destination, const Message* source) {
  destination->kind_ = source->kind_;
  if (destination->data_capacity_ < source->data_size_) {
    if (destination->data_) free(destination->data_);
    destination->data_ = malloc(source->data_size_);
    destination->data_capacity_ = source->data_size_;
  }
  memcpy(destination->data_, source->data_, source->data_size_);
  destination->data_size_ = source->data_size_;
}

void* ThreadRunner(void* arg) {
  WorkerState* state = reinterpret_cast<WorkerState*>(arg);
  current_worker = state;
  Konan_main(state->argc_, const_cast<const char**>(state->argv_));
  getWorkers()->RemoveWorker(current_worker);
  return nullptr;
}

void copyOrEnqueueMessageLocked(WorkerState* sender, WorkerState* receiver, const Message* message) {
  bool use_blocked = receiver->blocked_on_message_ != nullptr &&
                     !receiver->blocked_message_used_;
  Message* result =
      use_blocked ? receiver->blocked_on_message_ : CreateMessage(message->data_size_);
  result->source_ = sender->id_;
  result->destination_ = receiver->id_;
  copyMessageNoSteal(result, message);
  if (use_blocked)
    receiver->blocked_message_used_ = true;
  else
    receiver->queue_.push_back(result);
}

int WorkerState::GetMessageNotEmptyLocked(Message* result) {
  bool blocked_used = blocked_on_message_ != nullptr && blocked_message_used_;
  if (blocked_used) {
    assert(result == blocked_on_message_);
    blocked_on_message_ = nullptr;
    blocked_message_used_ = false;
    return 0;
  }
  Message* from_queue = queue_.front();
  result->source_ = from_queue->source_;
  result->destination_ = from_queue->destination_;
  copyMessageSteal(result, from_queue);

  queue_.pop_front();
  ReleaseMessage(from_queue);

  return 0;
}

}  // namespace

#ifdef __cplusplus
extern "C" {
#endif

Message* CreateMessage(int64_t data_capacity) {
  auto result = reinterpret_cast<Message*>(calloc(1, sizeof(Message)));
  result->data_size_ = 0;
  if (data_capacity == 0) {
    result->data_ = nullptr;
  } else {
    result->data_ = malloc(data_capacity);
  }
  result->data_capacity_ = data_capacity;
  return result;
}

void ReleaseMessage(Message* message) {
  if (message->data_ != nullptr) free(message->data_);
  free(message);
}

worker_id_t CreateWorker(const char* where, int argc, const char** argv) {
  auto workers = getWorkers();
  worker_id_t id = INVALID_WORKER;
  pthread_mutex_lock(&workers->mutex_);

  WorkerState* state = new WorkerState(workers->current_worker_id_++, argc, argv);
  pthread_t tid;
  pthread_attr_t attr;
  pthread_attr_init(&attr);
  pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
  int rc = pthread_create(&tid, &attr, ThreadRunner, state);
  if (rc != 0) {
    printf("pthread_create(): %d\n", rc);
    delete state;
  } else {
    workers->AddWorkerLocked(tid, state);
    id = state->id_;
  }

  pthread_mutex_unlock(&workers->mutex_);

  return id;
}

worker_id_t CurrentWorker() {
  return current_worker->id_;
}

int SendMessage(worker_id_t destination_id, const Message* message) {
  auto workers = getWorkers();
  int rc = 0;
  pthread_mutex_lock(&workers->mutex_);
  auto destination = workers->id_map_[destination_id];
  if (destination != nullptr) {
    pthread_mutex_lock(&destination->mutex_);
    copyOrEnqueueMessageLocked(current_worker, destination, message);
    pthread_cond_signal(&destination->cond_);
    pthread_mutex_unlock(&destination->mutex_);
  } else {
    rc = -1;
  }
  pthread_mutex_unlock(&workers->mutex_);
  return rc;
}

int GetMessage(Message* message, int timeout_ms) {
  auto worker = current_worker;
  int result = -1;
  pthread_mutex_lock(&worker->mutex_);
  if (!worker->queue_.empty()) {
    result = worker->GetMessageNotEmptyLocked(message);
  } else {
    worker->blocked_on_message_ = message;
    worker->blocked_message_used_ = false;
    if (timeout_ms < 0) {
      // Infinite wait.
      while (!worker->HasMessageLocked()) {
        int rc = pthread_cond_wait(&worker->cond_, &worker->mutex_);
        if (rc != 0) break;
      }
      if (worker->HasMessageLocked()) {
        result = worker->GetMessageNotEmptyLocked(message);
      }
    } else if (timeout_ms > 0) {
      // Timed wait.
      struct timespec   ts;
      struct timeval    tp;
      gettimeofday(&tp, NULL);
      ts.tv_sec  = tp.tv_sec;
      ts.tv_nsec = tp.tv_usec * 1000 + timeout_ms * 1000000;
      ts.tv_sec  += ts.tv_nsec / 1000000000;
      ts.tv_nsec %= 1000000000;
      while (!worker->HasMessageLocked()) {
        int rc = pthread_cond_timedwait(&worker->cond_, &worker->mutex_, &ts);
        if (rc != 0) break;
      }
      if (worker->HasMessageLocked()) {
        result = worker->GetMessageNotEmptyLocked(message);
      }
    }
    worker->blocked_on_message_ = nullptr;
    worker->blocked_message_used_ = false;
  }
  pthread_mutex_unlock(&worker->mutex_);
  return result;
}

#ifdef __cplusplus
} // extern "C"
#endif
