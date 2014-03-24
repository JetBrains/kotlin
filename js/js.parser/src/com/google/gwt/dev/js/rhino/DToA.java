/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1997-1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s):
 * Waldemar Horwat
 * Roger Lawrence
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Public License (the "GPL"), in which case the
 * provisions of the GPL are applicable instead of those above.
 * If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use your
 * version of this file under the NPL, indicate your decision by
 * deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this
 * file under either the NPL or the GPL.
 */
// Modified by Google

/****************************************************************
  *
  * The author of this software is David M. Gay.
  *
  * Copyright (c) 1991, 2000, 2001 by Lucent Technologies.
  *
  * Permission to use, copy, modify, and distribute this software for any
  * purpose without fee is hereby granted, provided that this entire notice
  * is included in all copies of any software which is or includes a copy
  * or modification of this software and in all copies of the supporting
  * documentation for such software.
  *
  * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
  * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHOR NOR LUCENT MAKES ANY
  * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
  * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
  *
  ***************************************************************/

package com.google.gwt.dev.js.rhino;

import java.math.BigInteger;

class DToA {


/* "-0.0000...(1073 zeros after decimal point)...0001\0" is the longest string that we could produce,
 * which occurs when printing -5e-324 in binary.  We could compute a better estimate of the size of
 * the output string and malloc fewer bytes depending on d and base, but why bother? */

    static final int DTOBASESTR_BUFFER_SIZE = 1078;

    static char BASEDIGIT(int digit) {
        return (char)((digit >= 10) ? 'a' - 10 + digit : '0' + digit);
    }

    static final int
        DTOSTR_STANDARD = 0,              /* Either fixed or exponential format; round-trip */
        DTOSTR_STANDARD_EXPONENTIAL = 1,  /* Always exponential format; round-trip */
        DTOSTR_FIXED = 2,                 /* Round to <precision> digits after the decimal point; exponential if number is large */
        DTOSTR_EXPONENTIAL = 3,           /* Always exponential format; <precision> significant digits */
        DTOSTR_PRECISION = 4;             /* Either fixed or exponential format; <precision> significant digits */


    static final int Frac_mask = 0xfffff;
    static final int Exp_shift = 20;
    static final int Exp_msk1 = 0x100000;
    static final int Bias = 1023;
    static final int P = 53;

    static final int Exp_shift1 = 20;
    static final int Exp_mask  = 0x7ff00000;
    static final int Bndry_mask  = 0xfffff;
    static final int Log2P = 1;

    static final int Sign_bit = 0x80000000;
    static final int Exp_11  = 0x3ff00000;
    static final int Ten_pmax = 22;
    static final int Quick_max = 14;
    static final int Bletch = 0x10;
    static final int Frac_mask1 = 0xfffff;
    static final int Int_max = 14;
    static final int n_bigtens = 5;


    static final double tens[] = {
        1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9,
        1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19,
        1e20, 1e21, 1e22
    };

    static final double bigtens[] = { 1e16, 1e32, 1e64, 1e128, 1e256 };

    static int lo0bits(int y)
    {
        int k;
        int x = y;

        if ((x & 7) != 0) {
            if ((x & 1) != 0)
                return 0;
            if ((x & 2) != 0) {
                return 1;
            }
            return 2;
        }
        k = 0;
        if ((x & 0xffff) == 0) {
            k = 16;
            x >>>= 16;
        }
        if ((x & 0xff) == 0) {
            k += 8;
            x >>>= 8;
        }
        if ((x & 0xf) == 0) {
            k += 4;
            x >>>= 4;
        }
        if ((x & 0x3) == 0) {
            k += 2;
            x >>>= 2;
        }
        if ((x & 1) == 0) {
            k++;
            x >>>= 1;
            if ((x & 1) == 0)
                return 32;
        }
        return k;
    }

    /* Return the number (0 through 32) of most significant zero bits in x. */
    static int hi0bits(int x)
    {
        int k = 0;

        if ((x & 0xffff0000) == 0) {
            k = 16;
            x <<= 16;
        }
        if ((x & 0xff000000) == 0) {
            k += 8;
            x <<= 8;
        }
        if ((x & 0xf0000000) == 0) {
            k += 4;
            x <<= 4;
        }
        if ((x & 0xc0000000) == 0) {
            k += 2;
            x <<= 2;
        }
        if ((x & 0x80000000) == 0) {
            k++;
            if ((x & 0x40000000) == 0)
                return 32;
        }
        return k;
    }

    static void stuffBits(byte bits[], int offset, int val)
    {
        bits[offset] = (byte)(val >> 24);
        bits[offset + 1] = (byte)(val >> 16);
        bits[offset + 2] = (byte)(val >> 8);
        bits[offset + 3] = (byte)(val);
    }

    /* Convert d into the form b*2^e, where b is an odd integer.  b is the returned
     * Bigint and e is the returned binary exponent.  Return the number of significant
     * bits in b in bits.  d must be finite and nonzero. */
    static BigInteger d2b(double d, int[] e, int[] bits)
    {
        byte dbl_bits[];
        int i, k, y, z, de;
        long dBits = Double.doubleToLongBits(d);
        int d0 = (int)(dBits >>> 32);
        int d1 = (int)(dBits);

        z = d0 & Frac_mask;
        d0 &= 0x7fffffff;   /* clear sign bit, which we ignore */

        if ((de = (int)(d0 >>> Exp_shift)) != 0)
            z |= Exp_msk1;

        if ((y = d1) != 0) {
            dbl_bits = new byte[8];
            k = lo0bits(y);
            y >>>= k;
            if (k != 0) {
                stuffBits(dbl_bits, 4, y | z << (32 - k));
                z >>= k;
            }
            else
                stuffBits(dbl_bits, 4, y);
            stuffBits(dbl_bits, 0, z);
            i = (z != 0) ? 2 : 1;
        }
        else {
    //        JS_ASSERT(z);
            dbl_bits = new byte[4];
            k = lo0bits(z);
            z >>>= k;
            stuffBits(dbl_bits, 0, z);
            k += 32;
            i = 1;
        }
        if (de != 0) {
            e[0] = de - Bias - (P-1) + k;
            bits[0] = P - k;
        }
        else {
            e[0] = de - Bias - (P-1) + 1 + k;
            bits[0] = 32*i - hi0bits(z);
        }
        return new BigInteger(dbl_bits);
    }

    public static String JS_dtobasestr(int base, double d)
    {
        char[] buffer;       /* The output string */
        int p;               /* index to current position in the buffer */
        int pInt;            /* index to the beginning of the integer part of the string */

        int q;
        int digit;
        double di;           /* d truncated to an integer */
        double df;           /* The fractional part of d */

//        JS_ASSERT(base >= 2 && base <= 36);

        buffer = new char[DTOBASESTR_BUFFER_SIZE];

        p = 0;
        if (d < 0.0) {
            buffer[p++] = '-';
            d = -d;
        }

        /* Check for Infinity and NaN */
        if (Double.isNaN(d))
            return "NaN";
        else
            if (Double.isInfinite(d))
                return "Infinity";

        /* Output the integer part of d with the digits in reverse order. */
        pInt = p;
        di = (int)d;
        BigInteger b = BigInteger.valueOf((int)di);
        String intDigits = b.toString(base);
        intDigits.getChars(0, intDigits.length(), buffer, p);
        p += intDigits.length();

        df = d - di;
        if (df != 0.0) {
            /* We have a fraction. */
            buffer[p++] = '.';

            long dBits = Double.doubleToLongBits(d);
            int word0 = (int)(dBits >> 32);
            int word1 = (int)(dBits);

            int[] e = new int[1];
            int[] bbits = new int[1];

            b = d2b(df, e, bbits);
//            JS_ASSERT(e < 0);
            /* At this point df = b * 2^e.  e must be less than zero because 0 < df < 1. */

            int s2 = -(word0 >>> Exp_shift1 & Exp_mask >> Exp_shift1);
            if (s2 == 0)
                s2 = -1;
            s2 += Bias + P;
            /* 1/2^s2 = (nextDouble(d) - d)/2 */
//            JS_ASSERT(-s2 < e);
            BigInteger mlo = BigInteger.valueOf(1);
            BigInteger mhi = mlo;
            if ((word1 == 0) && ((word0 & Bndry_mask) == 0)
                && ((word0 & (Exp_mask & Exp_mask << 1)) != 0)) {
                /* The special case.  Here we want to be within a quarter of the last input
                   significant digit instead of one half of it when the output string's value is less than d.  */
                s2 += Log2P;
                mhi = BigInteger.valueOf(1<<Log2P);
            }

            b = b.shiftLeft(e[0] + s2);
            BigInteger s = BigInteger.valueOf(1);
            s = s.shiftLeft(s2);
            /* At this point we have the following:
             *   s = 2^s2;
             *   1 > df = b/2^s2 > 0;
             *   (d - prevDouble(d))/2 = mlo/2^s2;
             *   (nextDouble(d) - d)/2 = mhi/2^s2. */
            BigInteger bigBase = BigInteger.valueOf(base);

            boolean done = false;
            do {
                b = b.multiply(bigBase);
                BigInteger[] divResult = b.divideAndRemainder(s);
                b = divResult[1];
                digit = (char)(divResult[0].intValue());
                if (mlo == mhi)
                    mlo = mhi = mlo.multiply(bigBase);
                else {
                    mlo = mlo.multiply(bigBase);
                    mhi = mhi.multiply(bigBase);
                }

                /* Do we yet have the shortest string that will round to d? */
                int j = b.compareTo(mlo);
                /* j is b/2^s2 compared with mlo/2^s2. */
                BigInteger delta = s.subtract(mhi);
                int j1 = (delta.signum() <= 0) ? 1 : b.compareTo(delta);
                /* j1 is b/2^s2 compared with 1 - mhi/2^s2. */
                if (j1 == 0 && ((word1 & 1) == 0)) {
                    if (j > 0)
                        digit++;
                    done = true;
                } else
                if (j < 0 || (j == 0 && ((word1 & 1) == 0))) {
                    if (j1 > 0) {
                        /* Either dig or dig+1 would work here as the least significant digit.
                           Use whichever would produce an output value closer to d. */
                        b = b.shiftLeft(1);
                        j1 = b.compareTo(s);
                        if (j1 > 0) /* The even test (|| (j1 == 0 && (digit & 1))) is not here because it messes up odd base output
                                     * such as 3.5 in base 3.  */
                            digit++;
                    }
                    done = true;
                } else if (j1 > 0) {
                    digit++;
                    done = true;
                }
//                JS_ASSERT(digit < (uint32)base);
                buffer[p++] = BASEDIGIT(digit);
            } while (!done);
        }

        return new String(buffer, 0, p);
    }

    /* dtoa for IEEE arithmetic (dmg): convert double to ASCII string.
     *
     * Inspired by "How to Print Floating-Point Numbers Accurately" by
     * Guy L. Steele, Jr. and Jon L. White [Proc. ACM SIGPLAN '90, pp. 92-101].
     *
     * Modifications:
     *  1. Rather than iterating, we use a simple numeric overestimate
     *     to determine k = floor(log10(d)).  We scale relevant
     *     quantities using O(log2(k)) rather than O(k) multiplications.
     *  2. For some modes > 2 (corresponding to ecvt and fcvt), we don't
     *     try to generate digits strictly left to right.  Instead, we
     *     compute with fewer bits and propagate the carry if necessary
     *     when rounding the final digit up.  This is often faster.
     *  3. Under the assumption that input will be rounded nearest,
     *     mode 0 renders 1e23 as 1e23 rather than 9.999999999999999e22.
     *     That is, we allow equality in stopping tests when the
     *     round-nearest rule will give the same floating-point value
     *     as would satisfaction of the stopping test with strict
     *     inequality.
     *  4. We remove common factors of powers of 2 from relevant
     *     quantities.
     *  5. When converting floating-point integers less than 1e16,
     *     we use floating-point arithmetic rather than resorting
     *     to multiple-precision integers.
     *  6. When asked to produce fewer than 15 digits, we first try
     *     to get by with floating-point arithmetic; we resort to
     *     multiple-precision integer arithmetic only if we cannot
     *     guarantee that the floating-point calculation has given
     *     the correctly rounded result.  For k requested digits and
     *     "uniformly" distributed input, the probability is
     *     something like 10^(k-15) that we must resort to the Long
     *     calculation.
     */

    static int word0(double d)
    {
        long dBits = Double.doubleToLongBits(d);
        return (int)(dBits >> 32);
    }

    static double setWord0(double d, int i)
    {
        long dBits = Double.doubleToLongBits(d);
        dBits = ((long)i << 32) | (dBits & 0x0FFFFFFFFL);
        return Double.longBitsToDouble(dBits);
    }

    static int word1(double d)
    {
        long dBits = Double.doubleToLongBits(d);
        return (int)(dBits);
    }

    /* Return b * 5^k.  k must be nonnegative. */
    // XXXX the C version built a cache of these
    static BigInteger pow5mult(BigInteger b, int k)
    {
        return b.multiply(BigInteger.valueOf(5).pow(k));
    }

    static boolean roundOff(StringBuffer buf)
    {
        char lastCh;
        while ((lastCh = buf.charAt(buf.length() - 1)) == '9') {
            buf.setLength(buf.length() - 1);
            if (buf.length() == 0) {
                return true;
            }
        }
        buf.append((char)(lastCh + 1));
        return false;
    }

    /* Always emits at least one digit. */
    /* If biasUp is set, then rounding in modes 2 and 3 will round away from zero
     * when the number is exactly halfway between two representable values.  For example,
     * rounding 2.5 to zero digits after the decimal point will return 3 and not 2.
     * 2.49 will still round to 2, and 2.51 will still round to 3. */
    /* bufsize should be at least 20 for modes 0 and 1.  For the other modes,
     * bufsize should be two greater than the maximum number of output characters expected. */
    static int
    JS_dtoa(double d, int mode, boolean biasUp, int ndigits,
                    boolean[] sign, StringBuffer buf)
    {
        /*  Arguments ndigits, decpt, sign are similar to those
            of ecvt and fcvt; trailing zeros are suppressed from
            the returned string.  If not null, *rve is set to point
            to the end of the return value.  If d is +-Infinity or NaN,
            then *decpt is set to 9999.

            mode:
            0 ==> shortest string that yields d when read in
            and rounded to nearest.
            1 ==> like 0, but with Steele & White stopping rule;
            e.g. with IEEE P754 arithmetic , mode 0 gives
            1e23 whereas mode 1 gives 9.999999999999999e22.
            2 ==> max(1,ndigits) significant digits.  This gives a
            return value similar to that of ecvt, except
            that trailing zeros are suppressed.
            3 ==> through ndigits past the decimal point.  This
            gives a return value similar to that from fcvt,
            except that trailing zeros are suppressed, and
            ndigits can be negative.
            4-9 should give the same return values as 2-3, i.e.,
            4 <= mode <= 9 ==> same return as mode
            2 + (mode & 1).  These modes are mainly for
            debugging; often they run slower but sometimes
            faster than modes 2-3.
            4,5,8,9 ==> left-to-right digit generation.
            6-9 ==> don't try fast floating-point estimate
            (if applicable).

            Values of mode other than 0-9 are treated as mode 0.

            Sufficient space is allocated to the return value
            to hold the suppressed trailing zeros.
        */

        int b2, b5, i, ieps, ilim, ilim0, ilim1,
            j, j1, k, k0, m2, m5, s2, s5;
        char dig;
        long L;
        long x;
        BigInteger b, b1, delta, mlo, mhi, S;
        int[] be = new int[1];
        int[] bbits = new int[1];
        double d2, ds, eps;
        boolean spec_case, denorm, k_check, try_quick, leftright;

        if ((word0(d) & Sign_bit) != 0) {
            /* set sign for everything, including 0's and NaNs */
            sign[0] = true;
            // word0(d) &= ~Sign_bit;  /* clear sign bit */
            d = setWord0(d, word0(d) & ~Sign_bit);
        }
        else
            sign[0] = false;

        if ((word0(d) & Exp_mask) == Exp_mask) {
            /* Infinity or NaN */
            buf.append(((word1(d) == 0) && ((word0(d) & Frac_mask) == 0)) ? "Infinity" : "NaN");
            return 9999;
        }
        if (d == 0) {
//          no_digits:
            buf.setLength(0);
            buf.append('0');        /* copy "0" to buffer */
            return 1;
        }

        b = d2b(d, be, bbits);
        if ((i = (int)(word0(d) >>> Exp_shift1 & (Exp_mask>>Exp_shift1))) != 0) {
            d2 = setWord0(d, (word0(d) & Frac_mask1) | Exp_11);
            /* log(x)   ~=~ log(1.5) + (x-1.5)/1.5
             * log10(x)  =  log(x) / log(10)
             *      ~=~ log(1.5)/log(10) + (x-1.5)/(1.5*log(10))
             * log10(d) = (i-Bias)*log(2)/log(10) + log10(d2)
             *
             * This suggests computing an approximation k to log10(d) by
             *
             * k = (i - Bias)*0.301029995663981
             *  + ( (d2-1.5)*0.289529654602168 + 0.176091259055681 );
             *
             * We want k to be too large rather than too small.
             * The error in the first-order Taylor series approximation
             * is in our favor, so we just round up the constant enough
             * to compensate for any error in the multiplication of
             * (i - Bias) by 0.301029995663981; since |i - Bias| <= 1077,
             * and 1077 * 0.30103 * 2^-52 ~=~ 7.2e-14,
             * adding 1e-13 to the constant term more than suffices.
             * Hence we adjust the constant term to 0.1760912590558.
             * (We could get a more accurate k by invoking log10,
             *  but this is probably not worthwhile.)
             */
            i -= Bias;
            denorm = false;
        }
        else {
            /* d is denormalized */
            i = bbits[0] + be[0] + (Bias + (P-1) - 1);
            x = (i > 32) ? word0(d) << (64 - i) | word1(d) >>> (i - 32) : word1(d) << (32 - i);
//            d2 = x;
//            word0(d2) -= 31*Exp_msk1; /* adjust exponent */
            d2 = setWord0(x, word0(x) - 31*Exp_msk1);
            i -= (Bias + (P-1) - 1) + 1;
            denorm = true;
        }
        /* At this point d = f*2^i, where 1 <= f < 2.  d2 is an approximation of f. */
        ds = (d2-1.5)*0.289529654602168 + 0.1760912590558 + i*0.301029995663981;
        k = (int)ds;
        if (ds < 0.0 && ds != k)
            k--;    /* want k = floor(ds) */
        k_check = true;
        if (k >= 0 && k <= Ten_pmax) {
            if (d < tens[k])
                k--;
            k_check = false;
        }
        /* At this point floor(log10(d)) <= k <= floor(log10(d))+1.
           If k_check is zero, we're guaranteed that k = floor(log10(d)). */
        j = bbits[0] - i - 1;
        /* At this point d = b/2^j, where b is an odd integer. */
        if (j >= 0) {
            b2 = 0;
            s2 = j;
        }
        else {
            b2 = -j;
            s2 = 0;
        }
        if (k >= 0) {
            b5 = 0;
            s5 = k;
            s2 += k;
        }
        else {
            b2 -= k;
            b5 = -k;
            s5 = 0;
        }
        /* At this point d/10^k = (b * 2^b2 * 5^b5) / (2^s2 * 5^s5), where b is an odd integer,
           b2 >= 0, b5 >= 0, s2 >= 0, and s5 >= 0. */
        if (mode < 0 || mode > 9)
            mode = 0;
        try_quick = true;
        if (mode > 5) {
            mode -= 4;
            try_quick = false;
        }
        leftright = true;
        ilim = ilim1 = 0;
        switch(mode) {
            case 0:
            case 1:
                ilim = ilim1 = -1;
                i = 18;
                ndigits = 0;
                break;
            case 2:
                leftright = false;
                /* no break */
            case 4:
                if (ndigits <= 0)
                    ndigits = 1;
                ilim = ilim1 = i = ndigits;
                break;
            case 3:
                leftright = false;
                /* no break */
            case 5:
                i = ndigits + k + 1;
                ilim = i;
                ilim1 = i - 1;
                if (i <= 0)
                    i = 1;
        }
        /* ilim is the maximum number of significant digits we want, based on k and ndigits. */
        /* ilim1 is the maximum number of significant digits we want, based on k and ndigits,
           when it turns out that k was computed too high by one. */

        boolean fast_failed = false;
        if (ilim >= 0 && ilim <= Quick_max && try_quick) {

            /* Try to get by with floating-point arithmetic. */

            i = 0;
            d2 = d;
            k0 = k;
            ilim0 = ilim;
            ieps = 2; /* conservative */
            /* Divide d by 10^k, keeping track of the roundoff error and avoiding overflows. */
            if (k > 0) {
                ds = tens[k&0xf];
                j = k >> 4;
                if ((j & Bletch) != 0) {
                    /* prevent overflows */
                    j &= Bletch - 1;
                    d /= bigtens[n_bigtens-1];
                    ieps++;
                }
                for(; (j != 0); j >>= 1, i++)
                    if ((j & 1) != 0) {
                        ieps++;
                        ds *= bigtens[i];
                    }
                d /= ds;
            }
            else if ((j1 = -k) != 0) {
                d *= tens[j1 & 0xf];
                for(j = j1 >> 4; (j != 0); j >>= 1, i++)
                    if ((j & 1) != 0) {
                        ieps++;
                        d *= bigtens[i];
                    }
            }
            /* Check that k was computed correctly. */
            if (k_check && d < 1.0 && ilim > 0) {
                if (ilim1 <= 0)
                    fast_failed = true;
                else {
                    ilim = ilim1;
                    k--;
                    d *= 10.;
                    ieps++;
                }
            }
            /* eps bounds the cumulative error. */
//            eps = ieps*d + 7.0;
//            word0(eps) -= (P-1)*Exp_msk1;
            eps = ieps*d + 7.0;
            eps = setWord0(eps, word0(eps) - (P-1)*Exp_msk1);
            if (ilim == 0) {
                S = mhi = null;
                d -= 5.0;
                if (d > eps) {
                    buf.append('1');
                    k++;
                    return k + 1;
                }
                if (d < -eps) {
                    buf.setLength(0);
                    buf.append('0');        /* copy "0" to buffer */
                    return 1;
                }
                fast_failed = true;
            }
            if (!fast_failed) {
                fast_failed = true;
                if (leftright) {
                    /* Use Steele & White method of only
                     * generating digits needed.
                     */
                    eps = 0.5/tens[ilim-1] - eps;
                    for(i = 0;;) {
                        L = (long)d;
                        d -= L;
                        buf.append((char)('0' + L));
                        if (d < eps) {
                            return k + 1;
                        }
                        if (1.0 - d < eps) {
//                            goto bump_up;
                                char lastCh;
                                while (true) {
                                    lastCh = buf.charAt(buf.length() - 1);
                                    buf.setLength(buf.length() - 1);
                                    if (lastCh != '9') break;
                                    if (buf.length() == 0) {
                                        k++;
                                        lastCh = '0';
                                        break;
                                    }
                                }
                                buf.append((char)(lastCh + 1));
                                return k + 1;
                        }
                        if (++i >= ilim)
                            break;
                        eps *= 10.0;
                        d *= 10.0;
                    }
                }
                else {
                    /* Generate ilim digits, then fix them up. */
                    eps *= tens[ilim-1];
                    for(i = 1;; i++, d *= 10.0) {
                        L = (long)d;
                        d -= L;
                        buf.append((char)('0' + L));
                        if (i == ilim) {
                            if (d > 0.5 + eps) {
//                                goto bump_up;
                                char lastCh;
                                while (true) {
                                    lastCh = buf.charAt(buf.length() - 1);
                                    buf.setLength(buf.length() - 1);
                                    if (lastCh != '9') break;
                                    if (buf.length() == 0) {
                                        k++;
                                        lastCh = '0';
                                        break;
                                    }
                                }
                                buf.append((char)(lastCh + 1));
                                return k + 1;
                            }
                            else
                                if (d < 0.5 - eps) {
                                    while (buf.charAt(buf.length() - 1) == '0')
                                        buf.setLength(buf.length() - 1);
//                                    while(*--s == '0') ;
//                                    s++;
                                    return k + 1;
                                }
                            break;
                        }
                    }
                }
            }
            if (fast_failed) {
                buf.setLength(0);
                d = d2;
                k = k0;
                ilim = ilim0;
            }
        }

        /* Do we have a "small" integer? */

        if (be[0] >= 0 && k <= Int_max) {
            /* Yes. */
            ds = tens[k];
            if (ndigits < 0 && ilim <= 0) {
                S = mhi = null;
                if (ilim < 0 || d < 5*ds || (!biasUp && d == 5*ds)) {
                    buf.setLength(0);
                    buf.append('0');        /* copy "0" to buffer */
                    return 1;
                }
                buf.append('1');
                k++;
                return k + 1;
            }
            for(i = 1;; i++) {
                L = (long) (d / ds);
                d -= L*ds;
                buf.append((char)('0' + L));
                if (i == ilim) {
                    d += d;
                    if ((d > ds) || (d == ds && (((L & 1) != 0) || biasUp))) {
//                    bump_up:
//                        while(*--s == '9')
//                            if (s == buf) {
//                                k++;
//                                *s = '0';
//                                break;
//                            }
//                        ++*s++;
                        char lastCh;
                        while (true) {
                            lastCh = buf.charAt(buf.length() - 1);
                            buf.setLength(buf.length() - 1);
                            if (lastCh != '9') break;
                            if (buf.length() == 0) {
                                k++;
                                lastCh = '0';
                                break;
                            }
                        }
                        buf.append((char)(lastCh + 1));
                    }
                    break;
                }
                d *= 10.0;
                if (d == 0)
                    break;
            }
            return k + 1;
        }

        m2 = b2;
        m5 = b5;
        mhi = mlo = null;
        if (leftright) {
            if (mode < 2) {
                i = (denorm) ? be[0] + (Bias + (P-1) - 1 + 1) : 1 + P - bbits[0];
                /* i is 1 plus the number of trailing zero bits in d's significand. Thus,
                   (2^m2 * 5^m5) / (2^(s2+i) * 5^s5) = (1/2 lsb of d)/10^k. */
            }
            else {
                j = ilim - 1;
                if (m5 >= j)
                    m5 -= j;
                else {
                    s5 += j -= m5;
                    b5 += j;
                    m5 = 0;
                }
                if ((i = ilim) < 0) {
                    m2 -= i;
                    i = 0;
                }
                /* (2^m2 * 5^m5) / (2^(s2+i) * 5^s5) = (1/2 * 10^(1-ilim))/10^k. */
            }
            b2 += i;
            s2 += i;
            mhi = BigInteger.valueOf(1);
            /* (mhi * 2^m2 * 5^m5) / (2^s2 * 5^s5) = one-half of last printed (when mode >= 2) or
               input (when mode < 2) significant digit, divided by 10^k. */
        }
        /* We still have d/10^k = (b * 2^b2 * 5^b5) / (2^s2 * 5^s5).  Reduce common factors in
           b2, m2, and s2 without changing the equalities. */
        if (m2 > 0 && s2 > 0) {
            i = (m2 < s2) ? m2 : s2;
            b2 -= i;
            m2 -= i;
            s2 -= i;
        }

        /* Fold b5 into b and m5 into mhi. */
        if (b5 > 0) {
            if (leftright) {
                if (m5 > 0) {
                    mhi = pow5mult(mhi, m5);
                    b1 = mhi.multiply(b);
                    b = b1;
                }
                if ((j = b5 - m5) != 0)
                    b = pow5mult(b, j);
            }
            else
                b = pow5mult(b, b5);
        }
        /* Now we have d/10^k = (b * 2^b2) / (2^s2 * 5^s5) and
           (mhi * 2^m2) / (2^s2 * 5^s5) = one-half of last printed or input significant digit, divided by 10^k. */

        S = BigInteger.valueOf(1);
        if (s5 > 0)
            S = pow5mult(S, s5);
        /* Now we have d/10^k = (b * 2^b2) / (S * 2^s2) and
           (mhi * 2^m2) / (S * 2^s2) = one-half of last printed or input significant digit, divided by 10^k. */

        /* Check for special case that d is a normalized power of 2. */
        spec_case = false;
        if (mode < 2) {
            if ( (word1(d) == 0) && ((word0(d) & Bndry_mask) == 0)
                && ((word0(d) & (Exp_mask & Exp_mask << 1)) != 0)
                ) {
                /* The special case.  Here we want to be within a quarter of the last input
                   significant digit instead of one half of it when the decimal output string's value is less than d.  */
                b2 += Log2P;
                s2 += Log2P;
                spec_case = true;
            }
        }

        /* Arrange for convenient computation of quotients:
         * shift left if necessary so divisor has 4 leading 0 bits.
         *
         * Perhaps we should just compute leading 28 bits of S once
         * and for all and pass them and a shift to quorem, so it
         * can do shifts and ors to compute the numerator for q.
         */
        byte [] S_bytes = S.toByteArray();
        int S_hiWord = 0;
        for (int idx = 0; idx < 4; idx++) {
            S_hiWord = (S_hiWord << 8);
            if (idx < S_bytes.length)
                S_hiWord |= (S_bytes[idx] & 0xFF);
        }
        if ((i = (((s5 != 0) ? 32 - hi0bits(S_hiWord) : 1) + s2) & 0x1f) != 0)
            i = 32 - i;
        /* i is the number of leading zero bits in the most significant word of S*2^s2. */
        if (i > 4) {
            i -= 4;
            b2 += i;
            m2 += i;
            s2 += i;
        }
        else if (i < 4) {
            i += 28;
            b2 += i;
            m2 += i;
            s2 += i;
        }
        /* Now S*2^s2 has exactly four leading zero bits in its most significant word. */
        if (b2 > 0)
            b = b.shiftLeft(b2);
        if (s2 > 0)
            S = S.shiftLeft(s2);
        /* Now we have d/10^k = b/S and
           (mhi * 2^m2) / S = maximum acceptable error, divided by 10^k. */
        if (k_check) {
            if (b.compareTo(S) < 0) {
                k--;
                b = b.multiply(BigInteger.valueOf(10));  /* we botched the k estimate */
                if (leftright)
                    mhi = mhi.multiply(BigInteger.valueOf(10));
                ilim = ilim1;
            }
        }
        /* At this point 1 <= d/10^k = b/S < 10. */

        if (ilim <= 0 && mode > 2) {
            /* We're doing fixed-mode output and d is less than the minimum nonzero output in this mode.
               Output either zero or the minimum nonzero output depending on which is closer to d. */
            if ((ilim < 0 )
                    || ((i = b.compareTo(S = S.multiply(BigInteger.valueOf(5)))) < 0)
                    || ((i == 0 && !biasUp))) {
            /* Always emit at least one digit.  If the number appears to be zero
               using the current mode, then emit one '0' digit and set decpt to 1. */
            /*no_digits:
                k = -1 - ndigits;
                goto ret; */
                buf.setLength(0);
                buf.append('0');        /* copy "0" to buffer */
                return 1;
//                goto no_digits;
            }
//        one_digit:
            buf.append('1');
            k++;
            return k + 1;
        }
        if (leftright) {
            if (m2 > 0)
                mhi = mhi.shiftLeft(m2);

            /* Compute mlo -- check for special case
             * that d is a normalized power of 2.
             */

            mlo = mhi;
            if (spec_case) {
                mhi = mlo;
                mhi = mhi.shiftLeft(Log2P);
            }
            /* mlo/S = maximum acceptable error, divided by 10^k, if the output is less than d. */
            /* mhi/S = maximum acceptable error, divided by 10^k, if the output is greater than d. */

            for(i = 1;;i++) {
                BigInteger[] divResult = b.divideAndRemainder(S);
                b = divResult[1];
                dig = (char)(divResult[0].intValue() + '0');
                /* Do we yet have the shortest decimal string
                 * that will round to d?
                 */
                j = b.compareTo(mlo);
                /* j is b/S compared with mlo/S. */
                delta = S.subtract(mhi);
                j1 = (delta.signum() <= 0) ? 1 : b.compareTo(delta);
                /* j1 is b/S compared with 1 - mhi/S. */
                if ((j1 == 0) && (mode == 0) && ((word1(d) & 1) == 0)) {
                    if (dig == '9') {
                        buf.append('9');
                        if (roundOff(buf)) {
                            k++;
                            buf.append('1');
                        }
                        return k + 1;
//                        goto round_9_up;
                    }
                    if (j > 0)
                        dig++;
                    buf.append(dig);
                    return k + 1;
                }
                if ((j < 0)
                        || ((j == 0)
                            && (mode == 0)
                            && ((word1(d) & 1) == 0)
                    )) {
                    if (j1 > 0) {
                        /* Either dig or dig+1 would work here as the least significant decimal digit.
                           Use whichever would produce a decimal value closer to d. */
                        b = b.shiftLeft(1);
                        j1 = b.compareTo(S);
                        if (((j1 > 0) || (j1 == 0 && (((dig & 1) == 1) || biasUp)))
                            && (dig++ == '9')) {
                                buf.append('9');
                                if (roundOff(buf)) {
                                    k++;
                                    buf.append('1');
                                }
                                return k + 1;
//                                goto round_9_up;
                        }
                    }
                    buf.append(dig);
                    return k + 1;
                }
                if (j1 > 0) {
                    if (dig == '9') { /* possible if i == 1 */
//                    round_9_up:
//                        *s++ = '9';
//                        goto roundoff;
                        buf.append('9');
                        if (roundOff(buf)) {
                            k++;
                            buf.append('1');
                        }
                        return k + 1;
                    }
                    buf.append((char)(dig + 1));
                    return k + 1;
                }
                buf.append(dig);
                if (i == ilim)
                    break;
                b = b.multiply(BigInteger.valueOf(10));
                if (mlo == mhi)
                    mlo = mhi = mhi.multiply(BigInteger.valueOf(10));
                else {
                    mlo = mlo.multiply(BigInteger.valueOf(10));
                    mhi = mhi.multiply(BigInteger.valueOf(10));
                }
            }
        }
        else
            for(i = 1;; i++) {
//                (char)(dig = quorem(b,S) + '0');
                BigInteger[] divResult = b.divideAndRemainder(S);
                b = divResult[1];
                dig = (char)(divResult[0].intValue() + '0');
                buf.append(dig);
                if (i >= ilim)
                    break;
                b = b.multiply(BigInteger.valueOf(10));
            }

        /* Round off last digit */

        b = b.shiftLeft(1);
        j = b.compareTo(S);
        if ((j > 0) || (j == 0 && (((dig & 1) == 1) || biasUp))) {
//        roundoff:
//            while(*--s == '9')
//                if (s == buf) {
//                    k++;
//                    *s++ = '1';
//                    goto ret;
//                }
//            ++*s++;
            if (roundOff(buf)) {
                k++;
                buf.append('1');
                return k + 1;
            }
        }
        else {
            /* Strip trailing zeros */
            while (buf.charAt(buf.length() - 1) == '0')
                buf.setLength(buf.length() - 1);
//            while(*--s == '0') ;
//            s++;
        }
//      ret:
//        Bfree(S);
//        if (mhi) {
//            if (mlo && mlo != mhi)
//                Bfree(mlo);
//            Bfree(mhi);
//        }
//      ret1:
//        Bfree(b);
//        JS_ASSERT(s < buf + bufsize);
        return k + 1;
    }

    /* Mapping of JSDToStrMode -> JS_dtoa mode */
    private static final int dtoaModes[] = {
        0,   /* DTOSTR_STANDARD */
        0,   /* DTOSTR_STANDARD_EXPONENTIAL, */
        3,   /* DTOSTR_FIXED, */
        2,   /* DTOSTR_EXPONENTIAL, */
        2};  /* DTOSTR_PRECISION */

    static void
    JS_dtostr(StringBuffer buffer, int mode, int precision, double d)
    {
        int decPt;                                    /* Position of decimal point relative to first digit returned by JS_dtoa */
        boolean[] sign = new boolean[1];            /* true if the sign bit was set in d */
        int nDigits;                                /* Number of significand digits returned by JS_dtoa */

//        JS_ASSERT(bufferSize >= (size_t)(mode <= DTOSTR_STANDARD_EXPONENTIAL ? DTOSTR_STANDARD_BUFFER_SIZE :
//                DTOSTR_VARIABLE_BUFFER_SIZE(precision)));

        if (mode == DTOSTR_FIXED && (d >= 1e21 || d <= -1e21))
            mode = DTOSTR_STANDARD; /* Change mode here rather than below because the buffer may not be large enough to hold a large integer. */

        decPt = JS_dtoa(d, dtoaModes[mode], mode >= DTOSTR_FIXED, precision, sign, buffer);
        nDigits = buffer.length();

        /* If Infinity, -Infinity, or NaN, return the string regardless of the mode. */
        if (decPt != 9999) {
            boolean exponentialNotation = false;
            int minNDigits = 0;         /* Minimum number of significand digits required by mode and precision */
            int p;
            int q;

            switch (mode) {
                case DTOSTR_STANDARD:
                    if (decPt < -5 || decPt > 21)
                        exponentialNotation = true;
                    else
                        minNDigits = decPt;
                    break;

                case DTOSTR_FIXED:
                    if (precision >= 0)
                        minNDigits = decPt + precision;
                    else
                        minNDigits = decPt;
                    break;

                case DTOSTR_EXPONENTIAL:
//                    JS_ASSERT(precision > 0);
                    minNDigits = precision;
                    /* Fall through */
                case DTOSTR_STANDARD_EXPONENTIAL:
                    exponentialNotation = true;
                    break;

                case DTOSTR_PRECISION:
//                    JS_ASSERT(precision > 0);
                    minNDigits = precision;
                    if (decPt < -5 || decPt > precision)
                        exponentialNotation = true;
                    break;
            }

            /* If the number has fewer than minNDigits, pad it with zeros at the end */
            if (nDigits < minNDigits) {
                p = minNDigits;
                nDigits = minNDigits;
                do {
                    buffer.append('0');
                } while (buffer.length() != p);
            }

            if (exponentialNotation) {
                /* Insert a decimal point if more than one significand digit */
                if (nDigits != 1) {
                    buffer.insert(1, '.');
                }
                buffer.append('e');
                if ((decPt - 1) >= 0)
                    buffer.append('+');
                buffer.append(decPt - 1);
//                JS_snprintf(numEnd, bufferSize - (numEnd - buffer), "e%+d", decPt-1);
            } else if (decPt != nDigits) {
                /* Some kind of a fraction in fixed notation */
//                JS_ASSERT(decPt <= nDigits);
                if (decPt > 0) {
                    /* dd...dd . dd...dd */
                    buffer.insert(decPt, '.');
                } else {
                    /* 0 . 00...00dd...dd */
                    for (int i = 0; i < 1 - decPt; i++)
                        buffer.insert(0, '0');
                    buffer.insert(1, '.');
                }
            }
        }

        /* If negative and neither -0.0 nor NaN, output a leading '-'. */
        if (sign[0] &&
                !(word0(d) == Sign_bit && word1(d) == 0) &&
                !((word0(d) & Exp_mask) == Exp_mask &&
                  ((word1(d) != 0) || ((word0(d) & Frac_mask) != 0)))) {
            buffer.insert(0, '-');
        }
    }

}
