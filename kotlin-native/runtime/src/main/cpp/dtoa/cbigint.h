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

#if !defined(cbigint_h)
#define cbigint_h
#include "fltconst.h"
#include "../Types.h"
//#include "vmi.h"
#define LOW_U32_FROM_VAR(u64) LOW_U32_FROM_LONG64(u64)
#define LOW_U32_FROM_PTR(u64ptr) LOW_U32_FROM_LONG64_PTR(u64ptr)
#define HIGH_U32_FROM_VAR(u64) HIGH_U32_FROM_LONG64(u64)
#define HIGH_U32_FROM_PTR(u64ptr) HIGH_U32_FROM_LONG64_PTR(u64ptr)
#if defined(__cplusplus)
extern "C"
{
#endif
  void multiplyHighPrecision (U_64 * arg1, IDATA length1, U_64 * arg2,
                              IDATA length2, U_64 * result, IDATA length);
  U_32 simpleAppendDecimalDigitHighPrecision (U_64 * arg1, IDATA length, U_64 digit);
  KDouble toDoubleHighPrecision (U_64 * arg, IDATA length);
  IDATA tenToTheEHighPrecision (U_64 * result, IDATA length, int e);
  U_64 doubleMantissa (KDouble z);
  IDATA compareHighPrecision (U_64 * arg1, IDATA length1, U_64 * arg2, IDATA length2);
  IDATA highestSetBitHighPrecision (U_64 * arg, IDATA length);
  void subtractHighPrecision (U_64 * arg1, IDATA length1, U_64 * arg2, IDATA length2);
  IDATA doubleExponent (KDouble z);
  U_32 simpleMultiplyHighPrecision (U_64 * arg1, IDATA length, U_64 arg2);
  IDATA addHighPrecision (U_64 * arg1, IDATA length1, U_64 * arg2, IDATA length2);
  void simpleMultiplyAddHighPrecisionBigEndianFix (U_64 * arg1, IDATA length, U_64 arg2, U_32 * result);
  IDATA lowestSetBit (U_64 * y);
  IDATA timesTenToTheEHighPrecision (U_64 * result, IDATA length, int e);
  void simpleMultiplyAddHighPrecision (U_64 * arg1, IDATA length, U_64 arg2, U_32 * result);
  IDATA highestSetBit (U_64 * y);
  IDATA lowestSetBitHighPrecision (U_64 * arg, IDATA length);
  void simpleShiftLeftHighPrecision (U_64 * arg1, IDATA length, IDATA arg2);
  UDATA floatMantissa (KFloat z);
  U_64 simpleMultiplyHighPrecision64 (U_64 * arg1, IDATA length, U_64 arg2);
  IDATA simpleAddHighPrecision (U_64 * arg1, IDATA length, U_64 arg2);
  IDATA floatExponent (KFloat z);
#if defined(__cplusplus)
}
#endif
#endif                          /* cbigint_h */
