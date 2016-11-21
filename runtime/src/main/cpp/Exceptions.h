#ifndef RUNTIME_EXCEPTIONS_H
#define RUNTIME_EXCEPTIONS_H

#include "Types.h"

#ifdef __cplusplus
extern "C" {
#endif
// Throws null pointer exception. Context is evaluated from caller's address.
void ThrowNullPointerException();
// Throws array index out of bounds exception.
// Context is evaluated from caller's address.
void ThrowArrayIndexOutOfBoundsException();
// Throws class cast exception.
void ThrowClassCastException();
// Throws arbitrary exception.
void ThrowException(KRef exception);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RUNTIME_NAMES_H
