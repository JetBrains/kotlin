#include "Memory.h"
#include "Porting.h"
#include <typeinfo>

#ifndef KONAN_NO_EXCEPTIONS

std::type_info const* ExceptionObjHolderRTTI;

// Just some DCE-surviving code referencing RTTI of ExceptionObjHolder.
// This is needed during compilation to cache.
void referenceExceptionObjHolderRTTI() {
  ExceptionObjHolderRTTI = &typeid(ExceptionObjHolder);
}

#endif