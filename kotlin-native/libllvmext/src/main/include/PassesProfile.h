// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#ifndef LIBLLVMEXT_PASSES_PROFILE_H
#define LIBLLVMEXT_PASSES_PROFILE_H

#include <llvm-c/ExternC.h>
#include <stdint.h>

LLVM_C_EXTERN_C_BEGIN

typedef struct LLVMKotlinOpaquePasesProfile *LLVMKotlinPassesProfileRef;

/// Destroys passes profile.
void LLVMKotlinDisposePassesProfile(LLVMKotlinPassesProfileRef P);

LLVM_C_EXTERN_C_END

#endif
