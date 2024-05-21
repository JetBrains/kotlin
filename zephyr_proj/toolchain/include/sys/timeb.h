/* timeb.h -- An implementation of the standard Unix <sys/timeb.h> file.
   Written by Ian Lance Taylor <ian@cygnus.com>
   Public domain; no rights reserved.

   <sys/timeb.h> declares the structure used by the ftime function, as
   well as the ftime function itself.  Newlib does not provide an
   implementation of ftime.  */

#ifndef _SYS_TIMEB_H

#ifdef __cplusplus
extern "C" {
#endif

#define _SYS_TIMEB_H

#include <_ansi.h>
#include <sys/_types.h>

#if !defined(__time_t_defined) && !defined(_TIME_T_DECLARED)
typedef	_TIME_T_	time_t;
#define	__time_t_defined
#define	_TIME_T_DECLARED
#endif

struct timeb
{
  time_t time;
  unsigned short millitm;
  short timezone;
  short dstflag;
};

extern int ftime (struct timeb *);

#ifdef __cplusplus
}
#endif

#endif /* ! defined (_SYS_TIMEB_H) */
