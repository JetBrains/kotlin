#ifndef RUNTIME_STRING_H
#define RUNTIME_STRING_H

#include "Common.h"
#include "Memory.h"
#include "Types.h"
#include "TypeInfo.h"

#ifdef __cplusplus
extern "C" {
#endif

OBJ_GETTER(CreateStringFromCString, const char* cstring);
OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t size);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_STRING_H
