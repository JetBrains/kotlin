// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "CallsChecker.h"

#include "llvm/IR/DerivedTypes.h"
#include "llvm/IR/Module.h"
#include "llvm/Transforms/Utils/Instrumentation.h"
#include "llvm/Transforms/Utils/ModuleUtils.h"

using namespace llvm;
using namespace llvm::kotlin;

static bool isAKnownFunction(Function &F) {
  // Just treat all defined functions as known functions (i.e. allowed to be
  // called in the runnable state). This also applies to the entire K/N runtime.
  return !F.isDeclaration();
}

PreservedAnalyses ModuleCallsCheckerPass::run(Module &M,
                                              ModuleAnalysisManager &) {
  if (!run(M))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

bool ModuleCallsCheckerPass::run(Module &M) {
  if (checkIfAlreadyInstrumented(M, "no_external_calls_check"))
    return false;

  SmallVector<Constant *> KnownFunctions;
  for (auto &F : M) {
    if (isAKnownFunction(F)) {
      KnownFunctions.push_back(&F);
    }
  }

  // If there are no known functions, no need to register this module with the
  // runtime.
  if (KnownFunctions.empty())
    return false;

  auto &Ctx = M.getContext();

  auto *KnownFunctionsArrValue = ConstantArray::get(
      ArrayType::get(PointerType::getUnqual(Ctx), KnownFunctions.size()),
      KnownFunctions);
  auto *KnownFunctionsArr = new GlobalVariable(
      M, KnownFunctionsArrValue->getType(), true, GlobalValue::PrivateLinkage,
      KnownFunctionsArrValue, "kotlin.callsChecker.knownFunctions");

  auto *KnownFunctionsSize =
      ConstantInt::get(Type::getInt64Ty(Ctx), KnownFunctions.size());

  getOrCreateSanitizerCtorAndInitFunctions(
      M, "kotlin.callsChecker.module_ctor", "Kotlin_callsChecker_init",
      {PointerType::getUnqual(Ctx), KnownFunctionsSize->getType()},
      {KnownFunctionsArr, KnownFunctionsSize},
      [&](Function *Ctor, FunctionCallee) { appendToGlobalCtors(M, Ctor, 0); });
  // Technically, we need to handle the destructor as well: the module may go
  // away and a new one could get placed in the same address space. But for
  // simplicity we will avoid this usecase for now.
  return true;
}
