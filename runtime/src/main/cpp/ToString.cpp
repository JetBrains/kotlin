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

OBJ_GETTER(Kotlin_Long_toString, KLong value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%lld", static_cast<long long>(value));
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
