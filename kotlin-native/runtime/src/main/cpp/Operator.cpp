/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <math.h>
#include <limits.h>

#include "DoubleConversions.h"
#include "Natives.h"

#include "Common.h"

extern "C" {

//--- Float -------------------------------------------------------------------//

PERFORMANCE_INLINE KInt    Kotlin_Float_toInt(KFloat a) {
  if (isnan(a)) return 0;
  if (a >= (KFloat) INT32_MAX) return INT32_MAX;
  if (a <= (KFloat) INT32_MIN) return INT32_MIN;
  return a;
}

PERFORMANCE_INLINE KLong   Kotlin_Float_toLong(KFloat a) {
  if (isnan(a)) return 0;
  if (a >= (KFloat) INT64_MAX) return INT64_MAX;
  if (a <= (KFloat) INT64_MIN) return INT64_MIN;
  return a;
}

PERFORMANCE_INLINE KByte   Kotlin_Float_toByte(KFloat a) { return (KByte)  Kotlin_Float_toInt(a); }
PERFORMANCE_INLINE KShort  Kotlin_Float_toShort(KFloat a) { return (KShort) Kotlin_Float_toInt(a); }

PERFORMANCE_INLINE KBoolean Kotlin_Float_isNaN(KFloat a)          { return isnan(a); }
PERFORMANCE_INLINE KBoolean Kotlin_Float_isInfinite(KFloat a)          { return isinf(a); }
PERFORMANCE_INLINE KBoolean Kotlin_Float_isFinite(KFloat a)          { return isfinite(a); }

//--- Double ------------------------------------------------------------------//

PERFORMANCE_INLINE KInt Kotlin_Double_toInt(KDouble a) {
  if (isnan(a)) return 0;
  if (a >= (KDouble) INT32_MAX) return INT32_MAX;
  if (a <= (KDouble) INT32_MIN) return INT32_MIN;
  return a;
}

PERFORMANCE_INLINE KLong Kotlin_Double_toLong(KDouble a) {
  if (isnan(a)) return 0;
  if (a >= (KDouble) INT64_MAX) return INT64_MAX;
  if (a <= (KDouble) INT64_MIN) return INT64_MIN;
  return a;
}

ALWAYS_INLINE KBoolean Kotlin_Double_isNaN(KDouble a)          { return isnan(a); }
ALWAYS_INLINE KBoolean Kotlin_Double_isInfinite(KDouble a)          { return isinf(a); }
ALWAYS_INLINE KBoolean Kotlin_Double_isFinite(KDouble a)          { return isfinite(a); }

//--- Bit operations ---------------------------------------------------------//

ALWAYS_INLINE KInt Kotlin_Int_countOneBits(KInt value) { return __builtin_popcount(value); }
ALWAYS_INLINE KInt Kotlin_Long_countOneBits(KLong value) { return __builtin_popcountll(value); }

ALWAYS_INLINE KInt Kotlin_Int_countTrailingZeroBits(KInt value) { return __builtin_ctz(value); }
ALWAYS_INLINE KInt Kotlin_Long_countTrailingZeroBits(KLong value) { return __builtin_ctzll(value); }

ALWAYS_INLINE KInt Kotlin_Int_countLeadingZeroBits(KInt value) { return __builtin_clz(value); }
ALWAYS_INLINE KInt Kotlin_Long_countLeadingZeroBits(KLong value) { return __builtin_clzll(value); }

}  // extern "C"
