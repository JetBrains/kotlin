/* http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/tgmath.h.html */
/*-
 * Copyright (c) 2004 Stefan Farfeleder.
 * All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * $FreeBSD$
 */

#ifndef _TGMATH_H_
#define	_TGMATH_H_

#include <complex.h>
#include <math.h>

#ifdef log2
#undef log2
#endif

/*
 * This implementation of <tgmath.h> requires two implementation-dependent
 * macros to be defined:
 * __tg_impl_simple(x, y, z, fn, fnf, fnl, ...)
 *	Invokes fnl() if the corresponding real type of x, y or z is long
 *	double, fn() if it is double or any has an integer type, and fnf()
 *	otherwise.
 * __tg_impl_full(x, y, z, fn, fnf, fnl, cfn, cfnf, cfnl, ...)
 *	Invokes [c]fnl() if the corresponding real type of x, y or z is long
 *	double, [c]fn() if it is double or any has an integer type, and
 *	[c]fnf() otherwise.  The function with the 'c' prefix is called if
 *	any of x, y or z is a complex number.
 * Both macros call the chosen function with all additional arguments passed
 * to them, as given by __VA_ARGS__.
 *
 * Note that these macros cannot be implemented with C's ?: operator,
 * because the return type of the whole expression would incorrectly be long
 * double complex regardless of the argument types.
 */

/* requires GCC >= 3.1 */
#if !__GNUC_PREREQ (3, 1)
#error "<tgmath.h> not implemented for this compiler"
#endif

#define	__tg_type(__e, __t)						\
	__builtin_types_compatible_p(__typeof__(__e), __t)
#define	__tg_type3(__e1, __e2, __e3, __t)				\
	(__tg_type(__e1, __t) || __tg_type(__e2, __t) || 		\
	 __tg_type(__e3, __t))
#define	__tg_type_corr(__e1, __e2, __e3, __t)				\
	(__tg_type3(__e1, __e2, __e3, __t) || 				\
	 __tg_type3(__e1, __e2, __e3, __t _Complex))
#define	__tg_integer(__e1, __e2, __e3)					\
	(((__typeof__(__e1))1.5 == 1) || ((__typeof__(__e2))1.5 == 1) ||	\
	 ((__typeof__(__e3))1.5 == 1))
#define	__tg_is_complex(__e1, __e2, __e3)				\
	(__tg_type3(__e1, __e2, __e3, float _Complex) ||		\
	 __tg_type3(__e1, __e2, __e3, double _Complex) ||		\
	 __tg_type3(__e1, __e2, __e3, long double _Complex) ||		\
	 __tg_type3(__e1, __e2, __e3, __typeof__(_Complex_I)))

#if defined (_LDBL_EQ_DBL) || defined (__CYGWIN__)
#define	__tg_impl_simple(x, y, z, fn, fnf, fnl, ...)			\
	__builtin_choose_expr(__tg_type_corr(x, y, z, long double),	\
	    fnl(__VA_ARGS__), __builtin_choose_expr(			\
		__tg_type_corr(x, y, z, double) || __tg_integer(x, y, z),\
		fn(__VA_ARGS__), fnf(__VA_ARGS__)))
#else
#define	__tg_impl_simple(__x, __y, __z, __fn, __fnf, __fnl, ...)	\
	(__tg_type_corr(__x, __y, __z, double) || __tg_integer(__x, __y, __z)) \
		? __fn(__VA_ARGS__) : __fnf(__VA_ARGS__)
#endif

#define	__tg_impl_full(__x, __y, __z, __fn, __fnf, __fnl, __cfn, __cfnf, __cfnl, ...)	\
	__builtin_choose_expr(__tg_is_complex(__x, __y, __z),		\
	    __tg_impl_simple(__x, __y, __z, __cfn, __cfnf, __cfnl, __VA_ARGS__),	\
	    __tg_impl_simple(__x, __y, __z, __fn, __fnf, __fnl, __VA_ARGS__))

/* Macros to save lots of repetition below */
#define	__tg_simple(__x, __fn)						\
	__tg_impl_simple(__x, __x, __x, __fn, __fn##f, __fn##l, __x)
#define	__tg_simple2(__x, __y, __fn)					\
	__tg_impl_simple(__x, __x, __y, __fn, __fn##f, __fn##l, __x, __y)
#define	__tg_simplev(__x, __fn, ...)					\
	__tg_impl_simple(__x, __x, __x, __fn, __fn##f, __fn##l, __VA_ARGS__)
#define	__tg_full(__x, __fn)						\
	__tg_impl_full(__x, __x, __x, __fn, __fn##f, __fn##l, c##__fn, c##__fn##f, c##__fn##l, __x)

/* 7.22#4 -- These macros expand to real or complex functions, depending on
 * the type of their arguments. */
#define	acos(__x)		__tg_full(__x, acos)
#define	asin(__x)		__tg_full(__x, asin)
#define	atan(__x)		__tg_full(__x, atan)
#define	acosh(__x)		__tg_full(__x, acosh)
#define	asinh(__x)		__tg_full(__x, asinh)
#define	atanh(__x)		__tg_full(__x, atanh)
#define	cos(__x)		__tg_full(__x, cos)
#define	sin(__x)		__tg_full(__x, sin)
#define	tan(__x)		__tg_full(__x, tan)
#define	cosh(__x)		__tg_full(__x, cosh)
#define	sinh(__x)		__tg_full(__x, sinh)
#define	tanh(__x)		__tg_full(__x, tanh)
#define	exp(__x)		__tg_full(__x, exp)
#define	log(__x)		__tg_full(__x, log)
#define	pow(__x, __y)		__tg_impl_full(__x, __x, __y, pow, powf, powl,	\
						cpow, cpowf, cpowl, __x, __y)
#define	sqrt(__x)		__tg_full(__x, sqrt)

/* "The corresponding type-generic macro for fabs and cabs is fabs." */
#define	fabs(__x)		__tg_impl_full(__x, __x, __x, fabs, fabsf, fabsl,	\
						cabs, cabsf, cabsl, __x)

/* 7.22#5 -- These macros are only defined for arguments with real type. */
#define	atan2(__x, __y)		__tg_simple2(__x, __y, atan2)
#define	cbrt(__x)		__tg_simple(__x, cbrt)
#define	ceil(__x)		__tg_simple(__x, ceil)
#define	copysign(__x, __y)	__tg_simple2(__x, __y, copysign)
#define	erf(__x)		__tg_simple(__x, erf)
#define	erfc(__x)		__tg_simple(__x, erfc)
#define	exp2(__x)		__tg_simple(__x, exp2)
#define	expm1(__x)		__tg_simple(__x, expm1)
#define	fdim(__x, __y)		__tg_simple2(__x, __y, fdim)
#define	floor(__x)		__tg_simple(__x, floor)
#define	fma(__x, __y, __z)	__tg_impl_simple(__x, __y, __z, fma, fmaf, fmal, \
						 __x, __y, __z)
#define	fmax(__x, __y)		__tg_simple2(__x, __y, fmax)
#define	fmin(__x, __y)		__tg_simple2(__x, __y, fmin)
#define	fmod(__x, __y)		__tg_simple2(__x, __y, fmod)
#define	frexp(__x, __y)		__tg_simplev(__x, frexp, __x, __y)
#define	hypot(__x, __y)		__tg_simple2(__x, __y, hypot)
#define	ilogb(__x)		__tg_simple(__x, ilogb)
#define	ldexp(__x, __y)		__tg_simplev(__x, ldexp, __x, __y)
#define	lgamma(__x)		__tg_simple(__x, lgamma)
#define	llrint(__x)		__tg_simple(__x, llrint)
#define	llround(__x)		__tg_simple(__x, llround)
#define	log10(__x)		__tg_simple(__x, log10)
#define	log1p(__x)		__tg_simple(__x, log1p)
#define	log2(__x)		__tg_simple(__x, log2)
#define	logb(__x)		__tg_simple(__x, logb)
#define	lrint(__x)		__tg_simple(__x, lrint)
#define	lround(__x)		__tg_simple(__x, lround)
#define	nearbyint(__x)		__tg_simple(__x, nearbyint)
#define	nextafter(__x, __y)	__tg_simple2(__x, __y, nextafter)
/* not yet implemented even for _LDBL_EQ_DBL platforms */
#ifdef __CYGWIN__
#define	nexttoward(__x, __y)	__tg_simplev(__x, nexttoward, __x, __y)
#endif
#define	remainder(__x, __y)	__tg_simple2(__x, __y, remainder)
#define	remquo(__x, __y, __z)	__tg_impl_simple(__x, __x, __y, remquo, remquof,	\
						 remquol, __x, __y, __z)
#define	rint(__x)		__tg_simple(__x, rint)
#define	round(__x)		__tg_simple(__x, round)
#define	scalbn(__x, __y)	__tg_simplev(__x, scalbn, __x, __y)
#define	scalbln(__x, __y)	__tg_simplev(__x, scalbln, __x, __y)
#define	tgamma(__x)		__tg_simple(__x, tgamma)
#define	trunc(__x)		__tg_simple(__x, trunc)

/* 7.22#6 -- These macros always expand to complex functions. */
#define	carg(__x)		__tg_simple(__x, carg)
#define	cimag(__x)		__tg_simple(__x, cimag)
#define	conj(__x)		__tg_simple(__x, conj)
#define	cproj(__x)		__tg_simple(__x, cproj)
#define	creal(__x)		__tg_simple(__x, creal)

#endif /* !_TGMATH_H_ */
