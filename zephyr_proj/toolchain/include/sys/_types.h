/* ANSI C namespace clean utility typedefs */

/* This file defines various typedefs needed by the system calls that support
   the C library.  Basically, they're just the POSIX versions with an '_'
   prepended.  Targets shall use <machine/_types.h> to define their own
   internal types if desired.

   There are three define patterns used for type definitions.  Lets assume
   xyz_t is a user type.

   The internal type definition uses __machine_xyz_t_defined.  It is defined by
   <machine/_types.h> to disable a default definition in <sys/_types.h>. It
   must not be used in other files.

   User type definitions are guarded by __xyz_t_defined in glibc and
   _XYZ_T_DECLARED in BSD compatible systems.
*/

#ifndef	_SYS__TYPES_H
#define _SYS__TYPES_H

#define __need_size_t
#define __need_wint_t
#include <stddef.h>
#include <newlib.h>
#include <sys/config.h>
#include <machine/_types.h>

#ifndef __machine_blkcnt_t_defined
typedef long __blkcnt_t;
#endif

#ifndef __machine_blksize_t_defined
typedef long __blksize_t;
#endif

#ifndef __machine_fsblkcnt_t_defined
typedef __uint64_t __fsblkcnt_t;
#endif

#ifndef __machine_fsfilcnt_t_defined
typedef __uint32_t __fsfilcnt_t;
#endif

#ifndef __machine_off_t_defined
typedef long _off_t;
#endif

#if defined(__XMK__)
typedef signed char __pid_t;
#else
typedef int __pid_t;
#endif

#ifndef __machine_dev_t_defined
typedef short __dev_t;
#endif

#ifndef __machine_uid_t_defined
typedef unsigned short __uid_t;
#endif
#ifndef __machine_gid_t_defined
typedef unsigned short __gid_t;
#endif

#ifndef __machine_id_t_defined
typedef __uint32_t __id_t;
#endif

#ifndef __machine_ino_t_defined
#if (defined(__i386__) && (defined(GO32) || defined(__MSDOS__))) || \
    defined(__sparc__) || defined(__SPU__)
typedef unsigned long __ino_t;
#else
typedef unsigned short __ino_t;
#endif
#endif

#ifndef __machine_mode_t_defined
#if defined(__i386__) && (defined(GO32) || defined(__MSDOS__))
typedef int __mode_t;
#else
#if defined(__sparc__) && !defined(__sparc_v9__)
#ifdef __svr4__
typedef unsigned long __mode_t;
#else
typedef unsigned short __mode_t;
#endif
#else
typedef __uint32_t __mode_t;
#endif
#endif
#endif

#ifndef __machine_off64_t_defined
__extension__ typedef long long _off64_t;
#endif

#if defined(__CYGWIN__) && !defined(__LP64__)
typedef _off64_t __off_t;
#else
typedef _off_t __off_t;
#endif

typedef _off64_t __loff_t;

#ifndef __machine_key_t_defined
typedef long __key_t;
#endif

/*
 * We need fpos_t for the following, but it doesn't have a leading "_",
 * so we use _fpos_t instead.
 */
#ifndef __machine_fpos_t_defined
typedef long _fpos_t;		/* XXX must match off_t in <sys/types.h> */
				/* (and must be `long' for now) */
#endif

#ifdef __LARGE64_FILES
#ifndef __machine_fpos64_t_defined
typedef _off64_t _fpos64_t;
#endif
#endif

/* Defined by GCC provided <stddef.h> */
#undef __size_t

#ifndef __machine_size_t_defined
#ifdef __SIZE_TYPE__
typedef __SIZE_TYPE__ __size_t;
#else
#if defined(__INT_MAX__) && __INT_MAX__ == 2147483647
typedef unsigned int __size_t;
#else
typedef unsigned long __size_t;
#endif
#endif
#endif

#ifndef __machine_ssize_t_defined
#ifdef __SIZE_TYPE__
/* If __SIZE_TYPE__ is defined (gcc) we define ssize_t based on size_t.
   We simply change "unsigned" to "signed" for this single definition
   to make sure ssize_t and size_t only differ by their signedness. */
#define unsigned signed
typedef __SIZE_TYPE__ _ssize_t;
#undef unsigned
#else
#if defined(__INT_MAX__) && __INT_MAX__ == 2147483647
typedef int _ssize_t;
#else
typedef long _ssize_t;
#endif
#endif
#endif

typedef _ssize_t __ssize_t;

#ifndef __machine_mbstate_t_defined
/* Conversion state information.  */
typedef struct
{
  int __count;
  union
  {
    wint_t __wch;
    unsigned char __wchb[4];
  } __value;		/* Value so far.  */
} _mbstate_t;
#endif

#ifndef __machine_iconv_t_defined
/* Iconv descriptor type */
typedef void *_iconv_t;
#endif

#ifndef __machine_clock_t_defined
#define	_CLOCK_T_	unsigned long	/* clock() */
#endif

typedef	_CLOCK_T_	__clock_t;

#if defined(_USE_LONG_TIME_T) || __LONG_MAX__ > 0x7fffffffL
#define	_TIME_T_ long
#else
#define	_TIME_T_ __int_least64_t
#endif
typedef	_TIME_T_	__time_t;

#ifndef __machine_clockid_t_defined
#define	_CLOCKID_T_ 	unsigned long
#endif

typedef	_CLOCKID_T_	__clockid_t;

#define	_TIMER_T_	unsigned long
typedef	_TIMER_T_	__timer_t;

#ifndef __machine_sa_family_t_defined
typedef	__uint8_t	__sa_family_t;
#endif

#ifndef __machine_socklen_t_defined
typedef	__uint32_t	__socklen_t;
#endif

typedef	int		__nl_item;
typedef	unsigned short	__nlink_t;
typedef	long		__suseconds_t;	/* microseconds (signed) */
typedef	unsigned long	__useconds_t;	/* microseconds (unsigned) */

/*
 * Must be identical to the __GNUCLIKE_BUILTIN_VAALIST definition in
 * <sys/cdefs.h>.  The <sys/cdefs.h> must not be included here to avoid cyclic
 * header dependencies.
 */
#if __GNUC_MINOR__ > 95 || __GNUC__ >= 3
typedef	__builtin_va_list	__va_list;
#else
typedef	char *			__va_list;
#endif

#endif	/* _SYS__TYPES_H */
