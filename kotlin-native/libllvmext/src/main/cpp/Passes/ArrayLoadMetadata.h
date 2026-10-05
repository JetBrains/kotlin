// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/Function.h"
#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

/// Attaches !invariant.load and !range [0, INT_MAX] metadata to
/// ArrayHeader::count_ loads identified via their struct-path TBAA tag.
class ArrayLoadMetadataPass : public PassInfoMixin<ArrayLoadMetadataPass> {
public:
  PreservedAnalyses run(Function &F, FunctionAnalysisManager &AM);
};

} // namespace llvm::kotlin
