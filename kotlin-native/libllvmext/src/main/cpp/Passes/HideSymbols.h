// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/ADT/StringSet.h"
#include "llvm/IR/Analysis.h"
#include "llvm/IR/GlobalValue.h"
#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

/// Pass similar to `InternalizePass`, but not configurable and makes symbols
/// hidden instead of internal.
///
/// Useful when the symbol needs to be visible during compilation, but become
/// internal during linking (i.e. become reclaimable by linker DCE)
class HideSymbolsPass : public PassInfoMixin<HideSymbolsPass> {
public:
  PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM);

  bool run(Module &M);

private:
  /// Set of symbols private to the compiler that this pass should not touch.
  StringSet<> AlwaysPreserved;

  /// Internalize GV if it is not externally visible.
  bool maybeHide(GlobalValue &GV);
};

} // namespace llvm::kotlin
