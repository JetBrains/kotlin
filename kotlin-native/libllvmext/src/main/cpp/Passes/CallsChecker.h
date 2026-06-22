// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/BasicBlock.h"
#include "llvm/IR/DerivedTypes.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

class CallsCheckerPass : public PassInfoMixin<CallsCheckerPass> {
public:
  PreservedAnalyses run(Function &F, FunctionAnalysisManager &AF);

  bool run(Function &F);
  bool run(BasicBlock &BB);
  bool run(CallBase &C);

private:
  bool load(Module &M);
  void loadIgnoredFunctions(Module &M);
  void loadGoodFunctions(Module &M);

  Value *placeCString(Module &M, StringRef S);

  bool isGoodFunction(StringRef Name) const;

  bool Loaded = false; // Assumes there's a single module and no paralellism.
  SmallPtrSet<Function *, 32> IgnoredFunctions;
  SmallVector<StringRef> GoodFunctions;
  FunctionCallee CheckStateAtExternalCall;
  FunctionCallee GetMethodImpl;
  FunctionCallee GetClass;
  FunctionCallee GetSuperClass;

  StringMap<Value *> Strings;
};

/// A module pass for external calls checker instrumentation.
///
/// Creates module constructor. Should be run after DCE: it saves all defined
/// functions in a global, preventing DCE from removing unused ones
class ModuleCallsCheckerPass : public PassInfoMixin<ModuleCallsCheckerPass> {
public:
  PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM);

  bool run(Module &M);

private:
};

} // namespace llvm::kotlin
