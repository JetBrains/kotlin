// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/Attributes.h"
#include "llvm/IR/PassManager.h"
#include "llvm/Support/Error.h"

namespace llvm::kotlin {

class MarkForStackProtector : public PassInfoMixin<MarkForStackProtector> {
  Attribute::AttrKind Kind;

public:
  explicit MarkForStackProtector(Attribute::AttrKind Kind) : Kind(Kind) {}

  bool maybeMark(Function &F);

  PreservedAnalyses run(Function &F, FunctionAnalysisManager &AF);
};

Expected<Attribute::AttrKind> parseStackProtectorPassOptions(StringRef Params);

}
