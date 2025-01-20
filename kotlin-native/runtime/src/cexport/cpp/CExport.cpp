/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Exceptions.h"
#include "ExternalRCRef.hpp"
#include "KString.h"
#include "Memory.h"
#include "Runtime.h"
#include "Types.h"

using namespace kotlin;

extern "C" RUNTIME_NOTHROW mm::RawExternalRCRef* Kotlin_CExport_createStablePointer(KRef obj) {
    return mm::createRetainedExternalRCRef(obj);
}

extern "C" RUNTIME_NOTHROW void Kotlin_CExport_disposeStablePointer(mm::RawExternalRCRef* ref) {
    mm::releaseAndDisposeExternalRCRef(ref);
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(Kotlin_CExport_derefStablePointer, mm::RawExternalRCRef* ref) {
    AssertThreadState(ThreadState::kRunnable);
    RETURN_OBJ(mm::dereferenceExternalRCRef(ref));
}

extern "C" RUNTIME_NOTHROW KBoolean Kotlin_CExport_isInstance(mm::RawExternalRCRef* ref, const TypeInfo* typeInfo) {
    auto refTypeInfo = mm::dereferenceExternalRCRef(ref)->type_info();
    return IsSubtype(refTypeInfo, typeInfo);
}

extern "C" RUNTIME_NOTHROW OBJ_GETTER(Kotlin_CExport_allocInstance, const TypeInfo* typeInfo) {
    RETURN_RESULT_OF(AllocInstance, typeInfo);
}

#define PRIMITIVE_TYPES(X) \
    X(Boolean) \
    X(Char) \
    X(Byte) \
    X(Short) \
    X(Int) \
    X(Long) \
    X(UByte) \
    X(UShort) \
    X(UInt) \
    X(ULong) \
    X(Float) \
    X(Double)

#define GENERATE_CEXPORT_BOX_UNBOX(name) \
    extern "C" OBJ_GETTER(Kotlin_box ## name, K ## name value); \
    extern "C" K ## name Kotlin_unbox ## name(KRef value); \
    extern "C" mm::RawExternalRCRef* Kotlin_CExport_box ## name(K ## name value) { \
        CalledFromNativeGuard guard; \
        ObjHolder holder; \
        auto result = Kotlin_box ## name(value, holder.slot()); \
        return Kotlin_CExport_createStablePointer(result); \
    } \
    extern "C" K ## name Kotlin_CExport_unbox ## name(mm::RawExternalRCRef* value) { \
        CalledFromNativeGuard guard; \
        ObjHolder holder; \
        auto result = Kotlin_CExport_derefStablePointer(value, holder.slot()); \
        return Kotlin_unbox ## name(result); \
    }
PRIMITIVE_TYPES(GENERATE_CEXPORT_BOX_UNBOX)
#undef GENERATE_CEXPORT_BOX_UNBOX
#undef PRIMITIVE_TYPES

extern "C" OBJ_GETTER0(Kotlin_boxUnit);
extern "C" KNativePtr Kotlin_CExport_boxUnit() {
    CalledFromNativeGuard guard;
    ObjHolder holder;
    auto result = Kotlin_boxUnit(holder.slot());
    return Kotlin_CExport_createStablePointer(result);
}

extern "C" void Kotlin_CExport_disposeCString(const char* ptr) {
    DisposeCString(const_cast<char*>(ptr));
}

extern "C" KNativePtr Kotlin_CExport_createSingleton(KRef (instance)(KRef*)) {
    CalledFromNativeGuard guard;
    ObjHolder holder;
    auto result = instance(holder.slot());
    return Kotlin_CExport_createStablePointer(result);
}

extern "C" OBJ_GETTER(Kotlin_CExport_createKotlinStringFromCString, const char* str) {
    RETURN_RESULT_OF(CreateStringFromCString, str);
}

extern "C" char* Kotlin_CExport_createCStringFromKotlinString(KRef str) {
    return CreateCStringFromString(str);
}

extern "C" FrameOverlay* Kotlin_CExport_enterBridge() {
    Kotlin_initRuntimeIfNeeded();
    Kotlin_mm_switchThreadStateRunnable();
    return getCurrentFrame();
}

extern "C" RUNTIME_NOTHROW void Kotlin_CExport_exitBridge() {
    Kotlin_mm_switchThreadStateNative();
}

extern "C" void Kotlin_CExport_handleBridgeException(FrameOverlay* frame) {
    SetCurrentFrame(reinterpret_cast<ObjHeader**>(frame));
    HandleCurrentExceptionWhenLeavingKotlinCode();
}

extern "C" void Kotlin_CExport_bridgeAddStackVariable(KRef* frame) {
    EnterFrame(frame, 0, sizeof(FrameOverlay) / sizeof(void*));
}

extern "C" void Kotlin_CExport_bridgeRemoveStackVariable(KRef* frame) {
    LeaveFrame(frame, 0, sizeof(FrameOverlay) / sizeof(void*));
}
