/* @(#)e_cosh.c 1.3 95/01/18 */
/*
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunSoft, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice 
 * is preserved.
 * ====================================================
 */

/* __ieee754_cosh(x)
 * Method : 
 * mathematically cosh(x) if defined to be (exp(x)+exp(-x))/2
 *	1. Replace x by |x| (cosh(x) = cosh(-x)). 
 *	2. 
 *		                                        [ exp(x) - 1 ]^2 
 *	    0        <= x <= ln2/2  :  cosh(x) := 1 + -------------------
 *			       			           2*exp(x)
 *
 *		                                  exp(x) +  1/exp(x)
 *	    ln2/2    <= x <= 22     :  cosh(x) := -------------------
 *			       			          2
 *	    22       <= x <= lnovft :  cosh(x) := exp(x)/2 
 *	    lnovft   <= x <= ln2ovft:  cosh(x) := exp(x/2)/2 * exp(x/2)
 *	    ln2ovft  <  x	    :  cosh(x) := huge*huge (overflow)
 *
 * Special cases:
 *	cosh(x) is |x| if x is +INF, -INF, or NaN.
 *	only cosh(0)=1 is exact for finite x.
 */

package kotlin.math.fdlibm

private const val one = 1.0
private const val half = 0.5
private const val huge = 1.0e300

internal fun __ieee754_cosh(x: Double): Double {
    var t: Double
    var w: Double
    var ix: Int
    var lx: UInt

    /* High word of |x|. */
    ix = __HI(x)
    ix = ix and 0x7fffffff

    /* x is INF or NaN */
    if (ix >= 0x7ff00000) return x * x

    /* |x| in [0,0.5*ln2], return 1+expm1(|x|)^2/(2*exp(|x|)) */
    if (ix < 0x3fd62e43) {
        t = expm1(fabs(x))
        w = one + t
        if (ix < 0x3c800000) return w    /* cosh(tiny) = 1 */
        return one + (t * t) / (w + w)
    }

    /* |x| in [0.5*ln2,22], return (exp(|x|)+1/exp(|x|)/2; */
    if (ix < 0x40360000) {
        t = __ieee754_exp(fabs(x))
        return half * t + half / t
    }

    /* |x| in [22, log(maxdouble)] return half*exp(|x|) */
    if (ix < 0x40862E42) return half * __ieee754_exp(fabs(x))

    /* |x| in [log(maxdouble), overflowthresold] */
    //lx = *( (((*(unsigned*)&one) shr 29)) + (unsigned*)&x);
    lx = __LOu(x)
    if (ix < 0x408633CE ||
        (ix == 0x408633ce) && (lx <= 0x8fb9f87d.toUInt())
    ) {
        w = __ieee754_exp(half * fabs(x))
        t = half * w
        return t * w
    }

    /* |x| > overflowthresold, cosh(x) overflow */
    return huge * huge
}
