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

#include <stdint.h>
#include <stddef.h>

namespace konan {

// Console operations.
void consoleInit();
void consolePrintf(const char* format, ...);
void consoleWriteUtf8(const void* utf8, uint32_t sizeBytes);
void consoleErrorUtf8(const void* utf8, uint32_t sizeBytes);
uint32_t consoleReadUtf8(void* utf8, uint32_t maxSizeBytes);

// Process control.
void abort();
void exit(int32_t status);

// Thread control.
void onThreadExit(void (*destructor)());

// String/byte operations.
// memcpy/memmove/memcmp are not here intentionally, as frequently implemented/optimized
// by C compiler.
void* memmem(const void *big, size_t bigLen, const void *little, size_t littleLen);
int snprintf(char* buffer, size_t size, const char* format, ...);
size_t strnlen(const char* buffer, size_t maxSize);

// Memory operations.
void* calloc(size_t count, size_t size);
void free(void* ptr);

// Time operations.
uint64_t getTimeMillis();
uint64_t getTimeMicros();
uint64_t getTimeNanos();

#if KONAN_NO_EXCEPTIONS
#define TRY_CATCH(tryAction, actionWithoutExceptions, catchAction) actionWithoutExceptions;
#else
#define TRY_CATCH(tryAction, actionWithoutExceptions, catchAction) \
do {                          \
  try { tryAction; }          \
  catch(...) { catchAction; } \
} while(0)
#endif

}  // namespace konan

#endif  // RUNTIME_PORTING_H
