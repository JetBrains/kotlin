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
#include "Porting.h"

#if KONAN_WASM || KONAN_ZEPHYR
extern "C" void Konan_abort(const char*);
extern "C" void Konan_exit(int32_t status);
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
#ifdef KONAN_ZEPHYR
  return 0;
#else 
  char* result = ::fgets(reinterpret_cast<char*>(utf8), maxSizeBytes - 1, stdin);
  if (result == nullptr) return 0;
  return ::strlen(result);
#endif
}

#if KONAN_INTERNAL_SNPRINTF
extern "C" int rpl_vsnprintf(char *, size_t, const char *, va_list);
#define vsnprintf_impl rpl_vsnprintf
#else
#define vsnprintf_impl ::vsnprintf
#endif


void consolePrintf(const char* format, ...) {
  char buffer[1024];
  va_list args;
  va_start(args, format);
  int rv = vsnprintf_impl(buffer, sizeof(buffer) - 1, format, args);
  va_end(args);
  consoleWriteUtf8(buffer, rv);
}


// Thread execution.
#if !KONAN_NO_THREADS

pthread_key_t terminationKey;
pthread_once_t terminationKeyOnceControl =  PTHREAD_ONCE_INIT;

typedef void (*destructor_t)();

struct DestructorRecord {
  struct DestructorRecord* next;
  destructor_t destructor;
};

static void onThreadExitCallback(void* value) {
  DestructorRecord* record = reinterpret_cast<DestructorRecord*>(value);
  while (record != nullptr) {
    record->destructor();
    auto next = record->next;
    free(record);
    record = next;
  }
}

static void onThreadExitInit() {
  pthread_key_create(&terminationKey, onThreadExitCallback);
}

#endif  // !KONAN_NO_THREADS

void onThreadExit(void (*destructor)()) {
#if KONAN_NO_THREADS
#if KONAN_WASM || KONAN_ZEPHYR
  // No way to do that.
#else
#error "How to do onThreadExit()?"
#endif
#else  // !KONAN_NO_THREADS
  // We cannot use pthread_cleanup_push() as it is lexical scope bound.
  pthread_once(&terminationKeyOnceControl, onThreadExitInit);
  DestructorRecord* destructorRecord = (DestructorRecord*)calloc(1, sizeof(DestructorRecord));
  destructorRecord->destructor = destructor;
  destructorRecord->next =
      reinterpret_cast<DestructorRecord*>(pthread_getspecific(terminationKey));
  pthread_setspecific(terminationKey, destructorRecord);
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
  int rv = vsnprintf_impl(buffer, size, format, args);
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
#define calloc_impl dlcalloc
#define free_impl dlfree
#else
#define calloc_impl ::calloc
#define free_impl ::free
#endif

void* calloc(size_t count, size_t size) {
  return calloc_impl(count, size);
}

void free(void* pointer) {
  free_impl(pointer);
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

uint64_t getTimeMillis() {
  return duration_cast<milliseconds>(high_resolution_clock::now().time_since_epoch()).count();
}

uint64_t getTimeNanos() {
  return duration_cast<nanoseconds>(high_resolution_clock::now().time_since_epoch()).count();
}

uint64_t getTimeMicros() {
  return duration_cast<microseconds>(high_resolution_clock::now().time_since_epoch()).count();
}
#endif

#if KONAN_INTERNAL_DLMALLOC
// This function is being called when memory allocator needs more RAM.

#if KONAN_WASM

// This one is an interface to query module.env.memory.buffer.byteLength
extern "C" unsigned long Konan_heap_upper();
extern "C" unsigned long Konan_heap_lower();
extern "C" unsigned long Konan_heap_grow(unsigned long);

#define MFAIL ((void*) ~(size_t)0)
#define WASM_PAGESIZE_EXPONENT 16
#define WASM_PAGESIZE  (1u << WASM_PAGESIZE_EXPONENT)
#define WASM_PAGEMASK ((WASM_PAGESIZE-(size_t)1))
#define PAGE_ALIGN(value) ((value + WASM_PAGEMASK) & ~(WASM_PAGEMASK))
#define IN_PAGES(value) (value >> WASM_PAGESIZE_EXPONENT)

void* moreCore(int size) {
    static int initialized = 0;
    static void* sbrk_top = MFAIL;
    static void* upperHeapLimit = MFAIL;

    if (!initialized) {
        sbrk_top = (void*)PAGE_ALIGN(Konan_heap_lower());
        initialized = 1;
    }

    if (size == 0) {
        return sbrk_top;
    } else if (size < 0) {
        return MFAIL;
    }

    size = PAGE_ALIGN(size);

    void* old_sbrk_top = sbrk_top;
    long excess = (char*)sbrk_top + size - (char*)Konan_heap_upper();
    if (excess > 0) {
        Konan_heap_grow(IN_PAGES(PAGE_ALIGN(excess)));
    }
    sbrk_top = (char*)sbrk_top + size;

    return old_sbrk_top;
}

// dlmalloc wants to know the page size.
long getpagesize() {
    return WASM_PAGESIZE;
}

#else
void* moreCore(int size) {
    return sbrk(size);
}

long getpagesize() {
    return sysconf(_SC_PAGESIZE);
}
#endif
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
        int table_length = sizeof(primes)/sizeof(unsigned long);

        if (n > primes[table_length - 1]) konan::abort();

        unsigned long prime = primes[0];
        for (unsigned long i=0; i< table_length; i++) {
            prime = primes[i];
            if (prime >= n) break;
        }
        return prime;
    }
    int _ZNSt3__112__next_primeEj(unsigned long n) {
        return _ZNSt3__212__next_primeEj(n);
    }
    void __assert_fail(const char * assertion, const char * file, unsigned int line, const char * function) {
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
        for (long i = 0; i != n; ++i)
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
        for (long i = 0; i != n; ++i) {
            if (*((char*)s1 + i) != *((char*)s2 + i)) {
                return *((char*)s1 + i) - *((char*)s2 + i);
            }
        }
        return 0;
    }

    void *memset(void *b, int c, size_t len) {
        for (long i = 0; i != len; ++i) {
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
        for (long i = 0; i<=maxlen; ++i) {
            if (s[i] == 0) return i;
        }
        return maxlen;
    }
#endif

#ifdef KONAN_ZEPHYR
    RUNTIME_USED void Konan_abort(const char*) {
        while(1) {}
    }

    /* Support the alias for the __aeabi_memset which may
       assume memory alignment.  */
    RUNTIME_USED void __aeabi_memset4 (void *dest, size_t n, int c)
        __attribute__ ((alias ("__aeabi_memset")));

    RUNTIME_USED void __aeabi_memset8 (void *dest, size_t n, int c)
        __attribute__ ((alias ("__aeabi_memset")));

    /* Support the routine __aeabi_memset.  Can't alias to memset
       because it's not defined in the same translation unit.  */
    RUNTIME_USED void __aeabi_memset (void *dest, size_t n, int c)
    {
      /*Note that relative to ANSI memset, __aeabi_memset hase the order
        of its second and third arguments reversed.  */
      memset (dest, c, n);
    }
    /* Support the alias for the __aeabi_memclr which may
       assume memory alignment.  */
    RUNTIME_USED void __aeabi_memclr4 (void *dest, size_t n)
        __attribute__ ((alias ("__aeabi_memclr")));

    RUNTIME_USED void __aeabi_memclr8 (void *dest, size_t n)
        __attribute__ ((alias ("__aeabi_memclr")));

    /* Support the routine __aeabi_memclr.  */
    RUNTIME_USED void __aeabi_memclr (void *dest, size_t n)
    {
      __aeabi_memset (dest, n, 0);
    }
#endif // KONAN_ZEPHYR
}
