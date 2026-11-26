// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#ifndef LIBLLVMEXT_KOTLIN_LLVM_DIALECT_H
#define LIBLLVMEXT_KOTLIN_LLVM_DIALECT_H

#include "llvm-c/Core.h"
#include "llvm-c/ExternC.h"
#include "llvm-c/Types.h"

LLVM_C_EXTERN_C_BEGIN

/// Create a new module for Kotlin LLVM code.
///
/// `RuntimeModule` is the runtime bitcode module declaring structs and
/// functions that will be used by LLVM lowerings. See
/// `kotlin-native/runtime/src/compiler_interface`, which generates it.
///
/// The module will inherit context, target and data layout from
/// `RuntimeModule`. It will additionally register a number of Kotlin-specific
/// intrinsics. See `kotlin-native/libllvmext/KotlinLLVMDialect.md` for
/// additional documentation.
///
/// The returned module should eventually be disposed with `LLVMDisposeModule`
/// to avoid memory leaks.
LLVMModuleRef LLVMKotlinModuleCreate(const char *ModuleID,
                                     LLVMModuleRef RuntimeModule);

/// Kotlin calling convention.
///
/// Only function with this calling convention are allowed to take Kotlin
/// objects as parameters or return a Kotlin object.
extern const unsigned LLVMKotlinCallConv;

typedef enum {
  /// Regular Kotlin function.
  LLVMKotlinKtFunctionOrigin,
  /// Kotlin function defined in the runtime library.
  LLVMKotlinKtRtFunctionOrigin,
  /// C->Kotlin function bridge.
  LLVMKotlinCToKtFunctionOrigin,
  /// A placeholder origin.
  LLVMKotlinNoFunctionOrigin = 127
} LLVMKotlinFunctionOrigin;

/// Set the origin for function.
///
/// `LLVMKotlinNoFunctionOrigin` erases the origin value.
void LLVMKotlinSetFunctionOrigin(LLVMValueRef Fn,
                                 LLVMKotlinFunctionOrigin Origin);

/// Get the origin of Kotlin function.
///
/// When no origin is present, returns `LLVMKotlinNoFunctionOrigin`
LLVMKotlinFunctionOrigin LLVMKotlinGetFunctionOrigin(LLVMValueRef Fn);

LLVM_C_EXTERN_C_END

#endif
