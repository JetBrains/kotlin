// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/Attributes.h"

namespace llvm::kotlin {

enum class FunctionOrigin {
  /// Regular Kotlin function.
  Kt,
  /// Kotlin function defined in the runtime library.
  KtRt,
  /// C->Kotlin function bridge.
  CToKt,
};

/// `FunctionOrigin` attribute kind.
constexpr StringRef FunctionOriginAttrKind = "kotlin-origin";

/// Create `Attribute` of kind `FunctionOriginAttrKind` with value `Origin`.
Attribute functionOriginAttribute(LLVMContext &Ctx, FunctionOrigin Origin);

/// Extract `FunctionOrigin` from `Attribute`.
/// The `Attribute` must be of kind `FunctionOriginAttrKind`.
FunctionOrigin functionOriginValue(Attribute A);

} // namespace llvm::kotlin
