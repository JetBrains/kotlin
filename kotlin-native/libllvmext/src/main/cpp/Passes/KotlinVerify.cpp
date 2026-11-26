// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "KotlinVerify.h"

#include "llvm/IR/PassManager.h"

using namespace llvm;
using namespace llvm::kotlin;

PreservedAnalyses KotlinVerifyPass::run(Module &M, ModuleAnalysisManager &AM) {
  run(M);
  // The pass never modifies anything, it only fails with an error, when
  // something is wrong.
  return PreservedAnalyses::all();
}

void KotlinVerifyPass::run(Module &M) {
  // Nothing to verify yet.
}
