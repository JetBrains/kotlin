/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RemoveRedundantSafepoints.hpp"

#include "llvm/ADT/SmallVector.h"
#include "llvm/IR/BasicBlock.h"
#include "llvm/IR/Analysis.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/PassManager.h"
#include "llvm/Support/Casting.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/FormatVariadic.h"
#include "llvm/Transforms/Utils/Cloning.h"

using namespace llvm;

namespace {

constexpr const char* functionPrologueSafepointName = "Kotlin_mm_safePointFunctionPrologue";

CallBase* instructionAsAPrologueSafepoint(Instruction &I) {
  if (auto* CallInstr = dyn_cast<CallBase>(&I)) {
    if (CallInstr->getCalledOperand()->getName() == functionPrologueSafepointName) {
      return CallInstr;
    }
  }
  return nullptr;
}

bool blockHasAPrologueSafepoint(BasicBlock &BB) {
  for (auto &I : BB) {
    if (instructionAsAPrologueSafepoint(I))
      return true;
  }
  return false;
}

}

bool kotlin::RemoveRedundantSafepointsPass::removeAndInlineSafepoints(BasicBlock &BB, bool RemoveFirst) {
  CallBase* ToInline = nullptr;
  SmallVector<CallBase*> ToErase;
  bool First = true;
  for (auto& I : BB) {
    if (auto* SPI = instructionAsAPrologueSafepoint(I)) {
      if (!First || RemoveFirst) {
        ToErase.push_back(SPI);
      } else if (ShouldInline && !cast<GlobalValue>(SPI->getCalledOperand())->isDeclaration()) {
        ToInline = SPI;
      }
      First = false;
    }
  }
  bool MadeChanges = !ToErase.empty();
  for (auto* SPI : ToErase) {
    SPI->eraseFromParent();
  }
  if (ToInline) {
    InlineFunctionInfo IFI;
    auto Result = InlineFunction(*ToInline, IFI);
    MadeChanges |= Result.isSuccess();
  }
  return MadeChanges;
}

bool kotlin::RemoveRedundantSafepointsPass::removeSafepoints(Function &F) {
  if (F.isDeclaration())
    return false;
  if (F.empty())
    return false;
  auto It = F.begin();
  auto &FirstBlock = *It;
  bool FirstBlockHasSafepoint = blockHasAPrologueSafepoint(FirstBlock);
  bool MadeChanges = removeAndInlineSafepoints(FirstBlock, false);
  ++It;
  for (; It != F.end(); ++It) {
    bool Result = removeAndInlineSafepoints(*It, FirstBlockHasSafepoint);
    MadeChanges |= Result;
  }
  return MadeChanges;
}

PreservedAnalyses kotlin::RemoveRedundantSafepointsPass::run(Function &F, FunctionAnalysisManager &) {
  if (!removeSafepoints(F))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

Expected<bool> kotlin::parseRemoveRedundantSafepointsPassOptions(StringRef Params) {
  if (Params.empty()) {
    return false;
  }
  if (Params == "inline") {
    return true;
  }
  return make_error<StringError>(
      formatv("invalid kotlin-remove-sp pass parameter '{0}'", Params).str(),
      inconvertibleErrorCode());
}
