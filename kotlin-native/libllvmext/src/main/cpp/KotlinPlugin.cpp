// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "KotlinPlugin.h"

#include "Passes/HideSymbols.h"
#include "Passes/PrepareStackProtector.h"
#include "Passes/PrepareThreadSanitizer.h"
#include "Passes/RemoveRedundantSafepoints.h"

#include "llvm/Passes/PassBuilder.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/FormatVariadic.h"

using namespace llvm;
using namespace llvm::kotlin;

static Expected<SspMode> ParseSspModePassOptions(StringRef Params) {
  if (Params.empty()) {
    return SspMode::Default;
  }
  if (Params == "strong") {
    return SspMode::Strong;
  }
  if (Params == "req") {
    return SspMode::Req;
  }
  return make_error<StringError>(
      formatv("invalid kotlin-ssp pass parameter '{0}'", Params).str(),
      inconvertibleErrorCode());
}

static Expected<bool> ParseShouldInlineSafepoints(StringRef Params) {
  if (Params.empty()) {
    return false;
  }
  if (Params == "inline") {
    return true;
  }
  return make_error<StringError>(
      formatv("invalid kotlin-remove-sp pass parameter '{0}'", Params).str(),
      inconvertibleErrorCode());
}

PassPluginLibraryInfo getKotlinPluginInfo() {
  return {
      LLVM_PLUGIN_API_VERSION, "Kotlin", LLVM_VERSION_STRING,
      [](PassBuilder &PB) {
        PB.registerPipelineParsingCallback(
            [](StringRef Name, ModulePassManager &PM,
               ArrayRef<PassBuilder::PipelineElement>) {
              if (Name == "kotlin-hide-symbols") {
                PM.addPass(HideSymbolsPass());
                return true;
              }
              if (PassBuilder::checkParametrizedPassName(Name,
                                                         "kotlin-remove-sp")) {
                auto Param = PassBuilder::parsePassParameters(
                    ParseShouldInlineSafepoints, Name, "kotlin-remove-sp");
                if (auto E = Param.takeError()) {
                  reportFatalUsageError(std::move(E));
                }
                PM.addPass(RemoveRedundantSafepointsPass(*Param));
                return true;
              }
              return false;
            });
        PB.registerPipelineParsingCallback(
            [](StringRef Name, FunctionPassManager &PM,
               ArrayRef<PassBuilder::PipelineElement>) {
              if (Name == "kotlin-tsan") {
                PM.addPass(PrepareThreadSanitizerPass());
                return true;
              }
              if (PassBuilder::checkParametrizedPassName(Name, "kotlin-ssp")) {
                auto Param = PassBuilder::parsePassParameters(
                    ParseSspModePassOptions, Name, "kotlin-ssp");
                if (auto E = Param.takeError()) {
                  reportFatalUsageError(std::move(E));
                }
                PM.addPass(PrepareStackProtectorPass(*Param));
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
