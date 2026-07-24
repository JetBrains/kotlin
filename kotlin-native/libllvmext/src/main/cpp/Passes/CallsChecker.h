// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

/// A module pass for external calls checker instrumentation.
///
/// Creates module constructor. Should be run after DCE: it saves all defined
/// functions in a global, preventing DCE from removing unused ones
class ModuleCallsCheckerPass : public PassInfoMixin<ModuleCallsCheckerPass> {
public:
  PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM);

  bool run(Module &M);

private:
};

} // namespace llvm::kotlin
