/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "TestSupportCompilerGenerated.hpp"

#include "ObjectTestSupport.hpp"
#include "Types.h"

using kotlin::test_support::internal::createCleanerWorkerMock;
using kotlin::test_support::internal::shutdownCleanerWorkerMock;
using kotlin::test_support::internal::reportUnhandledExceptionMock;
using kotlin::test_support::internal::Kotlin_runUnhandledExceptionHookMock;

testing::MockFunction<KInt()>* kotlin::test_support::internal::createCleanerWorkerMock = nullptr;
testing::MockFunction<void(KInt, bool)>* kotlin::test_support::internal::shutdownCleanerWorkerMock = nullptr;
testing::MockFunction<void(KRef)>* kotlin::test_support::internal::reportUnhandledExceptionMock = nullptr;
testing::MockFunction<void(KRef)>* kotlin::test_support::internal::Kotlin_runUnhandledExceptionHookMock = nullptr;

namespace {

struct EmptyPayload {
    using Field = ObjHeader* EmptyPayload::*;
    static constexpr std::array<Field, 0> kFields{};
};

kotlin::test_support::TypeInfoHolder theAnyTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
kotlin::test_support::TypeInfoHolder theArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<ObjHeader*>()};
kotlin::test_support::TypeInfoHolder theBooleanArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KBoolean>()};
kotlin::test_support::TypeInfoHolder theByteArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KByte>()};
kotlin::test_support::TypeInfoHolder theCharArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KChar>()};
kotlin::test_support::TypeInfoHolder theDoubleArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KDouble>()};
kotlin::test_support::TypeInfoHolder theFloatArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KFloat>()};
kotlin::test_support::TypeInfoHolder theForeignObjCObjectTypeInfoHolder{
        kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
kotlin::test_support::TypeInfoHolder theFreezableAtomicReferenceTypeInfoHolder{
        kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
kotlin::test_support::TypeInfoHolder theIntArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KInt>()};
kotlin::test_support::TypeInfoHolder theLongArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KLong>()};
kotlin::test_support::TypeInfoHolder theNativePtrArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KNativePtr>()};
kotlin::test_support::TypeInfoHolder theObjCObjectWrapperTypeInfoHolder{
        kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
kotlin::test_support::TypeInfoHolder theOpaqueFunctionTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
kotlin::test_support::TypeInfoHolder theShortArrayTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ArrayBuilder<KShort>()};
kotlin::test_support::TypeInfoHolder theStringTypeInfoHolder{
        kotlin::test_support::TypeInfoHolder::ArrayBuilder<KChar>().addFlag(TF_IMMUTABLE)};
kotlin::test_support::TypeInfoHolder theThrowableTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
kotlin::test_support::TypeInfoHolder theUnitTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
kotlin::test_support::TypeInfoHolder theWorkerBoundReferenceTypeInfoHolder{
        kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};
kotlin::test_support::TypeInfoHolder theCleanerImplTypeInfoHolder{kotlin::test_support::TypeInfoHolder::ObjectBuilder<EmptyPayload>()};

ArrayHeader theEmptyStringImpl = {theStringTypeInfoHolder.typeInfo(), /* element count */ 0};

template <class T>
struct KBox {
    ObjHeader header;
    const T value;
};

} // namespace

extern "C" {

extern const int32_t KonanNeedDebugInfo = 1;
extern const int32_t Kotlin_runtimeAssertsMode = static_cast<int32_t>(kotlin::compiler::RuntimeAssertsMode::kPanic);
extern const char* const Kotlin_runtimeLogs = nullptr;

extern const TypeInfo* theAnyTypeInfo = theAnyTypeInfoHolder.typeInfo();
extern const TypeInfo* theArrayTypeInfo = theArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theBooleanArrayTypeInfo = theBooleanArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theByteArrayTypeInfo = theByteArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theCharArrayTypeInfo = theCharArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theDoubleArrayTypeInfo = theDoubleArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theFloatArrayTypeInfo = theFloatArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theForeignObjCObjectTypeInfo = theForeignObjCObjectTypeInfoHolder.typeInfo();
extern const TypeInfo* theFreezableAtomicReferenceTypeInfo = theFreezableAtomicReferenceTypeInfoHolder.typeInfo();
extern const TypeInfo* theIntArrayTypeInfo = theIntArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theLongArrayTypeInfo = theLongArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theNativePtrArrayTypeInfo = theNativePtrArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theObjCObjectWrapperTypeInfo = theObjCObjectWrapperTypeInfoHolder.typeInfo();
extern const TypeInfo* theOpaqueFunctionTypeInfo = theOpaqueFunctionTypeInfoHolder.typeInfo();
extern const TypeInfo* theShortArrayTypeInfo = theShortArrayTypeInfoHolder.typeInfo();
extern const TypeInfo* theStringTypeInfo = theStringTypeInfoHolder.typeInfo();
extern const TypeInfo* theThrowableTypeInfo = theThrowableTypeInfoHolder.typeInfo();
extern const TypeInfo* theUnitTypeInfo = theUnitTypeInfoHolder.typeInfo();
extern const TypeInfo* theWorkerBoundReferenceTypeInfo = theWorkerBoundReferenceTypeInfoHolder.typeInfo();
extern const TypeInfo* theCleanerImplTypeInfo = theCleanerImplTypeInfoHolder.typeInfo();

extern const ArrayHeader theEmptyArray = {theArrayTypeInfoHolder.typeInfo(), /* element count */ 0};

OBJ_GETTER0(TheEmptyString) {
    RETURN_OBJ(theEmptyStringImpl.obj());
}

RUNTIME_NORETURN OBJ_GETTER(makeWeakReferenceCounter, void*) {
    throw std::runtime_error("Not implemented for tests");
}

RUNTIME_NORETURN OBJ_GETTER(makePermanentWeakReferenceImpl, void*) {
    throw std::runtime_error("Not implemented for tests");
}

RUNTIME_NORETURN OBJ_GETTER(makeObjCWeakReferenceImpl, void*) {
    throw std::runtime_error("Not implemented for tests");
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
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowWorkerAlreadyTerminated() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowWrongWorkerOrAlreadyTerminated() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowCannotTransferOwnership() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowFutureInvalidState() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowNullPointerException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowArrayIndexOutOfBoundsException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowClassCastException(const ObjHeader* instance, const TypeInfo* type_info) {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowArithmeticException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowNumberFormatException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowOutOfMemoryError() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowNotImplementedError() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowCharacterCodingException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowIllegalArgumentException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowIllegalStateException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowInvalidMutabilityException(KConstRef where) {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowIncorrectDereferenceException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowFileFailedToInitializeException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowIllegalObjectSharingException(KConstNativePtr typeInfo, KConstNativePtr address) {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowFreezingException(KRef toFreeze, KRef blocker) {
    throw std::runtime_error("Not implemented for tests");
}

void ReportUnhandledException(KRef throwable) {
    if (!reportUnhandledExceptionMock) throw std::runtime_error("Not implemented for tests");

    return reportUnhandledExceptionMock->Call(throwable);
}

RUNTIME_NORETURN OBJ_GETTER(DescribeObjectForDebugging, KConstNativePtr typeInfo, KConstNativePtr address) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_runUnhandledExceptionHook(KRef throwable) {
    if (!Kotlin_runUnhandledExceptionHookMock) throw std::runtime_error("Not implemented for tests");

    return Kotlin_runUnhandledExceptionHookMock->Call(throwable);
}

void Kotlin_WorkerBoundReference_freezeHook(KRef thiz) {
    throw std::runtime_error("Not implemented for tests");
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

OBJ_GETTER(Kotlin_boxByte, KByte value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxShort, KShort value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxInt, KInt value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxLong, KLong value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxUByte, KUByte value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxUShort, KUShort value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxUInt, KUInt value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxULong, KULong value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxFloat, KFloat value) {
    throw std::runtime_error("Not implemented for tests");
}

OBJ_GETTER(Kotlin_boxDouble, KDouble value) {
    throw std::runtime_error("Not implemented for tests");
}

RUNTIME_NORETURN OBJ_GETTER(Kotlin_Throwable_getMessage, KRef throwable) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_CleanerImpl_shutdownCleanerWorker(KInt worker, bool executeScheduledCleaners) {
    if (!shutdownCleanerWorkerMock) throw std::runtime_error("Not implemented for tests");

    return shutdownCleanerWorkerMock->Call(worker, executeScheduledCleaners);
}

KInt Kotlin_CleanerImpl_createCleanerWorker() {
    if (!createCleanerWorkerMock) throw std::runtime_error("Not implemented for tests");

    return createCleanerWorkerMock->Call();
}

} // extern "C"

