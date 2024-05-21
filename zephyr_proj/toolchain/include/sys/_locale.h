/* Definition of opaque POSIX-1.2008 type locale_t for userspace. */

#ifndef	_SYS__LOCALE_H
#define _SYS__LOCALE_H

#include <newlib.h>
#include <sys/config.h>

struct __locale_t;
typedef struct __locale_t *locale_t;

#endif	/* _SYS__LOCALE_H */
