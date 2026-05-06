// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "RemoveRedundantSafepoints.h"

#include "llvm/ADT/SmallVector.h"
#include "llvm/IR/BasicBlock.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/Support/Casting.h"
#include "llvm/Transforms/Utils/Cloning.h"

using namespace llvm;
using namespace llvm::kotlin;

static constexpr const char *FunctionPrologueSafepointName =
    "Kotlin_mm_safePointFunctionPrologue";

static CallBase *asAPrologueSafepoint(Instruction &Inst) {
  if (auto *Call = dyn_cast_or_null<CallBase>(&Inst)) {
    if (Call->getCalledOperand()->getName() != FunctionPrologueSafepointName)
      return nullptr;
    return Call;
  }
  return nullptr;
}

/// Get all prologue safepoints in the basic block from last to first.
/// The order is convenient to be able to quickly remove the first safepoint
/// from the resulting vector.
static SmallVector<CallBase *> collectSafepointsBackwards(BasicBlock &BB) {
  SmallVector<CallBase *> SPs;
  for (auto Iter = BB.rbegin(); Iter != BB.rend(); ++Iter) {
    if (auto *SP = asAPrologueSafepoint(*Iter)) {
      SPs.push_back(SP);
    }
  }
  return SPs;
}

/// Erase all given safepoints.
/// Returns `true` if any were erased.
static bool eraseSafepoints(SmallVector<CallBase *> &SPs) {
  if (SPs.empty())
    return false;
  for (auto *SP : SPs) {
    SP->eraseFromParent();
  }
  return true;
}

/// Try inlinging a safepoint.
/// Returns `true` if successfully inlined.
static bool inlineSafepoint(CallBase &SP) {
  if (!cast<GlobalValue>(SP.getCalledOperand())->isDeclaration()) {
    InlineFunctionInfo IFI;
    return InlineFunction(SP, IFI).isSuccess();
  }
  return false;
}

RemoveRedundantSafepointsPass::RemoveRedundantSafepointsPass(
    bool IsSafepointInliningAllowed)
    : IsSafepointInliningAllowed(IsSafepointInliningAllowed) {}

PreservedAnalyses
RemoveRedundantSafepointsPass::run(Function &F, FunctionAnalysisManager &AF) {
  if (!run(F))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

namespace {

enum class CanRemoveAllSafepoints {
  Unknown,
  No,
  Yes,
};

}

bool RemoveRedundantSafepointsPass::run(Function &F) {
  if (F.isDeclaration())
    return false;
  CanRemoveAllSafepoints CanRemoveAllSafepoints =
      CanRemoveAllSafepoints::Unknown;
  bool Changed = false;
  for (BasicBlock &BB : F) {
    auto SPs = collectSafepointsBackwards(BB);
    bool HasSPs = !SPs.empty();
    if (HasSPs) {
      CallBase *FirstSP = nullptr;
      // If we can't yet remove all encountered prologue safepoints, preserve the first one.
      if (CanRemoveAllSafepoints != CanRemoveAllSafepoints::Yes) {
        FirstSP = SPs.pop_back_val();
      }
      // All other prologue safepoints in this basic block can be safely removed.
      Changed = eraseSafepoints(SPs) || Changed;
      // When allowed, the preserved safepoint should be inlined.
      if (IsSafepointInliningAllowed && FirstSP) {
        Changed = inlineSafepoint(*FirstSP) || Changed;
      }
    }
    if (CanRemoveAllSafepoints == CanRemoveAllSafepoints::Unknown) {
      // If the first basic block contains a prologue safepoint, all other prologue safepoints
      // can be removed.
      CanRemoveAllSafepoints =
          HasSPs ? CanRemoveAllSafepoints::Yes : CanRemoveAllSafepoints::No;
    }
  }
  return Changed;
}
