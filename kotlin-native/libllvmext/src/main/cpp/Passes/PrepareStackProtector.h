// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/Attributes.h"
#include "llvm/IR/PassManager.h"

namespace llvm::kotlin {

enum class SspMode {
  Default,
  Strong,
  Req,
};

/// Annotate all defined functions with one of the `ssp` attributes:
/// * `SspMode::Default`: `ssp`
/// * `SspMode::Strong`: `sspstrong`
/// * `SspMode::Req`: `sspreq`
///
/// This can't simply be done in the code generator, because we want
/// this attribute applied to the runtime code as well, which ships as LLVM
/// bitcode.
class PrepareStackProtectorPass
    : public PassInfoMixin<PrepareStackProtectorPass> {
public:
  explicit PrepareStackProtectorPass(SspMode Mode);

  PreservedAnalyses run(Function &F, FunctionAnalysisManager &AF);

  bool run(Function &F);

private:
  Attribute::AttrKind Attr;
};

} // namespace llvm::kotlin
