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

#include "Common.h"

namespace konan {

// Console operations.
void consoleInit();
void consolePrintf(const char* format, ...);
void consoleErrorf(const char* format, ...);
void consoleWriteUtf8(const void* utf8, uint32_t sizeBytes);
void consoleErrorUtf8(const void* utf8, uint32_t sizeBytes);
// Negative return value denotes that read wasn't successful.
int32_t consoleReadUtf8(void* utf8, uint32_t maxSizeBytes);
void consoleFlush();

// Process control.
RUNTIME_NORETURN void abort(void);
RUNTIME_NORETURN void exit(int32_t status);

// Thread control.
void onThreadExit(void (*destructor)(void*), void* destructorParameter);

// String/byte operations.
// memcpy/memmove/memcmp are not here intentionally, as frequently implemented/optimized
// by C compiler.
void* memmem(const void *big, size_t bigLen, const void *little, size_t littleLen);
int snprintf(char* buffer, size_t size, const char* format, ...);
size_t strnlen(const char* buffer, size_t maxSize);


// These functions should be marked with RUNTIME_USED attribute for wasm target
// because clang replaces these operations with intrinsics that will be
// replaced back to library calls only on codegen step. And there is no stdlib
// for wasm target for now :(
// Otherwise `opt` will see no usages of these definitions and will remove them.
extern "C" {
#ifdef KONAN_WASM

RUNTIME_USED
double pow(double x, double y);

RUNTIME_USED
void *memcpy(void *dst, const void *src, size_t n);

RUNTIME_USED
void *memmove(void *dst, const void *src, size_t len);

RUNTIME_USED
int memcmp(const void *s1, const void *s2, size_t n);

RUNTIME_USED
void *memset(void *b, int c, size_t len);

#endif
}

// Memory operations.
void* calloc(size_t count, size_t size);
void* calloc_aligned(size_t count, size_t size, size_t alignment);
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
