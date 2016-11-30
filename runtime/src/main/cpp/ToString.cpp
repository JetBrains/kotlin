#include <stdio.h>
#include <string.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" {

KString Kotlin_Byte_toString(KByte value) {
  char cstring[8];
  snprintf(cstring, sizeof(cstring), "%d", value);
  return makeString(cstring);
}

KString Kotlin_Char_toString(KChar value) {
  char cstring[5];
  // TODO: support UTF-8.
  snprintf(cstring, sizeof(cstring), "%c", value);
  return makeString(cstring);
}

KString Kotlin_Short_toString(KShort value) {
  char cstring[8];
  snprintf(cstring, sizeof(cstring), "%d", value);
  return makeString(cstring);
}

KString Kotlin_Int_toString(KInt value) {
  char cstring[16];
  snprintf(cstring, sizeof(cstring), "%d", value);
  return makeString(cstring);
}

KString Kotlin_Long_toString(KLong value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%lld", value);
  return makeString(cstring);
}

// TODO: use David Gay's dtoa() here instead. It's *very* big and ugly.
KString Kotlin_Float_toString(KFloat value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%G", value);
  return makeString(cstring);
}

KString Kotlin_Double_toString(KDouble value) {
  char cstring[32];
  snprintf(cstring, sizeof(cstring), "%G", value);
  return makeString(cstring);
}

KString Kotlin_Boolean_toString(KBoolean value) {
  return  makeString(value ? "true" : "false");
}

} // extern "C"
