/*-
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * Copyright (c) 1989, 1993
 *	The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 *	@(#)dirent.h	8.3 (Berkeley) 8/10/94
 * $FreeBSD: head/include/dirent.h 326024 2017-11-20 19:45:28Z pfg $
 */

#ifndef _DIRENT_H_
#define	_DIRENT_H_

#include <sys/cdefs.h>
#include <sys/dirent.h>

#if !defined(MAXNAMLEN) && __BSD_VISIBLE
#define MAXNAMLEN 1024
#endif

__BEGIN_DECLS
#if __MISC_VISIBLE || __POSIX_VISIBLE >= 200809 || __XSI_VISIBLE >= 700
int	 alphasort(const struct dirent **, const struct dirent **);
int	 dirfd(DIR *);
#endif
#if __BSD_VISIBLE
int	 fdclosedir(DIR *);
#endif
DIR	*opendir(const char *);
DIR	*fdopendir(int);
struct dirent *
	 readdir(DIR *);
#if __POSIX_VISIBLE >= 199506 || __XSI_VISIBLE >= 500
int	 readdir_r(DIR *__restrict, struct dirent *__restrict,
	    struct dirent **__restrict);
#endif
void	 rewinddir(DIR *);
#if __MISC_VISIBLE || __POSIX_VISIBLE >= 200809 || __XSI_VISIBLE >= 700
int	 scandir(const char *, struct dirent ***,
	    int (*)(const struct dirent *), int (*)(const struct dirent **,
	    const struct dirent **));
#endif
#ifdef _COMPILING_NEWLIB
void	 _seekdir(DIR *, long);
#endif
#if __MISC_VISIBLE || __XSI_VISIBLE
#ifndef __INSIDE_CYGWIN__
void	 seekdir(DIR *, long);
long	 telldir(DIR *);
#endif
#endif
int	 closedir(DIR *);
#if __GNU_VISIBLE
int	 scandirat(int, const char *, struct dirent ***,
	    int (*) (const struct dirent *), int (*) (const struct dirent **,
	    const struct dirent **));
int	 versionsort(const struct dirent **, const struct dirent **);
#endif
__END_DECLS

#endif /*_DIRENT_H_*/
