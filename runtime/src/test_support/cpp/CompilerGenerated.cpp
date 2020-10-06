/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CompilerGenerated.h"
#include "Types.h"

namespace {

TypeInfo theAnyTypeInfoImpl = {};
TypeInfo theArrayTypeInfoImpl = {};
TypeInfo theBooleanArrayTypeInfoImpl = {};
TypeInfo theByteArrayTypeInfoImpl = {};
TypeInfo theCharArrayTypeInfoImpl = {};
TypeInfo theDoubleArrayTypeInfoImpl = {};
TypeInfo theFloatArrayTypeInfoImpl = {};
TypeInfo theForeignObjCObjectTypeInfoImpl = {};
TypeInfo theFreezableAtomicReferenceTypeInfoImpl = {};
TypeInfo theIntArrayTypeInfoImpl = {};
TypeInfo theLongArrayTypeInfoImpl = {};
TypeInfo theNativePtrArrayTypeInfoImpl = {};
TypeInfo theObjCObjectWrapperTypeInfoImpl = {};
TypeInfo theOpaqueFunctionTypeInfoImpl = {};
TypeInfo theShortArrayTypeInfoImpl = {};
TypeInfo theStringTypeInfoImpl = {};
TypeInfo theThrowableTypeInfoImpl = {};
TypeInfo theUnitTypeInfoImpl = {};
TypeInfo theWorkerBoundReferenceTypeInfoImpl = {};

ArrayHeader theEmptyStringImpl = { &theStringTypeInfoImpl, /* element count */ 0 };

template <class T>
struct KBox {
    ObjHeader header;
    const T value;
};

} // namespace

extern "C" {

extern const int KonanNeedDebugInfo = 0;

extern const TypeInfo* theAnyTypeInfo = &theAnyTypeInfoImpl;
extern const TypeInfo* theArrayTypeInfo = &theArrayTypeInfoImpl;
extern const TypeInfo* theBooleanArrayTypeInfo = &theBooleanArrayTypeInfoImpl;
extern const TypeInfo* theByteArrayTypeInfo = &theByteArrayTypeInfoImpl;
extern const TypeInfo* theCharArrayTypeInfo = &theCharArrayTypeInfoImpl;
extern const TypeInfo* theDoubleArrayTypeInfo = &theDoubleArrayTypeInfoImpl;
extern const TypeInfo* theFloatArrayTypeInfo = &theFloatArrayTypeInfoImpl;
extern const TypeInfo* theForeignObjCObjectTypeInfo = &theForeignObjCObjectTypeInfoImpl;
extern const TypeInfo* theFreezableAtomicReferenceTypeInfo = &theFreezableAtomicReferenceTypeInfoImpl;
extern const TypeInfo* theIntArrayTypeInfo = &theIntArrayTypeInfoImpl;
extern const TypeInfo* theLongArrayTypeInfo = &theLongArrayTypeInfoImpl;
extern const TypeInfo* theNativePtrArrayTypeInfo = &theNativePtrArrayTypeInfoImpl;
extern const TypeInfo* theObjCObjectWrapperTypeInfo = &theObjCObjectWrapperTypeInfoImpl;
extern const TypeInfo* theOpaqueFunctionTypeInfo = &theOpaqueFunctionTypeInfoImpl;
extern const TypeInfo* theShortArrayTypeInfo = &theShortArrayTypeInfoImpl;
extern const TypeInfo* theStringTypeInfo = &theStringTypeInfoImpl;
extern const TypeInfo* theThrowableTypeInfo = &theThrowableTypeInfoImpl;
extern const TypeInfo* theUnitTypeInfo = &theUnitTypeInfoImpl;
extern const TypeInfo* theWorkerBoundReferenceTypeInfo = &theWorkerBoundReferenceTypeInfoImpl;

extern const ArrayHeader theEmptyArray = { &theArrayTypeInfoImpl, /* element count */0 };

OBJ_GETTER0(TheEmptyString) {
    RETURN_OBJ(theEmptyStringImpl.obj());
}

RUNTIME_NORETURN OBJ_GETTER(makeWeakReferenceCounter, void*) {
    THROW_NOT_IMPLEMENTED
}

RUNTIME_NORETURN OBJ_GETTER(makePermanentWeakReferenceImpl, void*) {
    THROW_NOT_IMPLEMENTED
}

RUNTIME_NORETURN OBJ_GETTER(makeObjCWeakReferenceImpl, void*) {
    THROW_NOT_IMPLEMENTED
}

void checkRangeIndexes(KInt from, KInt to, KInt size) {
    if (from < 0 || to > size) {
        throw std::out_of_range("Index out of bounds: from=" + std::to_string(from)
                + ", to=" + std::to_string(to)
                + ", size=" + std::to_string(size));
    }
    if (from > to) {
        throw std::invalid_argument("Illegal argument: from > to, from=" + std::to_string(from) + ", to=" + std::to_string(to));
    }
}

RUNTIME_NORETURN OBJ_GETTER(WorkerLaunchpad, KRef) {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowWorkerInvalidState() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowNullPointerException() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowArrayIndexOutOfBoundsException() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowClassCastException(const ObjHeader* instance, const TypeInfo* type_info) {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowArithmeticException() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowNumberFormatException() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowOutOfMemoryError() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowNotImplementedError() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowCharacterCodingException() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowIllegalArgumentException() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowIllegalStateException() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowInvalidMutabilityException(KConstRef where) {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowIncorrectDereferenceException() {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowIllegalObjectSharingException(KConstNativePtr typeInfo, KConstNativePtr address) {
    THROW_NOT_IMPLEMENTED
}

void RUNTIME_NORETURN ThrowFreezingException(KRef toFreeze, KRef blocker) {
    THROW_NOT_IMPLEMENTED
}

void ReportUnhandledException(KRef throwable) {
    konan::consolePrintf("Uncaught Kotlin exception.");
}

RUNTIME_NORETURN OBJ_GETTER(DescribeObjectForDebugging, KConstNativePtr typeInfo, KConstNativePtr address) {
    THROW_NOT_IMPLEMENTED
}

void ExceptionReporterLaunchpad(KRef reporter, KRef throwable) {
    THROW_NOT_IMPLEMENTED
}

void Kotlin_WorkerBoundReference_freezeHook(KRef thiz) {
    THROW_NOT_IMPLEMENTED
}

extern const KBoolean BOOLEAN_RANGE_FROM = false;
extern const KBoolean BOOLEAN_RANGE_TO = true;
extern KBox<KBoolean> BOOLEAN_CACHE[] = {
        {{}, false},
        {{}, true},
};

OBJ_GETTER(Kotlin_boxBoolean, KBoolean value) {
    if (value) {
        RETURN_OBJ(&BOOLEAN_CACHE[1].header);
    } else {
        RETURN_OBJ(&BOOLEAN_CACHE[0].header);
    }
}

extern const KByte BYTE_RANGE_FROM = -1;
extern const KByte BYTE_RANGE_TO = 1;
extern KBox<KByte> BYTE_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KChar CHAR_RANGE_FROM = 0;
extern const KChar CHAR_RANGE_TO = 2;
extern KBox<KChar> CHAR_CACHE[] = {
        {{}, 0},
        {{}, 1},
        {{}, 2},
};

extern const KShort SHORT_RANGE_FROM = -1;
extern const KShort SHORT_RANGE_TO = 1;
extern KBox<KShort> SHORT_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KInt INT_RANGE_FROM = -1;
extern const KInt INT_RANGE_TO = 1;
extern KBox<KInt> INT_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KLong LONG_RANGE_FROM = -1;
extern const KLong LONG_RANGE_TO = 1;
extern KBox<KLong> LONG_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

RUNTIME_NORETURN OBJ_GETTER(Kotlin_Throwable_getMessage, KRef throwable) {
    THROW_NOT_IMPLEMENTED
}

} // extern "C"
