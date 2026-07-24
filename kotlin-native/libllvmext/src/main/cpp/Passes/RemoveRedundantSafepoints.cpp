// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "RemoveRedundantSafepoints.h"

#include "llvm-c/Core.h"

#include "llvm/ADT/SmallVector.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/Transforms/Utils/Cloning.h"

using namespace llvm;
using namespace llvm::kotlin;

static constexpr const char *FunctionPrologueSafepointName =
    "Kotlin_mm_safePointFunctionPrologue";

static CallBase *asAPrologueSafepoint(Instruction &Inst) {
  if (auto *Call = dyn_cast<CallBase>(&Inst)) {
    if (Call->getCalledOperand()->getName() == FunctionPrologueSafepointName)
      return Call;
  }
  return nullptr;
}

static bool isAPrologueSafepoint(Instruction &Inst) {
  return asAPrologueSafepoint(Inst) != nullptr;
}

static bool containsPrologueSafepoint(BasicBlock &BB) {
  return std::any_of(BB.begin(), BB.end(), isAPrologueSafepoint);
}

static bool
removeOrInlinePrologueSafepointInstructions(BasicBlock &BB, bool RemoveFirst,
                                            bool IsSafepointInliningAllowed) {
  // Collect safepoint calls to erase and inline.
  CallBase *ToInline = nullptr;
  SmallVector<CallBase *> ToErase;
  bool First = true;
  for (auto &Inst : BB) {
    if (auto *Call = asAPrologueSafepoint(Inst)) {
      if (!First || RemoveFirst) {
        ToErase.push_back(Call);
      } else if (IsSafepointInliningAllowed) {
        ToInline = Call;
      }
      First = false;
    }
  }
  bool Changed = !ToErase.empty();
  for (auto *Inst : ToErase) {
    Inst->eraseFromParent();
  }
  if (ToInline) {
    InlineFunctionInfo IFI;
    if (InlineFunction(*ToInline, IFI).isSuccess())
      Changed = true;
  }
  return Changed;
}

static bool removeRedundantSafepoints(Function &F,
                                      bool IsSafepointInliningAllowed) {
  if (F.isDeclaration())
    return false;
  if (F.empty())
    return false;
  auto BBIter = F.begin();
  auto &FirstBB = *BBIter;
  bool FirstBBHasSafepoint = containsPrologueSafepoint(FirstBB);
  bool Changed = removeOrInlinePrologueSafepointInstructions(
      FirstBB, /*RemoveFirst=*/false, IsSafepointInliningAllowed);
  ++BBIter;
  for (; BBIter != F.end(); ++BBIter) {
    bool Result = removeOrInlinePrologueSafepointInstructions(
        *BBIter, FirstBBHasSafepoint, IsSafepointInliningAllowed);
    Changed |= Result;
  }
  return Changed;
}

extern "C" void
LLVMKotlinRemoveRedundantSafepoints(LLVMModuleRef M,
                                    int IsSafepointInliningAllowed) {
  for (auto F = LLVMGetFirstFunction(M); F; F = LLVMGetNextFunction(F)) {
    removeRedundantSafepoints(*cast<Function>(unwrap(F)),
                              IsSafepointInliningAllowed);
  }
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

bool RemoveRedundantSafepointsPass::run(Function &F) {
  return removeRedundantSafepoints(F, IsSafepointInliningAllowed);
}
