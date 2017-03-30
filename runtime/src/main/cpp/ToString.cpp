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

#include <stdio.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Types.h"

extern "C" {

OBJ_GETTER(Kotlin_Any_toString, KConstRef thiz) {
  char cstring[80];
  snprintf(cstring, sizeof(cstring), "%s %p type %p",
           IsArray(thiz) ? "array" : "object",
           thiz, thiz->type_info_);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Byte_toString, KByte value) {
  char cstring[8];
  snprintf(cstring, sizeof(cstring), "%d", value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Char_toString, KChar value) {
  ArrayHeader* result = AllocArrayInstance(
      theStringTypeInfo, 1, OBJ_RESULT)->array();
  *CharArrayAddressOfElementAt(result, 0) = value;
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_Short_toString, KShort value) {
  char cstring[8];
  snprintf(cstring, sizeof(cstring), "%d", value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Int_toString, KInt value) {
  char cstring[16];
  snprintf(cstring, sizeof(cstring), "%d", value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Int_toStringRadix, KInt value, KInt radix) {
  // TODO: maye not fit for smaller radices.
  char cstring[32];
  switch (radix) {
    case 8:
      snprintf(cstring, sizeof(cstring), "%o", value);
      break;
    case 10:
      snprintf(cstring, sizeof(cstring), "%d", value);
      break;
    case 16:
      snprintf(cstring, sizeof(cstring), "%x", value);
      break;
    default:
      RuntimeAssert(false, "Unsupported radix");
  }
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Long_toString, KLong value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%lld", static_cast<long long>(value));
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Long_toStringRadix, KLong value, KInt radix) {
  // TODO: may not fit for smaller radices.
  char cstring[64];
  switch (radix) {
    case 8:
      snprintf(cstring, sizeof(cstring), "%llo", value);
      break;
    case 10:
      snprintf(cstring, sizeof(cstring), "%lld", value);
      break;
    case 16:
      snprintf(cstring, sizeof(cstring), "%llx", value);
      break;
    default:
      RuntimeAssert(false, "Unsupported radix");
  }
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

// TODO: use David Gay's dtoa() here instead. It's *very* big and ugly.
OBJ_GETTER(Kotlin_Float_toString, KFloat value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%G", value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Double_toString, KDouble value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%G", value);
  RETURN_RESULT_OF(CreateStringFromCString, cstring);
}

OBJ_GETTER(Kotlin_Boolean_toString, KBoolean value) {
  RETURN_RESULT_OF(CreateStringFromCString, value ? "true" : "false");
}

} // extern "C"
