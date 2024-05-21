#ifndef	_SYS_TIMES_H
#ifdef __cplusplus
extern "C" {
#endif
#define	_SYS_TIMES_H

#include <_ansi.h>
#include <sys/_types.h>

#if !defined(__clock_t_defined) && !defined(_CLOCK_T_DECLARED)
typedef	_CLOCK_T_	clock_t;
#define	__clock_t_defined
#define	_CLOCK_T_DECLARED
#endif

/*  Get Process Times, P1003.1b-1993, p. 92 */
struct tms {
	clock_t	tms_utime;		/* user time */
	clock_t	tms_stime;		/* system time */
	clock_t	tms_cutime;		/* user time, children */
	clock_t	tms_cstime;		/* system time, children */
};

clock_t times (struct tms *);
#ifdef _COMPILING_NEWLIB
clock_t _times (struct tms *);
#endif

#ifdef __cplusplus
}
#endif
#endif	/* !_SYS_TIMES_H */
