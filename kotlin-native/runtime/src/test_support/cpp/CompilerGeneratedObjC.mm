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

// Declarations from the "objc" module (see ObjCExportCollections.mm).
OBJ_GETTER0(Kotlin_NSArrayAsKList_create) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER0(Kotlin_NSMutableArrayAsKMutableList_create) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER0(Kotlin_NSSetAsKSet_create) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER0(Kotlin_NSDictionaryAsKMap_create) {
    throw std::runtime_error("Not implemented for tests");
}

KBoolean Kotlin_Iterator_hasNext(KRef iterator) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_Iterator_next, KRef iterator) {
    throw std::runtime_error("Not implemented for tests");
}

KInt Kotlin_Collection_getSize(KRef collection) {
    throw std::runtime_error("Not implemented for tests");
}

KBoolean Kotlin_Set_contains(KRef set, KRef element) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_Set_getElement, KRef set, KRef element) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_Set_iterator, KRef set) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_MutableCollection_removeObject(KRef collection, KRef element) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_MutableCollection_addObject(KRef list, KRef obj) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_MutableSet_createWithCapacity, KInt capacity) {
    throw std::runtime_error("Not implemented for tests");
}

KInt Kotlin_Map_getSize(KRef map) {
    throw std::runtime_error("Not implemented for tests");
}

KBoolean Kotlin_Map_containsKey(KRef map, KRef key) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_Map_get, KRef map, KRef key) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_Map_keyIterator, KRef map) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_List_get, KRef list, KInt index) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_MutableMap_createWithCapacity, KInt capacity) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_MutableMap_set(KRef map, KRef key, KRef value) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_MutableMap_remove(KRef map, KRef key) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_MutableList_addObjectAtIndex(KRef list, KInt index, KRef obj) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_MutableList_removeObjectAtIndex(KRef list, KInt index) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_MutableList_removeLastObject(KRef list) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_MutableList_setObject(KRef list, KInt index, KRef obj) {
    throw std::runtime_error("Not implemented for tests");
}

} // extern "C"

#endif // KONAN_OBJC_INTEROP
