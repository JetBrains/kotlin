/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#if !defined(hycomp_h)
#define hycomp_h
// TODO: Move to settings
#define MACOSX
/**
 * USE_PROTOTYPES:         Use full ANSI prototypes.
 *
 * CLOCK_PRIMS:            We want the timer/clock prims to be used
 *
 * LITTLE_ENDIAN:          This is for the intel machines or other
 *                         little endian processors. Defaults to big endian.
 *
 * NO_LVALUE_CASTING:      This is for compilers that don't like the left side
 *                         of assigns to be cast.  It hacks around to do the
 *                         right thing.
 *
 * ATOMIC_FLOAT_ACCESS:    So that float operations will work.
 *
 * LINKED_USER_PRIMITIVES: Indicates that user primitives are statically linked
 *                         with the VM executeable.
 *
 * OLD_SPACE_SIZE_DIFF:    The 68k uses a different amount of old space.
 *                         This "legitimizes" the change.
 *
 * SIMPLE_SIGNAL:          For machines that don't use real signals in C.
 *                         (eg: PC, 68k)
 *
 * OS_NAME_LOOKUP:         Use nlist to lookup user primitive addresses.
 *
 * VMCALL:                 Tag for all functions called by the VM.
 *
 * VMAPICALL:              Tag for all functions called via the PlatformFunction
 *                         callWith: mechanism.
 *
 * SYS_FLOAT:              For some math functions where extended types (80 or 96 bits) are returned
 *                         Most platforms return as a double
 *
 * FLOAT_EXTENDED:         If defined, the type name for extended precision floats.
 *
 * PLATFORM_IS_ASCII:      Must be defined if the platform is ASCII
 *
 * EXE_EXTENSION_CHAR:     the executable has a delimiter that we want to stop at as part of argv[0].
 */

 /**
 * By default order doubles in the native (that is big/little endian) ordering.
 */

#define HY_PLATFORM_DOUBLE_ORDER

/**
 * Define common types:
 * <ul>
 * <li><code>U_32 / I_32</code>  - unsigned/signed 32 bits</li>
 * <li><code>U_16 / I_16</code>  - unsigned/signed 16 bits</li>
 * <li><code>U_8 / I_8</code>    - unsigned/signed 8 bits (bytes -- not to be
 *                                 confused with char)</li>
 * </ul>
 */

typedef int I_32;
typedef short I_16;
typedef signed char I_8; /* chars can be unsigned */
typedef unsigned int U_32;
typedef unsigned short U_16;
typedef unsigned char U_8;

/**
 * Define platform specific types:
 * <ul>
 * <li><code>U_64 / I_64</code>  - unsigned/signed 64 bits</li>
 * </ul>
 */

#if defined(LINUX) || defined(FREEBSD) || defined(AIX) || defined(MACOSX)

#define DATA_TYPES_DEFINED

/* NOTE: Linux supports different processors -- do not assume 386 */
#if defined(HYX86_64) || defined(HYIA64) || defined(HYPPC64) || defined(HYS390X)

typedef unsigned long int U_64;         /* 64bits */
typedef long int I_64;
#define TOC_UNWRAP_ADDRESS(wrappedPointer) ((void *) (wrappedPointer)[0])
#define TOC_STORE_TOC(dest,wrappedPointer) (dest = ((UDATA*)wrappedPointer)[1])

#define HY_WORD64

#else

typedef long long I_64;
typedef unsigned long long U_64;

#endif

#if defined(HYS390X) || defined(HYS390) || defined(HYPPC64) || defined(HYPPC32) || defined(__MIPSEB__)
#define HY_BIG_ENDIAN
#else
#define HY_LITTLE_ENDIAN
#endif

#if defined(HYPPC32) && defined(LINUX)
#define VA_PTR(valist) (&valist[0])
#endif

typedef double SYS_FLOAT;
#define HYCONST64(x) x##LL
#define NO_LVALUE_CASTING
#define FLOAT_EXTENDED  long double
#define PLATFORM_IS_ASCII
#define PLATFORM_LINE_DELIMITER "\012"
#define DIR_SEPARATOR '/'
#define DIR_SEPARATOR_STR "/"
#define PATH_SEPARATOR ':'
#define PATH_SEPARATOR_STR ":"
#if defined(AIX)
#define LIBPATH_ENV_VAR "LIBPATH"
#else
#if defined(MACOSX)
#define LIBPATH_ENV_VAR "DYLD_LIBRARY_PATH"
#else
#define LIBPATH_ENV_VAR "LD_LIBRARY_PATH"
#endif
#endif
#if defined(MACOSX)
#define PLATFORM_DLL_EXTENSION ".dylib"
#else
#define PLATFORM_DLL_EXTENSION ".so"
#endif

/**
 * No priorities on Linux
 */

#define HY_PRIORITY_MAP {0,0,0,0,0,0,0,0,0,0,0,0}

typedef U_32 BOOLEAN;

#endif

/* Win32 - Windows 3.1 & NT using Win32 */
#if defined(WIN32)

#define HY_LITTLE_ENDIAN

/* Define 64-bit integers for Windows */
typedef __int64 I_64;
typedef unsigned __int64 U_64;

typedef double SYS_FLOAT;
#define NO_LVALUE_CASTING
#define VMAPICALL _stdcall
#define VMCALL _cdecl
#define EXE_EXTENSION_CHAR  '.'

#define DIR_SEPARATOR '\\'
#define DIR_SEPARATOR_STR "\\"
#define PATH_SEPARATOR ';'
#define PATH_SEPARATOR_STR ";"
#define LIBPATH_ENV_VAR "PATH"

/* Modifications for the Alpha running WIN-NT */
#if defined(_ALPHA_)
#undef small                    /* defined as char in rpcndr.h */
typedef double FLOAT_EXTENDED;
#endif

#define HY_PRIORITY_MAP { \
  THREAD_PRIORITY_IDLE,             /* 0 */\
  THREAD_PRIORITY_LOWEST,           /* 1 */\
  THREAD_PRIORITY_BELOW_NORMAL,     /* 2 */\
  THREAD_PRIORITY_BELOW_NORMAL,     /* 3 */\
  THREAD_PRIORITY_BELOW_NORMAL,     /* 4 */\
  THREAD_PRIORITY_NORMAL,           /* 5 */\
  THREAD_PRIORITY_ABOVE_NORMAL,     /* 6 */\
  THREAD_PRIORITY_ABOVE_NORMAL,     /* 7 */\
  THREAD_PRIORITY_ABOVE_NORMAL,     /* 8 */\
  THREAD_PRIORITY_ABOVE_NORMAL,     /* 9 */\
  THREAD_PRIORITY_HIGHEST,          /*10 */\
  THREAD_PRIORITY_TIME_CRITICAL     /*11 */}

#endif /* defined(WIN32) */

#if defined(ZOS)

#define HY_BIG_ENDIAN

#define DATA_TYPES_DEFINED
typedef unsigned int   BOOLEAN;
#if defined (HYS390X)
typedef unsigned long U_64;
typedef long I_64;
#else
typedef signed long long	I_64;
typedef unsigned long long	U_64;
#endif

typedef double				SYS_FLOAT;

#define HYCONST64(x) x##LL

#define NO_LVALUE_CASTING
#define PLATFORM_LINE_DELIMITER	"\012"
#define DIR_SEPARATOR '/'
#define DIR_SEPARATOR_STR "/"
#define PATH_SEPARATOR ':'
#define PATH_SEPARATOR_STR ":"
#define LIBPATH_ENV_VAR "LIBPATH"

#define VA_PTR(valist) (&valist[0])

typedef struct {
#if !defined(HYS390X)
	char stuff[16];
#endif
	char *ada;
	void (*rawFnAddress)();
} HyFunctionDescriptor_T;

#define TOC_UNWRAP_ADDRESS(wrappedPointer) (((HyFunctionDescriptor_T *) (wrappedPointer))->rawFnAddress)

#define PLATFORM_DLL_EXTENSION ".so"

#ifdef HYS390X
#ifndef HY_WORD64
#define HY_WORD64
#endif /* ifndef HY_WORD64 */
#endif /* HYS390X */

#endif /* ZOS */


#if !defined(VMCALL)
#define VMCALL
#define VMAPICALL
#endif
#define PVMCALL VMCALL *

#define GLOBAL_DATA(symbol) ((void*)&(symbol))
#define GLOBAL_TABLE(symbol) GLOBAL_DATA(symbol)

/**
 * Define platform specific types:
 * <ul>
 * <li><code>UDATA</code>        - unsigned data, can be used as an integer or
 *                                 pointer storage</li>
 * <li><code>IDATA</code>        - signed data, can be used as an integer or
 *                                 pointer storage</li>
 * </ul>
 */
/* FIXME: POINTER64 */
#if defined(HYX86_64) || defined(HYIA64) || defined(HYPPC64) || defined(HYS390X) || defined(POINTER64)

typedef I_64 IDATA;
typedef U_64 UDATA;

#else /* this is default for non-64bit systems */

typedef I_32 IDATA;
typedef U_32 UDATA;

#endif /* defined(HYX86_64) */

#if !defined(DATA_TYPES_DEFINED)
/* no generic U_64 or I_64 */

/* don't typedef BOOLEAN since it's already def'ed on Win32 */
#define BOOLEAN UDATA

#ifndef HY_BIG_ENDIAN
#define HY_LITTLE_ENDIAN
#endif

#endif

#if !defined(HYCONST64)
#define HYCONST64(x) x##L
#endif

#if !defined(HY_DEFAULT_SCHED)

/**
 * By default, pthreads platforms use the <code>SCHED_OTHER</code> thread
 * scheduling policy.
 */

#define HY_DEFAULT_SCHED SCHED_OTHER
#endif

#if !defined(HY_PRIORITY_MAP)

/**
 * If no priority map if provided, priorities will be determined
 * algorithmically.
 */

#endif

#if !defined(FALSE)
#define FALSE   ((BOOLEAN) 0)
#if !defined(TRUE)
#define TRUE    ((BOOLEAN) (!FALSE))
#endif
#endif

#if !defined(NULL)
#if defined(__cplusplus)
#define NULL    (0)
#else
#define NULL    ((void *)0)
#endif
#endif
#define USE_PROTOTYPES
#if defined(USE_PROTOTYPES)
#define PROTOTYPE(x)  x
#define VARARGS   , ...
#else
#define PROTOTYPE(x)  ()
#define VARARGS
#endif

/**
 * Assign the default line delimiter, if it was not set.
 */

#if !defined(PLATFORM_LINE_DELIMITER)
#define PLATFORM_LINE_DELIMITER "\015\012"
#endif

/**
 * Set the max path length, if it was not set.
 */

#if !defined(MAX_IMAGE_PATH_LENGTH)
#define MAX_IMAGE_PATH_LENGTH (2048)
#endif
typedef double ESDOUBLE;
typedef float ESSINGLE;

/**
 * Helpers for U_64s.
 */

#define CLEAR_U64(u64)  (u64 = (U_64)0)
#define LOW_LONG(l) (*((U_32 *) &(l)))
#define HIGH_LONG(l)  (*(((U_32 *) &(l)) + 1))
#define I8(x)       ((I_8) (x))
#define I8P(x)      ((I_8 *) (x))
#define U16(x)      ((U_16) (x))
#define I16(x)      ((I_16) (x))
#define I16P(x)     ((I_16 *) (x))
#define U32(x)      ((U_32) (x))
#define I32(x)      ((I_32) (x))
#define I32P(x)     ((I_32 *) (x))
#define U16P(x)     ((U_16 *) (x))
#define U32P(x)     ((U_32 *) (x))
#define OBJP(x)     ((HyObject *) (x))
#define OBJPP(x)    ((HyObject **) (x))
#define OBJPPP(x)   ((HyObject ***) (x))
#define CLASSP(x)   ((Class *) (x))
#define CLASSPP(x)  ((Class **) (x))
#define BYTEP(x)    ((BYTE *) (x))

/**
 * Test - was conflicting with OS2.h
 */

#define ESCHAR(x)   ((CHARACTER) (x))
#define FLT(x)      ((FLOAT) x)
#define FLTP(x)     ((FLOAT *) (x))
#if defined(NO_LVALUE_CASTING)
#define LI8(x)      (*((I_8 *) &(x)))
#define LI8P(x)     (*((I_8 **) &(x)))
#define LU16(x)     (*((U_16 *) &(x)))
#define LI16(x)     (*((I_16 *) &(x)))
#define LU32(x)     (*((U_32 *) &(x)))
#define LI32(x)     (*((I_32 *) &(x)))
#define LI32P(x)    (*((I_32 **) &(x)))
#define LU16P(x)    (*((U_16 **) &(x)))
#define LU32P(x)    (*((U_32 **) &(x)))
#define LOBJP(x)    (*((HyObject **) &(x)))
#define LOBJPP(x)   (*((HyObject ***) &(x)))
#define LOBJPPP(x)  (*((HyObject ****) &(x))
#define LCLASSP(x)  (*((Class **) &(x)))
#define LBYTEP(x)   (*((BYTE **) &(x)))
#define LCHAR(x)    (*((CHARACTER) &(x)))
#define LFLT(x)     (*((FLOAT) &x))
#define LFLTP(x)    (*((FLOAT *) &(x)))
#else
#define LI8(x)      I8((x))
#define LI8P(x)     I8P((x))
#define LU16(x)     U16((x))
#define LI16(x)     I16((x))
#define LU32(x)     U32((x))
#define LI32(x)     I32((x))
#define LI32P(x)    I32P((x))
#define LU16P(x)    U16P((x))
#define LU32P(x)    U32P((x))
#define LOBJP(x)    OBJP((x))
#define LOBJPP(x)   OBJPP((x))
#define LOBJPPP(x)  OBJPPP((x))
#define LIOBJP(x)   IOBJP((x))
#define LCLASSP(x)  CLASSP((x))
#define LBYTEP(x)   BYTEP((x))
#define LCHAR(x)    CHAR((x))
#define LFLT(x)     FLT((x))
#define LFLTP(x)    FLTP((x))
#endif

/**
 * Macros for converting between words and longs and accessing bits.
 */

#define HIGH_WORD(x)  U16(U32((x)) >> 16)
#define LOW_WORD(x)   U16(U32((x)) & 0xFFFF)
#define LOW_BIT(o)    (U32((o)) & 1)
#define LOW_2_BITS(o) (U32((o)) & 3)
#define LOW_3_BITS(o) (U32((o)) & 7)
#define LOW_4_BITS(o) (U32((o)) & 15)
#define MAKE_32(h, l) ((U32((h)) << 16) | U32((l)))
#define MAKE_64(h, l) ((((I_64)(h)) << 32) | (l))
#if defined(__cplusplus)
#define HY_CFUNC "C"
#define HY_CDATA "C"
#else
#define HY_CFUNC
#define HY_CDATA
#endif

/**
 * Macros for tagging functions which read/write the vm thread.
 */

#define READSVMTHREAD
#define WRITESVMTHREAD
#define REQUIRESSTACKFRAME

/**
 * Macro for tagging functions, which never return.
 */

#if defined(__GNUC__)

/**
 * On GCC, we can actually pass this information on to the compiler.
 */

#define NORETURN __attribute__((noreturn))
#else
#define NORETURN
#endif

/**
 * On some systems va_list is an array type.  This is probably in
 * violation of the ANSI C spec, but it's not entirely clear.  Because of
 * this, we end up with an undesired extra level of indirection if we take
 * the address of a va_list argument. 
 *
 * To get it right, always use the VA_PTR macro
 */

#if !defined(VA_PTR)
#define VA_PTR(valist) (&valist)
#endif
#if !defined(TOC_UNWRAP_ADDRESS)
#define TOC_UNWRAP_ADDRESS(wrappedPointer) (wrappedPointer)
#endif

#if !defined(TOC_STORE_TOC)
#define TOC_STORE_TOC(dest,wrappedPointer)
#endif
/**
 * Macros for accessing I_64 values.
 */

#if defined(ATOMIC_LONG_ACCESS)
#define PTR_LONG_STORE(dstPtr, aLongPtr) ((*U32P(dstPtr) = *U32P(aLongPtr)), (*(U32P(dstPtr)+1) = *(U32P(aLongPtr)+1)))
#define PTR_LONG_VALUE(dstPtr, aLongPtr) ((*U32P(aLongPtr) = *U32P(dstPtr)), (*(U32P(aLongPtr)+1) = *(U32P(dstPtr)+1)))
#else
#define PTR_LONG_STORE(dstPtr, aLongPtr) (*(dstPtr) = *(aLongPtr))
#define PTR_LONG_VALUE(dstPtr, aLongPtr) (*(aLongPtr) = *(dstPtr))
#endif

/**
 * Macro used when declaring tables which require relocations.
 */

#if !defined(HYCONST_TABLE)
#define HYCONST_TABLE const
#endif

/**
 * ANSI qsort is not always available.
 */

#if !defined(HY_SORT)
#define HY_SORT(base, nmemb, size, compare) qsort((base), (nmemb), (size), (compare))
#endif

/**
 * Helper macros for storing/restoring pointers to jlong.
 */
#define jlong2addr(a, x) ((a *)((IDATA)(x)))
#define addr2jlong(x) ((jlong)((IDATA)(x)))

#endif /* hycomp_h */
