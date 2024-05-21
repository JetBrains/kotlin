/*
 * libgen.h - defined by XPG4
 */

#ifndef _LIBGEN_H_
#define _LIBGEN_H_

#include "_ansi.h"
#include <sys/cdefs.h>
#include <sys/reent.h>

#ifdef __cplusplus
extern "C" {
#endif

/* There are two common basename variants.  If you do NOT #include <libgen.h>
   and you do

     #define _GNU_SOURCE
     #include <string.h>

   you get the GNU version.  Otherwise you get the POSIX versionfor which you
   should #include <libgen.h>i for the function prototype.  POSIX requires that
   #undef basename will still let you invoke the underlying function.  However,
   this also implies that the POSIX version is used in this case.  That's made
   sure here. */
#undef basename
#define basename __xpg_basename
char      *basename (char *) __asm__(__ASMNAME("basename"));
char      *dirname (char *);

#ifdef __cplusplus
}
#endif

#endif /* _LIBGEN_H_ */

