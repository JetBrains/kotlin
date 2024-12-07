/* @(#)k_rem_pio2.c 1.3 95/01/18 */
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

/*
 * __kernel_rem_pio2(x,y,e0,nx,prec,ipio2)
 * double x[],y[]; int e0,nx,prec; int ipio2[];
 * 
 * __kernel_rem_pio2 return the last three digits of N with 
 *		y = x - N*pi/2
 * so that |y| < pi/2.
 *
 * The method is to compute the integer (mod 8) and fraction parts of 
 * (2/pi)*x without doing the full multiplication. In general we
 * skip the part of the product that are known to be a huge integer (
 * more accurately, = 0 mod 8 ). Thus the number of operations are
 * independent of the exponent of the input.
 *
 * (2/pi) is represented by an array of 24-bit integers in ipio2[].
 *
 * Input parameters:
 * 	x[]	The input value (must be positive) is broken into nx 
 *		pieces of 24-bit integers in double precision format.
 *		x[i] will be the i-th 24 bit of x. The scaled exponent 
 *		of x[0] is given in input parameter e0 (i.e., x[0]*2^e0 
 *		match x's up to 24 bits.
 *
 *		Example of breaking a double positive z into x[0]+x[1]+x[2]:
 *			e0 = ilogb(z)-23
 *			z  = scalbn(z,-e0)
 *		for i = 0,1,2
 *			x[i] = floor(z)
 *			z    = (z-x[i])*2**24
 *
 *
 *	y[]	ouput result in an array of double precision numbers.
 *		The dimension of y[] is:
 *			24-bit  precision	1
 *			53-bit  precision	2
 *			64-bit  precision	2
 *			113-bit precision	3
 *		The actual value is the sum of them. Thus for 113-bit
 *		precison, one may have to do something like:
 *
 *		long double t,w,r_head, r_tail;
 *		t = (long double)y[2] + (long double)y[1];
 *		w = (long double)y[0];
 *		r_head = t+w;
 *		r_tail = w - (r_head - t);
 *
 *	e0	The exponent of x[0]
 *
 *	nx	dimension of x[]
 *
 *  	prec	an integer indicating the precision:
 *			0	24  bits (single)
 *			1	53  bits (double)
 *			2	64  bits (extended)
 *			3	113 bits (quad)
 *
 *	ipio2[]
 *		integer array, contains the (24*i)-th to (24*i+23)-th 
 *		bit of 2/pi after binary point. The corresponding 
 *		floating value is
 *
 *			ipio2[i] * 2^(-24(i+1)).
 *
 * External function:
 *	double scalbn(), floor();
 *
 *
 * Here is the description of some local variables:
 *
 * 	jk	jk+1 is the initial number of terms of ipio2[] needed
 *		in the computation. The recommended value is 2,3,4,
 *		6 for single, double, extended,and quad.
 *
 * 	jz	local integer variable indicating the number of 
 *		terms of ipio2[] used. 
 *
 *	jx	nx - 1
 *
 *	jv	index for pointing to the suitable ipio2[] for the
 *		computation. In general, we want
 *			( 2^e0*x[0] * ipio2[jv-1]*2^(-24jv) )/8
 *		is an integer. Thus
 *			e0-3-24*jv >= 0 or (e0-3)/24 >= jv
 *		Hence jv = max(0,(e0-3)/24).
 *
 *	jp	jp+1 is the number of terms in PIo2[] needed, jp = jk.
 *
 * 	q[]	double array with integral value, representing the
 *		24-bits chunk of the product of x and 2/pi.
 *
 *	q0	the corresponding exponent of q[0]. Note that the
 *		exponent for q[i] would be q0-24*i.
 *
 *	PIo2[]	double precision array, obtained by cutting pi/2
 *		into 24 bits chunks. 
 *
 *	f[]	ipio2[] in floating point 
 *
 *	iq[]	integer array by breaking up q[] in 24-bits chunk.
 *
 *	fq[]	final product of x*(2/pi) in fq[0],..,fq[jk]
 *
 *	ih	integer. If >0 it indicates q[] is >= 0.5, hence
 *		it also indicates the *sign* of the result.
 *
 */


/*
 * Constants:
 * The hexadecimal values are the intended ones for the following 
 * constants. The decimal values may be used, provided that the 
 * compiler will convert from decimal to binary accurately enough 
 * to produce the hexadecimal values shown.
 */

package kotlin.math.fdlibm

import kotlin.wasm.internal.wasm_f64_floor as floor

private val init_jk = intArrayOf(2, 3, 4, 6) /* initial value for jk */
private val PIo2 = doubleArrayOf(
    1.57079625129699707031e+00, /* 0x3FF921FB, 0x40000000 */
    7.54978941586159635335e-08, /* 0x3E74442D, 0x00000000 */
    5.39030252995776476554e-15, /* 0x3CF84698, Int.MIN_VALUE */
    3.28200341580791294123e-22, /* 0x3B78CC51, 0x60000000 */
    1.27065575308067607349e-29, /* 0x39F01B83, Int.MIN_VALUE */
    1.22933308981111328932e-36, /* 0x387A2520, 0x40000000 */
    2.73370053816464559624e-44, /* 0x36E38222, Int.MIN_VALUE */
    2.16741683877804819444e-51, /* 0x3569F31D, 0x00000000 */
)


private const val zero = 0.0
private const val one = 1.0
private const val two24 = 1.67772160000000000000e+07 /* 0x41700000, 0x00000000 */
private const val twon24 = 5.96046447753906250000e-08 /* 0x3E700000, 0x00000000 */

internal fun __kernel_rem_pio2(x: DoubleArray, y: DoubleArray, e0: Int, nx: Int, prec: Int, ipio2: IntArray): Int {
    var jz: Int
    var jx: Int
    var jv: Int
    var jp: Int
    var jk: Int
    var carry: Int
    var n: Int
    var iq: IntArray = IntArray(20)
    var i: Int
    var j: Int
    var k: Int
    var m: Int
    var q0: Int
    var ih: Int
    var z: Double
    var fw: Double
    var f: DoubleArray = DoubleArray(20)
    var fq: DoubleArray = DoubleArray(20)
    var q: DoubleArray = DoubleArray(20)

    /* initialize jk*/
    jk = init_jk[prec]
    jp = jk

    /* determine jx,jv,q0, note that 3>q0 */
    jx = nx - 1
    jv = (e0 - 3) / 24; if (jv < 0) jv = 0
    q0 = e0 - 24 * (jv + 1)

    /* set up f[0] to f[jx+jk] where f[jx+jk] = ipio2[jv+jk] */
    j = jv - jx; m = jx + jk
    //for(i=0;i<=m;i++,j++) {
    i = 0
    while (i <= m) {
        f[i] = if (j < 0) zero else ipio2[j].toDouble()
        //--
        i++; j++
    }

    /* compute q[0],q[1],...q[jk] */
    //for (i=0;i<=jk;i++) {
    i = 0
    while (i <= jk) {
        j = 0; fw = 0.0
        while (j <= jx) {
            fw += x[j] * f[jx + i - j]; q[i] = fw
            //--
            j++
        }
        //--
        i++
    }

    jz = jk
    goto@ while (true) {
        /* distill q[] into iq[] reversingly */
        //for(i=0,j=jz,z=q[jz];j>0;i++,j--) {
        i = 0; j = jz; z = q[jz]
        while (j > 0) {
            fw = ((twon24 * z).toInt()).toDouble()
            iq[i] = (z - two24 * fw).toInt()
            z = q[j - 1] + fw
            //--
            i++; j--
        }

        /* compute n */
        z = scalbn(z, q0)        /* actual value of z */
        z -= 8.0 * floor(z * 0.125)        /* trim off integer >= 8 */
        n = z.toInt()
        z -= n.toDouble()
        ih = 0
        if (q0 > 0) {    /* need iq[jz-1] to determine n */
            i = (iq[jz - 1] shr (24 - q0)); n += i
            iq[jz - 1] -= i shl (24 - q0)
            ih = iq[jz - 1] shr (23 - q0)
        } else if (q0 == 0) ih = iq[jz - 1] shr 23
        else if (z >= 0.5) ih = 2

        if (ih > 0) {    /* q > 0.5 */
            n += 1; carry = 0
            //for(i=0;i<jz ;i++) {	/* compute 1-q */
            i = 0
            while (i < jz) {    /* compute 1-q */
                j = iq[i]
                if (carry == 0) {
                    if (j != 0) {
                        carry = 1; iq[i] = 0x1000000 - j
                    }
                } else iq[i] = 0xffffff - j
                //--
                i++
            }
            if (q0 > 0) {        /* rare case: chance is 1 in 12 */
                when (q0) {
                    1 ->
                        iq[jz - 1] = iq[jz - 1] and 0x7fffff
                    2 ->
                        iq[jz - 1] = iq[jz - 1] and 0x3fffff
                }
            }
            if (ih == 2) {
                z = one - z
                if (carry != 0) z -= scalbn(one, q0)
            }
        }

        /* check if recomputation is needed */
        if (z == zero) {
            j = 0
            //for (i=jz-1;i>=jk;i--) {
            i = jz - 1
            while (i >= jk) {
                j = j or iq[i]
                //--
                i--
            }
            if (j == 0) { /* need recomputation */
                //for(k=1;iq[jk-k]==0;k++);   /* k = no. of terms needed */
                k = 1
                while (iq[jk - k] == 0) { /* k = no. of terms needed */
                    //--
                    k++
                }

                //for(i=jz+1;i<=jz+k;i++) {   /* add q[jz+1] to q[jz+k] */
                i = jz + 1
                while (i <= jz + k) {   /* add q[jz+1] to q[jz+k] */
                    f[jx + i] = ipio2[jv + i].toDouble()
                    //for(j=0,fw=0.0;j<=jx;j++) {
                    j = 0; fw = 0.0
                    while (j <= jx) {
                        fw += x[j] * f[jx + i - j]
                        //--
                        j++
                    }
                    q[i] = fw
                    //--
                    i++
                }
                jz += k
                continue@goto
            }
        }
        break@goto
    }

    /* chop off zero terms */
    if (z == 0.0) {
        jz -= 1; q0 -= 24
        while (iq[jz] == 0) {
            jz--; q0 -= 24
        }
    } else { /* break z into 24-bit if necessary */
        z = scalbn(z, -q0)
        if (z >= two24) {
            fw = ((twon24 * z).toInt()).toDouble()
            iq[jz] = (z - two24 * fw).toInt()
            jz += 1; q0 += 24
            iq[jz] = fw.toInt()
        } else iq[jz] = z.toInt()
    }

    /* convert integer "bit" chunk to floating-point value */
    fw = scalbn(one, q0)
    //for(i=jz;i>=0;i--) {
    i = jz
    while (i >= 0) {
        q[i] = fw * iq[i].toDouble(); fw *= twon24
        //--
        i--
    }

    /* compute PIo2[0,...,jp]*q[jz,...,0] */
    //for(i=jz;i>=0;i--) {
    i = jz
    while (i >= 0) {
        //for(fw=0.0,k=0;k<=jp&&k<=jz-i;k++) {
        fw = 0.0; k = 0
        while (k <= jp && k <= jz - i) {
            fw += PIo2[k] * q[i + k]
            //--
            k++
        }
        fq[jz - i] = fw
        //--
        i--
    }

    /* compress fq[] into y[] */
    when (prec) {
        0 -> {
            fw = 0.0
            //for (i= jz;i >= 0;i--) {
            i = jz
            while (i >= 0) {
                fw += fq[i]
                //--
                i--
            }
            y[0] = if (ih == 0) fw else -fw
        }
        1, 2 -> {
            fw = 0.0
            //for (i= jz;i >= 0;i--) {
            i = jz
            while (i >= 0) {
                fw += fq[i]
                //--
                i--
            }
            y[0] = if (ih == 0) fw else -fw
            fw = fq[0] - fw
            //for (i= 1;i <= jz;i++) {
            i = 1
            while (i <= jz) {
                fw += fq[i]
                //--
                i++
            }
            y[1] = if (ih == 0) fw else -fw
        }
        3 -> {    /* painful */
            //for (i= jz;i > 0;i--) {
            i = jz
            while (i > 0) {
                fw = fq[i - 1] + fq[i]
                fq[i] += fq[i - 1] - fw
                fq[i - 1] = fw
                //--
                i--
            }
            //for (i= jz;i > 1;i--) {
            i = jz
            while (i > 1) {
                fw = fq[i - 1] + fq[i]
                fq[i] += fq[i - 1] - fw
                fq[i - 1] = fw
                //--
                i--
            }
            //for (fw= 0.0, i = jz;i >= 2;i--) {
            fw = 0.0; i = jz
            while (i >= 2) {
                fw += fq[i]
                //--
                i--
            }
            if (ih == 0) {
                y[0] = fq[0]; y[1] = fq[1]; y[2] = fw
            } else {
                y[0] = -fq[0]; y[1] = -fq[1]; y[2] = -fw
            }
        }
    }
    return n and 7
}
