#include <assert.h>
#include <stdio.h>
#include <stdint.h>
#include <ffi.h>

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

void callWithVarargs(void* codePtr, void* returnValuePtr, FfiTypeKind returnTypeKind,
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

}
