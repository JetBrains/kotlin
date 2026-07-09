// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "RemoveRedundantSafepoints.h"

#include "CAPIExtensions.h"

#include "llvm-c/Core.h"

#include "llvm/ADT/SmallVector.h"
#include "llvm/IR/Function.h"

#include <cstring>
#include <string>

using namespace llvm;
using namespace llvm::kotlin;

static constexpr const char *FunctionPrologueSafepointName =
    "Kotlin_mm_safePointFunctionPrologue";

static constexpr size_t FunctionPrologueSafepointNameLength =
    std::char_traits<char>::length(FunctionPrologueSafepointName);

static bool isAPrologueSafepoint(LLVMValueRef Inst) {
  if (LLVMIsACallInst(Inst) || LLVMIsAInvokeInst(Inst)) {
    size_t CalledNameLength = 0;
    const char *CalledName =
        LLVMGetValueName2(LLVMGetCalledValue(Inst), &CalledNameLength);
    return FunctionPrologueSafepointNameLength == CalledNameLength &&
           strncmp(CalledName, FunctionPrologueSafepointName,
                   CalledNameLength) == 0;
  }
  return false;
}

static bool containsPrologueSafepoint(LLVMBasicBlockRef BB) {
  for (auto Inst = LLVMGetFirstInstruction(BB); Inst;
       Inst = LLVMGetNextInstruction(Inst)) {
    if (isAPrologueSafepoint(Inst))
      return true;
  }
  return false;
}

static void removeOrInlinePrologueSafepointInstructions(
    LLVMBasicBlockRef BB, bool RemoveFirst, bool IsSafepointInliningAllowed) {
  // Collect safepoint calls to erase and inline.
  LLVMValueRef ToInline = nullptr;
  SmallVector<LLVMValueRef> ToErase;
  bool First = true;
  for (auto Inst = LLVMGetFirstInstruction(BB); Inst;
       Inst = LLVMGetNextInstruction(Inst)) {
    if (isAPrologueSafepoint(Inst)) {
      if (!First || RemoveFirst) {
        ToErase.push_back(Inst);
      } else if (IsSafepointInliningAllowed &&
                 !LLVMIsDeclaration(LLVMGetCalledValue(Inst))) {
        ToInline = Inst;
      }
      First = false;
    }
  }
  for (auto Inst : ToErase) {
    LLVMInstructionEraseFromParent(Inst);
  }
  if (ToInline) {
    LLVMKotlinInlineCall(ToInline);
  }
}

static void removeRedundantSafepoints(LLVMValueRef F,
                                      int IsSafepointInliningAllowed) {
  if (LLVMIsDeclaration(F))
    return;
  LLVMBasicBlockRef FirstBB = LLVMGetFirstBasicBlock(F);
  if (!FirstBB)
    return;
  bool FirstBBHasSafepoint = containsPrologueSafepoint(FirstBB);
  removeOrInlinePrologueSafepointInstructions(FirstBB, /*RemoveFirst=*/false,
                                              IsSafepointInliningAllowed);
  for (auto BB = LLVMGetNextBasicBlock(FirstBB); BB;
       BB = LLVMGetNextBasicBlock(BB)) {
    removeOrInlinePrologueSafepointInstructions(BB, FirstBBHasSafepoint,
                                                IsSafepointInliningAllowed);
  }
}

extern "C" void
LLVMKotlinRemoveRedundantSafepoints(LLVMModuleRef M,
                                    int IsSafepointInliningAllowed) {
  for (auto F = LLVMGetFirstFunction(M); F; F = LLVMGetNextFunction(F)) {
    removeRedundantSafepoints(F, IsSafepointInliningAllowed);
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
  removeRedundantSafepoints(wrap(&F), IsSafepointInliningAllowed);
  return true;
}
