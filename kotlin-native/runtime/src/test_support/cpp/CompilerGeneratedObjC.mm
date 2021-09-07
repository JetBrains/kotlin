/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if KONAN_OBJC_INTEROP

#include "TestSupportCompilerGenerated.hpp"

#include <Foundation/NSObject.h>
#include <objc/runtime.h>

#include "Types.h"

extern "C" {

Class Kotlin_Interop_getObjCClass(const char* name) {
    Class result = objc_lookUpClass(name);
    if (result == nil) {
        // GTest can display error messages of C++ exceptions so we use them instead of ObjC ones.
        throw std::invalid_argument("Incorrect class name");
    }
    return result;
}

RUNTIME_NORETURN OBJ_GETTER0(Kotlin_NSEnumeratorAsKIterator_create) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_NSEnumeratorAsKIterator_done(KRef thiz) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_NSEnumeratorAsKIterator_setNext(KRef thiz, KRef value) {
    throw std::runtime_error("Not implemented for tests");
}

RUNTIME_NORETURN OBJ_GETTER(Kotlin_ObjCExport_NSErrorAsExceptionImpl, KRef message, KRef error) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_ObjCExport_ThrowCollectionConcurrentModification() {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_ObjCExport_ThrowCollectionTooLarge() {
    throw std::runtime_error("Not implemented for tests");
}

typedef OBJ_GETTER((*convertReferenceFromObjC), id obj);
extern convertReferenceFromObjC Kotlin_ObjCExport_blockToFunctionConverters[] = {};
extern int Kotlin_ObjCExport_blockToFunctionConverters_size = 0;

RUNTIME_NORETURN OBJ_GETTER(Kotlin_ObjCExport_createContinuationArgumentImpl, KRef completionHolder, const TypeInfo** exceptionTypes) {
    throw std::runtime_error("Not implemented for tests");
}

RUNTIME_NORETURN OBJ_GETTER(Kotlin_ObjCExport_getWrappedError, KRef throwable) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_ObjCExport_resumeContinuationFailure(KRef continuation, KRef exception) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_ObjCExport_resumeContinuationSuccess(KRef continuation, KRef result) {
    throw std::runtime_error("Not implemented for tests");
}

} // extern "C"

#endif // KONAN_OBJC_INTEROP
