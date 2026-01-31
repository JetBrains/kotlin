// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

class RemoveRedundantSafepointsPass : public PassInfoMixin<RemoveRedundantSafepointsPass> {
  bool ShouldInline;

public:
  explicit RemoveRedundantSafepointsPass(bool ShouldInline) : ShouldInline(ShouldInline) {}

  bool removeSafepoints(Module &M);

  PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM);
};

Expected<bool> parseRemoveRedundantSafepointsPassOptions(StringRef Params);

}
