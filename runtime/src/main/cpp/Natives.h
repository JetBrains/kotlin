#ifndef RUNTIME_NATIVES_H
#define RUNTIME_NATIVES_H

#include "Memory.h"
#include "Types.h"

typedef uint16_t KChar;
typedef uint8_t KByte;

#ifdef __cplusplus
extern "C" {
#endif

// TODO: those must be compiler intrinsics afterwards.
KByte Kotlin_ByteArray_get(const ArrayHeader* obj, int32_t index);
void Kotlin_ByteArray_set(ArrayHeader* obj, int32_t index, KByte value);
KChar Kotlin_String_get(const ArrayHeader* obj, int32_t index);
ArrayHeader* Kotlin_String_fromUtf8Array(const ArrayHeader* array);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_NATIVES_H
