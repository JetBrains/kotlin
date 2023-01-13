/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef LIBLLVMEXT_REMOVE_REDUNDANT_SAFEPOINTS_H
#define LIBLLVMEXT_REMOVE_REDUNDANT_SAFEPOINTS_H

#endif // LIBLLVMEXT_REMOVE_REDUNDANT_SAFEPOINTS_H

#include <llvm-c/Core.h>
#include <llvm-c/Target.h>

# ifdef __cplusplus
extern "C" {
# endif

void LLVMKotlinRemoveRedundantSafepoints(LLVMModuleRef module, int isSafePointInliningAllowed);

# ifdef __cplusplus
}
# endif