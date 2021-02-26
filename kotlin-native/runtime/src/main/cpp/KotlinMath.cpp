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

#include <float.h>
#include <math.h>
#include <stdlib.h>

#include "DoubleConversions.h"
#include "Exceptions.h"
#include "KotlinMath.h"
#include "ReturnSlot.h"
#include "Types.h"

#if (__MINGW32__ || __MINGW64__)
#define KONAN_NEED_ASINH_ACOSH 1
#else
#define KONAN_NEED_ASINH_ACOSH 0
#endif


#if KONAN_NEED_ASINH_ACOSH
namespace {

    // MinGW's implmenetation of asinh/acosh function returns NaN for large arguments so we use another implementation.
    // Both implementations derived from boost special math functions and are also used by Kotlin/JVM.
    // Copyright Eric Ford & Hubert Holin 2001.

    constexpr KDouble LN2 = 0.69314718055994530942;
    constexpr KDouble SQRT2 = 1.41421356237309504880;

    KDouble taylor_2_bound = sqrt(DBL_EPSILON);
    KDouble taylor_n_bound = sqrt(taylor_2_bound);
    KDouble upper_taylor_2_bound = 1.0 / taylor_2_bound;
    KDouble upper_taylor_n_bound = 1.0 / taylor_n_bound;

    KDouble custom_asinh(KDouble x) {
        if (x >= +taylor_n_bound) {
            if (x > upper_taylor_n_bound) {
                if (x > upper_taylor_2_bound) {
                    // approximation by laurent series in 1/x at 0+ order from -1 to 0
                    return log(x) + LN2;
                } else {
                    // approximation by laurent series in 1/x at 0+ order from -1 to 1
                    return log(x * 2 + (1.0 / (x * 2)));
                }
            } else {
                return log(x + sqrt(x * x + 1));
            }
        } else if (x <= -taylor_n_bound) {
            return -custom_asinh(-x);
        } else {
            // approximation by taylor series in x at 0 up to order 2
            KDouble result = x;
            if (fabs(x) >= taylor_2_bound) {
                // approximation by taylor series in x at 0 up to order 4
                result -= (x * x * x) / 6;
            }
            return result;
        }
    }

    KDouble custom_acosh(KDouble x) {
        if (x < 1) {
            return NAN;
        } else if (x > upper_taylor_2_bound) {
            // approximation by laurent series in 1/x at 0+ order from -1 to 0
            return log(x) + LN2;
        } else if (x - 1 >= taylor_n_bound) {
            return log(x + sqrt(x * x - 1));
        } else {
            KDouble y = sqrt(x - 1);
            // approximation by taylor series in y at 0 up to order 2
            KDouble result = y;
            if (y >= taylor_2_bound) {
                // approximation by taylor series in y at 0 up to order 4
                result -= (y * y * y) / 12;
            }
            return SQRT2 * result;
        }
    }
}
#endif

extern "C" {

#ifndef KONAN_NO_MATH // We have a platform math library. Call its math functions.

#ifndef KONAN_WASM // Use libm.

// region Double math.

KDouble Kotlin_math_sin(KDouble x) { return sin(x); }
KDouble Kotlin_math_cos(KDouble x) { return cos(x); }
KDouble Kotlin_math_tan(KDouble x) { return tan(x); }
KDouble Kotlin_math_asin(KDouble x) { return asin(x); }
KDouble Kotlin_math_acos(KDouble x) { return acos(x); }
KDouble Kotlin_math_atan(KDouble x) { return atan(x); }
KDouble Kotlin_math_atan2(KDouble y, KDouble x) { return atan2(y, x); }

KDouble Kotlin_math_sinh(KDouble x) { return sinh(x); }
KDouble Kotlin_math_cosh(KDouble x) { return cosh(x); }
KDouble Kotlin_math_tanh(KDouble x) { return tanh(x); }

KDouble Kotlin_math_asinh(KDouble x) {
#if (KONAN_NEED_ASINH_ACOSH)
    return custom_asinh(x);
#else
    return asinh(x);
#endif
}

KDouble Kotlin_math_acosh(KDouble x) {
#if (KONAN_NEED_ASINH_ACOSH)
    return custom_acosh(x);
#else
    return acosh(x);
#endif
}

KDouble Kotlin_math_atanh(KDouble x) { return atanh(x); }

KDouble Kotlin_math_hypot(KDouble x, KDouble y) {
  if (isinf(x) || isinf(y)) return INFINITY;
  if (isnan(x) || isnan(y)) return NAN;
  return hypot(x, y);
}

KDouble Kotlin_math_sqrt(KDouble x) { return sqrt(x); }
KDouble Kotlin_math_exp(KDouble x) { return exp(x); }
KDouble Kotlin_math_expm1(KDouble x) { return expm1(x); }

KDouble Kotlin_math_ln(KDouble x) { return log(x); }
KDouble Kotlin_math_log10(KDouble x) { return log10(x); }
KDouble Kotlin_math_log2(KDouble x) { return log2(x); }
KDouble Kotlin_math_ln1p(KDouble x) { return log1p(x); }

KDouble Kotlin_math_ceil(KDouble x) { return ceil(x); }
KDouble Kotlin_math_floor(KDouble x) { return floor(x); }
KDouble Kotlin_math_round(KDouble x) { return rint(x); }

KDouble Kotlin_math_abs(KDouble x) { return fabs(x); }

// extensions

KDouble Kotlin_math_Double_pow(KDouble thiz, KDouble x) {
  // Kotlin corner cases
  if (x == 0.0 || x == -0.0) return 1.0;
  if (isinf(x) && (thiz == 1.0 || thiz == -1.0)) return NAN;
  return pow(thiz, x);
}

KDouble Kotlin_math_Double_IEEErem(KDouble thiz, KDouble divisor) { return remainder(thiz, divisor); }
KDouble Kotlin_math_Double_withSign(KDouble thiz, KDouble sign) { return copysign(thiz, sign); }

KDouble Kotlin_math_Double_nextUp(KDouble thiz) { return nextafter(thiz, HUGE_VAL); }
KDouble Kotlin_math_Double_nextDown(KDouble thiz) { return nextafter(thiz, -HUGE_VAL); }
KDouble Kotlin_math_Double_nextTowards(KDouble thiz, KDouble to) {
    return (thiz == to) ? to : nextafter(thiz, to);
}

KBoolean Kotlin_math_Double_signBit(KDouble thiz) { return signbit(thiz) != 0; }

// endregion

// region Float math.

KFloat Kotlin_math_sinf(KFloat x) { return sinf(x); }
KFloat Kotlin_math_cosf(KFloat x) { return cosf(x); }
KFloat Kotlin_math_tanf(KFloat x) { return tanf(x); }
KFloat Kotlin_math_asinf(KFloat x) { return asinf(x); }
KFloat Kotlin_math_acosf(KFloat x) { return acosf(x); }
KFloat Kotlin_math_atanf(KFloat x) { return atanf(x); }
KFloat Kotlin_math_atan2f(KFloat y, KFloat x) { return atan2f(y, x); }

KFloat Kotlin_math_sinhf(KFloat x) { return sinhf(x); }
KFloat Kotlin_math_coshf(KFloat x) { return coshf(x); }
KFloat Kotlin_math_tanhf(KFloat x) { return tanhf(x); }

KFloat Kotlin_math_asinhf(KFloat x) {
#if (KONAN_NEED_ASINH_ACOSH)
    return (KFloat)custom_asinh((KDouble)x);
#else
    return asinhf(x);
#endif
}

KFloat Kotlin_math_acoshf(KFloat x) {
#if (KONAN_NEED_ASINH_ACOSH)
    return (KFloat)custom_acosh((KDouble)x);
#else
    return acoshf(x);
#endif
}

KFloat Kotlin_math_atanhf(KFloat x) { return atanhf(x); }

KFloat Kotlin_math_hypotf(KFloat x, KFloat y) {
  if (isinf(x) || isinf(y)) return INFINITY;
  if (isnan(x) || isnan(y)) return NAN;
  return hypotf(x, y);
}

KFloat Kotlin_math_sqrtf(KFloat x) { return sqrtf(x); }
KFloat Kotlin_math_expf(KFloat x) { return expf(x); }
KFloat Kotlin_math_expm1f(KFloat x) { return expm1f(x); }

KFloat Kotlin_math_lnf(KFloat x) { return logf(x); }
KFloat Kotlin_math_log10f(KFloat x) { return log10f(x); }
KFloat Kotlin_math_log2f(KFloat x) { return log2f(x); }
KFloat Kotlin_math_ln1pf(KFloat x) { return log1pf(x); }

KFloat Kotlin_math_ceilf(KFloat x) { return ceilf(x); }
KFloat Kotlin_math_floorf(KFloat x) { return floorf(x); }
KFloat Kotlin_math_roundf(KFloat x) { return rintf(x); }

KFloat Kotlin_math_absf(KFloat x) { return fabsf(x); }

// extensions

KFloat Kotlin_math_Float_pow(KFloat thiz, KFloat x) {
  // Kotlin corner cases
  if (x == 0.0 || x == -0.0) return 1.0;
  if (isinf(x) && (thiz == 1.0 || thiz == -1.0)) return NAN;
  return powf(thiz, x);
}

KFloat Kotlin_math_Float_IEEErem(KFloat thiz, KFloat divisor) { return remainderf(thiz, divisor); }
KFloat Kotlin_math_Float_withSign(KFloat thiz, KFloat sign) { return copysignf(thiz, sign); }

KFloat Kotlin_math_Float_nextUp(KFloat thiz) { return nextafterf(thiz, HUGE_VALF); }
KFloat Kotlin_math_Float_nextDown(KFloat thiz) { return nextafterf(thiz, -HUGE_VALF); }
KFloat Kotlin_math_Float_nextTowards(KFloat thiz, KFloat to) {
    return (thiz == to) ? to : nextafterf(thiz, to);
}

KBoolean Kotlin_math_Float_signBit(KFloat thiz) { return signbit(thiz) != 0; }

// endregion

// region Integer math.

KInt Kotlin_math_absi(KInt x) { return labs(x); }
KLong Kotlin_math_absl(KLong x) { return llabs(x); }

// endregion

#else // KONAN_WASM defined. Use JS math implementation.

#define RETURN_RESULT_OF_JS_CALL(call, doubleArg) {     \
  call(doubleUpper(doubleArg), doubleLower(doubleArg)); \
  return ReturnSlot_getDouble();                        \
}

#define RETURN_RESULT_OF_JS_CALL2(call, doubleArg1, doubleArg2) { \
  call(doubleUpper(doubleArg1),                                   \
       doubleLower(doubleArg1),                                   \
       doubleUpper(doubleArg2),                                   \
       doubleLower(doubleArg2));                                  \
  return ReturnSlot_getDouble();                                  \
}

KDouble Kotlin_math_sin(KDouble x)   { RETURN_RESULT_OF_JS_CALL(knjs__Math_sin,   x); }
KDouble Kotlin_math_cos(KDouble x)   { RETURN_RESULT_OF_JS_CALL(knjs__Math_cos,   x); }
KDouble Kotlin_math_tan(KDouble x)   { RETURN_RESULT_OF_JS_CALL(knjs__Math_tan,   x); }
KDouble Kotlin_math_asin(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_asin,  x); }
KDouble Kotlin_math_acos(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_acos,  x); }
KDouble Kotlin_math_atan(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_atan,  x); }

KDouble Kotlin_math_sinh(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_sinh,  x); }
KDouble Kotlin_math_cosh(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_cosh,  x); }
KDouble Kotlin_math_tanh(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_tanh,  x); }
KDouble Kotlin_math_asinh(KDouble x) { RETURN_RESULT_OF_JS_CALL(knjs__Math_asinh, x); }
KDouble Kotlin_math_acosh(KDouble x) { RETURN_RESULT_OF_JS_CALL(knjs__Math_acosh, x); }
KDouble Kotlin_math_atanh(KDouble x) { RETURN_RESULT_OF_JS_CALL(knjs__Math_atanh, x); }

KDouble Kotlin_math_sqrt(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_sqrt,  x); }
KDouble Kotlin_math_exp(KDouble x)   { RETURN_RESULT_OF_JS_CALL(knjs__Math_exp,   x); }
KDouble Kotlin_math_expm1(KDouble x) { RETURN_RESULT_OF_JS_CALL(knjs__Math_expm1, x); }

KDouble Kotlin_math_ln(KDouble x)    { RETURN_RESULT_OF_JS_CALL(knjs__Math_log,   x); }
KDouble Kotlin_math_log10(KDouble x) { RETURN_RESULT_OF_JS_CALL(knjs__Math_log10, x); }
KDouble Kotlin_math_log2(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_log2,  x); }
KDouble Kotlin_math_ln1p(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_log1p, x); }

KDouble Kotlin_math_ceil(KDouble x)  { RETURN_RESULT_OF_JS_CALL(knjs__Math_ceil,  x); }
KDouble Kotlin_math_floor(KDouble x) { RETURN_RESULT_OF_JS_CALL(knjs__Math_floor, x); }

KDouble Kotlin_math_round(KDouble x) {
  if (fmod(x, 0.5) != 0.0) {
    RETURN_RESULT_OF_JS_CALL(knjs__Math_round, x);
  }
  KDouble f = floor(x);
  return (fmod(f, 2) == 0.0) ? f : ceil(x);
}

KDouble Kotlin_math_abs(KDouble x)   { RETURN_RESULT_OF_JS_CALL(knjs__Math_abs,   x); }

KDouble Kotlin_math_atan2(KDouble y, KDouble x) { RETURN_RESULT_OF_JS_CALL2(knjs__Math_atan2, y, x); }
KDouble Kotlin_math_hypot(KDouble x, KDouble y) { RETURN_RESULT_OF_JS_CALL2(knjs__Math_hypot, x, y); }

// extensions

KDouble Kotlin_math_Double_pow(KDouble thiz, KDouble x) { RETURN_RESULT_OF_JS_CALL2(knjs__Math_pow, thiz, x); }

KDouble Kotlin_math_Double_IEEErem(KDouble thiz, KDouble divisor)  {
  if (isnan(thiz) || isnan(divisor) || isinf(thiz) || divisor == 0.0) {
    return NAN;
  }
  if (!isinf(thiz) && isinf(divisor)) {
    return thiz;
  }
  KDouble rounded = Kotlin_math_round(thiz / divisor);
  return thiz - rounded * divisor;
}

KDouble Kotlin_math_Double_nextUp(KDouble thiz) {
  if (isnan(thiz) || thiz == HUGE_VAL) {
    return thiz;
  }
  if (thiz == 0.0) {
    return DBL_TRUE_MIN;
  }
  return bitsToDouble(doubleToBits(thiz) + (thiz > 0 ? 1 : -1));
}

KDouble Kotlin_math_Double_nextDown(KDouble thiz) {
  if (isnan(thiz) || thiz == -HUGE_VAL) {
    return thiz;
  }
  if (thiz == 0.0) {
    return -DBL_TRUE_MIN;
  }
  return bitsToDouble(doubleToBits(thiz) - (thiz > 0 ? 1 : -1));
}

KDouble Kotlin_math_Double_nextTowards(KDouble thiz, KDouble to) {
  if (isnan(thiz) || isnan(to)) {
    return NAN;
  }
  if (to > thiz) {
    return Kotlin_math_Double_nextUp(thiz);
  }
  if (to < thiz) {
    return Kotlin_math_Double_nextDown(thiz);
  }
  // thiz == to
  return to;
}

KBoolean Kotlin_math_Double_signBit(KDouble thiz) {
  return (doubleToBits(thiz) & (KLong) 1 << 63) != 0;
}

KDouble Kotlin_math_Double_withSign(KDouble thiz, KDouble sign) {
  bool oldSign = Kotlin_math_Double_signBit(thiz);
  bool newSign = Kotlin_math_Double_signBit(sign);
  return (oldSign == newSign) ? thiz : -thiz;
}

// endregion

// region Float math.

KFloat Kotlin_math_sinf(KFloat x)   { return (KFloat)Kotlin_math_sin   (x); }
KFloat Kotlin_math_cosf(KFloat x)   { return (KFloat)Kotlin_math_cos   (x); }
KFloat Kotlin_math_tanf(KFloat x)   { return (KFloat)Kotlin_math_tan   (x); }
KFloat Kotlin_math_asinf(KFloat x)  { return (KFloat)Kotlin_math_asin  (x); }
KFloat Kotlin_math_acosf(KFloat x)  { return (KFloat)Kotlin_math_acos  (x); }
KFloat Kotlin_math_atanf(KFloat x)  { return (KFloat)Kotlin_math_atan  (x); }

KFloat Kotlin_math_sinhf(KFloat x)  { return (KFloat)Kotlin_math_sinh  (x); }
KFloat Kotlin_math_coshf(KFloat x)  { return (KFloat)Kotlin_math_cosh  (x); }
KFloat Kotlin_math_tanhf(KFloat x)  { return (KFloat)Kotlin_math_tanh  (x); }
KFloat Kotlin_math_asinhf(KFloat x) { return (KFloat)Kotlin_math_asinh (x); }
KFloat Kotlin_math_acoshf(KFloat x) { return (KFloat)Kotlin_math_acosh (x); }
KFloat Kotlin_math_atanhf(KFloat x) { return (KFloat)Kotlin_math_atanh (x); }

KFloat Kotlin_math_sqrtf(KFloat x)  { return (KFloat)Kotlin_math_sqrt  (x); }
KFloat Kotlin_math_expf(KFloat x)   { return (KFloat)Kotlin_math_exp   (x); }
KFloat Kotlin_math_expm1f(KFloat x) { return (KFloat)Kotlin_math_expm1 (x); }

KFloat Kotlin_math_lnf(KFloat x)    { return (KFloat)Kotlin_math_ln    (x); }
KFloat Kotlin_math_log10f(KFloat x) { return (KFloat)Kotlin_math_log10 (x); }
KFloat Kotlin_math_log2f(KFloat x)  { return (KFloat)Kotlin_math_log2  (x); }
KFloat Kotlin_math_ln1pf(KFloat x)  { return (KFloat)Kotlin_math_ln1p  (x); }

KFloat Kotlin_math_ceilf(KFloat x)  { return (KFloat)Kotlin_math_ceil  (x); }
KFloat Kotlin_math_floorf(KFloat x) { return (KFloat)Kotlin_math_floor (x); }
KFloat Kotlin_math_roundf(KFloat x) { return (KFloat)Kotlin_math_round (x); }

KFloat Kotlin_math_absf(KFloat x)   { return (KFloat)Kotlin_math_abs   (x); }

KFloat Kotlin_math_atan2f(KFloat y, KFloat x) { return (KFloat)Kotlin_math_atan2(y, x); }
KFloat Kotlin_math_hypotf(KFloat x, KFloat y) { return (KFloat)Kotlin_math_hypot(x, y); }

// extensions

KFloat Kotlin_math_Float_pow(KFloat thiz, KFloat x) { return (KFloat)Kotlin_math_Double_pow(thiz, x); }

KFloat Kotlin_math_Float_IEEErem(KFloat thiz, KFloat divisor) {
  return (KFloat)Kotlin_math_Double_IEEErem(thiz, divisor);
}

KFloat Kotlin_math_Float_withSign(KFloat thiz, KFloat sign) {
  return (KFloat)Kotlin_math_Double_withSign(thiz, sign);
}

KFloat Kotlin_math_Float_nextUp(KFloat thiz)   {
  if (isnan(thiz) || thiz == HUGE_VALF) {
    return thiz;
  }
  if (thiz == 0.0) {
    return FLT_TRUE_MIN;
  }
  return bitsToFloat(floatToBits(thiz) + (thiz > 0 ? 1 : -1));
}

KFloat Kotlin_math_Float_nextDown(KFloat thiz) {
  if (isnan(thiz) || thiz == -HUGE_VALF) {
    return thiz;
  }
  if (thiz == 0.0) {
    return -FLT_TRUE_MIN;
  }
  return bitsToFloat(floatToBits(thiz) - (thiz > 0 ? 1 : -1));
}

KFloat Kotlin_math_Float_nextTowards(KFloat thiz, KFloat to) {
  if (isnan(thiz) || isnan(to)) {
    return NAN;
  }
  if (to > thiz) {
    return Kotlin_math_Float_nextUp(thiz);
  }
  if (to < thiz) {
    return Kotlin_math_Float_nextDown(thiz);
  }
  // thiz == to
  return to;
}

KBoolean Kotlin_math_Float_signBit(KFloat thiz) { return Kotlin_math_Double_signBit(thiz); }

// endregion

// region Integer math

KInt Kotlin_math_absi(KInt x)   { return (x >= 0) ? x : -x; }
KLong Kotlin_math_absl(KLong x) { return (x >= 0) ? x : -x; }

#endif // #ifndef KONAN_WASM

#else // KONAN_NO_MATH defined - we have no patform math library. Throw NotImplementedError from math functions.

namespace {

RUNTIME_NORETURN void NotImplemented() {
  ThrowNotImplementedError();
}

}  // namespace

KDouble Kotlin_math_sin(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_cos(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_tan(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_asin(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_acos(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_atan(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_atan2(KDouble y, KDouble x) { NotImplemented(); }

KDouble Kotlin_math_sinh(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_cosh(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_tanh(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_asinh(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_acosh(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_atanh(KDouble x) { NotImplemented(); }

KDouble Kotlin_math_hypot(KDouble x, KDouble y) { NotImplemented(); }
KDouble Kotlin_math_sqrt(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_exp(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_expm1(KDouble x) { NotImplemented(); }

KDouble Kotlin_math_ln(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_log10(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_log2(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_ln1p(KDouble x) { NotImplemented(); }

KDouble Kotlin_math_ceil(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_floor(KDouble x) { NotImplemented(); }
KDouble Kotlin_math_round(KDouble x) { NotImplemented(); }

KDouble Kotlin_math_abs(KDouble x) { NotImplemented(); }

// extensions

KDouble Kotlin_math_Double_pow(KDouble thiz, KDouble x) { NotImplemented(); }
KDouble Kotlin_math_Double_IEEErem(KDouble thiz, KDouble divisor) { NotImplemented(); }
KDouble Kotlin_math_Double_withSign(KDouble thiz, KDouble sign) { NotImplemented(); }

KDouble Kotlin_math_Double_nextUp(KDouble thiz) { NotImplemented(); }
KDouble Kotlin_math_Double_nextDown(KDouble thiz) { NotImplemented(); }
KDouble Kotlin_math_Double_nextTowards(KDouble thiz, KDouble to) { NotImplemented(); }

KBoolean Kotlin_math_Double_signBit(KDouble thiz) { NotImplemented(); }

// endregion

// region Float math.

KFloat Kotlin_math_sinf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_cosf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_tanf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_asinf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_acosf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_atanf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_atan2f(KFloat y, KFloat x) { NotImplemented(); }

KFloat Kotlin_math_sinhf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_coshf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_tanhf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_asinhf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_acoshf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_atanhf(KFloat x) { NotImplemented(); }

KFloat Kotlin_math_hypotf(KFloat x, KFloat y) { NotImplemented(); }
KFloat Kotlin_math_sqrtf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_expf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_expm1f(KFloat x) { NotImplemented(); }

KFloat Kotlin_math_lnf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_log10f(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_log2f(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_ln1pf(KFloat x) { NotImplemented(); }

KFloat Kotlin_math_ceilf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_floorf(KFloat x) { NotImplemented(); }
KFloat Kotlin_math_roundf(KFloat x) { NotImplemented(); }

KFloat Kotlin_math_absf(KFloat x) { NotImplemented(); }

// extensions

KFloat Kotlin_math_Float_pow(KFloat thiz, KFloat x) { NotImplemented(); }
KFloat Kotlin_math_Float_IEEErem(KFloat thiz, KFloat divisor) { NotImplemented(); }
KFloat Kotlin_math_Float_withSign(KFloat thiz, KFloat sign) { NotImplemented(); }

KFloat Kotlin_math_Float_nextUp(KFloat thiz) { NotImplemented(); }
KFloat Kotlin_math_Float_nextDown(KFloat thiz) {  NotImplemented(); }
KFloat Kotlin_math_Float_nextTowards(KFloat thiz, KFloat to) { NotImplemented(); }

KBoolean Kotlin_math_Float_signBit(KFloat thiz) { NotImplemented(); }

// endregion

// region Integer math

KInt Kotlin_math_absi(KInt x) { NotImplemented(); }
KInt Kotlin_math_mini(KInt a, KInt b) { NotImplemented(); }
KInt Kotlin_math_maxi(KInt a, KInt b) { NotImplemented(); }

KLong Kotlin_math_absl(KLong x) { NotImplemented(); }
KLong Kotlin_math_minl(KLong a, KLong b) { NotImplemented(); }
KLong Kotlin_math_maxl(KLong a, KLong b) { NotImplemented(); }

#endif // #ifndef KONAN_NO_MATH

} // extern "C"
