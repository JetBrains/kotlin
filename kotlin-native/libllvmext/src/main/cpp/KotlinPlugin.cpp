// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "KotlinPlugin.h"

#include "Passes/CallsChecker.h"
#include "Passes/HideSymbols.h"
#include "Passes/PrepareStackProtector.h"
#include "Passes/PrepareThreadSanitizer.h"
#include "Passes/RemoveRedundantSafepoints.h"

#include "llvm/Passes/PassBuilder.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/FormatVariadic.h"

#include <optional>

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

static bool parsePass(StringRef Name, StringRef PassName) {
  return Name == PassName;
}

template <typename F>
static auto parsePass(StringRef Name, StringRef PassName, F &&Parser)
    -> std::optional<typename decltype(Parser(StringRef{}))::value_type> {
  if (!PassBuilder::checkParametrizedPassName(Name, PassName)) {
    return std::nullopt;
  }
  auto Param =
      PassBuilder::parsePassParameters(std::forward<F>(Parser), Name, PassName);
  if (auto E = Param.takeError()) {
    reportFatalUsageError(std::move(E));
  }
  return std::make_optional(std::move(*Param));
}

PassPluginLibraryInfo getKotlinPluginInfo() {
  return {LLVM_PLUGIN_API_VERSION, "Kotlin", LLVM_VERSION_STRING,
          [](PassBuilder &PB) {
            PB.registerPipelineParsingCallback(
                [](StringRef Name, ModulePassManager &PM,
                   ArrayRef<PassBuilder::PipelineElement>) {
                  if (parsePass(Name, "kotlin-hide-symbols")) {
                    PM.addPass(HideSymbolsPass());
                    return true;
                  }
                  if (parsePass(Name, "kotlin-calls-checker-module")) {
                    PM.addPass(ModuleCallsCheckerPass());
                    return true;
                  }
                  return false;
                });
            PB.registerPipelineParsingCallback(
                [](StringRef Name, FunctionPassManager &PM,
                   ArrayRef<PassBuilder::PipelineElement>) {
                  if (parsePass(Name, "kotlin-tsan")) {
                    PM.addPass(PrepareThreadSanitizerPass());
                    return true;
                  }
                  if (auto Param = parsePass(Name, "kotlin-ssp",
                                             ParseSspModePassOptions)) {
                    PM.addPass(PrepareStackProtectorPass(*Param));
                    return true;
                  }
                  if (auto Param = parsePass(Name, "kotlin-remove-sp",
                                             ParseShouldInlineSafepoints)) {
                    PM.addPass(RemoveRedundantSafepointsPass(*Param));
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
