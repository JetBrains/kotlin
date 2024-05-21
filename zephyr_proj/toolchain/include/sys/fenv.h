/*
  (c) Copyright 2019 Joel Sherrill <joel@rtems.org
  (c) Copyright 2019 Craig Howlang <craig.howland@caci.com>
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

  1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#ifndef _SYS_FENV_H_
#define _SYS_FENV_H_

/*******************************************************************************
 * THIS FILE IS A TEMPLATE, INTENDED TO BE USED AS A STARTING POINT FOR
 * TARGET-SPECIFIC FLOATING-POINT IMPLEMENTATIONS.  NOTES BELOW HIGHLIGHT THE
 * BASICS OF WHAT NEEDS TO BE DEFINED.  THE DEFAULT IMPLEMTATION IS
 * DEGENERATE, WITH ALL FUNCTIONS RETURNING ERROR AND NO EXCEPTIONS AND NO
 * ROUNDING MODES DEFINED (SINCE NONE ARE SUPPORTED).
 * THE MACRO VALUES ARE EXAMPLES ONLY, ALTHOUGH TAKEN FROM A WORKING
 * IMPLEMENTATION.
 * REMOVE THIS NOTICE WHEN COPYING TO A REAL IMPLEMENTATION, REPLACING IT WITH
 * ANY TARGET-SPECIFIC NOTES OF INTEREST.  THE FENV FUNCTION MAN PAGES POINT TO
 * THIS FILE AS A MEANS OF DETERMINING A FUNCTIONAL VS. NON-FUNCTIONAL
 * IMPLEMENTATION.
 ******************************************************************************/
/*
 * The following macros are to be defined if the respective exception is
 * supported by the implementation, each with a unique bit mask:
 *
 *	FE_DIVBYZERO
 *	FE_INEXACT
 *	FE_INVALID
 *	FE_OVERFLOW
 *	FE_UNDERFLOW
 *
 * Other implementation-specific exceptions may be defined, and must start
 * with FE_ followed by a capital letter.
 *
 * FE_ALL_EXCEPT must be defined as the logical OR of all exceptions.
 */
//#define FE_DIVBYZERO 0x00000001
//#define FE_INEXACT   0x00000002
//#define FE_INVALID   0x00000004
//#define FE_OVERFLOW  0x00000008
//#define FE_UNDERFLOW 0x00000010

//#define FE_ALL_EXCEPT \
          //(FE_DIVBYZERO|FE_INEXACT|FE_INVALID|FE_OVERFLOW|FE_UNDERFLOW)
#define FE_ALL_EXCEPT 0	/* NONE SUPPORTED IN PLACEHOLDER TEMPLATE */

/*
 * The following macros are to be defined if the respective rounding
 * direction is supported by the implementation via the fegetround() and
 * fesetround() functions, each with a unique positive value.
 *
 *	FE_DOWNWARD
 *	FE_TONEAREST
 *	FE_TOWARDZERO
 *	FE_UPWARD
 *
 * Other implementation-specific rounding modes may be defined, and must start
 * with FE_ followed by a capital letter.
 */
//#define FE_DOWNWARD   	1
//#define FE_TONEAREST  	2
//#define FE_TOWARDZERO 	3
//#define FE_UPWARD     	4

/*
 * The following typedefs are required. These should be defined properly
 * to support the architecture specific implementation. See the C and
 * POSIX standards for details:
 *
 *	fenv_t
 *	fexcept_t
 */
typedef int fenv_t;
typedef int fexcept_t;

/*
 * Lastly, a FE_DFL_ENV macro must be defined, representing a pointer
 * to const fenv_t that contains the value of the default floating point
 * environment.
 *
 * NOTE: The extern'ed variable fe_default_env_p is an implementation
 *       detail of this stub.  FE_DFL_ENV must point to an instance of
 *       fenv_t with the default fenv_t. The format of fenv_t and where
 *       FE_DFL_ENV is are implementation specific.
 */
extern const fenv_t *_fe_dfl_env;
#define FE_DFL_ENV _fe_dfl_env

#endif /* _SYS_FENV_H_ */
