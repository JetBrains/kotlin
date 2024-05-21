/*
 *  Written by Joel Sherrill <joel@OARcorp.com>.
 *
 *  COPYRIGHT (c) 1989-2010.
 *  On-Line Applications Research Corporation (OAR).
 *
 *  Permission to use, copy, modify, and distribute this software for any
 *  purpose without fee is hereby granted, provided that this entire notice
 *  is included in all copies of any software which is or includes a copy
 *  or modification of this software.
 *
 *  THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 *  WARRANTY.  IN PARTICULAR,  THE AUTHOR MAKES NO REPRESENTATION
 *  OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY OF THIS
 *  SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 *
 *  $Id$
 */

#ifndef _SCHED_H_
#define _SCHED_H_

#include <sys/types.h>
#include <sys/sched.h>

#ifdef __cplusplus
extern "C" {
#endif

#if defined(_POSIX_PRIORITY_SCHEDULING)
/*
 *  XBD 13 - Set Scheduling Parameters, P1003.1b-2008, p. 1803
 */
int sched_setparam(
  pid_t                     __pid,
  const struct sched_param *__param
);

/*
 *  XBD 13 - Set Scheduling Parameters, P1003.1b-2008, p. 1800
 */
int sched_getparam(
  pid_t                     __pid,
  struct sched_param       *__param
);

/*
 *  XBD 13 - Set Scheduling Policy and Scheduling Parameters,
 *         P1003.1b-2008, p. 1805
 */
int sched_setscheduler(
  pid_t                     __pid,
  int                       __policy,
  const struct sched_param *__param
);

/*
 *  XBD 13 - Get Scheduling Policy, P1003.1b-2008, p. 1801
 */
int sched_getscheduler(
  pid_t                     __pid
);

/*
 *  XBD 13 - Get Scheduling Parameter Limits, P1003.1b-2008, p. 1799
 */
int sched_get_priority_max(
  int __policy
);

int sched_get_priority_min(
  int  __policy
);

/*
 *  XBD 13 - Get Scheduling Parameter Limits, P1003.1b-2008, p. 1802
 */
int sched_rr_get_interval(
  pid_t             __pid,
  struct timespec  *__interval
);
#endif /* _POSIX_PRIORITY_SCHEDULING */

#if defined(_POSIX_THREADS) || defined(_POSIX_PRIORITY_SCHEDULING)

/*
 *  XBD 13 - Yield Processor, P1003.1b-2008, p. 1807
 */
int sched_yield( void );

#endif /* _POSIX_THREADS or _POSIX_PRIORITY_SCHEDULING */

#if __GNU_VISIBLE
int sched_getcpu(void);

/* The following functions should only be declared if the type
   cpu_set_t is defined through indirect inclusion of sys/cpuset.h,
   only available on some targets. */
#ifdef _SYS_CPUSET_H_
int sched_getaffinity (pid_t, size_t, cpu_set_t *);
int sched_get_thread_affinity (void *, size_t, cpu_set_t *);
int sched_setaffinity (pid_t, size_t, const cpu_set_t *);
int sched_set_thread_affinity (void *, size_t, const cpu_set_t *);
#endif /* _SYS_CPUSET_H_ */

#endif

#ifdef __cplusplus
}
#endif

#endif /* _SCHED_H_ */
