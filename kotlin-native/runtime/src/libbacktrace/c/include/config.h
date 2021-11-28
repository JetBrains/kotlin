/* config.h.  Generated from config.h.in by configure.  */
/* config.h.in.  Generated from configure.ac by autoheader.  */

/* Define to 1 if you have the __atomic functions */
#define HAVE_ATOMIC_FUNCTIONS 1

/* Define to 1 if you have the `clock_gettime' function. */
#define HAVE_CLOCK_GETTIME 1

/* Define to 1 if you have the declaration of `getpagesize', and to 0 if you
   don't. */
#define HAVE_DECL_GETPAGESIZE 1

/* Define to 1 if you have the declaration of `strnlen', and to 0 if you
   don't. */
#define HAVE_DECL_STRNLEN 1

/* Define to 1 if you have the <dlfcn.h> header file. */
#if __has_include(<dlfcn.h>)
#define HAVE_DLFCN_H 1
#endif


/* Define to 1 if you have the fcntl function */
#ifndef KONAN_WINDOWS
#define HAVE_FCNTL 1
#endif

/* Define if getexecname is available. */
/* #undef HAVE_GETEXECNAME */

/* Define if _Unwind_GetIPInfo is available. */
#define HAVE_GETIPINFO 1

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Define to 1 if you have KERN_PROC and KERN_PROC_PATHNAME in <sys/sysctl.h>.
   */
/* #undef HAVE_KERN_PROC */

/* Define to 1 if you have KERN_PROCARGS and KERN_PROC_PATHNAME in
   <sys/sysctl.h>. */
/* #undef HAVE_KERN_PROC_ARGS */

/* Define if -llzma is available. */
/* #undef HAVE_LIBLZMA 1 */

/* Define to 1 if you have the <link.h> header file. */
#if __has_include(<link.h>)
#define HAVE_LINK_H 1
/* Define if dl_iterate_phdr is available. */
#define HAVE_DL_ITERATE_PHDR 1
#endif

/* Define if AIX loadquery is available. */
/* #undef HAVE_LOADQUERY */

/* Define to 1 if you have the `lstat' function. */
#if __has_include(<sys/stat.h>)
#define HAVE_LSTAT 1
#endif

/* Define to 1 if you have the <mach-o/dyld.h> header file. */
#if __has_include(<mach-o/dyld.h>)
#define HAVE_MACH_O_DYLD_H 1
#endif

/* Define to 1 if you have the <memory.h> header file. */
#if __has_include(<memory.h>)
#define HAVE_MEMORY_H 1
#endif

/* Define to 1 if you have the `readlink' function. */
#ifndef KONAN_WINDOWS
#define HAVE_READLINK 1
#endif

/* Define to 1 if you have the <stdint.h> header file. */
#if __has_include(<stdint.h>)
#define HAVE_STDINT_H 1
#endif

/* Define to 1 if you have the <stdlib.h> header file. */
#if __has_include(<stdlib.h>)
#define HAVE_STDLIB_H 1
#endif

/* Define to 1 if you have the <strings.h> header file. */
#if __has_include(<strings.h>)
#define HAVE_STRINGS_H 1
#endif

/* Define to 1 if you have the <string.h> header file. */
#if __has_include(<string.h>)
#define HAVE_STRING_H 1
#endif

/* Define to 1 if you have the __sync functions */
#define HAVE_SYNC_FUNCTIONS 1

/* Define to 1 if you have the <sys/ldr.h> header file. */
#if __has_include(<sys/ldr.h>)
#define HAVE_SYS_LDR_H 1
#endif

/* Define to 1 if you have the <sys/mman.h> header file. */
#if __has_include(<sys/mman.h>)
#define HAVE_SYS_MMAN_H 1
#endif

/* Define to 1 if you have the <sys/stat.h> header file. */
#if __has_include(<sys/stat.h>)
#define HAVE_SYS_STAT_H 1
#endif

/* Define to 1 if you have the <sys/types.h> header file. */
#if __has_include(<sys/types.h>)
#define HAVE_SYS_TYPES_H 1
#endif

/* Define to 1 if you have the <unistd.h> header file. */
#if __has_include(<unistd.h>)
#define HAVE_UNISTD_H 1
#endif

/* Define if -lz is available. */
/* #undef HAVE_ZLIB */

/* Define to the sub-directory in which libtool stores uninstalled libraries.
   */
#define LT_OBJDIR ".libs/"

/* Define to the address where bug reports for this package should be sent. */
#define PACKAGE_BUGREPORT ""

/* Define to the full name of this package. */
#define PACKAGE_NAME "package-unused"

/* Define to the full name and version of this package. */
#define PACKAGE_STRING "package-unused version-unused"

/* Define to the one symbol short name of this package. */
#define PACKAGE_TARNAME "libbacktrace"

/* Define to the home page for this package. */
#define PACKAGE_URL ""

/* Define to the version of this package. */
#define PACKAGE_VERSION "version-unused"

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Enable extensions on AIX 3, Interix.  */
#ifndef _ALL_SOURCE
# define _ALL_SOURCE 1
#endif
/* Enable GNU extensions on systems that have them.  */
#ifndef _GNU_SOURCE
# define _GNU_SOURCE 1
#endif
/* Enable threading extensions on Solaris.  */
#ifndef _POSIX_PTHREAD_SEMANTICS
# define _POSIX_PTHREAD_SEMANTICS 1
#endif
/* Enable extensions on HP NonStop.  */
#ifndef _TANDEM_SOURCE
# define _TANDEM_SOURCE 1
#endif
/* Enable general extensions on Solaris.  */
#ifndef __EXTENSIONS__
# define __EXTENSIONS__ 1
#endif


/* Enable large inode numbers on Mac OS X 10.5.  */
#ifndef _DARWIN_USE_64_BIT_INODE
# define _DARWIN_USE_64_BIT_INODE 1
#endif

/* Number of bits in a file offset, on hosts where this is settable. */
#ifdef KONAN_WINDOWS
#define _FILE_OFFSET_BITS 64
#endif

/* Define for large files, on AIX-style hosts. */
/* #undef _LARGE_FILES */

/* Define to 1 if on MINIX. */
/* #undef _MINIX */

/* Define to 2 if the system does not provide POSIX.1 features except with
   this defined. */
/* #undef _POSIX_1_SOURCE */

/* Define to 1 if you need to in order for `stat' and other things to work. */
/* #undef _POSIX_SOURCE */
