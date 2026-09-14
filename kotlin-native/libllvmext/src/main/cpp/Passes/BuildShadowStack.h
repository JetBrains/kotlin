// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {
enum class SlotClearing { Off, Early, Late };

class BuildShadowStackPass : public PassInfoMixin<BuildShadowStackPass> {
public:
  explicit BuildShadowStackPass(SlotClearing Clearing = SlotClearing::Off)
      : Clearing(Clearing) {}

  PreservedAnalyses run(Module &M, ModuleAnalysisManager &MAM);

  static constexpr unsigned GCAddressSpace = 1;

private:
  SlotClearing Clearing;
};
}
