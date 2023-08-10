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
#include <cstdio>
#include <cstdlib>
#include <stdarg.h>
#include <stdint.h>
#include <string.h>
#include <pthread.h>
#include <unistd.h>
#if KONAN_WINDOWS
#include <windows.h>
#endif

#include <chrono>

#include "Common.h"
#include "CompilerConstants.hpp"
#include "Porting.h"
#include "KAssert.h"

using namespace kotlin;

namespace konan {

// Console operations.
void consoleInit() {
#if KONAN_WINDOWS
  // Note that this code enforces UTF-8 console output, so we may want to rethink
  // how we perform console IO, if it turns out, that UTF-16 is better output format.
  ::SetConsoleCP(CP_UTF8);
  ::SetConsoleOutputCP(CP_UTF8);
  // FIXME: should set original CP back during the deinit of the program.
  //  Otherwise, this codepage remains in the console.
#endif
}

void consoleWriteUtf8(const char* utf8, uint32_t sizeBytes) {
#ifdef KONAN_ANDROID
  if (kotlin::compiler::printToAndroidLogcat()) {
    // TODO: use sizeBytes!
    __android_log_print(ANDROID_LOG_INFO, "Konan_main", "%s", utf8);
  } else {
    ::write(STDOUT_FILENO, utf8, sizeBytes);
  }
#else
  ::write(STDOUT_FILENO, utf8, sizeBytes);
#endif
}

void consoleErrorUtf8(const char* utf8, uint32_t sizeBytes) {
#ifdef KONAN_ANDROID
  if (kotlin::compiler::printToAndroidLogcat()) {
    // TODO: use sizeBytes!
    __android_log_print(ANDROID_LOG_ERROR, "Konan_main", "%s", utf8);
  } else {
    ::write(STDERR_FILENO, utf8, sizeBytes);
  }
#else
  ::write(STDERR_FILENO, utf8, sizeBytes);
#endif
}

#if KONAN_WINDOWS
int getLastErrorMessage(char* message, uint32_t size) {
  auto errCode = ::GetLastError();
  if (errCode) {
    auto flags = FORMAT_MESSAGE_FROM_SYSTEM;
    auto errMsgBufSize = size / 4;
    wchar_t errMsgBuffer[errMsgBufSize];
    ::FormatMessageW(flags, NULL, errCode, 0, errMsgBuffer, errMsgBufSize, NULL);
    ::WideCharToMultiByte(CP_UTF8, 0, errMsgBuffer, -1, message, size, NULL, NULL);
  }
  return errCode;
}
#endif

int32_t consoleReadUtf8(void* utf8, uint32_t maxSizeBytes) {
#if KONAN_WINDOWS
  auto length = 0;
  void *stdInHandle = ::GetStdHandle(STD_INPUT_HANDLE);
  if (::GetFileType(stdInHandle) == FILE_TYPE_CHAR) {
    unsigned long bufferRead;
    // In UTF-16 there are surrogate pairs that a 2 * 16-bit long (4 bytes).
    auto bufferLength = maxSizeBytes / 4 - 1;
    wchar_t buffer[bufferLength];
    if (::ReadConsoleW(stdInHandle, buffer, bufferLength, &bufferRead, NULL)) {
      length = ::WideCharToMultiByte(CP_UTF8, 0, buffer, bufferRead, (char*) utf8,
                                     maxSizeBytes - 1, NULL, NULL);
      if (!length && kotlin::compiler::shouldContainDebugInfo()) {
        char msg[512];
        auto errCode = getLastErrorMessage(msg, sizeof(msg));
        consoleErrorf("UTF-16 to UTF-8 conversion error %d: %s", errCode, msg);
      }
      ((char*) utf8)[length] = 0;
    } else if (kotlin::compiler::shouldContainDebugInfo()) {
      char msg[512];
      auto errCode = getLastErrorMessage(msg, sizeof(msg));
      consoleErrorf("Console read failure: %d %s", errCode, msg);
    }
  } else {
    length = ::read(STDIN_FILENO, utf8, maxSizeBytes - 1);
  }
#else
  auto length = ::read(STDIN_FILENO, utf8, maxSizeBytes - 1);
#endif
  if (length <= 0) return -1;
  char* start = reinterpret_cast<char*>(utf8);
  char* current = start + length - 1;
  bool isTrimming = true;
  while (current >= start && isTrimming) {
    switch (*current) {
      case '\n':
      case '\r':
        *current = 0;
        length--;
        break;
      default:
        isTrimming = false;
    }
    current--;
  }
  return length;
}

NO_EXTERNAL_CALLS_CHECK void consolePrintf(const char* format, ...) {
  char buffer[1024];
  va_list args;
  va_start(args, format);
  int rv = std::vsnprintf(buffer, sizeof(buffer), format, args);
  if (rv < 0) return; // TODO: this may be too much exotic, but should i try to print itoa(error) and terminate?
  if (static_cast<size_t>(rv) >= sizeof(buffer)) rv = sizeof(buffer) - 1;  // TODO: Consider realloc or report truncating.
  va_end(args);
  consoleWriteUtf8(buffer, rv);
}

// TODO: Avoid code duplication.
NO_EXTERNAL_CALLS_CHECK void consoleErrorf(const char* format, ...) {
  char buffer[1024];
  va_list args;
  va_start(args, format);
  int rv = std::vsnprintf(buffer, sizeof(buffer), format, args);
  if (rv < 0) return; // TODO: this may be too much exotic, but should i try to print itoa(error) and terminate?
  if (static_cast<size_t>(rv) >= sizeof(buffer)) rv = sizeof(buffer) - 1;  // TODO: Consider realloc or report truncating.
  va_end(args);
  consoleErrorUtf8(buffer, rv);
}

void consoleFlush() {
  ::fflush(stdout);
  ::fflush(stderr);
}

pthread_key_t terminationKey;
pthread_once_t terminationKeyOnceControl = PTHREAD_ONCE_INIT;

typedef void (*destructor_t)(void*);

struct DestructorRecord {
  struct DestructorRecord* next;
  destructor_t destructor;
  void* destructorParameter;
};

static void onThreadExitCallback(void* value) {
  DestructorRecord* record = reinterpret_cast<DestructorRecord*>(value);
  pthread_setspecific(terminationKey, nullptr);
  while (record != nullptr) {
    record->destructor(record->destructorParameter);
    auto next = record->next;
    std::free(record);
    record = next;
  }
}

NO_EXTERNAL_CALLS_CHECK bool isOnThreadExitNotSetOrAlreadyStarted() {
    return terminationKey != 0 && pthread_getspecific(terminationKey) == nullptr;
}

#if KONAN_LINUX
static pthread_key_t dummyKey;
#endif
static void onThreadExitInit() {
#if KONAN_LINUX
  // Due to glibc bug we have to create first key as dummy, to avoid
  // conflicts with potentially uninitialized dlfcn error key.
  // https://code.woboq.org/userspace/glibc/dlfcn/dlerror.c.html#237
  // As one may see, glibc checks value of the key even if it was not inited (and == 0),
  // and so data associated with our legit key (== 0 as being the first one) is used.
  // Other libc are not affected, as usually == 0 pthread key is impossible.
  pthread_key_create(&dummyKey, nullptr);
#endif
  pthread_key_create(&terminationKey, onThreadExitCallback);
}

void onThreadExit(void (*destructor)(void*), void* destructorParameter) {
  // We cannot use pthread_cleanup_push() as it is lexical scope bound.
  pthread_once(&terminationKeyOnceControl, onThreadExitInit);
  DestructorRecord* destructorRecord = (DestructorRecord*)std::calloc(1, sizeof(DestructorRecord));
  destructorRecord->destructor = destructor;
  destructorRecord->destructorParameter = destructorParameter;
  destructorRecord->next =
      reinterpret_cast<DestructorRecord*>(pthread_getspecific(terminationKey));
  pthread_setspecific(terminationKey, destructorRecord);
}

#if KONAN_LINUX
#include <unistd.h>
#include <sys/types.h>
#include <sys/syscall.h>

/**
 * We can't use gettid from glibc 2.30 because it is too new for us.
 */
NO_EXTERNAL_CALLS_CHECK NO_INLINE int gettid() {
    return static_cast<int>(syscall(__NR_gettid));
}
#endif

NO_EXTERNAL_CALLS_CHECK int currentThreadId() {
#if defined(KONAN_OSX) or defined(KONAN_IOS) or defined(KONAN_TVOS) or defined(KONAN_WATCHOS)
    uint64_t tid;
    pthread_t self = pthread_self();
    RuntimeCheck(!pthread_threadid_np(self, &tid), "Error getting thread id");
    RuntimeCheck((*(reinterpret_cast<int32_t*>(&tid) + 1)) == 0, "Thread id is not a uint32");
    return tid;
#elif KONAN_ANDROID
    return gettid();
#elif KONAN_LINUX
    return gettid();
#elif KONAN_WINDOWS
  return GetCurrentThreadId();
#else
#error "How to find currentThreadId()?"
#endif
}

// String/byte operations.
// memcpy/memmove are not here intentionally, as frequently implemented/optimized
// by C compiler.
void* memmem(const void *big, size_t bigLen, const void *little, size_t littleLen) {
#if KONAN_NO_MEMMEM
  for (size_t i = 0; i + littleLen <= bigLen; ++i) {
    void* pos = ((char*)big) + i;
    if (::memcmp(little, pos, littleLen) == 0) return pos;
  }
  return nullptr;
#else
  return ::memmem(big, bigLen, little, littleLen);
#endif

}

// Time operations.
using namespace std::chrono;

// Get steady clock as a source of time
using steady_time_clock = std::conditional<high_resolution_clock::is_steady, high_resolution_clock, steady_clock>::type;

uint64_t getTimeMillis() {
  return duration_cast<milliseconds>(steady_time_clock::now().time_since_epoch()).count();
}

uint64_t getTimeNanos() {
  return duration_cast<nanoseconds>(steady_time_clock::now().time_since_epoch()).count();
}

uint64_t getTimeMicros() {
  return duration_cast<microseconds>(steady_time_clock::now().time_since_epoch()).count();
}

}  // namespace konan
