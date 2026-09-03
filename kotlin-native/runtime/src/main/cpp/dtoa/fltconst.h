/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#if !defined(fltconst_h)
#define fltconst_h

#include "hycomp.h" 
/* IEEE floats consist of: sign bit, exponent field, significand field
	single:  31 = sign bit, 30..23 = exponent (8 bits), 22..0 = significand (23 bits)
	double:  63 = sign bit, 62..52 = exponent (11 bits), 51..0 = significand (52 bits)
	inf				==	(all exponent bits set) and (all mantissa bits clear)
	nan				==	(all exponent bits set) and (at least one mantissa bit set)
	finite			==	(at least one exponent bit clear)
	zero      		==	(all exponent bits clear) and (all mantissa bits clear)
	denormal		==	(all exponent bits clear) and (at least one mantissa bit set)
	positive		==	sign bit clear
	negative		==	sign bit set
*/
#define	MAX_U32_DOUBLE	(ESDOUBLE) (4294967296.0)	/* 2^32 */
#define	MAX_U32_SINGLE		(ESSINGLE) (4294967296.0)	/* 2^32 */
#define HY_POS_PI      (ESDOUBLE)(3.141592653589793)

#ifdef HY_LITTLE_ENDIAN
#ifdef HY_PLATFORM_DOUBLE_ORDER
#define DOUBLE_LO_OFFSET		0
#define DOUBLE_HI_OFFSET			1
#else
#define DOUBLE_LO_OFFSET                1
#define DOUBLE_HI_OFFSET                0
#endif
#define LONG_LO_OFFSET			0
#define LONG_HI_OFFSET			1
#else
#ifdef HY_PLATFORM_DOUBLE_ORDER
#define DOUBLE_LO_OFFSET                1
#define DOUBLE_HI_OFFSET                0
#else
#define DOUBLE_LO_OFFSET                0
#define DOUBLE_HI_OFFSET                1
#endif
#define LONG_LO_OFFSET                  1
#define LONG_HI_OFFSET                  0
#endif

#define RETURN_FINITE				0
#define RETURN_NAN					1
#define RETURN_POS_INF			2
#define RETURN_NEG_INF			3
#define DOUBLE_SIGN_MASK_HI				0x80000000
#define DOUBLE_EXPONENT_MASK_HI		0x7FF00000
#define DOUBLE_MANTISSA_MASK_LO		0xFFFFFFFF
#define DOUBLE_MANTISSA_MASK_HI		0x000FFFFF
#define SINGLE_SIGN_MASK				0x80000000
#define SINGLE_EXPONENT_MASK	0x7F800000
#define SINGLE_MANTISSA_MASK	0x007FFFFF
#define SINGLE_NAN_BITS				(SINGLE_EXPONENT_MASK | 0x00400000)
typedef union u64u32dbl_tag {
	U_64    u64val;
	U_32    u32val[2];
    I_32    i32val[2];
	double  dval;
} U64U32DBL;
/* Replace P_FLOAT_HI and P_FLOAT_LOW */
/* These macros are used to access the high and low 32-bit parts of a double (64-bit) value. */
#define LOW_U32_FROM_DBL_PTR(dblptr) (((U64U32DBL *)(dblptr))->u32val[DOUBLE_LO_OFFSET])
#define HIGH_U32_FROM_DBL_PTR(dblptr) (((U64U32DBL *)(dblptr))->u32val[DOUBLE_HI_OFFSET])
#define LOW_I32_FROM_DBL_PTR(dblptr) (((U64U32DBL *)(dblptr))->i32val[DOUBLE_LO_OFFSET])
#define HIGH_I32_FROM_DBL_PTR(dblptr) (((U64U32DBL *)(dblptr))->i32val[DOUBLE_HI_OFFSET])
#define LOW_U32_FROM_DBL(dbl) LOW_U32_FROM_DBL_PTR(&(dbl))
#define HIGH_U32_FROM_DBL(dbl) HIGH_U32_FROM_DBL_PTR(&(dbl))
#define LOW_I32_FROM_DBL(dbl) LOW_I32_FROM_DBL_PTR(&(dbl))
#define HIGH_I32_FROM_DBL(dbl) HIGH_I32_FROM_DBL_PTR(&(dbl))
#define LOW_U32_FROM_LONG64_PTR(long64ptr) (((U64U32DBL *)(long64ptr))->u32val[LONG_LO_OFFSET])
#define HIGH_U32_FROM_LONG64_PTR(long64ptr) (((U64U32DBL *)(long64ptr))->u32val[LONG_HI_OFFSET])
#define LOW_I32_FROM_LONG64_PTR(long64ptr) (((U64U32DBL *)(long64ptr))->i32val[LONG_LO_OFFSET])
#define HIGH_I32_FROM_LONG64_PTR(long64ptr) (((U64U32DBL *)(long64ptr))->i32val[LONG_HI_OFFSET])
#define LOW_U32_FROM_LONG64(long64) LOW_U32_FROM_LONG64_PTR(&(long64))
#define HIGH_U32_FROM_LONG64(long64) HIGH_U32_FROM_LONG64_PTR(&(long64))
#define LOW_I32_FROM_LONG64(long64) LOW_I32_FROM_LONG64_PTR(&(long64))
#define HIGH_I32_FROM_LONG64(long64) HIGH_I32_FROM_LONG64_PTR(&(long64))
#define IS_ZERO_DBL_PTR(dblptr) ((LOW_U32_FROM_DBL_PTR(dblptr) == 0) && ((HIGH_U32_FROM_DBL_PTR(dblptr) == 0) || (HIGH_U32_FROM_DBL_PTR(dblptr) == DOUBLE_SIGN_MASK_HI)))
#define IS_ONE_DBL_PTR(dblptr) ((HIGH_U32_FROM_DBL_PTR(dblptr) == 0x3ff00000 || HIGH_U32_FROM_DBL_PTR(dblptr) == 0xbff00000) && (LOW_U32_FROM_DBL_PTR(dblptr) == 0))
#define IS_NAN_DBL_PTR(dblptr) (((HIGH_U32_FROM_DBL_PTR(dblptr) & DOUBLE_EXPONENT_MASK_HI) == DOUBLE_EXPONENT_MASK_HI) && (LOW_U32_FROM_DBL_PTR(dblptr) | (HIGH_U32_FROM_DBL_PTR(dblptr) & DOUBLE_MANTISSA_MASK_HI)))
#define IS_INF_DBL_PTR(dblptr) (((HIGH_U32_FROM_DBL_PTR(dblptr) & (DOUBLE_EXPONENT_MASK_HI|DOUBLE_MANTISSA_MASK_HI)) == DOUBLE_EXPONENT_MASK_HI) && (LOW_U32_FROM_DBL_PTR(dblptr) == 0))
#define IS_DENORMAL_DBL_PTR(dblptr) (((HIGH_U32_FROM_DBL_PTR(dblptr) & DOUBLE_EXPONENT_MASK_HI) == 0) && ((HIGH_U32_FROM_DBL_PTR(dblptr) & DOUBLE_MANTISSA_MASK_HI) != 0 || (LOW_U32_FROM_DBL_PTR(dblptr) != 0)))
#define IS_FINITE_DBL_PTR(dblptr) ((HIGH_U32_FROM_DBL_PTR(dblptr) & DOUBLE_EXPONENT_MASK_HI) < DOUBLE_EXPONENT_MASK_HI)
#define IS_POSITIVE_DBL_PTR(dblptr) ((HIGH_U32_FROM_DBL_PTR(dblptr) & DOUBLE_SIGN_MASK_HI) == 0)
#define IS_NEGATIVE_DBL_PTR(dblptr) ((HIGH_U32_FROM_DBL_PTR(dblptr) & DOUBLE_SIGN_MASK_HI) != 0)
#define IS_NEGATIVE_MAX_DBL_PTR(dblptr) ((HIGH_U32_FROM_DBL_PTR(dblptr) == 0xFFEFFFFF) && (LOW_U32_FROM_DBL_PTR(dblptr) == 0xFFFFFFFF))
#define IS_ZERO_DBL(dbl) IS_ZERO_DBL_PTR(&(dbl))
#define IS_ONE_DBL(dbl) IS_ONE_DBL_PTR(&(dbl))
#define IS_NAN_DBL(dbl) IS_NAN_DBL_PTR(&(dbl))
#define IS_INF_DBL(dbl) IS_INF_DBL_PTR(&(dbl))
#define IS_DENORMAL_DBL(dbl) IS_DENORMAL_DBL_PTR(&(dbl))
#define IS_FINITE_DBL(dbl) IS_FINITE_DBL_PTR(&(dbl))
#define IS_POSITIVE_DBL(dbl) IS_POSITIVE_DBL_PTR(&(dbl))
#define IS_NEGATIVE_DBL(dbl) IS_NEGATIVE_DBL_PTR(&(dbl))
#define IS_NEGATIVE_MAX_DBL(dbl) IS_NEGATIVE_MAX_DBL_PTR(&(dbl))
#define IS_ZERO_SNGL_PTR(fltptr)  ((*U32P((fltptr)) & (U_32)~SINGLE_SIGN_MASK) == (U_32)0)
#define IS_ONE_SNGL_PTR(fltptr) ((*U32P((fltptr)) == 0x3f800000) || (*U32P((fltptr)) == 0xbf800000))
#define IS_NAN_SNGL_PTR(fltptr)  ((*U32P((fltptr)) & (U_32)~SINGLE_SIGN_MASK) > (U_32)SINGLE_EXPONENT_MASK)
#define IS_INF_SNGL_PTR(fltptr)  ((*U32P((fltptr)) & (U_32)~SINGLE_SIGN_MASK) == (U_32)SINGLE_EXPONENT_MASK)
#define IS_DENORMAL_SNGL_PTR(fltptr)  (((*U32P((fltptr)) & (U_32)~SINGLE_SIGN_MASK)-(U_32)1) < (U_32)SINGLE_MANTISSA_MASK)
#define IS_FINITE_SNGL_PTR(fltptr)  ((*U32P((fltptr)) & (U_32)~SINGLE_SIGN_MASK) < (U_32)SINGLE_EXPONENT_MASK)
#define IS_POSITIVE_SNGL_PTR(fltptr)  ((*U32P((fltptr)) & (U_32)SINGLE_SIGN_MASK) == (U_32)0)
#define IS_NEGATIVE_SNGL_PTR(fltptr)  ((*U32P((fltptr)) & (U_32)SINGLE_SIGN_MASK) != (U_32)0)
#define IS_ZERO_SNGL(flt) IS_ZERO_SNGL_PTR(&(flt))
#define IS_ONE_SNGL(flt) IS_ONE_SNGL_PTR(&(flt))
#define IS_NAN_SNGL(flt) IS_NAN_SNGL_PTR(&(flt))
#define IS_INF_SNGL(flt) IS_INF_SNGL_PTR(&(flt))
#define IS_DENORMAL_SNGL(flt) IS_DENORMAL_SNGL_PTR(&(flt))
#define IS_FINITE_SNGL(flt) IS_FINITE_SNGL_PTR(&(flt))
#define IS_POSITIVE_SNGL(flt) IS_POSITIVE_SNGL_PTR(&(flt))
#define IS_NEGATIVE_SNGL(flt) IS_NEGATIVE_SNGL_PTR(&(flt))
#define SET_NAN_DBL_PTR(dblptr) HIGH_U32_FROM_DBL_PTR(dblptr) = (DOUBLE_EXPONENT_MASK_HI | 0x00080000); LOW_U32_FROM_DBL_PTR(dblptr) = 0
#define SET_PZERO_DBL_PTR(dblptr) HIGH_U32_FROM_DBL_PTR(dblptr) = 0; LOW_U32_FROM_DBL_PTR(dblptr) = 0
#define SET_NZERO_DBL_PTR(dblptr) HIGH_U32_FROM_DBL_PTR(dblptr) = DOUBLE_SIGN_MASK_HI; LOW_U32_FROM_DBL_PTR(dblptr) = 0
#define SET_PINF_DBL_PTR(dblptr) HIGH_U32_FROM_DBL_PTR(dblptr) = DOUBLE_EXPONENT_MASK_HI; LOW_U32_FROM_DBL_PTR(dblptr) = 0
#define SET_NINF_DBL_PTR(dblptr) HIGH_U32_FROM_DBL_PTR(dblptr) = (DOUBLE_EXPONENT_MASK_HI | DOUBLE_SIGN_MASK_HI); LOW_U32_FROM_DBL_PTR(dblptr) = 0
#define SET_NAN_SNGL_PTR(fltptr)  *U32P((fltptr)) = ((U_32)SINGLE_NAN_BITS)
#define SET_PZERO_SNGL_PTR(fltptr) *U32P((fltptr)) = 0
#define SET_NZERO_SNGL_PTR(fltptr) *U32P((fltptr)) = SINGLE_SIGN_MASK
#define SET_PINF_SNGL_PTR(fltptr) *U32P((fltptr)) = SINGLE_EXPONENT_MASK
#define SET_NINF_SNGL_PTR(fltptr) *U32P((fltptr)) = (SINGLE_EXPONENT_MASK | SINGLE_SIGN_MASK)

#if defined(HY_WORD64)
 #define PTR_DOUBLE_VALUE(dstPtr, aDoublePtr) ((U64U32DBL *)(aDoublePtr))->u64val = ((U64U32DBL *)(dstPtr))->u64val
 #define PTR_DOUBLE_STORE(dstPtr, aDoublePtr) ((U64U32DBL *)(dstPtr))->u64val = ((U64U32DBL *)(aDoublePtr))->u64val
 #define STORE_LONG(dstPtr, hi, lo) ((U64U32DBL *)(dstPtr))->u64val = (((U_64)(hi)) << 32) | (lo)
#else
 /* on some platforms (HP720) we cannot reference an unaligned float.  Build them by hand, one U_32 at a time. */
 #if defined(ATOMIC_FLOAT_ACCESS)
 #define PTR_DOUBLE_STORE(dstPtr, aDoublePtr) HIGH_U32_FROM_DBL_PTR(dstPtr) = HIGH_U32_FROM_DBL_PTR(aDoublePtr); LOW_U32_FROM_DBL_PTR(dstPtr) = LOW_U32_FROM_DBL_PTR(aDoublePtr)
 #define PTR_DOUBLE_VALUE(dstPtr, aDoublePtr) HIGH_U32_FROM_DBL_PTR(aDoublePtr) = HIGH_U32_FROM_DBL_PTR(dstPtr); LOW_U32_FROM_DBL_PTR(aDoublePtr) = LOW_U32_FROM_DBL_PTR(dstPtr)
 #else
 #define PTR_DOUBLE_STORE(dstPtr, aDoublePtr) (*(dstPtr) = *(aDoublePtr))
 #define PTR_DOUBLE_VALUE(dstPtr, aDoublePtr) (*(aDoublePtr) = *(dstPtr))
 #endif

 #define STORE_LONG(dstPtr, hi, lo) HIGH_U32_FROM_LONG64_PTR(dstPtr) = (hi); LOW_U32_FROM_LONG64_PTR(dstPtr) = (lo)
#endif /* HY_WORD64 */

#define PTR_SINGLE_VALUE(dstPtr, aSinglePtr) (*U32P(aSinglePtr) = *U32P(dstPtr))
#define PTR_SINGLE_STORE(dstPtr, aSinglePtr) *((U_32 *)(dstPtr)) = (*U32P(aSinglePtr))

#endif     /* fltconst_h */
