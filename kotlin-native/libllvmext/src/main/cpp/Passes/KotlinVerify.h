// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

/// A module pass that verifies LLVM IR conforming to Kotlin dialect.
///
/// See `kotlin-native/libllvmext/KotlinLLVMDialect.md` for details on the
/// dialect.
class KotlinVerifyPass : public PassInfoMixin<KotlinVerifyPass> {
public:
  PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM);

  void run(Module &M);

private:
};

} // namespace llvm::kotlin
