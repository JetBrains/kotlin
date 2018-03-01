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

#ifndef RUNTIME_KOTLINMATH_H
#define RUNTIME_KOTLINMATH_H

#include "Types.h"

#ifdef KONAN_WASM

extern "C" {

// TODO: consider auto-generating this header file.

// Bridges for JS math.
void knjs__Math_abs(KInt xUpper, KInt xLower);
void knjs__Math_acos(KInt xUpper, KInt xLower);
void knjs__Math_acosh(KInt xUpper, KInt xLower);
void knjs__Math_asin(KInt xUpper, KInt xLower);
void knjs__Math_asinh(KInt xUpper, KInt xLower);
void knjs__Math_atan(KInt xUpper, KInt xLower);
void knjs__Math_atan2(KInt yUpper, KInt yLower, KInt xUpper, KInt xLower);
void knjs__Math_atanh(KInt xUpper, KInt xLower);
void knjs__Math_cbrt(KInt xUpper, KInt xLower);
void knjs__Math_ceil(KInt xUpper, KInt xLower);
void knjs__Math_clz32(KInt xUpper, KInt xLower);
void knjs__Math_cos(KInt xUpper, KInt xLower);
void knjs__Math_cosh(KInt xUpper, KInt xLower);
void knjs__Math_exp(KInt xUpper, KInt xLower);
void knjs__Math_expm1(KInt xUpper, KInt xLower);
void knjs__Math_floor(KInt xUpper, KInt xLower);
void knjs__Math_fround(KInt xUpper, KInt xLower);
void knjs__Math_log(KInt xUpper, KInt xLower);
void knjs__Math_log1p(KInt xUpper, KInt xLower);
void knjs__Math_log10(KInt xUpper, KInt xLower);
void knjs__Math_log2(KInt xUpper, KInt xLower);
void knjs__Math_round(KInt xUpper, KInt xLower);
void knjs__Math_sign(KInt xUpper, KInt xLower);
void knjs__Math_sin(KInt xUpper, KInt xLower);
void knjs__Math_sinh(KInt xUpper, KInt xLower);
void knjs__Math_sqrt(KInt xUpper, KInt xLower);
void knjs__Math_tan(KInt xUpper, KInt xLower);
void knjs__Math_tanh(KInt xUpper, KInt xLower);
void knjs__Math_trunc(KInt xUpper, KInt xLower);

void knjs__Math_hypot(KInt xUpper, KInt xLower, KInt yUpper, KInt yLower);
void knjs__Math_max(KInt xUpper, KInt xLower, KInt yUpper, KInt yLower);
void knjs__Math_min(KInt xUpper, KInt xLower, KInt yUpper, KInt yLower);

void knjs__Math_pow(KInt xUpper, KInt xLower, KInt yUpper, KInt yLower);

}

#endif // KONAN_WASM

#endif // RUNTIME_KOTLINMATH_H
