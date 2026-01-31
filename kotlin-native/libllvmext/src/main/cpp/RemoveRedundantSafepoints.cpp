/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RemoveRedundantSafepoints.hpp"

#include <CAPIExtensions.h>
#include <llvm/IR/Constants.h>
#include <llvm/IR/Instructions.h>
#include <llvm/IR/Module.h>
#include "llvm/IR/Analysis.h"
#include "llvm/IR/PassManager.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/FormatVariadic.h"

using namespace llvm;

static constexpr const char* functionPrologueSafepointName = "Kotlin_mm_safePointFunctionPrologue";

static constexpr size_t functionPrologueSafepointNameLength =
  std::char_traits<char>::length(functionPrologueSafepointName);

static bool InstructionIsPrologueSafepoint(LLVMValueRef instruction) {
  if (LLVMIsACallInst(instruction) || LLVMIsAInvokeInst(instruction)) {
    size_t calledNameLength = 0;
    const char* calledName = LLVMGetValueName2(LLVMGetCalledValue(instruction), &calledNameLength);
    return functionPrologueSafepointNameLength == calledNameLength &&
      strncmp(calledName, functionPrologueSafepointName, calledNameLength) == 0;
  }
  return false;
}

static bool BlockHasSafepointInstruction(LLVMBasicBlockRef block) {
  LLVMValueRef current = LLVMGetFirstInstruction(block);
  while (current) {
    if (InstructionIsPrologueSafepoint(current)) {
      return true;
    }
    current = LLVMGetNextInstruction(current);
  }
  return false;
}

static void RemoveOrInlinePrologueSafepointInstructions(
  LLVMBasicBlockRef block, bool removeFirst, bool isSafepointInliningAllowed) {
  // Collect safepoint calls to erase and inline.
  LLVMValueRef toInline = nullptr;
  std::vector<LLVMValueRef> toErase;
  bool first = true;
  LLVMValueRef current = LLVMGetFirstInstruction(block);
  while (current) {
    if (InstructionIsPrologueSafepoint(current)) {
      if (!first || removeFirst) {
        toErase.push_back(current);
      } else if (isSafepointInliningAllowed && !LLVMIsDeclaration(LLVMGetCalledValue(current))) {
        toInline = current;
      }
      first = false;
    }
    current = LLVMGetNextInstruction(current);
  }
  // Perform actual modifications after iteration.
  for (auto it = toErase.cbegin(); it != toErase.cend(); ++it) {
    LLVMInstructionEraseFromParent(*it);
  }
  if (toInline) {
    LLVMInlineCall(toInline);
  }
}

bool kotlin::RemoveRedundantSafepointsPass::removeSafepoints(Function &F) {
  if (F.isDeclaration())
    return false;
  if (F.empty())
    return false;
  auto It = F.begin();
  auto &FirstBlock = *It;
  bool FirstBlockHasSafepoint = BlockHasSafepointInstruction(wrap(&FirstBlock));
  RemoveOrInlinePrologueSafepointInstructions(wrap(&FirstBlock), false, ShouldInline);
  ++It;
  for (; It != F.end(); ++It) {
    RemoveOrInlinePrologueSafepointInstructions(wrap(&*It), FirstBlockHasSafepoint, ShouldInline);
  }
  return true;
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
