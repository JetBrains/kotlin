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

#ifndef MESSAGE_CHANNEL_H
#define MESSAGE_CHANNEL_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef int32_t worker_id_t;
typedef int32_t message_kind_t;
typedef struct {
  worker_id_t source_;
  worker_id_t destination_;
  message_kind_t kind_;
  void* data_;
  int64_t data_size_;
  int64_t data_capacity_;
} Message;

#define INVALID_WORKER -1

// Creates new worker, return its id, or INVALID_WORKER if can not.
worker_id_t CreateWorker(const char* where, int argc, const char** argv);
// Gets id of the current worker.
worker_id_t CurrentWorker();

// Create message.
Message* CreateMessage(int64_t data_size);
void ReleaseMessage(Message* message);

// Returns 0 is message was delivered to 'destination' event queue,
// and -1 if destination is invalid or cannot accept more messages.
int SendMessage(worker_id_t destination, const Message* message);

// Gets next message, returns -1 if no message arrived until timeout
// expired. 'timeout_ms' can have following values:
//   * > 0 - number of milliseconds to wait
//   * == 0 - return immediately if there's a message already
//   * < 0 - wait forever, until event arrives or program terminates
int GetMessage(Message* message, int timeout_ms);

#ifdef __cplusplus
} // extern "C"
#endif

#endif  // MESSAGE_CHANNEL_H
