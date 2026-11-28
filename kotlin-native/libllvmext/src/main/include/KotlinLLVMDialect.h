// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#ifndef LIBLLVMEXT_KOTLIN_LLVM_DIALECT_H
#define LIBLLVMEXT_KOTLIN_LLVM_DIALECT_H

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

LLVM_C_EXTERN_C_END

#endif
