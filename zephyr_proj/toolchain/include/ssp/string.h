/*	$NetBSD: string.h,v 1.13 2014/11/29 13:23:48 pooka Exp $	*/

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
#ifndef _SSP_STRING_H_
#define _SSP_STRING_H_

#include <sys/cdefs.h>
#include <ssp/ssp.h>

__BEGIN_DECLS
void *__memcpy_chk(void *, const void *, size_t, size_t);
void *__memmove_chk(void *, void *, size_t, size_t);
void *__mempcpy_chk(void *, const void *, size_t, size_t);
void *__memset_chk(void *, int, size_t, size_t);
char *__stpcpy_chk(char *, const char *, size_t);
char *__strcat_chk(char *, const char *, size_t);
char *__strcpy_chk(char *, const char *, size_t);
char *__strncat_chk(char *, const char *, size_t, size_t);
char *__strncpy_chk(char *, const char *, size_t, size_t);
__END_DECLS

#if __SSP_FORTIFY_LEVEL > 0

#define __ssp_bos_check3(fun, dst, src, len) \
    ((__ssp_bos0(dst) != (size_t)-1) ? \
    __builtin___ ## fun ## _chk(dst, src, len, __ssp_bos0(dst)) : \
    __ ## fun ## _ichk(dst, src, len))

#define __ssp_bos_check2(fun, dst, src) \
    ((__ssp_bos0(dst) != (size_t)-1) ? \
    __builtin___ ## fun ## _chk(dst, src, __ssp_bos0(dst)) : \
    __ ## fun ## _ichk(dst, src))

#define __ssp_bos_icheck3_restrict(fun, type1, type2) \
__ssp_inline type1 __ ## fun ## _ichk(type1 __restrict, type2 __restrict, size_t); \
__ssp_inline type1 \
__ ## fun ## _ichk(type1 __restrict dst, type2 __restrict src, size_t len) { \
	return __builtin___ ## fun ## _chk(dst, src, len, __ssp_bos0(dst)); \
}

#define __ssp_bos_icheck3(fun, type1, type2) \
__ssp_inline type1 __ ## fun ## _ichk(type1, type2, size_t); \
__ssp_inline type1 \
__ ## fun ## _ichk(type1 dst, type2 src, size_t len) { \
	return __builtin___ ## fun ## _chk(dst, src, len, __ssp_bos0(dst)); \
}

#define __ssp_bos_icheck2_restrict(fun, type1, type2) \
__ssp_inline type1 __ ## fun ## _ichk(type1, type2); \
__ssp_inline type1 \
__ ## fun ## _ichk(type1 __restrict dst, type2 __restrict src) { \
	return __builtin___ ## fun ## _chk(dst, src, __ssp_bos0(dst)); \
}

__BEGIN_DECLS
__ssp_bos_icheck3_restrict(memcpy, void *, const void *)
__ssp_bos_icheck3(memmove, void *, const void *)
__ssp_bos_icheck3_restrict(mempcpy, void *, const void *)
__ssp_bos_icheck3(memset, void *, int)
__ssp_bos_icheck2_restrict(stpcpy, char *, const char *)
#if __GNUC_PREREQ__(4,8) || defined(__clang__)
__ssp_bos_icheck3_restrict(stpncpy, char *, const char *)
#endif
__ssp_bos_icheck2_restrict(strcpy, char *, const char *)
__ssp_bos_icheck2_restrict(strcat, char *, const char *)
__ssp_bos_icheck3_restrict(strncpy, char *, const char *)
__ssp_bos_icheck3_restrict(strncat, char *, const char *)
__END_DECLS

#define memcpy(dst, src, len) __ssp_bos_check3(memcpy, dst, src, len)
#define memmove(dst, src, len) __ssp_bos_check3(memmove, dst, src, len)
#if __GNU_VISIBLE
#define mempcpy(dst, src, len) __ssp_bos_check3(mempcpy, dst, src, len)
#endif
#define memset(dst, val, len) __ssp_bos_check3(memset, dst, val, len)
#if __POSIX_VISIBLE >= 200809
#define stpcpy(dst, src) __ssp_bos_check2(stpcpy, dst, src)
#if __GNUC_PREREQ__(4,8) || defined(__clang__)
#define stpncpy(dst, src, len) __ssp_bos_check3(stpncpy, dst, src, len)
#endif
#endif
#define strcpy(dst, src) __ssp_bos_check2(strcpy, dst, src)
#define strcat(dst, src) __ssp_bos_check2(strcat, dst, src)
#define strncpy(dst, src, len) __ssp_bos_check3(strncpy, dst, src, len)
#define strncat(dst, src, len) __ssp_bos_check3(strncat, dst, src, len)

#endif /* __SSP_FORTIFY_LEVEL > 0 */
#endif /* _SSP_STRING_H_ */
