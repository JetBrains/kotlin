// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

/// A module pass that erases the remnants of Kotlin dialect from LLVM IR.
///
/// Used for debugging the dialect passes.
///
/// See `kotlin-native/libllvmext/KotlinLLVMDialect.md` for details on the
/// dialect.
class EraseDialectPass : public PassInfoMixin<EraseDialectPass> {
public:
  PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM);

  bool run(Module &M);

private:
};

} // namespace llvm::kotlin
