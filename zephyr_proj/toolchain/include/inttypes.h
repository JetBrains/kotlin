/*
 * Copyright (c) 2004, 2005 by
 * Ralf Corsepius, Ulm/Germany. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * is freely granted, provided that this notice is preserved.
 */

/**
 *  @file  inttypes.h
 */

#ifndef _INTTYPES_H
#define _INTTYPES_H

#include <newlib.h>
#include <sys/config.h>
#include <sys/_intsup.h>
#include "_ansi.h"
#include <stdint.h>
#define __need_wchar_t
#include <stddef.h>

#if __BSD_VISIBLE
#include <sys/_locale.h>
#endif

#define __STRINGIFY(a) #a

/* 8-bit types */
#define __PRI8(x) __INT8 __STRINGIFY(x)
#define __PRI8LEAST(x) __LEAST8 __STRINGIFY(x)
#define __PRI8FAST(x) __FAST8 __STRINGIFY(x)

/* NOTICE: scanning 8-bit types requires use of the hh specifier
 * which is only supported on newlib platforms that
 * are built with C99 I/O format support enabled.  If the flag in
 * newlib.h hasn't been set during configuration to indicate this, the 8-bit
 * scanning format macros are disabled here as they result in undefined
 * behaviour which can include memory overwrite.  Overriding the flag after the
 * library has been built is not recommended as it will expose the underlying
 * undefined behaviour.
 */

#if defined(_WANT_IO_C99_FORMATS)
  #define __SCN8(x) __INT8 __STRINGIFY(x)
	#define __SCN8LEAST(x) __LEAST8 __STRINGIFY(x)
	#define __SCN8FAST(x) __FAST8 __STRINGIFY(x)
#endif /* _WANT_IO_C99_FORMATS */


#define PRId8		__PRI8(d)
#define PRIi8		__PRI8(i)
#define PRIo8		__PRI8(o)
#define PRIu8		__PRI8(u)
#define PRIx8		__PRI8(x)
#define PRIX8		__PRI8(X)

/* Macros below are only enabled for a newlib built with C99 I/O format support. */
#if defined(_WANT_IO_C99_FORMATS)

#define SCNd8		__SCN8(d)
#define SCNi8		__SCN8(i)
#define SCNo8		__SCN8(o)
#define SCNu8		__SCN8(u)
#define SCNx8		__SCN8(x)

#endif /* _WANT_IO_C99_FORMATS */


#define PRIdLEAST8	__PRI8LEAST(d)
#define PRIiLEAST8	__PRI8LEAST(i)
#define PRIoLEAST8	__PRI8LEAST(o)
#define PRIuLEAST8	__PRI8LEAST(u)
#define PRIxLEAST8	__PRI8LEAST(x)
#define PRIXLEAST8	__PRI8LEAST(X)

/* Macros below are only enabled for a newlib built with C99 I/O format support. */
#if defined(_WANT_IO_C99_FORMATS)

  #define SCNdLEAST8	__SCN8LEAST(d)
  #define SCNiLEAST8	__SCN8LEAST(i)
  #define SCNoLEAST8	__SCN8LEAST(o)
  #define SCNuLEAST8	__SCN8LEAST(u)
  #define SCNxLEAST8	__SCN8LEAST(x)

#endif /* _WANT_IO_C99_FORMATS */

#define PRIdFAST8	__PRI8FAST(d)
#define PRIiFAST8	__PRI8FAST(i)
#define PRIoFAST8	__PRI8FAST(o)
#define PRIuFAST8	__PRI8FAST(u)
#define PRIxFAST8	__PRI8FAST(x)
#define PRIXFAST8	__PRI8FAST(X)

/* Macros below are only enabled for a newlib built with C99 I/O format support. */
#if defined(_WANT_IO_C99_FORMATS)

  #define SCNdFAST8	__SCN8FAST(d)
  #define SCNiFAST8	__SCN8FAST(i)
  #define SCNoFAST8	__SCN8FAST(o)
  #define SCNuFAST8	__SCN8FAST(u)
  #define SCNxFAST8	__SCN8FAST(x)

#endif /* _WANT_IO_C99_FORMATS */

/* 16-bit types */
#define __PRI16(x) __INT16 __STRINGIFY(x)
#define __PRI16LEAST(x) __LEAST16 __STRINGIFY(x)
#define __PRI16FAST(x) __FAST16 __STRINGIFY(x)
#define __SCN16(x) __INT16 __STRINGIFY(x)
#define __SCN16LEAST(x) __LEAST16 __STRINGIFY(x)
#define __SCN16FAST(x) __FAST16 __STRINGIFY(x)


#define PRId16		__PRI16(d)
#define PRIi16		__PRI16(i)
#define PRIo16		__PRI16(o)
#define PRIu16		__PRI16(u)
#define PRIx16		__PRI16(x)
#define PRIX16		__PRI16(X)

#define SCNd16		__SCN16(d)
#define SCNi16		__SCN16(i)
#define SCNo16		__SCN16(o)
#define SCNu16		__SCN16(u)
#define SCNx16		__SCN16(x)


#define PRIdLEAST16	__PRI16LEAST(d)
#define PRIiLEAST16	__PRI16LEAST(i)
#define PRIoLEAST16	__PRI16LEAST(o)
#define PRIuLEAST16	__PRI16LEAST(u)
#define PRIxLEAST16	__PRI16LEAST(x)
#define PRIXLEAST16	__PRI16LEAST(X)

#define SCNdLEAST16	__SCN16LEAST(d)
#define SCNiLEAST16	__SCN16LEAST(i)
#define SCNoLEAST16	__SCN16LEAST(o)
#define SCNuLEAST16	__SCN16LEAST(u)
#define SCNxLEAST16	__SCN16LEAST(x)


#define PRIdFAST16	__PRI16FAST(d)
#define PRIiFAST16	__PRI16FAST(i)
#define PRIoFAST16	__PRI16FAST(o)
#define PRIuFAST16	__PRI16FAST(u)
#define PRIxFAST16	__PRI16FAST(x)
#define PRIXFAST16	__PRI16FAST(X)

#define SCNdFAST16	__SCN16FAST(d)
#define SCNiFAST16	__SCN16FAST(i)
#define SCNoFAST16	__SCN16FAST(o)
#define SCNuFAST16	__SCN16FAST(u)
#define SCNxFAST16	__SCN16FAST(x)

/* 32-bit types */
#define __PRI32(x) __INT32 __STRINGIFY(x)
#define __SCN32(x) __INT32 __STRINGIFY(x)
#define __PRI32LEAST(x) __LEAST32 __STRINGIFY(x)
#define __SCN32LEAST(x) __LEAST32 __STRINGIFY(x)
#define __PRI32FAST(x) __FAST32 __STRINGIFY(x)
#define __SCN32FAST(x) __FAST32 __STRINGIFY(x)

#define PRId32		__PRI32(d)
#define PRIi32		__PRI32(i)
#define PRIo32		__PRI32(o)
#define PRIu32		__PRI32(u)
#define PRIx32		__PRI32(x)
#define PRIX32		__PRI32(X)

#define SCNd32		__SCN32(d)
#define SCNi32		__SCN32(i)
#define SCNo32		__SCN32(o)
#define SCNu32		__SCN32(u)
#define SCNx32		__SCN32(x)


#define PRIdLEAST32	__PRI32LEAST(d)
#define PRIiLEAST32	__PRI32LEAST(i)
#define PRIoLEAST32	__PRI32LEAST(o)
#define PRIuLEAST32	__PRI32LEAST(u)
#define PRIxLEAST32	__PRI32LEAST(x)
#define PRIXLEAST32	__PRI32LEAST(X)

#define SCNdLEAST32	__SCN32LEAST(d)
#define SCNiLEAST32	__SCN32LEAST(i)
#define SCNoLEAST32	__SCN32LEAST(o)
#define SCNuLEAST32	__SCN32LEAST(u)
#define SCNxLEAST32	__SCN32LEAST(x)


#define PRIdFAST32	__PRI32FAST(d)
#define PRIiFAST32	__PRI32FAST(i)
#define PRIoFAST32	__PRI32FAST(o)
#define PRIuFAST32	__PRI32FAST(u)
#define PRIxFAST32	__PRI32FAST(x)
#define PRIXFAST32	__PRI32FAST(X)

#define SCNdFAST32	__SCN32FAST(d)
#define SCNiFAST32	__SCN32FAST(i)
#define SCNoFAST32	__SCN32FAST(o)
#define SCNuFAST32	__SCN32FAST(u)
#define SCNxFAST32	__SCN32FAST(x)


/* 64-bit types */
#define __PRI64(x) __INT64 __STRINGIFY(x)
#define __SCN64(x) __INT64 __STRINGIFY(x)

#define __PRI64LEAST(x) __LEAST64 __STRINGIFY(x)
#define __SCN64LEAST(x) __LEAST64 __STRINGIFY(x)
#define __PRI64FAST(x) __FAST64 __STRINGIFY(x)
#define __SCN64FAST(x) __FAST64 __STRINGIFY(x)

#if __int64_t_defined
#define PRId64		__PRI64(d)
#define PRIi64		__PRI64(i)
#define PRIo64		__PRI64(o)
#define PRIu64		__PRI64(u)
#define PRIx64		__PRI64(x)
#define PRIX64		__PRI64(X)

#define SCNd64		__SCN64(d)
#define SCNi64		__SCN64(i)
#define SCNo64		__SCN64(o)
#define SCNu64		__SCN64(u)
#define SCNx64		__SCN64(x)
#endif

#if __int_least64_t_defined
#define PRIdLEAST64	__PRI64LEAST(d)
#define PRIiLEAST64	__PRI64LEAST(i)
#define PRIoLEAST64	__PRI64LEAST(o)
#define PRIuLEAST64	__PRI64LEAST(u)
#define PRIxLEAST64	__PRI64LEAST(x)
#define PRIXLEAST64	__PRI64LEAST(X)

#define SCNdLEAST64	__SCN64LEAST(d)
#define SCNiLEAST64	__SCN64LEAST(i)
#define SCNoLEAST64	__SCN64LEAST(o)
#define SCNuLEAST64	__SCN64LEAST(u)
#define SCNxLEAST64	__SCN64LEAST(x)
#endif

#if __int_fast64_t_defined
#define PRIdFAST64	__PRI64FAST(d)
#define PRIiFAST64	__PRI64FAST(i)
#define PRIoFAST64	__PRI64FAST(o)
#define PRIuFAST64	__PRI64FAST(u)
#define PRIxFAST64	__PRI64FAST(x)
#define PRIXFAST64	__PRI64FAST(X)

#define SCNdFAST64	__SCN64FAST(d)
#define SCNiFAST64	__SCN64FAST(i)
#define SCNoFAST64	__SCN64FAST(o)
#define SCNuFAST64	__SCN64FAST(u)
#define SCNxFAST64	__SCN64FAST(x)
#endif

/* max-bit types */
#if __have_long64
#define __PRIMAX(x) __STRINGIFY(l##x)
#define __SCNMAX(x) __STRINGIFY(l##x)
#elif __have_longlong64
#define __PRIMAX(x) __STRINGIFY(ll##x)
#define __SCNMAX(x) __STRINGIFY(ll##x)
#else
#define __PRIMAX(x) __STRINGIFY(x)
#define __SCNMAX(x) __STRINGIFY(x)
#endif

#define PRIdMAX		__PRIMAX(d)
#define PRIiMAX		__PRIMAX(i)
#define PRIoMAX		__PRIMAX(o)
#define PRIuMAX		__PRIMAX(u)
#define PRIxMAX		__PRIMAX(x)
#define PRIXMAX		__PRIMAX(X)

#define SCNdMAX		__SCNMAX(d)
#define SCNiMAX		__SCNMAX(i)
#define SCNoMAX		__SCNMAX(o)
#define SCNuMAX		__SCNMAX(u)
#define SCNxMAX		__SCNMAX(x)

/* ptr types */
#if defined (_INTPTR_EQ_LONGLONG)
# define __PRIPTR(x) __STRINGIFY(ll##x)
# define __SCNPTR(x) __STRINGIFY(ll##x)
#elif defined (_INTPTR_EQ_LONG)
# define __PRIPTR(x) __STRINGIFY(l##x)
# define __SCNPTR(x) __STRINGIFY(l##x)
#else
# define __PRIPTR(x) __STRINGIFY(x)
# define __SCNPTR(x) __STRINGIFY(x)
#endif

#define PRIdPTR		__PRIPTR(d)
#define PRIiPTR		__PRIPTR(i)
#define PRIoPTR		__PRIPTR(o)
#define PRIuPTR		__PRIPTR(u)
#define PRIxPTR		__PRIPTR(x)
#define PRIXPTR		__PRIPTR(X)

#define SCNdPTR		__SCNPTR(d)
#define SCNiPTR		__SCNPTR(i)
#define SCNoPTR		__SCNPTR(o)
#define SCNuPTR		__SCNPTR(u)
#define SCNxPTR		__SCNPTR(x)


typedef struct {
  intmax_t	quot;
  intmax_t	rem;
} imaxdiv_t;

struct _reent;

#ifdef __cplusplus
extern "C" {
#endif

extern intmax_t  imaxabs(intmax_t j);
extern imaxdiv_t imaxdiv(intmax_t numer, intmax_t denomer);
extern intmax_t  strtoimax(const char *__restrict, char **__restrict, int);
extern intmax_t  _strtoimax_r(struct _reent *, const char *__restrict, char **__restrict, int);
extern uintmax_t strtoumax(const char *__restrict, char **__restrict, int);
extern uintmax_t _strtoumax_r(struct _reent *, const char *__restrict, char **__restrict, int);
extern intmax_t  wcstoimax(const wchar_t *__restrict, wchar_t **__restrict, int);
extern intmax_t  _wcstoimax_r(struct _reent *, const wchar_t *__restrict, wchar_t **__restrict, int);
extern uintmax_t wcstoumax(const wchar_t *__restrict, wchar_t **__restrict, int);
extern uintmax_t _wcstoumax_r(struct _reent *, const wchar_t *__restrict, wchar_t **__restrict, int);

#if __BSD_VISIBLE
extern intmax_t  strtoimax_l(const char *__restrict, char **_restrict, int, locale_t);
extern uintmax_t strtoumax_l(const char *__restrict, char **_restrict, int, locale_t);
extern intmax_t  wcstoimax_l(const wchar_t *__restrict, wchar_t **_restrict, int, locale_t);
extern uintmax_t wcstoumax_l(const wchar_t *__restrict, wchar_t **_restrict, int, locale_t);
#endif

#ifdef __cplusplus
}
#endif

#endif
