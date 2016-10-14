#ifndef RUNTIME_EXCEPTIONS_H
#define RUNTIME_EXCEPTIONS_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif
// Throws null pointer exception. Context is evaluated from caller's address.
void ThrowNullPointerException();
// Throws array index out of bounds exception.
// Context is evaluated from caller's address.
void ThrowArrayIndexOutOfBoundsException();

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RUNTIME_NAMES_H
