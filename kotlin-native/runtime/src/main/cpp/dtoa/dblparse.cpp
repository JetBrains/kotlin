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

#include <string.h>
#include <math.h>
#include <stdlib.h>
#include <string>

#include "cbigint.h"
#include "../Exceptions.h"
#include "../KString.h"
#include "../Natives.h"
#include "../Porting.h"
#include "../utf8.h"
#include "../DoubleConversions.h"

using namespace kotlin;

#if defined(LINUX) || defined(FREEBSD) || defined(ZOS) || defined(MACOSX) || defined(AIX)
#define USE_LL
#endif

#define LOW_I32_FROM_VAR(u64) LOW_I32_FROM_LONG64(u64)
#define LOW_I32_FROM_PTR(u64ptr) LOW_I32_FROM_LONG64_PTR(u64ptr)
#define HIGH_I32_FROM_VAR(u64) HIGH_I32_FROM_LONG64(u64)
#define HIGH_I32_FROM_PTR(u64ptr) HIGH_I32_FROM_LONG64_PTR(u64ptr)

#define MAX_ACCURACY_WIDTH 17

#define DEFAULT_WIDTH MAX_ACCURACY_WIDTH

extern "C" {
KDouble Kotlin_native_FloatingPointParser_parseDoubleImpl (KString s, KInt e);

void Kotlin_native_NumberConverter_bigIntDigitGeneratorInstImpl (KRef results,
                                                         KRef uArray,
                                                         KLong f,
                                                         KInt e,
                                                         KBoolean isDenormalized,
                                                         KBoolean mantissaIsZero,
                                                         KInt p);

KDouble Kotlin_native_NumberConverter_ceil(KDouble x) {
  return ceil(x);
}

void Kotlin_IntArray_set(KRef thiz, KInt index, KInt value);

KDouble Kotlin_native_long_bits_to_double(KLong x);
}

KDouble Kotlin_native_long_bits_to_double(KLong x) {
  union {
    int64_t x;
    double d;
  } tmp;
  tmp.x = x;
  return tmp.d;
}

KDouble createDouble (const char *s, KInt e);
KDouble createDouble1 (U_64 * f, IDATA length, KInt e);
KDouble doubleAlgorithm (U_64 * f, IDATA length, KInt e, KDouble z);

U_64 dblparse_shiftRight64 (U_64 * lp, volatile int mbe);

static const KDouble tens[] = {
  1.0,
  1.0e1,
  1.0e2,
  1.0e3,
  1.0e4,
  1.0e5,
  1.0e6,
  1.0e7,
  1.0e8,
  1.0e9,
  1.0e10,
  1.0e11,
  1.0e12,
  1.0e13,
  1.0e14,
  1.0e15,
  1.0e16,
  1.0e17,
  1.0e18,
  1.0e19,
  1.0e20,
  1.0e21,
  1.0e22
};

#define tenToTheE(e) (*(tens + (e)))
#define LOG5_OF_TWO_TO_THE_N 23
#define INV_LOG_OF_TEN_BASE_2 (0.30102999566398114)
#define DOUBLE_MIN_VALUE 5.0e-324

#define sizeOfTenToTheE(e) (((e) / 19) + 1)

#if defined(USE_LL)
#define INFINITE_LONGBITS (0x7FF0000000000000LL)
#else
#if defined(USE_L)
#define INFINITE_LONGBITS (0x7FF0000000000000L)
#else
#define INFINITE_LONGBITS (0x7FF0000000000000)
#endif /* USE_L */
#endif /* USE_LL */

#define MINIMUM_LONGBITS (0x1)

#if defined(USE_LL)
#define MANTISSA_MASK (0x000FFFFFFFFFFFFFLL)
#define EXPONENT_MASK (0x7FF0000000000000LL)
#define NORMAL_MASK   (0x0010000000000000LL)
#else
#if defined(USE_L)
#define MANTISSA_MASK (0x000FFFFFFFFFFFFFL)
#define EXPONENT_MASK (0x7FF0000000000000L)
#define NORMAL_MASK   (0x0010000000000000L)
#else
#define MANTISSA_MASK (0x000FFFFFFFFFFFFF)
#define EXPONENT_MASK (0x7FF0000000000000)
#define NORMAL_MASK   (0x0010000000000000)
#endif /* USE_L */
#endif /* USE_LL */

#define DOUBLE_TO_LONGBITS(dbl) (*((U_64 *)(&dbl)))

/* Keep a count of the number of times we decrement and increment to
 * approximate the double, and attempt to detect the case where we
 * could potentially toggle back and forth between decrementing and
 * incrementing. It is possible for us to be stuck in the loop when
 * incrementing by one or decrementing by one may exceed or stay below
 * the value that we are looking for. In this case, just break out of
 * the loop if we toggle between incrementing and decrementing for more
 * than twice.
 */
#define INCREMENT_DOUBLE(_x, _decCount, _incCount) \
        { \
                ++DOUBLE_TO_LONGBITS(_x); \
                _incCount++; \
                if( (_incCount > 2) && (_decCount > 2) ) { \
                        if( _decCount > _incCount ) { \
                                DOUBLE_TO_LONGBITS(_x) += _decCount - _incCount; \
                        } else if( _incCount > _decCount ) { \
                                DOUBLE_TO_LONGBITS(_x) -= _incCount - _decCount; \
                        } \
                        break; \
                } \
        }
#define DECREMENT_DOUBLE(_x, _decCount, _incCount) \
        { \
                --DOUBLE_TO_LONGBITS(_x); \
                _decCount++; \
                if( (_incCount > 2) && (_decCount > 2) ) { \
                        if( _decCount > _incCount ) { \
                                DOUBLE_TO_LONGBITS(_x) += _decCount - _incCount; \
                        } else if( _incCount > _decCount ) { \
                                DOUBLE_TO_LONGBITS(_x) -= _incCount - _decCount; \
                        } \
                        break; \
                } \
        }
#define ERROR_OCCURED(x) (HIGH_I32_FROM_VAR(x) < 0)

#define allocateU64(x, n) if (!((x) = (U_64*) std::calloc(1, (n) * sizeof(U_64)))) goto OutOfMemory;
#define release(r) if ((r)) std::free((r));

/*NB the Number converter methods are synchronized so it is possible to
 *have global data for use by bigIntDigitGenerator */
#define RM_SIZE 21
#define STemp_SIZE 22

KDouble createDouble (const char *s, KInt e)
{
  /* assumes s is a null terminated string with at least one
   * character in it */
  U_64 def[DEFAULT_WIDTH];
  U_64 defBackup[DEFAULT_WIDTH];
  U_64 *f, *fNoOverflow, *g, *tempBackup;
  U_32 overflow;
  KDouble result;
  IDATA index = 1;
  int unprocessedDigits = 0;

  f = def;
  fNoOverflow = defBackup;
  *f = 0;
  tempBackup = g = 0;
  do
    {
      if (*s >= '0' && *s <= '9')
        {
          /* Make a back up of f before appending, so that we can
           * back out of it if there is no more room, i.e. index >
           * MAX_ACCURACY_WIDTH.
           */
          memcpy (fNoOverflow, f, sizeof (U_64) * index);
          overflow =
            simpleAppendDecimalDigitHighPrecision (f, index, *s - '0');
          if (overflow)
            {
              f[index++] = overflow;
              /* There is an overflow, but there is no more room
               * to store the result. We really only need the top 52
               * bits anyway, so we must back out of the overflow,
               * and ignore the rest of the string.
               */
              if (index >= MAX_ACCURACY_WIDTH)
                {
                  index--;
                  memcpy (f, fNoOverflow, sizeof (U_64) * index);
                  break;
                }
              if (tempBackup)
                {
                  fNoOverflow = tempBackup;
                }
            }
        }
      else
        index = -1;
    }
  while (index > 0 && *(++s) != '\0');

  /* We've broken out of the parse loop either because we've reached
   * the end of the string or we've overflowed the maximum accuracy
   * limit of a double. If we still have unprocessed digits in the
   * given string, then there are three possible results:
   *   1. (unprocessed digits + e) == 0, in which case we simply
   *      convert the existing bits that are already parsed
   *   2. (unprocessed digits + e) < 0, in which case we simply
   *      convert the existing bits that are already parsed along
   *      with the given e
   *   3. (unprocessed digits + e) > 0 indicates that the value is
   *      simply too big to be stored as a double, so return Infinity
   */
  if ((unprocessedDigits = strlen (s)) > 0)
    {
      e += unprocessedDigits;
      if (index > -1)
        {
          if (e == 0)
            result = toDoubleHighPrecision (f, index);
          else if (e < 0)
            result = createDouble1 (f, index, e);
          else
            {
              DOUBLE_TO_LONGBITS (result) = INFINITE_LONGBITS;
            }
        }
      else
        {
          LOW_I32_FROM_VAR (result) = -1;
          HIGH_I32_FROM_VAR (result) = -1;
        }
    }
  else
    {
      if (index > -1)
        {
          if (e == 0)
            result = toDoubleHighPrecision (f, index);
          else
            result = createDouble1 (f, index, e);
        }
      else
        {
          LOW_I32_FROM_VAR (result) = -1;
          HIGH_I32_FROM_VAR (result) = -1;
        }
    }

  return result;

}

KDouble
createDouble1 (U_64 * f, IDATA length, KInt e)
{
  IDATA numBits;
  KDouble result;

#define APPROX_MIN_MAGNITUDE -309

#define APPROX_MAX_MAGNITUDE 309

  numBits = highestSetBitHighPrecision (f, length) + 1;
  numBits -= lowestSetBitHighPrecision (f, length);
  if (numBits < 54 && e >= 0 && e < LOG5_OF_TWO_TO_THE_N)
    {
      return toDoubleHighPrecision (f, length) * tenToTheE (e);
    }
  else if (numBits < 54 && e < 0 && (-e) < LOG5_OF_TWO_TO_THE_N)
    {
      return toDoubleHighPrecision (f, length) / tenToTheE (-e);
    }
  else if (e >= 0 && e < APPROX_MAX_MAGNITUDE)
    {
      result = toDoubleHighPrecision (f, length) * pow (10.0, (double) e);
    }
  else if (e >= APPROX_MAX_MAGNITUDE)
    {
      /* Convert the partial result to make sure that the
       * non-exponential part is not zero. This check fixes the case
       * where the user enters 0.0e309! */
      result = toDoubleHighPrecision (f, length);
      /* Don't go straight to zero as the fact that x*0 = 0 independent of x might
         cause the algorithm to produce an incorrect result.  Instead try the min value
         first and let it fall to zero if need be. */

      if (result == 0.0)

        DOUBLE_TO_LONGBITS (result) = MINIMUM_LONGBITS;
      else
        DOUBLE_TO_LONGBITS (result) = INFINITE_LONGBITS;
    }
  else if (e > APPROX_MIN_MAGNITUDE)
    {
      result = toDoubleHighPrecision (f, length) / pow (10.0, (double) -e);
    }

  if (e <= APPROX_MIN_MAGNITUDE)
    {

      result = toDoubleHighPrecision (f, length) * pow (10.0, (double) (e + 52));
      result = result * pow (10.0, (double) -52);

    }

  /* Don't go straight to zero as the fact that x*0 = 0 independent of x might
     cause the algorithm to produce an incorrect result.  Instead try the min value
     first and let it fall to zero if need be. */

  if (result == 0.0)

    DOUBLE_TO_LONGBITS (result) = MINIMUM_LONGBITS;

  return doubleAlgorithm (f, length, e, result);
}

U_64
dblparse_shiftRight64 (U_64 * lp, volatile int mbe)
{
  U_64 b1Value = 0;
  U_32 hi = HIGH_U32_FROM_LONG64_PTR (lp);
  U_32 lo = LOW_U32_FROM_LONG64_PTR (lp);
  int srAmt;

  if (mbe == 0)
    return 0;
  if (mbe >= 128)
    {
      HIGH_U32_FROM_LONG64_PTR (lp) = 0;
      LOW_U32_FROM_LONG64_PTR (lp) = 0;
      return 0;
    }

  /* Certain platforms do not handle de-referencing a 64-bit value
   * from a pointer on the stack correctly (e.g. MVL-hh/XScale)
   * because the pointer may not be properly aligned, so we'll have
   * to handle two 32-bit chunks. */
  if (mbe < 32)
    {
      LOW_U32_FROM_LONG64 (b1Value) = 0;
      HIGH_U32_FROM_LONG64 (b1Value) = lo << (32 - mbe);
      LOW_U32_FROM_LONG64_PTR (lp) = (hi << (32 - mbe)) | (lo >> mbe);
      HIGH_U32_FROM_LONG64_PTR (lp) = hi >> mbe;
    }
  else if (mbe == 32)
    {
      LOW_U32_FROM_LONG64 (b1Value) = 0;
      HIGH_U32_FROM_LONG64 (b1Value) = lo;
      LOW_U32_FROM_LONG64_PTR (lp) = hi;
      HIGH_U32_FROM_LONG64_PTR (lp) = 0;
    }
  else if (mbe < 64)
    {
      srAmt = mbe - 32;
      LOW_U32_FROM_LONG64 (b1Value) = lo << (32 - srAmt);
      HIGH_U32_FROM_LONG64 (b1Value) = (hi << (32 - srAmt)) | (lo >> srAmt);
      LOW_U32_FROM_LONG64_PTR (lp) = hi >> srAmt;
      HIGH_U32_FROM_LONG64_PTR (lp) = 0;
    }
  else if (mbe == 64)
    {
      LOW_U32_FROM_LONG64 (b1Value) = lo;
      HIGH_U32_FROM_LONG64 (b1Value) = hi;
      LOW_U32_FROM_LONG64_PTR (lp) = 0;
      HIGH_U32_FROM_LONG64_PTR (lp) = 0;
    }
  else if (mbe < 96)
    {
      srAmt = mbe - 64;
      b1Value = *lp;
      HIGH_U32_FROM_LONG64_PTR (lp) = 0;
      LOW_U32_FROM_LONG64_PTR (lp) = 0;
      LOW_U32_FROM_LONG64 (b1Value) >>= srAmt;
      LOW_U32_FROM_LONG64 (b1Value) |= (hi << (32 - srAmt));
      HIGH_U32_FROM_LONG64 (b1Value) >>= srAmt;
    }
  else if (mbe == 96)
    {
      LOW_U32_FROM_LONG64 (b1Value) = hi;
      HIGH_U32_FROM_LONG64 (b1Value) = 0;
      HIGH_U32_FROM_LONG64_PTR (lp) = 0;
      LOW_U32_FROM_LONG64_PTR (lp) = 0;
    }
  else
    {
      LOW_U32_FROM_LONG64 (b1Value) = hi >> (mbe - 96);
      HIGH_U32_FROM_LONG64 (b1Value) = 0;
      HIGH_U32_FROM_LONG64_PTR (lp) = 0;
      LOW_U32_FROM_LONG64_PTR (lp) = 0;
    }

  return b1Value;
}

#if defined(WIN32)
/* disable global optimizations on the microsoft compiler for the
 * doubleAlgorithm function otherwise it won't compile */
#pragma optimize("g",off)
#endif

/* The algorithm for the function doubleAlgorithm() below can be found
 * in:
 *
 *      "How to Read Floating-Point Numbers Accurately", William D.
 *      Clinger, Proceedings of the ACM SIGPLAN '90 Conference on
 *      Programming Language Design and Implementation, June 20-22,
 *      1990, pp. 92-101.
 *
 * There is a possibility that the function will end up in an endless
 * loop if the given approximating floating-point number (a very small
 * floating-point whose value is very close to zero) straddles between
 * two approximating integer values. We modified the algorithm slightly
 * to detect the case where it oscillates back and forth between
 * incrementing and decrementing the floating-point approximation. It
 * is currently set such that if the oscillation occurs more than twice
 * then return the original approximation.
 */
KDouble doubleAlgorithm (U_64 * f, IDATA length, KInt e, KDouble z)
{
  U_64 m;
  IDATA k, comparison, comparison2;
  U_64 *x, *y, *D, *D2;
  IDATA xLength, yLength, DLength, D2Length, decApproxCount, incApproxCount;
  //PORT_ACCESS_FROM_ENV (env);

  x = y = D = D2 = 0;
  xLength = yLength = DLength = D2Length = 0;
  decApproxCount = incApproxCount = 0;

  do
    {
      m = doubleMantissa (z);
      k = doubleExponent (z);

      if (x && x != f)
        //jclmem_free_memory (env, x);
        release(x);
      release (y);
      release (D);
      release (D2);

      if (e >= 0 && k >= 0)
        {
          xLength = sizeOfTenToTheE (e) + length;
          allocateU64 (x, xLength);
          memset (x + length, 0, sizeof (U_64) * (xLength - length));
          memcpy (x, f, sizeof (U_64) * length);
          timesTenToTheEHighPrecision (x, xLength, e);

          yLength = (k >> 6) + 2;
          allocateU64 (y, yLength);
          memset (y + 1, 0, sizeof (U_64) * (yLength - 1));
          *y = m;
          simpleShiftLeftHighPrecision (y, yLength, k);
        }
      else if (e >= 0)
        {
          xLength = sizeOfTenToTheE (e) + length + ((-k) >> 6) + 1;
          allocateU64 (x, xLength);
          memset (x + length, 0, sizeof (U_64) * (xLength - length));
          memcpy (x, f, sizeof (U_64) * length);
          timesTenToTheEHighPrecision (x, xLength, e);
          simpleShiftLeftHighPrecision (x, xLength, -k);

          yLength = 1;
          allocateU64 (y, 1);
          *y = m;
        }
      else if (k >= 0)
        {
          xLength = length;
          x = f;

          yLength = sizeOfTenToTheE (-e) + 2 + (k >> 6);
          allocateU64 (y, yLength);
          memset (y + 1, 0, sizeof (U_64) * (yLength - 1));
          *y = m;
          timesTenToTheEHighPrecision (y, yLength, -e);
          simpleShiftLeftHighPrecision (y, yLength, k);
        }
      else
        {
          xLength = length + ((-k) >> 6) + 1;
          allocateU64 (x, xLength);
          memset (x + length, 0, sizeof (U_64) * (xLength - length));
          memcpy (x, f, sizeof (U_64) * length);
          simpleShiftLeftHighPrecision (x, xLength, -k);

          yLength = sizeOfTenToTheE (-e) + 1;
          allocateU64 (y, yLength);
          memset (y + 1, 0, sizeof (U_64) * (yLength - 1));
          *y = m;
          timesTenToTheEHighPrecision (y, yLength, -e);
        }

      comparison = compareHighPrecision (x, xLength, y, yLength);
      if (comparison > 0)
        {                       /* x > y */
          DLength = xLength;
          allocateU64 (D, DLength);
          memcpy (D, x, DLength * sizeof (U_64));
          subtractHighPrecision (D, DLength, y, yLength);
        }
      else if (comparison)
        {                       /* y > x */
          DLength = yLength;
          allocateU64 (D, DLength);
          memcpy (D, y, DLength * sizeof (U_64));
          subtractHighPrecision (D, DLength, x, xLength);
        }
      else
        {                       /* y == x */
          DLength = 1;
          allocateU64 (D, 1);
          *D = 0;
        }

      D2Length = DLength + 1;
      allocateU64 (D2, D2Length);
      m <<= 1;
      multiplyHighPrecision (D, DLength, &m, 1, D2, D2Length);
      m >>= 1;

      comparison2 = compareHighPrecision (D2, D2Length, y, yLength);
      if (comparison2 < 0)
        {
          if (comparison < 0 && m == NORMAL_MASK)
            {
              simpleShiftLeftHighPrecision (D2, D2Length, 1);
              if (compareHighPrecision (D2, D2Length, y, yLength) > 0)
                {
                  DECREMENT_DOUBLE (z, decApproxCount, incApproxCount);
                }
              else
                {
                  break;
                }
            }
          else
            {
              break;
            }
        }
      else if (comparison2 == 0)
        {
          if ((LOW_U32_FROM_VAR (m) & 1) == 0)
            {
              if (comparison < 0 && m == NORMAL_MASK)
                {
                  DECREMENT_DOUBLE (z, decApproxCount, incApproxCount);
                }
              else
                {
                  break;
                }
            }
          else if (comparison < 0)
            {
              DECREMENT_DOUBLE (z, decApproxCount, incApproxCount);
              break;
            }
          else
            {
              INCREMENT_DOUBLE (z, decApproxCount, incApproxCount);
              break;
            }
        }
      else if (comparison < 0)
        {
          DECREMENT_DOUBLE (z, decApproxCount, incApproxCount);
        }
      else
        {
          if (DOUBLE_TO_LONGBITS (z) == INFINITE_LONGBITS)
            break;
          INCREMENT_DOUBLE (z, decApproxCount, incApproxCount);
        }
    }
  while (1);

  if (x && x != f)
    //jclmem_free_memory (env, x);
    release(x);
  release (y);
  release (D);
  release (D2);
  return z;

OutOfMemory:
  if (x && x != f)
    //jclmem_free_memory (env, x);
    release(x);
  release (y);
  release (D);
  release (D2);

  DOUBLE_TO_LONGBITS (z) = -2;

  return z;
}

#if defined(WIN32)
#pragma optimize("",on)         /*restore optimizations */
#endif

KDouble Kotlin_native_FloatingPointParser_parseDoubleImpl (KString s, KInt e)
{
  const KChar* utf16 = CharArrayAddressOfElementAt(s, 0);
  std::string utf8;
  utf8.reserve(s->count_);
  try {
    utf8::utf16to8(utf16, utf16 + s->count_, back_inserter(utf8));
  } catch (...) {
    /* Illegal UTF-16 string. */
    ThrowNumberFormatException();
  }
  const char *str = utf8.c_str();
  auto dbl = createDouble (str, e);

  if (!ERROR_OCCURED (dbl))
    {
      return dbl;
    }
  else if (LOW_I32_FROM_VAR (dbl) == (I_32) - 1)
    {                           /* NumberFormatException */
      ThrowNumberFormatException();
    }
  else
    {                           /* OutOfMemoryError */
      ThrowOutOfMemoryError();
    }

  return 0.0;
}

/* The algorithm for this particular function can be found in:
 *
 *      Printing Floating-Point Numbers Quickly and Accurately, Robert
 *      G. Burger, and R. Kent Dybvig, Programming Language Design and
 *      Implementation (PLDI) 1996, pp.108-116.
 *
 * The previous implementation of this function combined m+ and m- into
 * one single M which caused some inaccuracy of the last digit. The
 * particular case below shows this inaccuracy:
 *
 *       System.out.println(new Double((1.234123412431233E107)).toString());
 *       System.out.println(new Double((1.2341234124312331E107)).toString());
 *       System.out.println(new Double((1.2341234124312332E107)).toString());
 *
 *       outputs the following:
 *
 *           1.234123412431233E107
 *           1.234123412431233E107
 *           1.234123412431233E107
 *
 *       instead of:
 *
 *           1.234123412431233E107
 *           1.2341234124312331E107
 *           1.2341234124312331E107
 *
 */
void Kotlin_native_NumberConverter_bigIntDigitGeneratorInstImpl (KRef results,
                                                         KRef uArray,
                                                         KLong f,
                                                         KInt e,
                                                         KBoolean isDenormalized,
                                                         KBoolean mantissaIsZero,
                                                         KInt p)
{
  int RLength, SLength, TempLength, mplus_Length, mminus_Length;
  int high, low, i;
  int k, firstK, U;
  int getCount, setCount;

  U_64 R[RM_SIZE], S[STemp_SIZE], mplus[RM_SIZE], mminus[RM_SIZE], Temp[STemp_SIZE];

  memset (R, 0, RM_SIZE * sizeof (U_64));
  memset (S, 0, STemp_SIZE * sizeof (U_64));
  memset (mplus, 0, RM_SIZE * sizeof (U_64));
  memset (mminus, 0, RM_SIZE * sizeof (U_64));
  memset (Temp, 0, STemp_SIZE * sizeof (U_64));

  if (e >= 0)
    {
      *R = f;
      *mplus = *mminus = 1;
      simpleShiftLeftHighPrecision (mminus, RM_SIZE, e);
      if (f != (2 << (p - 1)))
        {
          simpleShiftLeftHighPrecision (R, RM_SIZE, e + 1);
          *S = 2;
          /*
           * m+ = m+ << e results in 1.0e23 to be printed as
           * 0.9999999999999999E23
           * m+ = m+ << e+1 results in 1.0e23 to be printed as
           * 1.0e23 (caused too much rounding)
           *      470fffffffffffff = 2.0769187434139308E34
           *      4710000000000000 = 2.076918743413931E34
           */
          simpleShiftLeftHighPrecision (mplus, RM_SIZE, e);
        }
      else
        {
          simpleShiftLeftHighPrecision (R, RM_SIZE, e + 2);
          *S = 4;
          simpleShiftLeftHighPrecision (mplus, RM_SIZE, e + 1);
        }
    }
  else
    {
      if (isDenormalized || (f != (2 << (p - 1))))
        {
          *R = f << 1;
          *S = 1;
          simpleShiftLeftHighPrecision (S, STemp_SIZE, 1 - e);
          *mplus = *mminus = 1;
        }
      else
        {
          *R = f << 2;
          *S = 1;
          simpleShiftLeftHighPrecision (S, STemp_SIZE, 2 - e);
          *mplus = 2;
          *mminus = 1;
        }
    }

  k = (int) ceil ((e + p - 1) * INV_LOG_OF_TEN_BASE_2 - 1e-10);

  if (k > 0)
    {
      timesTenToTheEHighPrecision (S, STemp_SIZE, k);
    }
  else
    {
      timesTenToTheEHighPrecision (R, RM_SIZE, -k);
      timesTenToTheEHighPrecision (mplus, RM_SIZE, -k);
      timesTenToTheEHighPrecision (mminus, RM_SIZE, -k);
    }

  RLength = mplus_Length = mminus_Length = RM_SIZE;
  SLength = TempLength = STemp_SIZE;

  memset (Temp + RM_SIZE, 0, (STemp_SIZE - RM_SIZE) * sizeof (U_64));
  memcpy (Temp, R, RM_SIZE * sizeof (U_64));

  while (RLength > 1 && R[RLength - 1] == 0)
    --RLength;
  while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0)
    --mplus_Length;
  while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0)
    --mminus_Length;
  while (SLength > 1 && S[SLength - 1] == 0)
    --SLength;
  TempLength = (RLength > mplus_Length ? RLength : mplus_Length) + 1;
  addHighPrecision (Temp, TempLength, mplus, mplus_Length);

  if (compareHighPrecision (Temp, TempLength, S, SLength) >= 0)
    {
      firstK = k;
    }
  else
    {
      firstK = k - 1;
      simpleAppendDecimalDigitHighPrecision (R, ++RLength, 0);
      simpleAppendDecimalDigitHighPrecision (mplus, ++mplus_Length, 0);
      simpleAppendDecimalDigitHighPrecision (mminus, ++mminus_Length, 0);
      while (RLength > 1 && R[RLength - 1] == 0)
        --RLength;
      while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0)
        --mplus_Length;
      while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0)
        --mminus_Length;
    }

  getCount = setCount = 0;
  do
    {
      U = 0;
      for (i = 3; i >= 0; --i)
        {
          TempLength = SLength + 1;
          Temp[SLength] = 0;
          memcpy (Temp, S, SLength * sizeof (U_64));
          simpleShiftLeftHighPrecision (Temp, TempLength, i);
          if (compareHighPrecision (R, RLength, Temp, TempLength) >= 0)
            {
              subtractHighPrecision (R, RLength, Temp, TempLength);
              U += 1 << i;
            }
        }

      low = compareHighPrecision (R, RLength, mminus, mminus_Length) <= 0;

      memset (Temp + RLength, 0, (STemp_SIZE - RLength) * sizeof (U_64));
      memcpy (Temp, R, RLength * sizeof (U_64));
      TempLength = (RLength > mplus_Length ? RLength : mplus_Length) + 1;
      addHighPrecision (Temp, TempLength, mplus, mplus_Length);

      high = compareHighPrecision (Temp, TempLength, S, SLength) >= 0;

      if (low || high)
        break;

      simpleAppendDecimalDigitHighPrecision (R, ++RLength, 0);
      simpleAppendDecimalDigitHighPrecision (mplus, ++mplus_Length, 0);
      simpleAppendDecimalDigitHighPrecision (mminus, ++mminus_Length, 0);
      while (RLength > 1 && R[RLength - 1] == 0)
        --RLength;
      while (mplus_Length > 1 && mplus[mplus_Length - 1] == 0)
        --mplus_Length;
      while (mminus_Length > 1 && mminus[mminus_Length - 1] == 0)
        --mminus_Length;
      Kotlin_IntArray_set(uArray, setCount++, U);
      //uArray[setCount++] = U;
    }
  while (1);

  simpleShiftLeftHighPrecision (R, ++RLength, 1);
  if (low && !high)
    Kotlin_IntArray_set(uArray, setCount++, U);
    //uArray[setCount++] = U;
  else if (high && !low)
    Kotlin_IntArray_set(uArray, setCount++, U + 1);
    //uArray[setCount++] = U + 1;
  else if (compareHighPrecision (R, RLength, S, SLength) < 0)
    Kotlin_IntArray_set(uArray, setCount++, U);
    //uArray[setCount++] = U;
  else
    Kotlin_IntArray_set(uArray, setCount++, U + 1);
    //uArray[setCount++] = U + 1;

  Kotlin_IntArray_set(results, 0, setCount);
//  fid = (*env)->GetFieldID (env, clazz, "setCount", "I");
//  (*env)->SetIntField (env, inst, fid, setCount);

  Kotlin_IntArray_set(results, 1, getCount);
//  fid = (*env)->GetFieldID (env, clazz, "getCount", "I");
//  (*env)->SetIntField (env, inst, fid, getCount);

  Kotlin_IntArray_set(results, 2, firstK);
//  fid = (*env)->GetFieldID (env, clazz, "firstK", "I");
//  (*env)->SetIntField (env, inst, fid, firstK);

}
