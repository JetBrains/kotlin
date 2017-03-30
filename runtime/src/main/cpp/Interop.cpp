/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <assert.h>
#include <stdio.h>
#include <stdint.h>
#include <ffi.h>

#include "Memory.h"
#include "Types.h"

namespace {

typedef int FfiTypeKind;
// Also declared in Varargs.kt
const FfiTypeKind FFI_TYPE_KIND_VOID = 0;
const FfiTypeKind FFI_TYPE_KIND_SINT8 = 1;
const FfiTypeKind FFI_TYPE_KIND_SINT16 = 2;
const FfiTypeKind FFI_TYPE_KIND_SINT32 = 3;
const FfiTypeKind FFI_TYPE_KIND_SINT64 = 4;
const FfiTypeKind FFI_TYPE_KIND_FLOAT = 5;
const FfiTypeKind FFI_TYPE_KIND_DOUBLE = 6;
const FfiTypeKind FFI_TYPE_KIND_POINTER = 7;

ffi_type* convertFfiTypeKindToType(FfiTypeKind typeKind) {
    switch (typeKind) {
        case FFI_TYPE_KIND_VOID: return &ffi_type_void;
        case FFI_TYPE_KIND_SINT8: return &ffi_type_sint8;
        case FFI_TYPE_KIND_SINT16: return &ffi_type_sint16;
        case FFI_TYPE_KIND_SINT32: return &ffi_type_sint32;
        case FFI_TYPE_KIND_SINT64: return &ffi_type_sint64;
        case FFI_TYPE_KIND_FLOAT: return &ffi_type_float;
        case FFI_TYPE_KIND_DOUBLE: return &ffi_type_double;
        case FFI_TYPE_KIND_POINTER: return &ffi_type_pointer;

        default: assert(false);
    }
}

}  // namespace

extern "C" {

void Kotlin_Interop_callWithVarargs(void* codePtr, void* returnValuePtr, FfiTypeKind returnTypeKind,
                     void** arguments, intptr_t* argumentTypeKinds,
                     int fixedArgumentsNumber, int totalArgumentsNumber) {


    ffi_type** argumentTypes = (ffi_type**)argumentTypeKinds;
    // In-place convertion:
    for (int i = 0; i < totalArgumentsNumber; ++i) {
        argumentTypes[i] = convertFfiTypeKindToType((FfiTypeKind) argumentTypeKinds[i]);
    }

    ffi_cif cif;
    ffi_prep_cif_var(&cif, FFI_DEFAULT_ABI,
                     fixedArgumentsNumber, totalArgumentsNumber,
                     convertFfiTypeKindToType(returnTypeKind), argumentTypes);

    ffi_call(&cif, (void (*)())codePtr, returnValuePtr, arguments);
}

void* Kotlin_Interop_createStablePointer(KRef any) {
    ::AddRef(any->container());
    return reinterpret_cast<void*>(any);
}

void Kotlin_Interop_disposeStablePointer(void* pointer) {
    KRef ref = reinterpret_cast<KRef>(pointer);
    ::Release(ref->container());
}

OBJ_GETTER(Kotlin_Interop_derefStablePointer, void* pointer) {
    KRef ref = reinterpret_cast<KRef>(pointer);
    RETURN_OBJ(ref);
}

}
