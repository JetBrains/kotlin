// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "PrepareStackProtector.h"

#include "llvm/IR/Function.h"

using namespace llvm;
using namespace llvm::kotlin;

static Attribute::AttrKind SSPAttributeForMode(SspMode Mode) {
  switch (Mode) {
  case SspMode::Default:
    return Attribute::AttrKind::StackProtect;
  case SspMode::Strong:
    return Attribute::AttrKind::StackProtectStrong;
  case SspMode::Req:
    return Attribute::AttrKind::StackProtectReq;
  }
}

PrepareStackProtectorPass::PrepareStackProtectorPass(SspMode Mode)
    : Attr(SSPAttributeForMode(Mode)) {}

PreservedAnalyses PrepareStackProtectorPass::run(Function &F,
                                                 FunctionAnalysisManager &AF) {
  if (!run(F))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

bool PrepareStackProtectorPass::run(Function &F) {
  if (F.isDeclaration())
    return false;
  if (F.getName() == "__clang_call_terminate")
    return false;
  F.addFnAttr(Attr);
  return true;
}
