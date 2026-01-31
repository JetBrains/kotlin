// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm {
class BasicBlock;
}

namespace llvm::kotlin {

class RemoveRedundantSafepointsPass : public PassInfoMixin<RemoveRedundantSafepointsPass> {
  bool ShouldInline;

  bool removeAndInlineSafepoints(BasicBlock &BB, bool RemoveFirst);

public:
  explicit RemoveRedundantSafepointsPass(bool ShouldInline) : ShouldInline(ShouldInline) {}

  bool removeSafepoints(Function &F);

  PreservedAnalyses run(Function &F, FunctionAnalysisManager &AF);
};

Expected<bool> parseRemoveRedundantSafepointsPassOptions(StringRef Params);

}
