/* @(#)e_rem_pio2.c 1.4 95/01/18 */
/*
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunSoft, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice 
 * is preserved.
 * ====================================================
 *
 */

/* __ieee754_rem_pio2(x,y)
 * 
 * return the remainder of x rem pi/2 in y[0]+y[1] 
 * use __kernel_rem_pio2()
 */

package kotlin.math.fdlibm

/*
 * Table of constants for 2/pi, 396 Hex digits (476 decimal) of 2/pi 
 */
private val two_over_pi = intArrayOf(
    0xA2F983, 0x6E4E44, 0x1529FC, 0x2757D1, 0xF534DD, 0xC0DB62,
    0x95993C, 0x439041, 0xFE5163, 0xABDEBB, 0xC561B7, 0x246E3A,
    0x424DD2, 0xE00649, 0x2EEA09, 0xD1921C, 0xFE1DEB, 0x1CB129,
    0xA73EE8, 0x8235F5, 0x2EBB44, 0x84E99C, 0x7026B4, 0x5F7E41,
    0x3991D6, 0x398353, 0x39F49C, 0x845F8B, 0xBDF928, 0x3B1FF8,
    0x97FFDE, 0x05980F, 0xEF2F11, 0x8B5A0A, 0x6D1F6D, 0x367ECF,
    0x27CB09, 0xB74F46, 0x3F669E, 0x5FEA2D, 0x7527BA, 0xC7EBE5,
    0xF17B3D, 0x0739F7, 0x8A5292, 0xEA6BFB, 0x5FB11F, 0x8D5D08,
    0x560330, 0x46FC7B, 0x6BABF0, 0xCFBC20, 0x9AF436, 0x1DA9E3,
    0x91615E, 0xE61B08, 0x659985, 0x5F14A0, 0x68408D, 0xFFD880,
    0x4D7327, 0x310606, 0x1556CA, 0x73A8C9, 0x60E27B, 0xC08C6B,
)

private val npio2_hw = intArrayOf(
    0x3FF921FB, 0x400921FB, 0x4012D97C, 0x401921FB, 0x401F6A7A, 0x4022D97C,
    0x4025FDBB, 0x402921FB, 0x402C463A, 0x402F6A7A, 0x4031475C, 0x4032D97C,
    0x40346B9C, 0x4035FDBB, 0x40378FDB, 0x403921FB, 0x403AB41B, 0x403C463A,
    0x403DD85A, 0x403F6A7A, 0x40407E4C, 0x4041475C, 0x4042106C, 0x4042D97C,
    0x4043A28C, 0x40446B9C, 0x404534AC, 0x4045FDBB, 0x4046C6CB, 0x40478FDB,
    0x404858EB, 0x404921FB,
)

/*
 * invpio2:  53 bits of 2/pi
 * pio2_1:   first  33 bit of pi/2
 * pio2_1t:  pi/2 - pio2_1
 * pio2_2:   second 33 bit of pi/2
 * pio2_2t:  pi/2 - (pio2_1+pio2_2)
 * pio2_3:   third  33 bit of pi/2
 * pio2_3t:  pi/2 - (pio2_1+pio2_2+pio2_3)
 */


private const val zero = 0.00000000000000000000e+00 /* 0x00000000, 0x00000000 */
private const val half = 5.00000000000000000000e-01 /* 0x3FE00000, 0x00000000 */
private const val two24 = 1.67772160000000000000e+07 /* 0x41700000, 0x00000000 */
private const val invpio2 = 6.36619772367581382433e-01 /* 0x3FE45F30, 0x6DC9C883 */
private const val pio2_1 = 1.57079632673412561417e+00 /* 0x3FF921FB, 0x54400000 */
private const val pio2_1t = 6.07710050650619224932e-11 /* 0x3DD0B461, 0x1A626331 */
private const val pio2_2 = 6.07710050630396597660e-11 /* 0x3DD0B461, 0x1A600000 */
private const val pio2_2t = 2.02226624879595063154e-21 /* 0x3BA3198A, 0x2E037073 */
private const val pio2_3 = 2.02226624871116645580e-21 /* 0x3BA3198A, 0x2E000000 */
private const val pio2_3t = 8.47842766036889956997e-32 /* 0x397B839A, 0x252049C1 */

internal fun __ieee754_rem_pio2(x: Double, y: DoubleArray): Int {
    var z: Double = 0.0
    var w: Double
    var t: Double
    var r: Double
    var fn: Double
    val tx: DoubleArray = DoubleArray(3)
    var e0: Int
    var i: Int
    var j: Int
    var nx: Int
    var n: Int
    var ix: Int
    var hx: Int

    hx = __HI(x)        /* high word of x */
    ix = hx and 0x7fffffff
    if (ix <= 0x3fe921fb)   /* |x| ~<= pi/4 , no need for reduction */ {
        y[0] = x; y[1] = 0.0; return 0
    }
    if (ix < 0x4002d97c) {  /* |x| < 3pi/4, special case with n=+-1 */
        if (hx > 0) {
            z = x - pio2_1
            if (ix != 0x3ff921fb) {    /* 33+53 bit pi is good enough */
                y[0] = z - pio2_1t
                y[1] = (z - y[0]) - pio2_1t
            } else {        /* near pi/2, use 33+33+53 bit pi */
                z -= pio2_2
                y[0] = z - pio2_2t
                y[1] = (z - y[0]) - pio2_2t
            }
            return 1
        } else {    /* negative x */
            z = x + pio2_1
            if (ix != 0x3ff921fb) {    /* 33+53 bit pi is good enough */
                y[0] = z + pio2_1t
                y[1] = (z - y[0]) + pio2_1t
            } else {        /* near pi/2, use 33+33+53 bit pi */
                z += pio2_2
                y[0] = z + pio2_2t
                y[1] = (z - y[0]) + pio2_2t
            }
            return -1
        }
    }
    if (ix <= 0x413921fb) { /* |x| ~<= 2^19*(pi/2), medium size */
        t = fabs(x)
        n = (t * invpio2 + half).toInt()
        fn = n.toDouble()
        r = t - fn * pio2_1
        w = fn * pio2_1t    /* 1st round good to 85 bit */
        if (n < 32 && ix != npio2_hw[n - 1]) {
            y[0] = r - w    /* quick check no cancellation */
        } else {
            j = ix shr 20
            y[0] = r - w
            i = j - (((__HI(y[0])) shr 20) and 0x7ff)
            if (i > 16) {  /* 2nd iteration needed, good to 118 */
                t = r
                w = fn * pio2_2
                r = t - w
                w = fn * pio2_2t - ((t - r) - w)
                y[0] = r - w
                i = j - (((__HI(y[0])) shr 20) and 0x7ff)
                if (i > 49) {    /* 3rd iteration need, 151 bits acc */
                    t = r    /* will cover all possible cases */
                    w = fn * pio2_3
                    r = t - w
                    w = fn * pio2_3t - ((t - r) - w)
                    y[0] = r - w
                }
            }
        }
        y[1] = (r - y[0]) - w
        if (hx < 0) {
            y[0] = -y[0]; y[1] = -y[1]; return -n
        } else return n
    }
    /* 
     * all other (large) arguments
     */
    if (ix >= 0x7ff00000) {        /* x is inf or NaN */
        y[1] = x - x
        y[0] = y[1]; return 0
    }
    /* set z = scalbn(|x|,ilogb(x)-23) */
    z = doubleSetWord(d = z, lo = __LO(x))
    e0 = (ix shr 20) - 1046    /* e0 = ilogb(z)-23; */
    z = doubleSetWord(d = z, hi = ix - (e0 shl 20))
    //for(i=0;i<2;i++) {
    i = 0
    while (i < 2) {
        tx[i] = (z.toInt()).toDouble()
        z = (z - tx[i]) * two24
        //--
        i++
    }
    tx[2] = z
    nx = 3
    while (tx[nx - 1] == zero) nx--    /* skip zero term */
    n = __kernel_rem_pio2(tx, y, e0, nx, 2, two_over_pi)
    if (hx < 0) {
        y[0] = -y[0]; y[1] = -y[1]; return -n
    }
    return n
}
