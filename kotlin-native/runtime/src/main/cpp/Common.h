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

#ifndef RUNTIME_COMMON_H
#define RUNTIME_COMMON_H

#define RUNTIME_NOTHROW __attribute__((nothrow))
#define RUNTIME_NORETURN __attribute__((noreturn))
#define RUNTIME_CONST __attribute__((const))
#define RUNTIME_PURE __attribute__((pure))
#define RUNTIME_USED __attribute__((used))
#define RUNTIME_WEAK __attribute__((weak))
#define RUNTIME_NODEBUG __attribute__((nodebug))

#if KONAN_NEED_SMALL_BINARY
  // On the one hand, ALWAYS_INLINE forces many performance-critical function to be, well,
  // inlined. Which is good for performance, of course.
  // On the other hand, 32-bit Mach-O object files can't be really big.
  // As a compromise, we let the compiler decide what should be inlined or not.
  #define ALWAYS_INLINE
#else
  #define ALWAYS_INLINE __attribute__((always_inline))
#endif
#define NO_INLINE __attribute__((noinline))

#define OPTNONE __attribute__((optnone))

#define THREAD_LOCAL_VARIABLE __thread

#define ARRAY_SIZE(a) (sizeof(a) / sizeof(a[0]))

#if KONAN_OBJC_INTEROP
#define KONAN_TYPE_INFO_HAS_WRITABLE_PART 1
#endif


#endif // RUNTIME_COMMON_H
