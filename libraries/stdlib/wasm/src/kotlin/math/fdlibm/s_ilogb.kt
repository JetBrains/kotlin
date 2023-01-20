/* @(#)s_ilogb.c 1.3 95/01/18 */
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

/* ilogb(x: Double)
 * return the binary exponent of non-zero x
 * ilogb(0) = 0x80000001
 * ilogb(inf/NaN) = 0x7fffffff (no signal is raised)
 */

package kotlin.math.fdlibm

internal fun ilogb(x: Double): Int {
    var hx: Int
    var lx: Int
    var ix: Int

    hx = (__HI(x)) and 0x7fffffff    /* high word of x */
    if (hx < 0x00100000) {
        lx = __LO(x)
        if ((hx or lx) == 0)
            return 0x80000001.toInt()    /* ilogb(0) = 0x80000001 */
        else            /* subnormal x */
            if (hx == 0) {
                //for (ix = -1043; lx>0; lx<<=1) {
                ix = -1043
                while (lx > 0) {
                    ix -= 1
                    //--
                    lx = lx shl 1
                }
            } else {
                //for (ix = -1022,hx<<=11; hx>0; hx<<=1) {
                ix = -1022; hx = hx shl 11
                while (hx > 0) {
                    ix -= 1
                    //--
                    hx = hx shl 1
                }
            }
        return ix
    } else if (hx < 0x7ff00000) return (hx shr 20) - 1023
    else return 0x7fffffff
}
