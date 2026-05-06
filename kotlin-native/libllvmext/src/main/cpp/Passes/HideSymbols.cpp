// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "HideSymbols.h"

#include "llvm/IR/Analysis.h"
#include "llvm/IR/GlobalAlias.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/PassManager.h"

using namespace llvm;
using namespace llvm::kotlin;

PreservedAnalyses HideSymbolsPass::run(Module &M, ModuleAnalysisManager &AM) {
  if (!run(M))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

bool HideSymbolsPass::run(Module &M) {
  // The implementation generally follows `InternalizePass::internalizeModule`,
  // but omits some details for simplicity.
  bool Changed = false;

  SmallVector<GlobalValue *, 4> Used;
  collectUsedGlobalVariables(M, Used, false);

  for (auto *V : Used) {
    AlwaysPreserved.insert(V->getName());
  }
  AlwaysPreserved.insert("llvm.used");

  for (Function &F : M) {
    if (!maybeHide(F))
      continue;
    Changed = true;
  }

  for (GlobalVariable &GV : M.globals()) {
    if (!maybeHide(GV))
      continue;
    Changed = true;
  }

  for (GlobalAlias &GA : M.aliases()) {
    if (!maybeHide(GA))
      continue;
    Changed = true;
  }

  return Changed;
}

bool HideSymbolsPass::maybeHide(GlobalValue &GV) {
  if (GV.hasLocalLinkage())
    return false;
  if (GV.isDeclaration())
    return false;
  if (AlwaysPreserved.contains(GV.getName()))
    return false;

  GV.setVisibility(GlobalValue::HiddenVisibility);
  return true;
}
