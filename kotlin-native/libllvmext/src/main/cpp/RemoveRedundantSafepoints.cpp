/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <CAPIExtensions.h>
#include <RemoveRedundantSafepoints.h>
#include <llvm/IR/Constants.h>
#include <llvm/IR/Instructions.h>

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

void LLVMKotlinRemoveRedundantSafepoints(LLVMModuleRef module, int isSafepointInliningAllowed) {
  bool inliningAllowed = isSafepointInliningAllowed != 0;
  LLVMValueRef currentFunction = LLVMGetFirstFunction(module);
  while (currentFunction) {
    if (!LLVMIsDeclaration(currentFunction)) {
      LLVMBasicBlockRef firstBlock = LLVMGetFirstBasicBlock(currentFunction);
      if (firstBlock) {
        bool firstBlockHasSafepoint = BlockHasSafepointInstruction(firstBlock);
        RemoveOrInlinePrologueSafepointInstructions(firstBlock, false, inliningAllowed);
        LLVMBasicBlockRef currentBlock = LLVMGetNextBasicBlock(firstBlock);
        while (currentBlock) {
          RemoveOrInlinePrologueSafepointInstructions(currentBlock, firstBlockHasSafepoint, inliningAllowed);
          currentBlock = LLVMGetNextBasicBlock(currentBlock);
        }
      }
    }
    currentFunction = LLVMGetNextFunction(currentFunction);
  }
}
