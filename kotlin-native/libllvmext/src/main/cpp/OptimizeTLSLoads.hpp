// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm {
class GlobalValue;
}

namespace llvm::kotlin {

class OptimizeTLSLoadsPass : public PassInfoMixin<OptimizeTLSLoadsPass> {
public:
  OptimizeTLSLoadsPass() = default;

  GlobalValue& getCurrentThreadTLV(Module &M);

  bool removeMultipleThreadDataLoads(Module &M);

  bool removeMultipleThreadDataLoads(Function &F, GlobalValue& CurrentThreadTLV);

  PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM);
};

}
