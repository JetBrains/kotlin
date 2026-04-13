// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#ifndef LIBLLVMEXT_REMOVE_REDUNDANT_SAFEPOINTS_H
#define LIBLLVMEXT_REMOVE_REDUNDANT_SAFEPOINTS_H

#include "llvm-c/ExternC.h"
#include "llvm-c/Types.h"

LLVM_C_EXTERN_C_BEGIN

void LLVMKotlinRemoveRedundantSafepoints(LLVMModuleRef M,
                                         int IsSafePointInliningAllowed);

LLVM_C_EXTERN_C_END

#endif
