// Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#ifndef LIBLLVMEXT_PASSES_PROFILE_H
#define LIBLLVMEXT_PASSES_PROFILE_H

#include <llvm-c/ExternC.h>

LLVM_C_EXTERN_C_BEGIN

typedef struct LLVMKotlinOpaquePasesProfile *LLVMKotlinPassesProfileRef;

/// Get serialized view of passes profile.
/// The view is a tsv (tab-separated values), the columns stand for:
/// - name of the pass
/// - pass wall time duration in nanoseconds
/// The returned string is alive until `LLVMKotlinDisposePassesProfile`.
const char* LLVMKotlinPassesProfileAsString(LLVMKotlinPassesProfileRef P);

/// Destroys passes profile.
void LLVMKotlinDisposePassesProfile(LLVMKotlinPassesProfileRef P);

LLVM_C_EXTERN_C_END

#endif // LIBLLVMEXT_PASSES_PROFILE_H
