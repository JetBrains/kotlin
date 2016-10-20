#include "Types.h"

namespace {
// Use 'echo -n "kotlin.ByteArray" | shasum -| xargs ~/bin/as_array' to generate hash.

const TypeInfo implAnyTypeInfo = {
  // kotlin.Any
  { 0x6a, 0x97, 0xc2, 0x61, 0x64, 0x3a, 0xc6, 0x5e, 0x2a, 0x03,
    0xf9, 0xc1, 0x4c, 0x1f, 0x66, 0x9a, 0x44, 0xb2, 0x6b, 0x11 },
  0,       // instanceSize_
  nullptr, // superType_
  nullptr, // objOffsets
  0,       // objOffsetsCount_
  nullptr, // implementedInterfaces_
  0,       // implementedInterfacesCount_
  nullptr, // vtable_
  nullptr, // openMethods_
  0,       // openMethodsCount_
  nullptr, // fields_
  0        // fieldsCount_
};

const TypeInfo implByteArrayTypeInfo = {
  // kotlin.ByteArray
  { 0x9e, 0x23, 0xa6, 0xa6, 0x91, 0x9b, 0x6b, 0x0a, 0x00, 0xc5,
    0x35, 0xe8, 0xd9, 0xd1, 0xa3, 0xb6, 0xc5, 0xc6, 0xd7, 0x65 },
  -1,               // instanceSize_, array of 1 byte elements
  &implAnyTypeInfo, // superType_
  nullptr,          // objOffsets
  0,                // objOffsetsCount_
  // TODO: actually, shall implement cloneable, it seems.
  nullptr,          // implementedInterfaces_
  0,                // implementedInterfacesCount_
  nullptr,          // vtable_
  nullptr,          // openMethods_
  0,                // openMethodsCount_
  nullptr,          // fields_
  0                 // fieldsCount_
};

const TypeInfo implStringTypeInfo = {
  { 0x5b, 0xac, 0x68, 0xed, 0x43, 0xd4, 0x81, 0x4a, 0x14, 0x18,
    0x10, 0xfa, 0x65, 0x09, 0xb9, 0xef, 0x20, 0x0a, 0xba, 0x78 }, // kotlin.String
  -1,               // instanceSize_, Strings are treated as array of 1 byte elements
  &implAnyTypeInfo, // superType_
  nullptr,          // objOffsets
  0,                // objOffsetsCount_
  nullptr,          // implementedInterfaces_
  0,                // implementedInterfacesCount_
  nullptr,          // vtable_
  nullptr,          // openMethods_
  0,                // openMethodsCount_
  nullptr,          // fields_
  0                 // fieldsCount_
};

}  // namespace

#ifdef __cplusplus
extern "C" {
#endif

const TypeInfo* theAnyTypeInfo = &implAnyTypeInfo;
const TypeInfo* theByteArrayTypeInfo = &implByteArrayTypeInfo;
const TypeInfo* theStringTypeInfo = &implStringTypeInfo;

#ifdef __cplusplus
}
#endif
