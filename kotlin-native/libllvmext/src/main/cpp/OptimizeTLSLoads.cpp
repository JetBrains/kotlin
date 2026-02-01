// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#include "OptimizeTLSLoads.hpp"

#include "llvm/IR/Analysis.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/PassManager.h"

using namespace llvm;

GlobalValue& kotlin::OptimizeTLSLoadsPass::getCurrentThreadTLV(Module &M) {
}

bool kotlin::OptimizeTLSLoadsPass::removeMultipleThreadDataLoads(Module &M) {
  auto& CurrentThreadTLV = getCurrentThreadTLV(M);
  bool MadeChanges = false;
  for (auto &F : M) {
    auto Result = removeMultipleThreadDataLoads(F, CurrentThreadTLV);
    MadeChanges |= Result;
  }
  return MadeChanges;
}

bool kotlin::OptimizeTLSLoadsPass::removeMultipleThreadDataLoads(Function &F, GlobalValue &CurrentThreadTLV) {
  if (F.isDeclaration())
    return false;
  if (!F.getName().starts_with("kfun:"))
    return false;
  // TODO
  return true;
}

PreservedAnalyses kotlin::OptimizeTLSLoadsPass::run(Module &M, ModuleAnalysisManager &) {
  if (!removeMultipleThreadDataLoads(M))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}
