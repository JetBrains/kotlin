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

#ifndef RUNTIME_PORTING_H
#define RUNTIME_PORTING_H

#include <stdarg.h>
#include <stdint.h>
#include <stddef.h>

#include "Common.h"

namespace konan {

// Console operations.
void consoleInit();
void consolePrintf(const char* format, ...) __attribute__((format(printf, 1, 2)));
void consoleErrorf(const char* format, ...) __attribute__((format(printf, 1, 2)));
void consoleWriteUtf8(const char* utf8, uint32_t sizeBytes);
void consoleErrorUtf8(const char* utf8, uint32_t sizeBytes);
// Negative return value denotes that read wasn't successful.
int32_t consoleReadUtf8(void* utf8, uint32_t maxSizeBytes);
void consoleFlush();

// Thread control.
void onThreadExit(void (*destructor)(void*), void* destructorParameter);
bool isOnThreadExitNotSetOrAlreadyStarted();
int currentThreadId();

// Time operations.
uint64_t getTimeMillis();
uint64_t getTimeMicros();
uint64_t getTimeNanos();

}  // namespace konan

#endif  // RUNTIME_PORTING_H
