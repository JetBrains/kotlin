/* @(#)e_sinh.c 1.3 95/01/18 */
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

/* __ieee754_sinh(x)
 * Method : 
 * mathematically sinh(x) if defined to be (exp(x)-exp(-x))/2
 *	1. Replace x by |x| (sinh(-x) = -sinh(x)). 
 *	2. 
 *		                                    E + E/(E+1)
 *	    0        <= x <= 22     :  sinh(x) := --------------, E=expm1(x)
 *			       			        2
 *
 *	    22       <= x <= lnovft :  sinh(x) := exp(x)/2 
 *	    lnovft   <= x <= ln2ovft:  sinh(x) := exp(x/2)/2 * exp(x/2)
 *	    ln2ovft  <  x	    :  sinh(x) := x*shuge (overflow)
 *
 * Special cases:
 *	sinh(x) is |x| if x is +INF, -INF, or NaN.
 *	only sinh(0)=0 is exact for finite x.
 */

package kotlin.math.fdlibm

private const val one = 1.0
private const val shuge = 1.0e307
internal fun __ieee754_sinh(x: Double): Double {
    var t: Double
    var w: Double
    var h: Double
    var ix: Int
    var jx: Int
    var lx: UInt

    /* High word of |x|. */
    jx = __HI(x)
    ix = jx and 0x7fffffff

    /* x is INF or NaN */
    if (ix >= 0x7ff00000) return x + x

    h = 0.5
    if (jx < 0) h = -h
    /* |x| in [0,22], return sign(x)*0.5*(E+E/(E+1))) */
    if (ix < 0x40360000) {        /* |x|<22 */
        if (ix < 0x3e300000)        /* |x|<2**-28 */
            if (shuge + x > one) return x/* sinh(tiny) = tiny with inexact */
        t = expm1(fabs(x))
        if (ix < 0x3ff00000) return h * (2.0 * t - t * t / (t + one))
        return h * (t + t / (t + one))
    }

    /* |x| in [22, log(maxdouble)] return 0.5*exp(|x|) */
    if (ix < 0x40862E42) return h * __ieee754_exp(fabs(x))

    /* |x| in [log(maxdouble), overflowthresold] */
    //lx = *( (((*(unsigned*)&one) shr 29)) + (unsigned*)&x);
    lx = (x.toBits() and 0xFFFFFFFF).toUInt()
    if (ix < 0x408633CE || (ix == 0x408633ce) && (lx <= 0x8fb9f87d.toUInt())) {
        w = __ieee754_exp(0.5 * fabs(x))
        t = h * w
        return t * w
    }

    /* |x| > overflowthresold, sinh(x) overflow */
    return x * shuge
}
