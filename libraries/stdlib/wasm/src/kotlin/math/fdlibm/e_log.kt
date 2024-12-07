/* @(#)e_log.c 1.3 95/01/18 */
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

/* __ieee754_log(x)
 * Return the logrithm of x
 *
 * Method :                  
 *   1. Argument Reduction: find k and f such that 
 *			x = 2^k * (1+f), 
 *	   where  sqrt(2)/2 < 1+f < sqrt(2) .
 *
 *   2. Approximation of log(1+f).
 *	Let s = f/(2+f) ; based on log(1+f) = log(1+s) - log(1-s)
 *		 = 2s + 2/3 s**3 + 2/5 s**5 + .....,
 *	     	 = 2s + s*R
 *      We use a special Reme algorithm on [0,0.1716] to generate 
 * 	a polynomial of degree 14 to approximate R The maximum error 
 *	of this polynomial approximation is bounded by 2**-58.45. In
 *	other words,
 *		        2      4      6      8      10      12      14
 *	    R(z) ~ Lg1*s +Lg2*s +Lg3*s +Lg4*s +Lg5*s  +Lg6*s  +Lg7*s
 *  	(the values of Lg1 to Lg7 are listed in the program)
 *	and
 *	    |      2          14          |     -58.45
 *	    | Lg1*s +...+Lg7*s    -  R(z) | <= 2 
 *	    |                             |
 *	Note that 2s = f - s*f = f - hfsq + s*hfsq, where hfsq = f*f/2.
 *	In order to guarantee error in log below 1ulp, we compute log
 *	by
 *		log(1+f) = f - s*(f - R)	(if f is not too large)
 *		log(1+f) = f - (hfsq - s*(hfsq+R)).	(better accuracy)
 *	
 *	3. Finally,  log(x) = k*ln2 + log(1+f).  
 *			    = k*ln2_hi+(f-(hfsq-(s*(hfsq+R)+k*ln2_lo)))
 *	   Here ln2 is split into two floating point number: 
 *			ln2_hi + ln2_lo,
 *	   where n*ln2_hi is always exact for |n| < 2000.
 *
 * Special cases:
 *	log(x) is NaN with signal if x < 0 (including -INF) ; 
 *	log(+INF) is +INF; log(0) is -INF with signal;
 *	log(NaN) is that NaN with no signal.
 *
 * Accuracy:
 *	according to an error analysis, the error is always less than
 *	1 ulp (unit in the last place).
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following 
 * constants. The decimal values may be used, provided that the 
 * compiler will convert from decimal to binary accurately enough 
 * to produce the hexadecimal values shown.
 */

package kotlin.math.fdlibm


private const val ln2_hi = 6.93147180369123816490e-01    /* 3fe62e42 fee00000 */
private const val ln2_lo = 1.90821492927058770002e-10    /* 3dea39ef 35793c76 */
private const val two54 = 1.80143985094819840000e+16  /* 43500000 00000000 */
private const val Lg1 = 6.666666666666735130e-01  /* 3FE55555 55555593 */
private const val Lg2 = 3.999999999940941908e-01  /* 3FD99999 9997FA04 */
private const val Lg3 = 2.857142874366239149e-01  /* 3FD24924 94229359 */
private const val Lg4 = 2.222219843214978396e-01  /* 3FCC71C5 1D8E78AF */
private const val Lg5 = 1.818357216161805012e-01  /* 3FC74664 96CB03DE */
private const val Lg6 = 1.531383769920937332e-01  /* 3FC39A09 D078C69F */
private const val Lg7 = 1.479819860511658591e-01  /* 3FC2F112 DF3E5244 */

private const val zero = 0.0

internal fun __ieee754_log(_x: Double): Double {
    var x: Double = _x
    var hfsq: Double
    var f: Double
    var s: Double
    var z: Double
    var R: Double
    var w: Double
    var t1: Double
    var t2: Double
    var dk: Double
    var k: Int
    var hx: Int
    var i: Int
    var j: Int
    var lx: UInt

    hx = __HI(x)        /* high word of x */
    lx = __LOu(x)        /* low  word of x */

    k = 0
    if (hx < 0x00100000) {            /* x < 2**-1022  */
        if (((hx and 0x7fffffff) or lx.toInt()) == 0)
            return Double.NEGATIVE_INFINITY        /* log(+-0)=-inf */
        if (hx < 0) return Double.NaN    /* log(-#) = NaN */
        k -= 54; x *= two54 /* subnormal number, scale up x */
        hx = __HI(x)        /* high word of x */
    }
    if (hx >= 0x7ff00000) return x + x
    k += (hx shr 20) - 1023
    hx = hx and 0x000fffff
    i = (hx + 0x95f64) and 0x100000
    x = doubleSetWord(d = x, hi = hx or (i xor 0x3ff00000))    /* normalize x or x/2 */
    k += (i shr 20)
    f = x - 1.0
    if ((0x000fffff and (2 + hx)) < 3) {    /* |f| < 2**-20 */
        if (f == zero) if (k == 0) return zero; else {
            dk = k.toDouble()
            return dk * ln2_hi + dk * ln2_lo
        }
        R = f * f * (0.5 - 0.33333333333333333 * f)
        if (k == 0) return f - R; else {
            dk = k.toDouble()
            return dk * ln2_hi - ((R - dk * ln2_lo) - f)
        }
    }
    s = f / (2.0 + f)
    dk = k.toDouble()
    z = s * s
    i = hx - 0x6147a
    w = z * z
    j = 0x6b851 - hx
    t1 = w * (Lg2 + w * (Lg4 + w * Lg6))
    t2 = z * (Lg1 + w * (Lg3 + w * (Lg5 + w * Lg7)))
    i = i or j
    R = t2 + t1
    if (i > 0) {
        hfsq = 0.5 * f * f
        if (k == 0) return f - (hfsq - s * (hfsq + R)); else
            return dk * ln2_hi - ((hfsq - (s * (hfsq + R) + dk * ln2_lo)) - f)
    } else {
        if (k == 0) return f - s * (f - R); else
            return dk * ln2_hi - ((s * (f - R) - dk * ln2_lo) - f)
    }
}
