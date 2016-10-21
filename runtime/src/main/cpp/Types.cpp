#include "Natives.h"
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

const TypeInfo implCloneableTypeInfo = {
  // kotlin.Cloneable
  { 0x2d, 0x4b, 0x3d, 0x2f, 0x69, 0x0d, 0x1c, 0xb5, 0x6e, 0xc6,
    0x67, 0xa7, 0x24, 0x57, 0x35, 0x98, 0xa4, 0x23, 0x0b, 0xc5 },
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

void* const implByteArrayTypeInfoVTbl[] = {
  reinterpret_cast<void*>(Kotlin_ByteArray_clone)
};

const MethodTableRecord implByteArrayTypeInfoMethods[] = {
  // TODO: fill in hash.
  { 0, reinterpret_cast<void*>(Kotlin_ByteArray_clone) }
};

const TypeInfo* implByteArrayTypeInfoIfaces[] = {
  &implCloneableTypeInfo
};

const TypeInfo implByteArrayTypeInfo = {
  // kotlin.ByteArray
  { 0x9e, 0x23, 0xa6, 0xa6, 0x91, 0x9b, 0x6b, 0x0a, 0x00, 0xc5,
    0x35, 0xe8, 0xd9, 0xd1, 0xa3, 0xb6, 0xc5, 0xc6, 0xd7, 0x65 },
  -1,                            // instanceSize_, array of 1 byte elements
  &implAnyTypeInfo,              // superType_
  nullptr,                       // objOffsets
  0,                             // objOffsetsCount_
  implByteArrayTypeInfoIfaces,   // implementedInterfaces_
  1,                             // implementedInterfacesCount_
  implByteArrayTypeInfoVTbl,     // vtable_
  implByteArrayTypeInfoMethods,  // openMethods_
  1,                             // openMethodsCount_
  nullptr,                       // fields_
  0                              // fieldsCount_
};

void* const implCharArrayTypeInfoVTbl[] = {
  reinterpret_cast<void*>(Kotlin_CharArray_clone)
};

const MethodTableRecord implCharArrayTypeInfoMethods[] = {
  // TODO: fill in hash.
  { 0, reinterpret_cast<void*>(Kotlin_CharArray_clone) }
};

const TypeInfo* implCharArrayTypeInfoIfaces[] = {
  &implCloneableTypeInfo
};

const TypeInfo implCharArrayTypeInfo = {
  // kotlin.CharArray
  { 0x70, 0x88, 0xd4, 0x20, 0x91, 0x6e, 0x25, 0x80, 0x33, 0x64,
    0x5a, 0x8d, 0x56, 0xaf, 0x99, 0x19, 0x14, 0xde, 0x5b, 0x71 },
  -2,                           // instanceSize_, array of 2 byte elements
  &implAnyTypeInfo,             // superType_
  nullptr,                      // objOffsets
  0,                            // objOffsetsCount_
  implCharArrayTypeInfoIfaces,  // implementedInterfaces_
  1,                            // implementedInterfacesCount_
  implCharArrayTypeInfoVTbl,    // vtable_
  implCharArrayTypeInfoMethods, // openMethods_
  1,                            // openMethodsCount_
  nullptr,                      // fields_
  0                             // fieldsCount_
};

void* const implIntArrayTypeInfoVTbl[] = {
  reinterpret_cast<void*>(Kotlin_IntArray_clone)
};

const MethodTableRecord implIntArrayTypeInfoMethods[] = {
  // TODO: fill in hash.
  { 0, reinterpret_cast<void*>(Kotlin_IntArray_clone) }
};

const TypeInfo* implIntArrayTypeInfoIfaces[] = {
  &implCloneableTypeInfo
};

const TypeInfo implIntArrayTypeInfo = {
  // kotlin.IntArray
  { 0xdd, 0x69, 0x38, 0x31, 0x3e, 0x03, 0xc6, 0xfd, 0x88, 0x8f,
    0x1c, 0x83, 0x18, 0x06, 0xcc, 0xcb, 0x8d, 0x71, 0xd1, 0x4c },
  -4,                           // instanceSize_, array of 4 byte elements
  &implAnyTypeInfo,             // superType_
  nullptr,                      // objOffsets
  0,                            // objOffsetsCount_
  implIntArrayTypeInfoIfaces,   // implementedInterfaces_
  1,                            // implementedInterfacesCount_
  implIntArrayTypeInfoVTbl,     // vtable_
  implIntArrayTypeInfoMethods,  // openMethods_
  1,                            // openMethodsCount_
  nullptr,                      // fields_
  0                             // fieldsCount_
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
const TypeInfo* theCloneableTypeInfo = &implCloneableTypeInfo;
const TypeInfo* theByteArrayTypeInfo = &implByteArrayTypeInfo;
const TypeInfo* theCharArrayTypeInfo = &implCharArrayTypeInfo;
const TypeInfo* theIntArrayTypeInfo = &implIntArrayTypeInfo;
const TypeInfo* theStringTypeInfo = &implStringTypeInfo;

#ifdef __cplusplus
}
#endif
