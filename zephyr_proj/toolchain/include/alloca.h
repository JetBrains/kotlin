/* libc/include/alloca.h - Allocate memory on stack */

/* Written 2000 by Werner Almesberger */
/* Rearranged for general inclusion by stdlib.h.
   2001, Corinna Vinschen <vinschen@redhat.com> */

#ifndef _NEWLIB_ALLOCA_H
#define _NEWLIB_ALLOCA_H

#include "_ansi.h"
#include <sys/reent.h>

#undef alloca

#ifdef __GNUC__
#define alloca(size) __builtin_alloca(size)
#else
void * alloca (size_t);
#endif

#endif
