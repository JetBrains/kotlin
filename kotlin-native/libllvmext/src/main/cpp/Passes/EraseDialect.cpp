// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "EraseDialect.h"

#include "llvm/IR/PassManager.h"

using namespace llvm;
using namespace llvm::kotlin;

PreservedAnalyses EraseDialectPass::run(Module &M, ModuleAnalysisManager &AM) {
  if (!run(M))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

bool EraseDialectPass::run(Module &M) {
  // Nothing to erase yet.
  return false;
}
