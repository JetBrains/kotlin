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
#if !KONAN_NO_THREADS
#include <pthread.h>
#endif
#include <unistd.h>
#if KONAN_WINDOWS
#include <windows.h>
#endif

#include <chrono>

#include "Common.h"
#include "CompilerConstants.hpp"
#include "Porting.h"
#include "KAssert.h"
#include "std_support/CStdlib.hpp"

using namespace kotlin;

#if KONAN_WASM || KONAN_ZEPHYR
extern "C" RUNTIME_NORETURN void Konan_abort(const char*);
extern "C" RUNTIME_NORETURN void Konan_exit(int32_t status);
#endif
#ifdef KONAN_ZEPHYR
// In Zephyr's Newlib strnlen(3) is not included from string.h by default.
extern "C" size_t strnlen(const char* buffer, size_t maxSize);
#endif

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
#ifdef KONAN_ZEPHYR
  return 0;
#elif KONAN_WINDOWS
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

#if KONAN_INTERNAL_SNPRINTF
extern "C" int rpl_vsnprintf(char *, size_t, const char *, va_list);
#define vsnprintf_impl rpl_vsnprintf
#else
#define vsnprintf_impl ::vsnprintf
#endif

NO_EXTERNAL_CALLS_CHECK void consolePrintf(const char* format, ...) {
  char buffer[1024];
  va_list args;
  va_start(args, format);
  int rv = vsnprintf_impl(buffer, sizeof(buffer), format, args);
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
  int rv = vsnprintf_impl(buffer, sizeof(buffer), format, args);
  if (rv < 0) return; // TODO: this may be too much exotic, but should i try to print itoa(error) and terminate?
  if (static_cast<size_t>(rv) >= sizeof(buffer)) rv = sizeof(buffer) - 1;  // TODO: Consider realloc or report truncating.
  va_end(args);
  consoleErrorUtf8(buffer, rv);
}

void consoleFlush() {
  ::fflush(stdout);
  ::fflush(stderr);
}

// Thread execution.
#if !KONAN_NO_THREADS

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
    std_support::free(record);
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

#endif  // !KONAN_NO_THREADS

void onThreadExit(void (*destructor)(void*), void* destructorParameter) {
#if KONAN_NO_THREADS
#if KONAN_WASM || KONAN_ZEPHYR
  // No way to do that.
#else
#error "How to do onThreadExit()?"
#endif
#else  // !KONAN_NO_THREADS
  // We cannot use pthread_cleanup_push() as it is lexical scope bound.
  pthread_once(&terminationKeyOnceControl, onThreadExitInit);
  DestructorRecord* destructorRecord = (DestructorRecord*)std_support::calloc(1, sizeof(DestructorRecord));
  destructorRecord->destructor = destructor;
  destructorRecord->destructorParameter = destructorParameter;
  destructorRecord->next =
      reinterpret_cast<DestructorRecord*>(pthread_getspecific(terminationKey));
  pthread_setspecific(terminationKey, destructorRecord);
#endif  // !KONAN_NO_THREADS
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
#if KONAN_NO_THREADS
#if KONAN_WASM || KONAN_ZEPHYR
    // No way to do that.
    return 0;
#else
#error "How to find currentThreadId()?"
#endif
#else  // !KONAN_NO_THREADS
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
#endif  // !KONAN_NO_THREADS
}

// Process execution.
void abort(void) {
  ::abort();
}

#if KONAN_WASM || KONAN_ZEPHYR
void exit(int32_t status) {
  Konan_exit(status);
}
#else
void exit(int32_t status) {
  ::exit(status);
}
#endif

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

// The sprintf family.
int snprintf(char* buffer, size_t size, const char* format, ...) {
  va_list args;
  va_start(args, format);
  int rv = vsnprintf(buffer, size, format, args);
  va_end(args);
  return rv;
}

int vsnprintf(char* buffer, size_t size, const char* format, va_list args) {
  return vsnprintf_impl(buffer, size, format, args);
}

size_t strnlen(const char* buffer, size_t maxSize) {
  return ::strnlen(buffer, maxSize);
}

#if KONAN_INTERNAL_NOW

#ifdef KONAN_ZEPHYR
void Konan_date_now(uint64_t* arg) {
    // TODO: so how will we support time for embedded?
    *arg = 0LL;
}
#else
extern "C" void Konan_date_now(uint64_t*);
#endif

uint64_t getTimeMillis() {
    uint64_t now;
    Konan_date_now(&now);
    return now;
}

uint64_t getTimeMicros() {
    return getTimeMillis() * 1000ULL;
}

uint64_t getTimeNanos() {
    return getTimeMillis() * 1000000ULL;
}

#else
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
#endif

}  // namespace konan

extern "C" {
// TODO: get rid of these.
#if (KONAN_WASM || KONAN_ZEPHYR)
    void _ZNKSt3__120__vector_base_commonILb1EE20__throw_length_errorEv(void) {
        Konan_abort("TODO: throw_length_error not implemented.");
    }
    void _ZNKSt3__220__vector_base_commonILb1EE20__throw_length_errorEv(void) {
        Konan_abort("TODO: throw_length_error not implemented.");
    }
    void _ZNKSt3__121__basic_string_commonILb1EE20__throw_length_errorEv(void) {
        Konan_abort("TODO: throw_length_error not implemented.");
    }
    void _ZNKSt3__221__basic_string_commonILb1EE20__throw_length_errorEv(void) {
        Konan_abort("TODO: throw_length_error not implemented.");
    }
    int _ZNSt3__212__next_primeEj(unsigned long n) {
        static unsigned long primes[] = {
                11UL,
                101UL,
                1009UL,
                10007UL,
                100003UL,
                1000003UL,
                10000019UL,
                100000007UL,
                1000000007UL
        };
        size_t table_length = sizeof(primes)/sizeof(unsigned long);

        if (n > primes[table_length - 1]) konan::abort();

        unsigned long prime = primes[0];
        for (unsigned long i=0; i< table_length; i++) {
            prime = primes[i];
            if (prime >= n) break;
        }
        return prime;
    }

    int _ZNSt3__212__next_primeEm(int n) {
       return _ZNSt3__212__next_primeEj(n);
    }

    int _ZNSt3__112__next_primeEj(unsigned long n) {
        return _ZNSt3__212__next_primeEj(n);
    }
    void __assert_fail(const char* assertion, const char* file, int line, const char* function) {
        char buf[1024];
        konan::snprintf(buf, sizeof(buf), "%s:%d in %s: runtime assert: %s\n", file, line, function, assertion);
        Konan_abort(buf);
    }
    int* __errno_location() {
        static int theErrno = 0;
        return &theErrno;
    }

    // Some math.h functions.

    double pow(double x, double y) {
        return __builtin_pow(x, y);
    }
#endif

#ifdef KONAN_WASM
    // Some string.h functions.
    void *memcpy(void *dst, const void *src, size_t n) {
        for (size_t i = 0; i != n; ++i)
            *((char*)dst + i) = *((char*)src + i);
        return dst;
    }

    void *memmove(void *dst, const void *src, size_t len)  {
        if (src < dst) {
            for (long i = len; i != 0; --i) {
                *((char*)dst + i - 1) = *((char*)src + i - 1);
            }
        } else {
            memcpy(dst, src, len);
        }
        return dst;
    }

    int memcmp(const void *s1, const void *s2, size_t n) {
        for (size_t i = 0; i != n; ++i) {
            if (*((char*)s1 + i) != *((char*)s2 + i)) {
                return *((char*)s1 + i) - *((char*)s2 + i);
            }
        }
        return 0;
    }

    void *memset(void *b, int c, size_t len) {
        for (size_t i = 0; i != len; ++i) {
            *((char*)b + i) = c;
        }
        return b;
    }

    size_t strlen(const char *s) {
        for (long i = 0;; ++i) {
            if (s[i] == 0) return i;
        }
    }

    size_t strnlen(const char *s, size_t maxlen) {
        for (size_t i = 0; i<=maxlen; ++i) {
            if (s[i] == 0) return i;
        }
        return maxlen;
    }
#endif

#ifdef KONAN_ZEPHYR
    RUNTIME_USED void Konan_abort(const char*) {
        while(1) {}
    }
#endif // KONAN_ZEPHYR

#if defined(KONAN_MIPS32) || defined(KONAN_MIPSEL32)

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Watomic-alignment"

// By some reasons clang generates __sync functions instead of __atomic ones,
// but they are not actually available on mips. So let's implement them ourselfs using existing __atomic ones.


int64_t replace_sync_fetch_and_add_8(int64_t *ptr, int64_t value) asm("__sync_fetch_and_add_8");
RUNTIME_USED int64_t replace_sync_fetch_and_add_8(int64_t *ptr, int64_t value) {
    return __atomic_fetch_add(ptr, value, __ATOMIC_SEQ_CST);
}
int64_t replace_sync_val_compare_and_swap(int64_t *ptr, int64_t oldval, int64_t newval) asm("__sync_val_compare_and_swap_8");
RUNTIME_USED int64_t replace_sync_val_compare_and_swap (int64_t *ptr, int64_t oldval, int64_t newval) {
    __atomic_compare_exchange_n(ptr, &oldval, newval, false, __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
    return oldval;
}

int64_t replace_sync_lock_test_and_set(int64_t *ptr, int64_t value) asm("__sync_lock_test_and_set_8");
RUNTIME_USED int64_t replace_sync_lock_test_and_set(int64_t *ptr, int64_t value) {
    return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
}


#pragma clang diagnostic pop

#endif


}  // extern "C"
