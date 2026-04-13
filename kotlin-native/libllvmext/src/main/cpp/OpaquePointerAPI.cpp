// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "OpaquePointerAPI.h"

#include "llvm/IR/Module.h"

using namespace llvm;

unsigned LLVMKotlinGetProgramAddressSpace(LLVMModuleRef M) {
  Module *Mod = unwrap(M);
  return Mod ? Mod->getDataLayout().getProgramAddressSpace() : 0;
}
