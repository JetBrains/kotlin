// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "ArrayLoadMetadata.h"

#include "llvm/IR/Constants.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Instructions.h"
#include "llvm/IR/Metadata.h"

using namespace llvm;
using namespace llvm::kotlin;

namespace {

// Matches Clang struct-path TBAA tags for ArrayHeader::count_:
//   !tag  = !{!base, !fieldType, i64 <AccessOffset>}
//   !base = !{"_ZTS11ArrayHeader", !typeInfoField, i64 0, !countField, i64 <CountFieldOffset>}
static bool isArrayHeaderCountTBAA(const MDNode *TBAA) {
  if (!TBAA || TBAA->getNumOperands() < 3)
    return false;
  const auto *BaseNode = dyn_cast_or_null<MDNode>(TBAA->getOperand(0));
  if (!BaseNode || BaseNode->getNumOperands() < 5)
    return false;
  const auto *BaseName = dyn_cast_or_null<MDString>(BaseNode->getOperand(0));
  if (!BaseName || BaseName->getString() != "_ZTS11ArrayHeader")
    return false;
  const auto *AccessOffset =
      mdconst::dyn_extract_or_null<ConstantInt>(TBAA->getOperand(2));
  const auto *CountFieldOffset =
      mdconst::dyn_extract_or_null<ConstantInt>(BaseNode->getOperand(4));
  if (!AccessOffset || !CountFieldOffset)
    return false;
  return AccessOffset->getZExtValue() == CountFieldOffset->getZExtValue();
}

} // namespace

PreservedAnalyses ArrayLoadMetadataPass::run(Function &F,
                                             FunctionAnalysisManager &AM) {
  if (F.isDeclaration() || F.empty())
    return PreservedAnalyses::all();

  MDNode *InvariantMD = nullptr;
  MDNode *RangeMD = nullptr;

  for (BasicBlock &BB : F) {
    for (Instruction &I : BB) {
      auto *LI = dyn_cast<LoadInst>(&I);
      if (!LI || !LI->getType()->isIntegerTy(32))
        continue;
      if (!isArrayHeaderCountTBAA(LI->getMetadata(LLVMContext::MD_tbaa)))
        continue;

      if (!InvariantMD) {
        LLVMContext &Ctx = F.getContext();
        InvariantMD = MDNode::get(Ctx, {});
        IntegerType *Int32Ty = Type::getInt32Ty(Ctx);
        Metadata *RangeOperands[] = {
            ConstantAsMetadata::get(ConstantInt::get(Int32Ty, 0)),
            ConstantAsMetadata::get(
                ConstantInt::get(Int32Ty, 0x80000000ULL, /*isSigned=*/false))};
        RangeMD = MDNode::get(Ctx, RangeOperands);
      }

      if (!LI->hasMetadata(LLVMContext::MD_invariant_load))
        LI->setMetadata(LLVMContext::MD_invariant_load, InvariantMD);
      if (!LI->hasMetadata(LLVMContext::MD_range))
        LI->setMetadata(LLVMContext::MD_range, RangeMD);
    }
  }

  return PreservedAnalyses::all();
}
