// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#pragma once

#include "llvm/IR/CallingConv.h"

namespace llvm::kotlin {

/// Calling convention for Kotlin functions.
///
/// A function must have this calling convention to accept or return Kotlin
/// objects.
constexpr CallingConv::ID KotlinCallingConv = CallingConv::MaxID - 1;

} // namespace llvm::kotlin
