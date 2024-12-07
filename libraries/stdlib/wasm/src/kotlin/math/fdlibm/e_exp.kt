/* @(#)e_exp.c 1.6 04/04/22 */
/*
 * ====================================================
 * Copyright (C) 2004 by Sun Microsystems, Inc. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice 
 * is preserved.
 * ====================================================
 */

/* __ieee754_exp(x)
 * Returns the exponential of x.
 *
 * Method
 *   1. Argument reduction:
 *      Reduce x to an r so that |r| <= 0.5*ln2 ~ 0.34658.
 *	Given x, find r and integer k such that
 *
 *               x = k*ln2 + r,  |r| <= 0.5*ln2.  
 *
 *      Here r will be represented as r = hi-lo for better 
 *	accuracy.
 *
 *   2. Approximation of exp(r) by a special rational function on
 *	the interval [0,0.34658]:
 *	Write
 *	    R(r**2) = r*(exp(r)+1)/(exp(r)-1) = 2 + r*r/6 - r**4/360 + ...
 *      We use a special Remes algorithm on [0,0.34658] to generate 
 * 	a polynomial of degree 5 to approximate R. The maximum error 
 *	of this polynomial approximation is bounded by 2**-59. In
 *	other words,
 *	    R(z) ~ 2.0 + P1*z + P2*z**2 + P3*z**3 + P4*z**4 + P5*z**5
 *  	(where z=r*r, and the values of P1 to P5 are listed below)
 *	and
 *	    |                  5          |     -59
 *	    | 2.0+P1*z+...+P5*z   -  R(z) | <= 2 
 *	    |                             |
 *	The computation of exp(r) thus becomes
 *                             2*r
 *		exp(r) = 1 + -------
 *		              R - r
 *                                 r*R1(r)	
 *		       = 1 + r + ----------- (for better accuracy)
 *		                  2 - R1(r)
 *	where
 *			         2       4             10
 *		R1(r) = r - (P1*r  + P2*r  + ... + P5*r   ).
 *	
 *   3. Scale back to obtain exp(x):
 *	From step 1, we have
 *	   exp(x) = 2^k * exp(r)
 *
 * Special cases:
 *	exp(INF) is INF, exp(NaN) is NaN;
 *	exp(-INF) is 0, and
 *	for finite argument, only exp(0)=1 is exact.
 *
 * Accuracy:
 *	according to an error analysis, the error is always less than
 *	1 ulp (unit in the last place).
 *
 * Misc. info.
 *	For IEEE double 
 *	    if x >  7.09782712893383973096e+02 then exp(x) overflow
 *	    if x < -7.45133219101941108420e+02 then exp(x) underflow
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following 
 * constants. The decimal values may be used, provided that the 
 * compiler will convert from decimal to binary accurately enough
 * to produce the hexadecimal values shown.
 */

package kotlin.math.fdlibm


private const val one = 1.0
private val halF = doubleArrayOf(0.5, -0.5)
private const val huge = 1.0e+300
private const val twom1000 = 9.33263618503218878990e-302     /* 2**-1000=0x01700000,0*/
private const val o_threshold = 7.09782712893383973096e+02  /* 0x40862E42, 0xFEFA39EF */
private const val u_threshold = -7.45133219101941108420e+02  /* 0xc0874910, 0xD52D3051 */
private val ln2HI = doubleArrayOf(
    6.93147180369123816490e-01,  /* 0x3fe62e42, 0xfee00000 */
    -6.93147180369123816490e-01,
)/* 0xbfe62e42, 0xfee00000 */
private val ln2LO = doubleArrayOf(
    1.90821492927058770002e-10,  /* 0x3dea39ef, 0x35793c76 */
    -1.90821492927058770002e-10,
)/* 0xbdea39ef, 0x35793c76 */
private const val invln2 = 1.44269504088896338700e+00 /* 0x3ff71547, 0x652b82fe */
private const val P1 = 1.66666666666666019037e-01 /* 0x3FC55555, 0x5555553E */
private const val P2 = -2.77777777770155933842e-03 /* 0xBF66C16C, 0x16BEBD93 */
private const val P3 = 6.61375632143793436117e-05 /* 0x3F11566A, 0xAF25DE2C */
private const val P4 = -1.65339022054652515390e-06 /* 0xBEBBBD41, 0xC5D26BF1 */
private const val P5 = 4.13813679705723846039e-08 /* 0x3E663769, 0x72BEA4D0 */


internal fun __ieee754_exp(_x: Double): Double    /* default IEEE double exp */ {
    var x: Double = _x
    var y: Double
    var hi: Double = 0.0
    var lo: Double = 0.0
    var c: Double
    var t: Double
    var k: Int = 0
    var xsb: Int
    var hx: UInt

    hx = __HIu(x)    /* high word of x */
    xsb = ((hx shr 31) and 1U).toInt()        /* sign bit of x */
    hx = hx and 0x7fffffffU        /* high word of |x| */

    /* filter out non-finite argument */
    if (hx >= 0x40862E42U) {            /* if |x|>=709.78... */
        if (hx >= 0x7ff00000U) {
            if (((hx and 0xfffffU) or __LOu(x)) != 0U)
                return x + x        /* NaN */
            else return if (xsb == 0) x else 0.0    /* exp(+-inf)={inf,0} */
        }
        if (x > o_threshold) return huge * huge /* overflow */
        if (x < u_threshold) return twom1000 * twom1000 /* underflow */
    }

    /* argument reduction */
    if (hx > 0x3fd62e42U) {        /* if  |x| > 0.5 ln2 */
        if (hx < 0x3FF0A2B2U) {    /* and |x| < 1.5 ln2 */
            hi = x - ln2HI[xsb]; lo = ln2LO[xsb]; k = 1 - xsb - xsb
        } else {
            k = (invln2 * x + halF[xsb]).toInt()
            t = k.toDouble()
            hi = x - t * ln2HI[0]    /* t*ln2HI is exact here */
            lo = t * ln2LO[0]
        }
        x = hi - lo
    } else if (hx < 0x3e300000U) {    /* when |x|<2**-28 */
        if (huge + x > one) return one + x/* trigger inexact */
    } else k = 0

    /* x is now in primary range */
    t = x * x
    c = x - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))))
    if (k == 0) return one - ((x * c) / (c - 2.0) - x)
    else y = one - ((lo - (x * c) / (2.0 - c)) - hi)
    if (k >= -1021) {
        y = doubleSetWord(d = y, hi = __HI(y) + (k shl 20))    /* add k to y's exponent */
        return y
    } else {
        y = doubleSetWord(d = y, hi = __HI(y) + ((k + 1000) shl 20))/* add k to y's exponent */
        return y * twom1000
    }
}
