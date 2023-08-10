/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

#include <cstdio>
#include <limits.h>
#include <string.h>

#include "KAssert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Porting.h"
#include "Types.h"

namespace {

char int_to_digit(uint32_t value) {
  if (value < 10) {
    return '0' + value;
  } else {
    return 'a' + (value - 10);
  }
}

// Radix is checked on the Kotlin side.
template <typename T> OBJ_GETTER(Kotlin_toStringRadix, T value, KInt radix) {
  if (value == 0) {
    RETURN_RESULT_OF(CreateStringFromCString, "0");
  }
  // In the worst case, we convert to binary, with sign.
  char cstring[sizeof(T) * CHAR_BIT + 2];
  bool negative = (value < 0);
  if  (!negative) {
    value = -value;
  }

  int32_t length = 0;
  while (value < 0) {
    cstring[length++] = int_to_digit(-(value % radix));
    value /= radix;
  }
  if (negative) {
    cstring[length++] = '-';
  }
  for (int i = 0, j = length - 1; i < j; i++, j--) {
    char tmp = cstring[i];
    cstring[i] = cstring[j];
    cstring[j] = tmp;
  }
  cstring[length] = '\0';
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

}  // namespace

extern "C" {

OBJ_GETTER(Kotlin_Byte_toString, KByte value) {
  char cstring[8];
  std::snprintf(cstring, sizeof(cstring), "%d", value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Char_toString, KChar value) {
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, 1, OBJ_RESULT)->array();
  *CharArrayAddressOfElementAt(result, 0) = value;
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_Short_toString, KShort value) {
  char cstring[8];
  std::snprintf(cstring, sizeof(cstring), "%d", value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Int_toString, KInt value) {
  char cstring[16];
  std::snprintf(cstring, sizeof(cstring), "%d", value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Int_toStringRadix, KInt value, KInt radix) {
  RETURN_RESULT_OF(Kotlin_toStringRadix<KInt>, value, radix)
}

OBJ_GETTER(Kotlin_Long_toString, KLong value) {
  char cstring[32];
  std::snprintf(cstring, sizeof(cstring), "%lld", static_cast<long long>(value));
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Long_toStringRadix, KLong value, KInt radix) {
  RETURN_RESULT_OF(Kotlin_toStringRadix<KLong>, value, radix)
}

OBJ_GETTER(Kotlin_DurationValue_formatToExactDecimals, KDouble value, KInt decimals) {
  char cstring[40]; // log(2^62*1_000_000) + 2 (sign, decimal point) + 12 (max decimals)
  std::snprintf(cstring, sizeof(cstring), "%.*f", decimals, value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring)
}


} // extern "C"
