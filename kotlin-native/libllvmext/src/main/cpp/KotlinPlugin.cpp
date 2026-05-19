// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "KotlinPlugin.h"

#include "Passes/HideSymbols.h"

#include "llvm/Passes/PassBuilder.h"

using namespace llvm;
using namespace llvm::kotlin;

PassPluginLibraryInfo getKotlinPluginInfo() {
  return {LLVM_PLUGIN_API_VERSION, "Kotlin", LLVM_VERSION_STRING,
          [](PassBuilder &PB) {
            PB.registerPipelineParsingCallback(
                [](StringRef Name, ModulePassManager &PM,
                   ArrayRef<PassBuilder::PipelineElement>) {
                  if (Name == "kotlin-hide-symbols") {
                    PM.addPass(HideSymbolsPass());
                    return true;
                  }
                  return false;
                });
          }};
}

// This macro is unlikely to be useful in the foreseeable future.
// Just following llvm/examples/Bye verbatim.
#ifndef LLVM_KOTLIN_LINK_INTO_TOOLS
extern "C" LLVM_ATTRIBUTE_WEAK PassPluginLibraryInfo llvmGetPassPluginInfo() {
  return getKotlinPluginInfo();
}
#endif
