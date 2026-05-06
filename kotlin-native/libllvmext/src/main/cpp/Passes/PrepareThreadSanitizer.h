// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

/// Annotate all defined functions with `sanitize_thread` attribute.
///
/// This can't simply be done in the code generator, because we want
/// this attribute applied to the runtime code as well, which ships as LLVM bitcode.
class PrepareThreadSanitizerPass
    : public PassInfoMixin<PrepareThreadSanitizerPass> {
public:
  PreservedAnalyses run(Function &F, FunctionAnalysisManager &AF);

  bool run(Function &F);
};

} // namespace llvm::kotlin
