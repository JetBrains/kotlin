// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

class RemoveRedundantSafepointsPass
    : public PassInfoMixin<RemoveRedundantSafepointsPass> {
public:
  explicit RemoveRedundantSafepointsPass(bool IsSafepointInliningAllowed);

  PreservedAnalyses run(Module &M, ModuleAnalysisManager &AF);

  bool run(Module &M);
  bool run(Function &F);

private:
  bool IsSafepointInliningAllowed;
};

} // namespace llvm::kotlin
