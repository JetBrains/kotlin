// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "PrepareThreadSanitizer.h"

#include "llvm/IR/Function.h"

using namespace llvm;
using namespace llvm::kotlin;

PreservedAnalyses PrepareThreadSanitizerPass::run(Function &F,
                                                  FunctionAnalysisManager &AF) {
  if (!run(F))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

bool PrepareThreadSanitizerPass::run(Function &F) {
  if (F.isDeclaration())
    return false;
  F.addFnAttr(Attribute::AttrKind::SanitizeThread);
  return true;
}
