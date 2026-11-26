// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "KotlinLLVMDialect.h"

#include "KotlinCallingConv.h"
#include "KotlinOrigin.h"

#include "llvm/IR/Function.h"
#include "llvm/IR/Module.h"

using namespace llvm;
using namespace llvm::kotlin;

const unsigned LLVMKotlinCallConv =
    static_cast<unsigned>(KotlinCallingConv);

LLVMModuleRef LLVMKotlinModuleCreate(const char *ModuleID,
                                     LLVMModuleRef RuntimeModule) {
  auto &RuntimeM = *unwrap(RuntimeModule);
  auto &Ctx = RuntimeM.getContext();
  auto *M = new Module(ModuleID, Ctx);
  M->setDataLayout(RuntimeM.getDataLayout());
  M->setTargetTriple(RuntimeM.getTargetTriple());
  return wrap(M);
}

void LLVMKotlinSetFunctionOrigin(LLVMValueRef Fn,
                                 LLVMKotlinFunctionOrigin Origin) {
  auto &F = *unwrap<Function>(Fn);
  if (Origin == LLVMKotlinNoFunctionOrigin) {
    F.removeFnAttr(FunctionOriginAttrKind);
    return;
  }
  F.addFnAttr(functionOriginAttribute(F.getContext(),
                                      static_cast<FunctionOrigin>(Origin)));
}

LLVMKotlinFunctionOrigin LLVMKotlinGetFunctionOrigin(LLVMValueRef Fn) {
  auto &F = *unwrap<Function>(Fn);
  auto A = F.getFnAttribute(FunctionOriginAttrKind);
  if (A.getKindAsString() != FunctionOriginAttrKind) {
    return LLVMKotlinNoFunctionOrigin;
  }
  return static_cast<LLVMKotlinFunctionOrigin>(functionOriginValue(A));
}
