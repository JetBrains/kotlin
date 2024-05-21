#ifndef _SYS_UNISTD_H
#define _SYS_UNISTD_H

#ifdef __cplusplus
extern "C" {
#endif

#include <_ansi.h>
#define __need_size_t
#define __need_ptrdiff_t
#include <sys/cdefs.h>
#include <sys/types.h>
#include <sys/_types.h>
#include <stddef.h>

extern char **environ;

void	_exit (int __status) _ATTRIBUTE ((__noreturn__));

int	access (const char *__path, int __amode);
unsigned  alarm (unsigned __secs);
int     chdir (const char *__path);
int     chmod (const char *__path, mode_t __mode);
#if !defined(__INSIDE_CYGWIN__)
int     chown (const char *__path, uid_t __owner, gid_t __group);
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE >= 4 && __POSIX_VISIBLE < 200112)
int     chroot (const char *__path);
#endif
int     close (int __fildes);
#if __POSIX_VISIBLE >= 199209
size_t	confstr (int __name, char *__buf, size_t __len);
#endif
#if __XSI_VISIBLE
char *  crypt (const char *__key, const char *__salt);
#endif
#if __XSI_VISIBLE && __XSI_VISIBLE < 700
char *  ctermid (char *__s);
#endif
#if __XSI_VISIBLE && __XSI_VISIBLE < 600
char *  cuserid (char *__s);
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE && __XSI_VISIBLE < 500)
int	daemon (int nochdir, int noclose);
#endif
int     dup (int __fildes);
int     dup2 (int __fildes, int __fildes2);
#if __GNU_VISIBLE
int     dup3 (int __fildes, int __fildes2, int flags);
int	eaccess (const char *__path, int __mode);
#endif
#if __XSI_VISIBLE
void	encrypt (char *__block, int __edflag);
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE && __XSI_VISIBLE < 500)
void	endusershell (void);
#endif
#if __GNU_VISIBLE
int	euidaccess (const char *__path, int __mode);
#endif
int     execl (const char *__path, const char *, ...);
int     execle (const char *__path, const char *, ...);
int     execlp (const char *__file, const char *, ...);
#if __MISC_VISIBLE
int     execlpe (const char *__file, const char *, ...);
#endif
int     execv (const char *__path, char * const __argv[]);
int     execve (const char *__path, char * const __argv[], char * const __envp[]);
int     execvp (const char *__file, char * const __argv[]);
#if __GNU_VISIBLE
int     execvpe (const char *__file, char * const __argv[], char * const __envp[]);
#endif
#if __ATFILE_VISIBLE
int	faccessat (int __dirfd, const char *__path, int __mode, int __flags);
#endif
#if __BSD_VISIBLE || __XSI_VISIBLE >= 4 || __POSIX_VISIBLE >= 200809
int     fchdir (int __fildes);
#endif
#if __POSIX_VISIBLE >= 199309
int     fchmod (int __fildes, mode_t __mode);
#endif
#if !defined(__INSIDE_CYGWIN__)
#if __BSD_VISIBLE || __XSI_VISIBLE >= 4 || __POSIX_VISIBLE >= 200809
int     fchown (int __fildes, uid_t __owner, gid_t __group);
#endif
#endif
#if __ATFILE_VISIBLE
int	fchownat (int __dirfd, const char *__path, uid_t __owner, gid_t __group, int __flags);
#endif
#if __POSIX_VISIBLE >= 200809
int	fexecve (int __fd, char * const __argv[], char * const __envp[]);
#endif
pid_t   fork (void);
long    fpathconf (int __fd, int __name);
int     fsync (int __fd);
#if __POSIX_VISIBLE >= 199309
int     fdatasync (int __fd);
#endif
#if __GNU_VISIBLE
char *  get_current_dir_name (void);
#endif
char *  getcwd (char *__buf, size_t __size);
#if __BSD_VISIBLE || (__XSI_VISIBLE && __XSI_VISIBLE < 500)
int	getdomainname  (char *__name, size_t __len);
#endif
#if __BSD_VISIBLE
int     getentropy (void *, size_t);
#endif
#if !defined(__INSIDE_CYGWIN__)
gid_t   getegid (void);
uid_t   geteuid (void);
gid_t   getgid (void);
#endif
int     getgroups (int __gidsetsize, gid_t __grouplist[]);
#if __BSD_VISIBLE || __XSI_VISIBLE >= 4
long    gethostid (void);
#endif
char *  getlogin (void);
#if defined(_POSIX_THREAD_SAFE_FUNCTIONS)
int getlogin_r (char *name, size_t namesize) ;
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE && __POSIX_VISIBLE < 200112)
char *  getpass (const char *__prompt);
int	getpagesize (void);
#endif
#if __BSD_VISIBLE
int    getpeereid (int, uid_t *, gid_t *);
#endif
#if __POSIX_VISIBLE >= 200809 || __XSI_VISIBLE >= 4
pid_t   getpgid (pid_t);
#endif
pid_t   getpgrp (void);
pid_t   getpid (void);
pid_t   getppid (void);
#if __POSIX_VISIBLE >= 200809 || __XSI_VISIBLE >= 4
pid_t   getsid (pid_t);
#endif
#if !defined(__INSIDE_CYGWIN__)
uid_t   getuid (void);
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE && __XSI_VISIBLE < 500)
char *	getusershell (void);
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE >= 4 && __POSIX_VISIBLE < 200809)
char *  getwd (char *__buf);
#endif
#if __BSD_VISIBLE
int	iruserok (unsigned long raddr, int superuser, const char *ruser, const char *luser);
#endif
int     isatty (int __fildes);
#if __BSD_VISIBLE
int        issetugid (void);
#endif
#if !defined(__INSIDE_CYGWIN__)
#if __BSD_VISIBLE || __XSI_VISIBLE >= 4 || __POSIX_VISIBLE >= 200809
int     lchown (const char *__path, uid_t __owner, gid_t __group);
#endif
#endif
int     link (const char *__path1, const char *__path2);
#if __ATFILE_VISIBLE
int     linkat (int __dirfd1, const char *__path1, int __dirfd2, const char *__path2, int __flags);
#endif
#if __MISC_VISIBLE || __XSI_VISIBLE
int	nice (int __nice_value);
#endif
#if !defined(__INSIDE_CYGWIN__)
off_t   lseek (int __fildes, off_t __offset, int __whence);
#endif
#if __MISC_VISIBLE || __XSI_VISIBLE >= 4
#define F_ULOCK	0
#define F_LOCK	1
#define F_TLOCK	2
#define F_TEST	3
int     lockf (int __fd, int __cmd, off_t __len);
#endif
long    pathconf (const char *__path, int __name);
int     pause (void);
#if __POSIX_VISIBLE >= 199506
int	pthread_atfork (void (*)(void), void (*)(void), void (*)(void));
#endif
int     pipe (int __fildes[2]);
#if __GNU_VISIBLE
int     pipe2 (int __fildes[2], int flags);
#endif
#if __POSIX_VISIBLE >= 200809 || __XSI_VISIBLE >= 500
ssize_t pread (int __fd, void *__buf, size_t __nbytes, off_t __offset);
ssize_t pwrite (int __fd, const void *__buf, size_t __nbytes, off_t __offset);
#endif
_READ_WRITE_RETURN_TYPE read (int __fd, void *__buf, size_t __nbyte);
#if __BSD_VISIBLE
int	rresvport (int *__alport);
int	revoke (char *__path);
#endif
int     rmdir (const char *__path);
#if __BSD_VISIBLE
int	ruserok (const char *rhost, int superuser, const char *ruser, const char *luser);
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE >= 4 && __POSIX_VISIBLE < 200112)
void *  sbrk (ptrdiff_t __incr);
#endif
#if !defined(__INSIDE_CYGWIN__)
#if __BSD_VISIBLE || __POSIX_VISIBLE >= 200112
int     setegid (gid_t __gid);
int     seteuid (uid_t __uid);
#endif
int     setgid (gid_t __gid);
#endif
#if __BSD_VISIBLE
int	setgroups (int ngroups, const gid_t *grouplist);
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE && __XSI_VISIBLE < 500)
int	sethostname (const char *, size_t);
#endif
int     setpgid (pid_t __pid, pid_t __pgid);
#if __SVID_VISIBLE || __XSI_VISIBLE >= 500
int     setpgrp (void);
#endif
#if (__BSD_VISIBLE || __XSI_VISIBLE >= 4) && !defined(__INSIDE_CYGWIN__)
int	setregid (gid_t __rgid, gid_t __egid);
int	setreuid (uid_t __ruid, uid_t __euid);
#endif
pid_t   setsid (void);
#if !defined(__INSIDE_CYGWIN__)
int     setuid (uid_t __uid);
#endif
#if __BSD_VISIBLE || (__XSI_VISIBLE && __XSI_VISIBLE < 500)
void	setusershell (void);
#endif
unsigned sleep (unsigned int __seconds);
#if __XSI_VISIBLE
void    swab (const void *__restrict, void *__restrict, ssize_t);
#endif
long    sysconf (int __name);
pid_t   tcgetpgrp (int __fildes);
int     tcsetpgrp (int __fildes, pid_t __pgrp_id);
char *  ttyname (int __fildes);
int     ttyname_r (int, char *, size_t);
int     unlink (const char *__path);
#if __XSI_VISIBLE >= 500 && __POSIX_VISIBLE < 200809 || __BSD_VISIBLE
int 	usleep (useconds_t __useconds);
#endif
#if __BSD_VISIBLE
int     vhangup (void);
#endif
_READ_WRITE_RETURN_TYPE write (int __fd, const void *__buf, size_t __nbyte);

#ifdef __CYGWIN__
# define __UNISTD_GETOPT__
# include <getopt.h>
# undef __UNISTD_GETOPT__
#else
extern char *optarg;			/* getopt(3) external variables */
extern int optind, opterr, optopt;
int	 getopt(int, char * const [], const char *);
extern int optreset;			/* getopt(3) external variable */
#endif

#if __BSD_VISIBLE || (__XSI_VISIBLE >= 4 && __POSIX_VISIBLE < 200809)
pid_t   vfork (void);
#endif

#ifdef _COMPILING_NEWLIB
/* Provide prototypes for most of the _<systemcall> names that are
   provided in newlib for some compilers.  */
int     _close (int __fildes);
pid_t   _fork (void);
pid_t   _getpid (void);
int	_isatty (int __fildes);
int     _link (const char *__path1, const char *__path2);
_off_t   _lseek (int __fildes, _off_t __offset, int __whence);
#ifdef __LARGE64_FILES
_off64_t _lseek64 (int __filedes, _off64_t __offset, int __whence);
#endif
_READ_WRITE_RETURN_TYPE _read (int __fd, void *__buf, size_t __nbyte);
void *  _sbrk (ptrdiff_t __incr);
int     _unlink (const char *__path);
_READ_WRITE_RETURN_TYPE _write (int __fd, const void *__buf, size_t __nbyte);
int     _execve (const char *__path, char * const __argv[], char * const __envp[]);
#endif

#if !defined(__INSIDE_CYGWIN__)
#if __POSIX_VISIBLE >= 200112 || __XSI_VISIBLE >= 500
int     ftruncate (int __fd, off_t __length);
#endif
#if __POSIX_VISIBLE >= 200809 || __XSI_VISIBLE >= 500
int     truncate (const char *, off_t __length);
#endif
#endif

#if __BSD_VISIBLE || __POSIX_VISIBLE < 200112
int	getdtablesize (void);
#endif
#if __BSD_VISIBLE || __POSIX_VISIBLE >= 200809 || __XSI_VISIBLE >= 500
useconds_t ualarm (useconds_t __useconds, useconds_t __interval);
#endif

#if __BSD_VISIBLE || __POSIX_VISIBLE >= 200112 || __XSI_VISIBLE >= 500
#if !(defined  (_WINSOCK_H) || defined (_WINSOCKAPI_) || defined (__USE_W32_SOCKETS))
/* winsock[2].h defines as __stdcall, and with int as 2nd arg */
 int	gethostname (char *__name, size_t __len);
#endif
#endif

#if __MISC_VISIBLE
int	setdtablesize (int);
#endif

#if __BSD_VISIBLE || __XSI_VISIBLE >= 500
void    sync (void);
#endif

#if __BSD_VISIBLE || __POSIX_VISIBLE >= 200112 || __XSI_VISIBLE >= 4
ssize_t readlink (const char *__restrict __path,
                          char *__restrict __buf, size_t __buflen);
int     symlink (const char *__name1, const char *__name2);
#endif
#if __ATFILE_VISIBLE
ssize_t        readlinkat (int __dirfd1, const char *__restrict __path,
                            char *__restrict __buf, size_t __buflen);
int	symlinkat (const char *, int, const char *);
int	unlinkat (int, const char *, int);
#endif

#define	F_OK	0
#define	R_OK	4
#define	W_OK	2
#define	X_OK	1

# define	SEEK_SET	0
# define	SEEK_CUR	1
# define	SEEK_END	2

#include <sys/features.h>

#define STDIN_FILENO    0       /* standard input file descriptor */
#define STDOUT_FILENO   1       /* standard output file descriptor */
#define STDERR_FILENO   2       /* standard error file descriptor */

/*
 *  sysconf values per IEEE Std 1003.1, 2008 Edition
 */

#define _SC_ARG_MAX                       0
#define _SC_CHILD_MAX                     1
#define _SC_CLK_TCK                       2
#define _SC_NGROUPS_MAX                   3
#define _SC_OPEN_MAX                      4
#define _SC_JOB_CONTROL                   5
#define _SC_SAVED_IDS                     6
#define _SC_VERSION                       7
#define _SC_PAGESIZE                      8
#define _SC_PAGE_SIZE                     _SC_PAGESIZE
/* These are non-POSIX values we accidentally introduced in 2000 without
   guarding them.  Keeping them unguarded for backward compatibility. */
#define _SC_NPROCESSORS_CONF              9
#define _SC_NPROCESSORS_ONLN             10
#define _SC_PHYS_PAGES                   11
#define _SC_AVPHYS_PAGES                 12
/* End of non-POSIX values. */
#define _SC_MQ_OPEN_MAX                  13
#define _SC_MQ_PRIO_MAX                  14
#define _SC_RTSIG_MAX                    15
#define _SC_SEM_NSEMS_MAX                16
#define _SC_SEM_VALUE_MAX                17
#define _SC_SIGQUEUE_MAX                 18
#define _SC_TIMER_MAX                    19
#define _SC_TZNAME_MAX                   20
#define _SC_ASYNCHRONOUS_IO              21
#define _SC_FSYNC                        22
#define _SC_MAPPED_FILES                 23
#define _SC_MEMLOCK                      24
#define _SC_MEMLOCK_RANGE                25
#define _SC_MEMORY_PROTECTION            26
#define _SC_MESSAGE_PASSING              27
#define _SC_PRIORITIZED_IO               28
#define _SC_REALTIME_SIGNALS             29
#define _SC_SEMAPHORES                   30
#define _SC_SHARED_MEMORY_OBJECTS        31
#define _SC_SYNCHRONIZED_IO              32
#define _SC_TIMERS                       33
#define _SC_AIO_LISTIO_MAX               34
#define _SC_AIO_MAX                      35
#define _SC_AIO_PRIO_DELTA_MAX           36
#define _SC_DELAYTIMER_MAX               37
#define _SC_THREAD_KEYS_MAX              38
#define _SC_THREAD_STACK_MIN             39
#define _SC_THREAD_THREADS_MAX           40
#define _SC_TTY_NAME_MAX                 41
#define _SC_THREADS                      42
#define _SC_THREAD_ATTR_STACKADDR        43
#define _SC_THREAD_ATTR_STACKSIZE        44
#define _SC_THREAD_PRIORITY_SCHEDULING   45
#define _SC_THREAD_PRIO_INHERIT          46
/* _SC_THREAD_PRIO_PROTECT was _SC_THREAD_PRIO_CEILING in early drafts */
#define _SC_THREAD_PRIO_PROTECT          47
#define _SC_THREAD_PRIO_CEILING          _SC_THREAD_PRIO_PROTECT
#define _SC_THREAD_PROCESS_SHARED        48
#define _SC_THREAD_SAFE_FUNCTIONS        49
#define _SC_GETGR_R_SIZE_MAX             50
#define _SC_GETPW_R_SIZE_MAX             51
#define _SC_LOGIN_NAME_MAX               52
#define _SC_THREAD_DESTRUCTOR_ITERATIONS 53
#define _SC_ADVISORY_INFO                54
#define _SC_ATEXIT_MAX                   55
#define _SC_BARRIERS                     56
#define _SC_BC_BASE_MAX                  57
#define _SC_BC_DIM_MAX                   58
#define _SC_BC_SCALE_MAX                 59
#define _SC_BC_STRING_MAX                60
#define _SC_CLOCK_SELECTION              61
#define _SC_COLL_WEIGHTS_MAX             62
#define _SC_CPUTIME                      63
#define _SC_EXPR_NEST_MAX                64
#define _SC_HOST_NAME_MAX                65
#define _SC_IOV_MAX                      66
#define _SC_IPV6                         67
#define _SC_LINE_MAX                     68
#define _SC_MONOTONIC_CLOCK              69
#define _SC_RAW_SOCKETS                  70
#define _SC_READER_WRITER_LOCKS          71
#define _SC_REGEXP                       72
#define _SC_RE_DUP_MAX                   73
#define _SC_SHELL                        74
#define _SC_SPAWN                        75
#define _SC_SPIN_LOCKS                   76
#define _SC_SPORADIC_SERVER              77
#define _SC_SS_REPL_MAX                  78
#define _SC_SYMLOOP_MAX                  79
#define _SC_THREAD_CPUTIME               80
#define _SC_THREAD_SPORADIC_SERVER       81
#define _SC_TIMEOUTS                     82
#define _SC_TRACE                        83
#define _SC_TRACE_EVENT_FILTER           84
#define _SC_TRACE_EVENT_NAME_MAX         85
#define _SC_TRACE_INHERIT                86
#define _SC_TRACE_LOG                    87
#define _SC_TRACE_NAME_MAX               88
#define _SC_TRACE_SYS_MAX                89
#define _SC_TRACE_USER_EVENT_MAX         90
#define _SC_TYPED_MEMORY_OBJECTS         91
#define _SC_V7_ILP32_OFF32               92
#define _SC_V6_ILP32_OFF32               _SC_V7_ILP32_OFF32
#define _SC_XBS5_ILP32_OFF32             _SC_V7_ILP32_OFF32
#define _SC_V7_ILP32_OFFBIG              93
#define _SC_V6_ILP32_OFFBIG              _SC_V7_ILP32_OFFBIG
#define _SC_XBS5_ILP32_OFFBIG            _SC_V7_ILP32_OFFBIG
#define _SC_V7_LP64_OFF64                94
#define _SC_V6_LP64_OFF64                _SC_V7_LP64_OFF64
#define _SC_XBS5_LP64_OFF64              _SC_V7_LP64_OFF64
#define _SC_V7_LPBIG_OFFBIG              95
#define _SC_V6_LPBIG_OFFBIG              _SC_V7_LPBIG_OFFBIG
#define _SC_XBS5_LPBIG_OFFBIG            _SC_V7_LPBIG_OFFBIG
#define _SC_XOPEN_CRYPT                  96
#define _SC_XOPEN_ENH_I18N               97
#define _SC_XOPEN_LEGACY                 98
#define _SC_XOPEN_REALTIME               99
#define _SC_STREAM_MAX                  100
#define _SC_PRIORITY_SCHEDULING         101
#define _SC_XOPEN_REALTIME_THREADS      102
#define _SC_XOPEN_SHM                   103
#define _SC_XOPEN_STREAMS               104
#define _SC_XOPEN_UNIX                  105
#define _SC_XOPEN_VERSION               106
#define _SC_2_CHAR_TERM                 107
#define _SC_2_C_BIND                    108
#define _SC_2_C_DEV                     109
#define _SC_2_FORT_DEV                  110
#define _SC_2_FORT_RUN                  111
#define _SC_2_LOCALEDEF                 112
#define _SC_2_PBS                       113
#define _SC_2_PBS_ACCOUNTING            114
#define _SC_2_PBS_CHECKPOINT            115
#define _SC_2_PBS_LOCATE                116
#define _SC_2_PBS_MESSAGE               117
#define _SC_2_PBS_TRACK                 118
#define _SC_2_SW_DEV                    119
#define _SC_2_UPE                       120
#define _SC_2_VERSION                   121
#define _SC_THREAD_ROBUST_PRIO_INHERIT  122
#define _SC_THREAD_ROBUST_PRIO_PROTECT  123
#define _SC_XOPEN_UUCP                  124
#define _SC_LEVEL1_ICACHE_SIZE          125
#define _SC_LEVEL1_ICACHE_ASSOC         126
#define _SC_LEVEL1_ICACHE_LINESIZE      127
#define _SC_LEVEL1_DCACHE_SIZE          128
#define _SC_LEVEL1_DCACHE_ASSOC         129
#define _SC_LEVEL1_DCACHE_LINESIZE      130
#define _SC_LEVEL2_CACHE_SIZE           131
#define _SC_LEVEL2_CACHE_ASSOC          132
#define _SC_LEVEL2_CACHE_LINESIZE       133
#define _SC_LEVEL3_CACHE_SIZE           134
#define _SC_LEVEL3_CACHE_ASSOC          135
#define _SC_LEVEL3_CACHE_LINESIZE       136
#define _SC_LEVEL4_CACHE_SIZE           137
#define _SC_LEVEL4_CACHE_ASSOC          138
#define _SC_LEVEL4_CACHE_LINESIZE       139
#define _SC_POSIX_26_VERSION            140

/*
 *  pathconf values per IEEE Std 1003.1, 2008 Edition
 */

#define _PC_LINK_MAX                      0
#define _PC_MAX_CANON                     1
#define _PC_MAX_INPUT                     2
#define _PC_NAME_MAX                      3
#define _PC_PATH_MAX                      4
#define _PC_PIPE_BUF                      5
#define _PC_CHOWN_RESTRICTED              6
#define _PC_NO_TRUNC                      7
#define _PC_VDISABLE                      8
#define _PC_ASYNC_IO                      9
#define _PC_PRIO_IO                      10
#define _PC_SYNC_IO                      11
#define _PC_FILESIZEBITS                 12
#define _PC_2_SYMLINKS                   13
#define _PC_SYMLINK_MAX                  14
#define _PC_ALLOC_SIZE_MIN               15
#define _PC_REC_INCR_XFER_SIZE           16
#define _PC_REC_MAX_XFER_SIZE            17
#define _PC_REC_MIN_XFER_SIZE            18
#define _PC_REC_XFER_ALIGN               19
#define _PC_TIMESTAMP_RESOLUTION         20
#ifdef __CYGWIN__
/* Ask for POSIX permission bits support. */
#define _PC_POSIX_PERMISSIONS            90
/* Ask for full POSIX permission support including uid/gid settings. */
#define _PC_POSIX_SECURITY               91
#define _PC_CASE_INSENSITIVE             92
#endif

/*
 *  confstr values per IEEE Std 1003.1, 2004 Edition
 */

#ifdef __CYGWIN__	/* Only defined on Cygwin for now. */
#define _CS_PATH                               0
#define _CS_POSIX_V7_ILP32_OFF32_CFLAGS        1
#define _CS_POSIX_V6_ILP32_OFF32_CFLAGS       _CS_POSIX_V7_ILP32_OFF32_CFLAGS
#define _CS_XBS5_ILP32_OFF32_CFLAGS           _CS_POSIX_V7_ILP32_OFF32_CFLAGS
#define _CS_POSIX_V7_ILP32_OFF32_LDFLAGS       2
#define _CS_POSIX_V6_ILP32_OFF32_LDFLAGS      _CS_POSIX_V7_ILP32_OFF32_LDFLAGS
#define _CS_XBS5_ILP32_OFF32_LDFLAGS          _CS_POSIX_V7_ILP32_OFF32_LDFLAGS
#define _CS_POSIX_V7_ILP32_OFF32_LIBS          3
#define _CS_POSIX_V6_ILP32_OFF32_LIBS         _CS_POSIX_V7_ILP32_OFF32_LIBS
#define _CS_XBS5_ILP32_OFF32_LIBS             _CS_POSIX_V7_ILP32_OFF32_LIBS
#define _CS_XBS5_ILP32_OFF32_LINTFLAGS         4
#define _CS_POSIX_V7_ILP32_OFFBIG_CFLAGS       5
#define _CS_POSIX_V6_ILP32_OFFBIG_CFLAGS      _CS_POSIX_V7_ILP32_OFFBIG_CFLAGS
#define _CS_XBS5_ILP32_OFFBIG_CFLAGS          _CS_POSIX_V7_ILP32_OFFBIG_CFLAGS
#define _CS_POSIX_V7_ILP32_OFFBIG_LDFLAGS      6
#define _CS_POSIX_V6_ILP32_OFFBIG_LDFLAGS     _CS_POSIX_V7_ILP32_OFFBIG_LDFLAGS
#define _CS_XBS5_ILP32_OFFBIG_LDFLAGS         _CS_POSIX_V7_ILP32_OFFBIG_LDFLAGS
#define _CS_POSIX_V7_ILP32_OFFBIG_LIBS         7
#define _CS_POSIX_V6_ILP32_OFFBIG_LIBS        _CS_POSIX_V7_ILP32_OFFBIG_LIBS
#define _CS_XBS5_ILP32_OFFBIG_LIBS            _CS_POSIX_V7_ILP32_OFFBIG_LIBS
#define _CS_XBS5_ILP32_OFFBIG_LINTFLAGS        8
#define _CS_POSIX_V7_LP64_OFF64_CFLAGS         9
#define _CS_POSIX_V6_LP64_OFF64_CFLAGS        _CS_POSIX_V7_LP64_OFF64_CFLAGS
#define _CS_XBS5_LP64_OFF64_CFLAGS            _CS_POSIX_V7_LP64_OFF64_CFLAGS
#define _CS_POSIX_V7_LP64_OFF64_LDFLAGS       10
#define _CS_POSIX_V6_LP64_OFF64_LDFLAGS       _CS_POSIX_V7_LP64_OFF64_LDFLAGS
#define _CS_XBS5_LP64_OFF64_LDFLAGS           _CS_POSIX_V7_LP64_OFF64_LDFLAGS
#define _CS_POSIX_V7_LP64_OFF64_LIBS          11
#define _CS_POSIX_V6_LP64_OFF64_LIBS          _CS_POSIX_V7_LP64_OFF64_LIBS
#define _CS_XBS5_LP64_OFF64_LIBS              _CS_POSIX_V7_LP64_OFF64_LIBS
#define _CS_XBS5_LP64_OFF64_LINTFLAGS         12
#define _CS_POSIX_V7_LPBIG_OFFBIG_CFLAGS      13
#define _CS_POSIX_V6_LPBIG_OFFBIG_CFLAGS      _CS_POSIX_V7_LPBIG_OFFBIG_CFLAGS
#define _CS_XBS5_LPBIG_OFFBIG_CFLAGS          _CS_POSIX_V7_LPBIG_OFFBIG_CFLAGS
#define _CS_POSIX_V7_LPBIG_OFFBIG_LDFLAGS     14
#define _CS_POSIX_V6_LPBIG_OFFBIG_LDFLAGS     _CS_POSIX_V7_LPBIG_OFFBIG_LDFLAGS
#define _CS_XBS5_LPBIG_OFFBIG_LDFLAGS         _CS_POSIX_V7_LPBIG_OFFBIG_LDFLAGS
#define _CS_POSIX_V7_LPBIG_OFFBIG_LIBS        15
#define _CS_POSIX_V6_LPBIG_OFFBIG_LIBS        _CS_POSIX_V7_LPBIG_OFFBIG_LIBS
#define _CS_XBS5_LPBIG_OFFBIG_LIBS            _CS_POSIX_V7_LPBIG_OFFBIG_LIBS
#define _CS_XBS5_LPBIG_OFFBIG_LINTFLAGS       16
#define _CS_POSIX_V7_WIDTH_RESTRICTED_ENVS    17
#define _CS_POSIX_V6_WIDTH_RESTRICTED_ENVS    _CS_POSIX_V7_WIDTH_RESTRICTED_ENVS
#define _CS_XBS5_WIDTH_RESTRICTED_ENVS        _CS_POSIX_V7_WIDTH_RESTRICTED_ENVS
#define _CS_POSIX_V7_THREADS_CFLAGS           18
#define _CS_POSIX_V7_THREADS_LDFLAGS          19
#define _CS_V7_ENV                            20
#define _CS_V6_ENV                            _CS_V7_ENV
#define _CS_LFS_CFLAGS                        21
#define _CS_LFS_LDFLAGS                       22
#define _CS_LFS_LIBS                          23
#define _CS_LFS_LINTFLAGS                     24
#endif

#ifdef __cplusplus
}
#endif

#if __SSP_FORTIFY_LEVEL > 0
#include <ssp/unistd.h>
#endif

#endif /* _SYS_UNISTD_H */
