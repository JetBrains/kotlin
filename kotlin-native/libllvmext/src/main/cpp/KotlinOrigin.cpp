// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "KotlinOrigin.h"

#include "llvm/IR/Attributes.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/FormatVariadic.h"

#include <iterator>

using namespace llvm;
using namespace llvm::kotlin;

static constexpr StringRef FunctionOriginValues[] = {
    "kt",
    "kt-rt",
    "c-to-kt",
};

Attribute kotlin::functionOriginAttribute(LLVMContext &Ctx, FunctionOrigin Origin) {
  auto Idx = static_cast<size_t>(Origin);
  if (Idx >= std::size(FunctionOriginValues)) {
    reportFatalInternalError(
        formatv("Origin {0} is outside the known values range", Idx));
  }
  return Attribute::get(Ctx, FunctionOriginAttrKind, FunctionOriginValues[Idx]);
}

FunctionOrigin kotlin::functionOriginValue(Attribute A) {
  if (auto Kind = A.getKindAsString(); Kind != FunctionOriginAttrKind) {
    reportFatalInternalError(
        formatv("Attribute with kind {0} does not represent origin", Kind));
  }
  auto V = A.getValueAsString();
  auto It = std::find(std::begin(FunctionOriginValues),
                      std::end(FunctionOriginValues), V);
  if (It == std::end(FunctionOriginValues)) {
    reportFatalInternalError(
        formatv("Origin attribute has an unknown value {0}", V));
  }
  auto Idx = std::distance(std::begin(FunctionOriginValues), It);
  return static_cast<FunctionOrigin>(Idx);
}
