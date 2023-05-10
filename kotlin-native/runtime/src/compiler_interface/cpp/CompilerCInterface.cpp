/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "TypeInfo.h"
#include "Memory.h"
#include "Types.h"
#include "Runtime.h"
#include "Exceptions.h"
#include "MemorySharedRefs.hpp"

#define touchType(type) void touch##type(type*) {}
#define touchFunction(function) void* touch##function() { return reinterpret_cast<void*>(&::function); }

// Types and functions used by the compiler (at Runtime.kt and ContextUtils.kt)
#ifdef __cplusplus
extern "C" {
#endif

touchType(TypeInfo)
touchType(ExtendedTypeInfo)
touchType(InterfaceTableRecord)
touchType(AssociatedObjectTableRecord)

touchType(ObjHeader)
touchType(ArrayHeader)
touchType(FrameOverlay)

touchType(KRefSharedHolder)

touchFunction(AllocInstance)
touchFunction(AllocArrayInstance)
touchFunction(InitAndRegisterGlobal)
touchFunction(UpdateHeapRef)
touchFunction(UpdateStackRef)
touchFunction(UpdateVolatileHeapRef)
touchFunction(CompareAndSwapVolatileHeapRef)
touchFunction(CompareAndSetVolatileHeapRef)
touchFunction(GetAndSetVolatileHeapRef)
touchFunction(UpdateReturnRef)
touchFunction(ZeroHeapRef)
touchFunction(ZeroArrayRefs)

touchFunction(EnterFrame)
touchFunction(LeaveFrame)
touchFunction(SetCurrentFrame)
touchFunction(CheckCurrentFrame)

touchFunction(MutationCheck)
touchFunction(CheckLifetimesConstraint)
touchFunction(FreezeSubgraph)
touchFunction(CheckGlobalsAccessible)

touchFunction(LookupInterfaceTableRecord)
touchFunction(IsSubtype)
touchFunction(IsSubclassFast)

touchFunction(ThrowException)
touchFunction(Kotlin_getExceptionObject)

touchFunction(AppendToInitializersTail)
touchFunction(CallInitGlobalPossiblyLock)
touchFunction(CallInitThreadLocal)

touchFunction(AddTLSRecord)
touchFunction(LookupTLS)

touchFunction(Kotlin_initRuntimeIfNeeded)

touchFunction(KRefSharedHolder_initLocal)
touchFunction(KRefSharedHolder_init)
touchFunction(KRefSharedHolder_dispose)
touchFunction(KRefSharedHolder_ref)

touchFunction(Kotlin_mm_switchThreadStateNative)
touchFunction(Kotlin_mm_switchThreadStateRunnable)
touchFunction(Kotlin_mm_safePointFunctionPrologue)
touchFunction(Kotlin_mm_safePointWhileLoopBody)

touchFunction(Kotlin_processObjectInMark)
touchFunction(Kotlin_processArrayInMark)
touchFunction(Kotlin_processFieldInMark)
touchFunction(Kotlin_processEmptyObjectInMark)

#ifdef __cplusplus
} // extern "C"
#endif
