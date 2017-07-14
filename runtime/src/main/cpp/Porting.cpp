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

#ifdef KONAN_ANDROID
#include <android/log.h>
#endif
#include <stdarg.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#if KONAN_WINDOWS
#include <windows.h>
#endif

#include <chrono>

#include "Porting.h"

namespace konan {

// Console operations.
void consoleInit() {
#if KONAN_WINDOWS
  // Note that this code enforces UTF-8 console output, so we may want to rethink
  // how we perform console IO, if it turns out, that UTF-16 is better output format.
  ::SetConsoleCP(CP_UTF8);
  ::SetConsoleOutputCP(CP_UTF8);
#endif
}

void consoleWriteUtf8(const void* utf8, uint32_t sizeBytes) {
#ifdef KONAN_ANDROID
  // TODO: use sizeBytes!
  __android_log_print(ANDROID_LOG_INFO, "Konan_main", "%s", utf8);
#else
  ::write(STDOUT_FILENO, utf8, sizeBytes);
#endif
}

void consoleErrorUtf8(const void* utf8, uint32_t sizeBytes) {
#ifdef KONAN_ANDROID
  // TODO: use sizeBytes!
  __android_log_print(ANDROID_LOG_ERROR, "Konan_main", "%s", utf8);
#else
  ::write(STDERR_FILENO, utf8, sizeBytes);
#endif
}

uint32_t consoleReadUtf8(void* utf8, uint32_t maxSizeBytes) {
  char* result = ::fgets(reinterpret_cast<char*>(utf8), maxSizeBytes - 1, stdin);
  if (result == nullptr) return 0;
  return ::strlen(result);
}

// Process execution.
void abort() {
  ::abort();
}

// String/byte operations.
// memcpy/memmove are not here intentionally, as frequently implemented/optimized
// by C compiler.
void* memmem(const void *big, size_t bigLen, const void *little, size_t littleLen) {
#if KONAN_WINDOWS
  for (size_t i = 0; i + littleLen <= bigLen; ++i) {
    void* pos = ((char*)big) + i;
    if (::memcmp(little, pos, littleLen) == 0) return pos;
  }
  return nullptr;
#else
  return ::memmem(big, bigLen, little, littleLen);
#endif

}

int snprintf(char* buffer, size_t size, const char* format, ...) {
  va_list args;
  va_start(args, format);
  int rv = ::vsnprintf(buffer, size, format, args);
  va_end(args);
  return rv;
}

size_t strnlen(const char* buffer, size_t maxSize) {
  return ::strnlen(buffer, maxSize);
}

// Memory operations.
#if KONAN_INTERNAL_DLMALLOC
extern "C" void* dlcalloc(size_t, size_t);
extern "C" void dlfree(void*);
#endif

void* calloc(size_t count, size_t size) {
#if KONAN_INTERNAL_DLMALLOC
  return dlcalloc(count, size);
#else
  return ::calloc(count, size);
#endif
}

void free(void* pointer) {
#if KONAN_INTERNAL_DLMALLOC
  dlfree(pointer);
#else
  ::free(pointer);
#endif
}

// Time operations.
using namespace std::chrono;

uint64_t getTimeMillis() {
  return duration_cast<milliseconds>(high_resolution_clock::now().time_since_epoch()).count();
}

uint64_t getTimeNanos() {
  return duration_cast<nanoseconds>(high_resolution_clock::now().time_since_epoch()).count();
}

uint64_t getTimeMicros() {
  return duration_cast<microseconds>(high_resolution_clock::now().time_since_epoch()).count();
}

#if KONAN_INTERNAL_DLMALLOC
// This function is being called when memory allocator needs more RAM.
void* moreCore(int size) {
  return sbrk(size);
}
#endif

}  // namespace konan
