// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#include "ThreadSanitizer.hpp"

#include "llvm/IR/Analysis.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/PassManager.h"

using namespace llvm;

bool kotlin::MarkForThreadSanitizer::maybeMark(Function &F) {
  if (F.isDeclaration())
    return false;
  F.addFnAttr(Attribute::AttrKind::SanitizeThread);
  return true;
}

PreservedAnalyses kotlin::MarkForThreadSanitizer::run(Function &F, FunctionAnalysisManager &) {
  if (!maybeMark(F))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}
