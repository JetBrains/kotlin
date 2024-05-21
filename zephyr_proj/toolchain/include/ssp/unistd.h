/*	$NetBSD: unistd.h,v 1.7 2015/06/25 18:41:03 joerg Exp $	*/

/*-
 * Copyright (c) 2006 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Christos Zoulas.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
#ifndef _SSP_UNISTD_H_
#define _SSP_UNISTD_H_

#include <ssp/ssp.h>

#if __SSP_FORTIFY_LEVEL > 0
__BEGIN_DECLS

#if __POSIX_VISIBLE >= 199209
__ssp_redirect(size_t, confstr, (int __name, char *__buf, size_t __len), \
    (__name, __buf, __len));
#endif

__ssp_redirect_raw(char *, getcwd, (char *__buf, size_t __len),
    (__buf, __len), __buf != 0, __ssp_bos);

#if __BSD_VISIBLE || (__XSI_VISIBLE && __XSI_VISIBLE < 500)
__ssp_redirect(int, getdomainname, (char *__buf, size_t __len), \
    (__buf, __len));
#endif

__ssp_decl(int, getgroups, (int __n, gid_t __buf[]))
{
  __ssp_check(__buf, __n * sizeof(gid_t), __ssp_bos);
  return __ssp_real_getgroups (__n, __buf);
}

#if __BSD_VISIBLE || __POSIX_VISIBLE >= 200112 || __XSI_VISIBLE >= 500
#if !(defined  (_WINSOCK_H) || defined (_WINSOCKAPI_) || defined (__USE_W32_SOCKETS))
__ssp_redirect(int, gethostname, (char *__buf, size_t __len), \
    (__buf, __len));
#endif
#endif

__ssp_redirect(int, getlogin_r, (char *__buf, size_t __len), \
    (__buf, __len));

#if __POSIX_VISIBLE >= 200809 || __XSI_VISIBLE >= 500
__ssp_redirect0(ssize_t, pread, (int __fd, void *__buf, size_t __len, off_t __off), \
    (__fd, __buf, __len, __off));
#endif

__ssp_redirect0(_READ_WRITE_RETURN_TYPE, read, \
    (int __fd, void *__buf, size_t __len), (__fd, __buf, __len));

#if __BSD_VISIBLE || __POSIX_VISIBLE >= 200112 || __XSI_VISIBLE >= 4
__ssp_redirect(ssize_t, readlink, (const char *__restrict __path, \
    char *__restrict __buf, size_t __len), (__path, __buf, __len));
#endif

#if __ATFILE_VISIBLE
__ssp_redirect(ssize_t, readlinkat, \
    (int __dirfd1, const char *__restrict __path, char *__restrict __buf, size_t __len), \
    (__dirfd1, __path, __buf, __len));
#endif

__ssp_redirect(int, ttyname_r, (int __fd, char *__buf, size_t __len), \
    (__fd, __buf, __len));

__END_DECLS

#endif /* __SSP_FORTIFY_LEVEL > 0 */
#endif /* _SSP_UNISTD_H_ */
