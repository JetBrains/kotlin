// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#ifndef LIBLLVMEXT_C_API_EXTENSIONS_H
#define LIBLLVMEXT_C_API_EXTENSIONS_H

#include "PassesProfile.h"

#include "llvm-c/Error.h"
#include "llvm-c/ExternC.h"
#include "llvm-c/TargetMachine.h"
#include "llvm-c/Types.h"

LLVM_C_EXTERN_C_BEGIN

void LLVMKotlinInitializeTargets(void);

void LLVMKotlinSetNoTailCall(LLVMValueRef Call);

int LLVMKotlinInlineCall(LLVMValueRef Call);

/// Run `Passes` on module `M`.
/// When `Profile` is not `NULL` also collect profiling data and store the
/// result in it.
///
/// NOTE: This function is not thread-safe, because it may write
///       into global variables by modifying CLI-defined options.
///       Currently these arguments affect the global environment:
///       - SaveIRAfterPasses
///       - SaveIRDirectory
LLVMErrorRef LLVMKotlinRunPasses(LLVMModuleRef M, const char *Passes,
                                 LLVMTargetMachineRef TM, int InlinerThreshold,
                                 LLVMKotlinPassesProfileRef *Profile,
                                 const char *SaveIRAfterPasses,
                                 const char *SaveIRDirectory);

LLVM_C_EXTERN_C_END

#endif
