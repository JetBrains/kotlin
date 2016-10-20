#ifndef RUNTIME_TYPES_H
#define RUNTIME_TYPES_H

#include "TypeInfo.h"

#ifdef __cplusplus
extern "C" {
#endif

extern const TypeInfo* theAnyTypeInfo;
extern const TypeInfo* theByteArrayTypeInfo;
extern const TypeInfo* theStringTypeInfo;

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_TYPES_H
