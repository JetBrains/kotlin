// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#include "StackProtector.hpp"

#include "llvm/IR/Analysis.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/PassManager.h"
#include "llvm/Support/FormatVariadic.h"

using namespace llvm;

bool kotlin::MarkForStackProtector::maybeMark(Function &F) {
  if (F.isDeclaration())
    return false;
  F.addFnAttr(Attribute::AttrKind::SanitizeThread);
  if (F.getName() == "__clang_call_terminate")
    return false;
  F.addFnAttr(Kind);
  return true;
}

PreservedAnalyses kotlin::MarkForStackProtector::run(Function &F, FunctionAnalysisManager &) {
  if (!maybeMark(F))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

Expected<Attribute::AttrKind> parseStackProtectorPassOptions(StringRef Params) {
  if (Params.empty()) {
    return Attribute::StackProtect;
  }
  if (Params == "strong") {
    return Attribute::StackProtectStrong;
  }
  if (Params == "req") {
    return Attribute::StackProtectReq;
  }
  return make_error<StringError>(
      formatv("invalid kotlin-ssp pass parameter '{0}'", Params).str(),
      inconvertibleErrorCode());
}
