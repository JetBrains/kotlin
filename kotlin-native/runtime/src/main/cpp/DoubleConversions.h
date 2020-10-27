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

#ifndef RUNTIME_DOUBLECONVERSIONS_H
#define RUNTIME_DOUBLECONVERSIONS_H

#include "Types.h"

namespace {

typedef union {
  KLong l;
  KDouble d;
} DoubleAlias;

typedef union {
  KInt i;
  KFloat f;
} FloatAlias;

}

inline KDouble bitsToDouble(KLong bits) {
  DoubleAlias alias;
  alias.l = bits;
  return alias.d;
}

inline KLong doubleToBits(KDouble value) {
  DoubleAlias alias;
  alias.d = value;
  return alias.l;
}

inline KFloat bitsToFloat(KInt bits) {
  FloatAlias alias;
  alias.i = bits;
  return alias.f;
}

inline KInt floatToBits(KFloat value) {
  FloatAlias alias;
  alias.f = value;
  return alias.i;
}

extern "C" KInt doubleUpper(KDouble value);
extern "C" KInt doubleLower(KDouble value);

#endif // RUNTIME_DOUBLECONVERSIONS_H
