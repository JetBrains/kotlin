#ifndef _IEEE_FP_H_
#define _IEEE_FP_H_

#include "_ansi.h"

#include <machine/ieeefp.h>
#include <float.h>

_BEGIN_STD_C

/* FIXME FIXME FIXME:
   Neither of __ieee_{float,double}_shape_type seem to be used anywhere
   except in libm/test.  If that is the case, please delete these from here.
   If that is not the case, please insert documentation here describing why
   they're needed.  */

#ifdef __IEEE_BIG_ENDIAN

typedef union 
{
  double value;
  struct 
  {
    unsigned int sign : 1;
    unsigned int exponent: 11;
    unsigned int fraction0:4;
    unsigned int fraction1:16;
    unsigned int fraction2:16;
    unsigned int fraction3:16;
    
  } number;
  struct 
  {
    unsigned int sign : 1;
    unsigned int exponent: 11;
    unsigned int quiet:1;
    unsigned int function0:3;
    unsigned int function1:16;
    unsigned int function2:16;
    unsigned int function3:16;
  } nan;
  struct 
  {
    unsigned long msw;
    unsigned long lsw;
  } parts;
    long aslong[2];
} __ieee_double_shape_type;

#elif defined __IEEE_LITTLE_ENDIAN

typedef union 
{
  double value;
  struct 
  {
#ifdef __SMALL_BITFIELDS
    unsigned int fraction3:16;
    unsigned int fraction2:16;
    unsigned int fraction1:16;
    unsigned int fraction0: 4;
#else
    unsigned int fraction1:32;
    unsigned int fraction0:20;
#endif
    unsigned int exponent :11;
    unsigned int sign     : 1;
  } number;
  struct 
  {
#ifdef __SMALL_BITFIELDS
    unsigned int function3:16;
    unsigned int function2:16;
    unsigned int function1:16;
    unsigned int function0:3;
#else
    unsigned int function1:32;
    unsigned int function0:19;
#endif
    unsigned int quiet:1;
    unsigned int exponent: 11;
    unsigned int sign : 1;
  } nan;
  struct 
  {
    unsigned long lsw;
    unsigned long msw;
  } parts;

  long aslong[2];

} __ieee_double_shape_type;

#endif /* __IEEE_LITTLE_ENDIAN */

#ifdef __IEEE_BIG_ENDIAN

typedef union
{
  float value;
  struct 
  {
    unsigned int sign : 1;
    unsigned int exponent: 8;
    unsigned int fraction0: 7;
    unsigned int fraction1: 16;
  } number;
  struct 
  {
    unsigned int sign:1;
    unsigned int exponent:8;
    unsigned int quiet:1;
    unsigned int function0:6;
    unsigned int function1:16;
  } nan;
  long p1;
  
} __ieee_float_shape_type;

#elif defined __IEEE_LITTLE_ENDIAN

typedef union
{
  float value;
  struct 
  {
    unsigned int fraction0: 7;
    unsigned int fraction1: 16;
    unsigned int exponent: 8;
    unsigned int sign : 1;
  } number;
  struct 
  {
    unsigned int function1:16;
    unsigned int function0:6;
    unsigned int quiet:1;
    unsigned int exponent:8;
    unsigned int sign:1;
  } nan;
  long p1;
  
} __ieee_float_shape_type;

#endif /* __IEEE_LITTLE_ENDIAN */

#ifndef _LDBL_EQ_DBL

#ifndef LDBL_MANT_DIG
#error "LDBL_MANT_DIG not defined - should be found in float.h"

#elif LDBL_MANT_DIG == DBL_MANT_DIG
#error "double and long double are the same size but LDBL_EQ_DBL is not defined"

#elif LDBL_MANT_DIG == 53
/* This happens when doubles are 32-bits and long doubles are 64-bits.  */
#define	EXT_EXPBITS	11
#define EXT_FRACHBITS	20
#define	EXT_FRACLBITS	32
#define __ieee_ext_field_type unsigned long

#elif LDBL_MANT_DIG == 64
#define	EXT_EXPBITS	15
#define EXT_FRACHBITS	32
#define	EXT_FRACLBITS	32
#define __ieee_ext_field_type unsigned int

#elif LDBL_MANT_DIG == 65
#define	EXT_EXPBITS	15
#define EXT_FRACHBITS	32
#define	EXT_FRACLBITS	32
#define __ieee_ext_field_type unsigned int

#elif LDBL_MANT_DIG == 112
#define	EXT_EXPBITS	15
#define EXT_FRACHBITS	48
#define	EXT_FRACLBITS	64
#define __ieee_ext_field_type unsigned long long

#elif LDBL_MANT_DIG == 113
#define	EXT_EXPBITS	15
#define EXT_FRACHBITS	48
#define	EXT_FRACLBITS	64
#define __ieee_ext_field_type unsigned long long

#else
#error Unsupported value for LDBL_MANT_DIG
#endif

#define	EXT_EXP_INFNAN	   ((1 << EXT_EXPBITS) - 1) /* 32767 */
#define	EXT_EXP_BIAS	   ((1 << (EXT_EXPBITS - 1)) - 1) /* 16383 */
#define	EXT_FRACBITS	   (EXT_FRACLBITS + EXT_FRACHBITS)

typedef struct ieee_ext
{
  __ieee_ext_field_type	 ext_fracl : EXT_FRACLBITS;
  __ieee_ext_field_type	 ext_frach : EXT_FRACHBITS;
  __ieee_ext_field_type	 ext_exp   : EXT_EXPBITS;
  __ieee_ext_field_type	 ext_sign  : 1;
} ieee_ext;

typedef union ieee_ext_u
{
  long double		extu_ld;
  struct ieee_ext	extu_ext;
} ieee_ext_u;

#endif /* ! _LDBL_EQ_DBL */


/* FLOATING ROUNDING */

typedef int fp_rnd;
#define FP_RN 0 	/* Round to nearest 		*/
#define FP_RM 1		/* Round down 			*/
#define FP_RP 2		/* Round up 			*/
#define FP_RZ 3		/* Round to zero (trunate) 	*/

fp_rnd fpgetround (void);
fp_rnd fpsetround (fp_rnd);

/* EXCEPTIONS */

typedef int fp_except;
#define FP_X_INV 0x10	/* Invalid operation 		*/
#define FP_X_DX  0x80	/* Divide by zero		*/
#define FP_X_OFL 0x04	/* Overflow exception		*/
#define FP_X_UFL 0x02	/* Underflow exception		*/
#define FP_X_IMP 0x01	/* imprecise exception		*/

fp_except fpgetmask (void);
fp_except fpsetmask (fp_except);
fp_except fpgetsticky (void);
fp_except fpsetsticky (fp_except);

/* INTEGER ROUNDING */

typedef int fp_rdi;
#define FP_RDI_TOZ 0	/* Round to Zero 		*/
#define FP_RDI_RD  1	/* Follow float mode		*/

fp_rdi fpgetroundtoi (void);
fp_rdi fpsetroundtoi (fp_rdi);

#define __IEEE_DBL_EXPBIAS 1023
#define __IEEE_FLT_EXPBIAS 127

#define __IEEE_DBL_EXPLEN 11
#define __IEEE_FLT_EXPLEN 8


#define __IEEE_DBL_FRACLEN (64 - (__IEEE_DBL_EXPLEN + 1))
#define __IEEE_FLT_FRACLEN (32 - (__IEEE_FLT_EXPLEN + 1))

#define __IEEE_DBL_MAXPOWTWO	((double)(1L << 32 - 2) * (1L << (32-11) - 32 + 1))
#define __IEEE_FLT_MAXPOWTWO	((float)(1L << (32-8) - 1))

#define __IEEE_DBL_NAN_EXP 0x7ff
#define __IEEE_FLT_NAN_EXP 0xff

#ifdef __ieeefp_isnanf
#define isnanf(x)	__ieeefp_isnanf(x)
#endif

#ifdef __ieeefp_isinff
#define isinff(x)	__ieeefp_isinff(x)
#endif

#ifdef __ieeefp_finitef
#define finitef(x)	__ieeefp_finitef(x)
#endif

#ifdef _DOUBLE_IS_32BITS
#undef __IEEE_DBL_EXPBIAS
#define __IEEE_DBL_EXPBIAS __IEEE_FLT_EXPBIAS

#undef __IEEE_DBL_EXPLEN
#define __IEEE_DBL_EXPLEN __IEEE_FLT_EXPLEN

#undef __IEEE_DBL_FRACLEN
#define __IEEE_DBL_FRACLEN __IEEE_FLT_FRACLEN

#undef __IEEE_DBL_MAXPOWTWO
#define __IEEE_DBL_MAXPOWTWO __IEEE_FLT_MAXPOWTWO

#undef __IEEE_DBL_NAN_EXP
#define __IEEE_DBL_NAN_EXP __IEEE_FLT_NAN_EXP

#undef __ieee_double_shape_type
#define __ieee_double_shape_type __ieee_float_shape_type

#endif /* _DOUBLE_IS_32BITS */

_END_STD_C

#endif /* _IEEE_FP_H_ */
