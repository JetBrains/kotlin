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

using namespace kotlin;

#if defined(LINUX) || defined(FREEBSD) || defined(MACOSX) || defined(ZOS) || defined(AIX)
#define USE_LL
#endif

#ifdef HY_LITTLE_ENDIAN
#define LOW_I32_FROM_PTR(ptr64) (*(I_32 *) (ptr64))
#else
#define LOW_I32_FROM_PTR(ptr64) (*(((I_32 *) (ptr64)) + 1))
#endif

#define MAX_ACCURACY_WIDTH 8

#define DEFAULT_WIDTH MAX_ACCURACY_WIDTH

extern "C" KFloat Kotlin_native_int_bits_to_float(KInt x) {
  union {
    int32_t x;
    float f;
  } tmp;
  tmp.x = x;
  return tmp.f;
}

KFloat createFloat1 (U_64 * f, IDATA length, KInt e);
KFloat floatAlgorithm (U_64 * f, IDATA length, KInt e, KFloat z);
KFloat createFloat (const char *s, KInt e);

static const U_32 tens[] = {
  0x3f800000,
  0x41200000,
  0x42c80000,
  0x447a0000,
  0x461c4000,
  0x47c35000,
  0x49742400,
  0x4b189680,
  0x4cbebc20,
  0x4e6e6b28,
  0x501502f9                    /* 10 ^ 10 in float */
};

#define tenToTheE(e) (*((KFloat *) (tens + (e))))

#define LOG5_OF_TWO_TO_THE_N 11

#define sizeOfTenToTheE(e) (((e) / 19) + 1)

#define INFINITE_INTBITS (0x7F800000)
#define MINIMUM_INTBITS (1)

#define MANTISSA_MASK (0x007FFFFF)
#define EXPONENT_MASK (0x7F800000)
#define NORMAL_MASK   (0x00800000)
#define FLOAT_TO_INTBITS(flt) (*((U_32 *)(&flt)))

/* Keep a count of the number of times we decrement and increment to
 * approximate the double, and attempt to detect the case where we
 * could potentially toggle back and forth between decrementing and
 * incrementing. It is possible for us to be stuck in the loop when
 * incrementing by one or decrementing by one may exceed or stay below
 * the value that we are looking for. In this case, just break out of
 * the loop if we toggle between incrementing and decrementing for more
 * than twice.
 */
#define INCREMENT_FLOAT(_x, _decCount, _incCount) \
    { \
        ++FLOAT_TO_INTBITS(_x); \
        _incCount++; \
        if( (_incCount > 2) && (_decCount > 2) ) { \
            if( _decCount > _incCount ) { \
                FLOAT_TO_INTBITS(_x) += _decCount - _incCount; \
            } else if( _incCount > _decCount ) { \
                FLOAT_TO_INTBITS(_x) -= _incCount - _decCount; \
            } \
            break; \
        } \
    }
#define DECREMENT_FLOAT(_x, _decCount, _incCount) \
    { \
        --FLOAT_TO_INTBITS(_x); \
        _decCount++; \
        if( (_incCount > 2) && (_decCount > 2) ) { \
            if( _decCount > _incCount ) { \
                FLOAT_TO_INTBITS(_x) += _decCount - _incCount; \
            } else if( _incCount > _decCount ) { \
                FLOAT_TO_INTBITS(_x) -= _incCount - _decCount; \
            } \
            break; \
        } \
    }

#define allocateU64(x, n) if (!((x) = (U_64*) std::calloc(1, (n) * sizeof(U_64)))) goto OutOfMemory;
#define release(r) if ((r)) std::free((r));

KFloat createFloat(const char *s, KInt e) {
  /* assumes s is a null terminated string with at least one
   * character in it */
  U_64 def[DEFAULT_WIDTH];
  U_64 defBackup[DEFAULT_WIDTH];
  U_64 *f, *fNoOverflow, *g, *tempBackup;
  U_32 overflow;
  KFloat result;
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
          if (e <= 0)
            {
              result = createFloat1 (f, index, e);
            }
          else
            {
              FLOAT_TO_INTBITS (result) = INFINITE_INTBITS;
            }
        }
      else
        {
          result = *(KFloat *) & index;
        }
    }
  else
    {
      if (index > -1)
        {
          result = createFloat1 (f, index, e);
        }
      else
        {
        result = *(KFloat *) & index;
        }
    }

  return result;

}

KFloat
createFloat1 (U_64 * f, IDATA length, KInt e)
{
  IDATA numBits;
  KDouble dresult;
  KFloat result;

  numBits = highestSetBitHighPrecision (f, length) + 1;
  if (numBits < 25 && e >= 0 && e < LOG5_OF_TWO_TO_THE_N)
    {
      return ((KFloat) LOW_I32_FROM_PTR (f)) * tenToTheE (e);
    }
  else if (numBits < 25 && e < 0 && (-e) < LOG5_OF_TWO_TO_THE_N)
    {
      return ((KFloat) LOW_I32_FROM_PTR (f)) / tenToTheE (-e);
    }
  else if (e >= 0 && e < 39)
    {
      result = (KFloat) (toDoubleHighPrecision (f, length) * pow (10.0, (double) e));
    }
  else if (e >= 39)
    {
      /* Convert the partial result to make sure that the
       * non-exponential part is not zero. This check fixes the case
       * where the user enters 0.0e309! */
      result = (KFloat) toDoubleHighPrecision (f, length);

      if (result == 0.0)

        FLOAT_TO_INTBITS (result) = MINIMUM_INTBITS;
      else
        FLOAT_TO_INTBITS (result) = INFINITE_INTBITS;
    }
  else if (e > -309)
    {
      int dexp;
      U_32 fmant, fovfl;
      U_64 dmant;
      dresult = toDoubleHighPrecision (f, length) / pow (10.0, (double) -e);
      if (IS_DENORMAL_DBL (dresult))
        {
          FLOAT_TO_INTBITS (result) = 0;
          return result;
        }
      dexp = doubleExponent (dresult) + 51;
      dmant = doubleMantissa (dresult);
      /* Is it too small to be represented by a single-precision
       * float? */
      if (dexp <= -155)
        {
          FLOAT_TO_INTBITS (result) = 0;
          return result;
        }
      /* Is it a denormalized single-precision float? */
      if ((dexp <= -127) && (dexp > -155))
        {
          /* Only interested in 24 msb bits of the 53-bit double mantissa */
          fmant = (U_32) (dmant >> 29);
          fovfl = ((U_32) (dmant & 0x1FFFFFFF)) << 3;
          while ((dexp < -127) && ((fmant | fovfl) != 0))
            {
              if ((fmant & 1) != 0)
                {
                  fovfl |= 0x80000000;
                }
              fovfl >>= 1;
              fmant >>= 1;
              dexp++;
            }
          if ((fovfl & 0x80000000) != 0)
            {
              if ((fovfl & 0x7FFFFFFC) != 0)
                {
                  fmant++;
                }
              else if ((fmant & 1) != 0)
                {
                  fmant++;
                }
            }
          else if ((fovfl & 0x40000000) != 0)
            {
              if ((fovfl & 0x3FFFFFFC) != 0)
                {
                  fmant++;
                }
            }
          FLOAT_TO_INTBITS (result) = fmant;
        }
      else
        {
          result = (KFloat) dresult;
        }
    }

  /* Don't go straight to zero as the fact that x*0 = 0 independent
   * of x might cause the algorithm to produce an incorrect result.
   * Instead try the min  value first and let it fall to zero if need
   * be.
   */
  if (e <= -309 || FLOAT_TO_INTBITS (result) == 0)
    FLOAT_TO_INTBITS (result) = MINIMUM_INTBITS;

  return floatAlgorithm (f, length, e, result);
}

#if defined(WIN32)
/* disable global optimizations on the microsoft compiler for the
 * floatAlgorithm function otherwise it won't properly compile */
#pragma optimize("g",off)
#endif

/* The algorithm for the function floatAlgorithm() below can be found
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
KFloat
floatAlgorithm (U_64 * f, IDATA length, KInt e, KFloat z)
{
  U_64 m;
  IDATA k, comparison, comparison2;
  U_64 *x, *y, *D, *D2;
  IDATA xLength, yLength, DLength, D2Length;
  IDATA decApproxCount, incApproxCount;
  //PORT_ACCESS_FROM_ENV (env);

  x = y = D = D2 = 0;
  xLength = yLength = DLength = D2Length = 0;
  decApproxCount = incApproxCount = 0;

  do
    {
      m = floatMantissa (z);
      k = floatExponent (z);

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
                  DECREMENT_FLOAT (z, decApproxCount, incApproxCount);
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
          if ((m & 1) == 0)
            {
              if (comparison < 0 && m == NORMAL_MASK)
                {
                  DECREMENT_FLOAT (z, decApproxCount, incApproxCount);
                }
              else
                {
                  break;
                }
            }
          else if (comparison < 0)
            {
              DECREMENT_FLOAT (z, decApproxCount, incApproxCount);
              break;
            }
          else
            {
              INCREMENT_FLOAT (z, decApproxCount, incApproxCount);
              break;
            }
        }
      else if (comparison < 0)
        {
          DECREMENT_FLOAT (z, decApproxCount, incApproxCount);
        }
      else
        {
          if (FLOAT_TO_INTBITS (z) == EXPONENT_MASK)
            break;
          INCREMENT_FLOAT (z, decApproxCount, incApproxCount);
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

  FLOAT_TO_INTBITS (z) = -2;

  return z;
}

#if defined(WIN32)
#pragma optimize("",on)         /*restore optimizations */
#endif

extern "C" KFloat
Kotlin_native_FloatingPointParser_parseFloatImpl(KString s, KInt e)
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
  auto flt = createFloat(str, e);

  if (((I_32) FLOAT_TO_INTBITS (flt)) >= 0) {
    return flt;
  } else if (((I_32) FLOAT_TO_INTBITS (flt)) == (I_32) - 1) {
      /* NumberFormatException */
    ThrowNumberFormatException();
  } else {
    /* OutOfMemoryError */
    ThrowOutOfMemoryError();
  }

  return 0.0f;
}
