// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#ifndef LIBLLVMEXT_BUILD_SHADOW_STACK_H
#define LIBLLVMEXT_BUILD_SHADOW_STACK_H

#include "llvm-c/ExternC.h"
#include "llvm-c/Types.h"

LLVM_C_EXTERN_C_BEGIN

void LLVMKotlinBuildShadowStack(LLVMModuleRef M);

LLVM_C_EXTERN_C_END

#endif
