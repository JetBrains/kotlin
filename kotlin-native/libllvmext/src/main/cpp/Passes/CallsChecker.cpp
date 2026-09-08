// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "CallsChecker.h"

#include "llvm/Analysis/ValueTracking.h"
#include "llvm/IR/Argument.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/DerivedTypes.h"
#include "llvm/IR/Instruction.h"
#include "llvm/IR/Instructions.h"
#include "llvm/IR/Module.h"
#include "llvm/Support/Casting.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/FormatVariadic.h"
#include "llvm/Transforms/Utils/Instrumentation.h"
#include "llvm/Transforms/Utils/ModuleUtils.h"

using namespace llvm;
using namespace llvm::kotlin;

static bool isAKnownFunction(Function &F) {
  // Just treat all defined functions as known functions (i.e. allowed to be
  // called in the runnable state). This also applies to the entire K/N runtime.
  return !F.isDeclaration();
}

static constexpr int MSG_SEND_TO_NULL = -1;
static constexpr int CALLED_LLVM_BUILTIN = -2;

namespace {

struct ExternalCallInfo {
  std::optional<StringRef> Name;
  Value *CalledPtr;

  ExternalCallInfo(std::optional<StringRef> Name, Value *CalledPtr)
      : Name(Name), CalledPtr(CalledPtr) {}
};

} // namespace

static bool isIndirectCallArgument(Value *V) {
  return isa<LoadInst>(V) || isa<Argument>(V) || isa<PHINode>(V) ||
         isa<SelectInst>(V) || isa<CallInst>(V) || isa<ExtractElementInst>(V);
}

static std::optional<ExternalCallInfo>
getPossiblyExternalCalledFunction(Value *V) {
  if (auto *F = dyn_cast<Function>(V)) {
    if (isAKnownFunction(*F))
      return std::nullopt;
    if (F->isIntrinsic()) {
      auto &Ctx = V->getContext();
      auto *Value =
          ConstantInt::get(Type::getInt64Ty(Ctx), CALLED_LLVM_BUILTIN);
      return ExternalCallInfo(
          F->getName(),
          ConstantExpr::getIntToPtr(Value, PointerType::getUnqual(Ctx)));
    }
    return ExternalCallInfo(F->getName(), F);
  }
  if (auto *Cast = dyn_cast<CastInst>(V)) {
    return getPossiblyExternalCalledFunction(Cast->getOperand(0));
  }
  if (isIndirectCallArgument(V)) {
    return ExternalCallInfo(std::nullopt, V);
  }
  if (isa<InlineAsm>(V)) {
    return std::nullopt;
  }
  if (auto *Expr = dyn_cast<ConstantExpr>(V)) {
    switch (Expr->getOpcode()) {
    case Instruction::BitCast:
      return getPossiblyExternalCalledFunction(Expr->getOperand(0));
    default:
      reportFatalInternalError(
          formatv("Not implemented constant type {0}", Expr->getOpcodeName()));
    }
  }
  if (auto *A = dyn_cast<GlobalAlias>(V)) {
    return getPossiblyExternalCalledFunction(A->getAliasee());
  }
  reportFatalInternalError(formatv("Not implemented call argument {0}", V));
}

PreservedAnalyses CallsCheckerPass::run(Function &F,
                                        FunctionAnalysisManager &) {
  if (!run(F))
    return PreservedAnalyses::all();
  return PreservedAnalyses::none();
}

bool CallsCheckerPass::run(Function &F) {
  if (F.isDeclaration())
    return false;
  bool Changed = load(*F.getParent());
  if (IgnoredFunctions.contains(&F))
    return Changed;

  for (auto &BB : F) {
    Changed = run(BB) || Changed;
  }

  return Changed;
}

bool CallsCheckerPass::run(BasicBlock &BB) {
  // First collect all the calls and only then start modifying the instructions.
  SmallVector<CallBase *> Calls;
  for (auto &I : BB) {
    if (auto *C = dyn_cast<CallBase>(&I)) {
      Calls.push_back(C);
    }
  }

  bool Changed = false;
  for (auto *C : Calls) {
    Changed = run(*C) || Changed;
  }

  return Changed;
}

bool CallsCheckerPass::run(CallBase &C) {
  auto CalleeInfo = getPossiblyExternalCalledFunction(C.getCalledOperand());
  if (!CalleeInfo)
    return false;

  if (auto Name = CalleeInfo->Name) {
    if (isGoodFunction(*Name))
      return false;
  }

  // Generate the instrumentation before the call. It is the safest option.
  // For example:
  // - We check before calling, so we don't call the function if it is not
  //   allowed.
  // - For Objective-C method calls, the object is still alive, so we can safely
  //   get the method implementation. (The method itself may be 'release', which
  //   might destroy the object).
  IRBuilder<> Builder(&C);
  if (CalleeInfo->Name == "llvm.objc.retainAutoreleasedReturnValue") {
    // We are about to generate some code around this call.
    // Generating it before the call is harmful:
    // the retainAutoreleasedReturnValue call is supposed to go right after
    // another call, and the latter detects the former and eliminates the
    // matching autorelease operation. Inserting anything in between would break
    // this optimization. So, here we go an alternative way: generate the
    // instrumentation after the call and not before. It is not perfect, but it
    // is safe enough for this particular case, and the easiest option here.

    // For simplicity, we support handling here only 'call' instructions and not
    // 'invoke'. (invoke instructions are intertwined with basic blocks, so
    // getting the next instruction requires more code). The function doesn't
    // throw, so nobody should generate "invokes" to it anyway.
    if (!isa<CallInst>(C)) {
      reportFatalUsageError(
          formatv("retainAutoReleasedReturnValue wasn't a call: {0}", C));
    }
    auto InsertPoint = std::next(Builder.GetInsertPoint());
    if (InsertPoint == Builder.GetInsertBlock()->end()) {
      reportFatalUsageError(
          formatv("Expected a next instruction after {0}", C));
    }
    Builder.SetInsertPoint(InsertPoint);
  }
  // TODO(KT-87596): consider removing, if the tests now pass.
  Builder.SetCurrentDebugLocation(nullptr);

  SmallString<64> CallSiteDescription;
  std::optional<StringRef> CalledName;
  Value *CalledPtr = nullptr;
  if (CalleeInfo->Name == "objc_msgSend") {
    // objc_msgSend has wrong declaration in header, so generated wrapper is
    // strange, Let's just skip it
    if (C.getNumOperands() < 2)
      return false;
    CallSiteDescription =
        formatv("{0} (over objc_msgSend)", C.getFunction()->getName());
    CalledName = std::nullopt;
    auto *Obj = C.getArgOperand(0);
    auto *ObjClass = Builder.CreateCall(GetClass, {Obj});
    auto *IsNil =
        Builder.CreateICmpEQ(Obj, ConstantPointerNull::get(Builder.getPtrTy()));
    auto *Selector = C.getArgOperand(1);
    auto *CalledPtrIfNotNil =
        Builder.CreateCall(GetMethodImpl, {ObjClass, Selector});
    auto *CalledPtrIfNil = ConstantExpr::getIntToPtr(
        Builder.getInt64(MSG_SEND_TO_NULL), Builder.getPtrTy());
    CalledPtr = Builder.CreateSelect(IsNil, CalledPtrIfNil, CalledPtrIfNotNil);
  } else if (CalleeInfo->Name == "objc_msgSendSuper2") {
    // objc_msgSendSuper2 has wrong declaration in header, so generated wrapper
    // is strange, Let's just skip it
    if (C.getNumOperands() < 2)
      return false;
    CallSiteDescription =
        formatv("{0} (over objc_msgSendSuper2)", C.getFunction()->getName());
    CalledName = std::nullopt;
    // This is
    // https://developer.apple.com/documentation/objectivec/objc_super?language=objc
    // We don't want to look this type up, so let's just use our own struct.
    auto *SuperStructType =
        StructType::get(Builder.getPtrTy(), Builder.getPtrTy());
    auto *SuperStruct = C.getArgOperand(0);
    auto *SuperClassPtrPtr =
        Builder.CreateStructGEP(SuperStructType, SuperStruct, 1);
    auto *SuperClassPtr =
        Builder.CreateLoad(Builder.getPtrTy(), SuperClassPtrPtr);
    auto *ClassPtr = Builder.CreateCall(GetSuperClass, {SuperClassPtr});
    auto *Selector = C.getArgOperand(1);
    CalledPtr = Builder.CreateCall(GetMethodImpl, {ClassPtr, Selector});
  } else {
    CallSiteDescription = C.getFunction()->getName();
    CalledName = CalleeInfo->Name;
    switch (CalleeInfo->CalledPtr->getType()->getTypeID()) {
    case Type::PointerTyID:
      CalledPtr = CalleeInfo->CalledPtr;
      break;
    case Type::IntegerTyID:
      CalledPtr =
          Builder.CreateIntToPtr(CalleeInfo->CalledPtr, Builder.getPtrTy());
      break;
    default:
      reportFatalUsageError(formatv("Unsupported type {0} of {1}",
                                    CalleeInfo->CalledPtr->getType(),
                                    CalleeInfo->CalledPtr));
    }
  }

  auto *CallSiteDescriptionGlobal =
      placeCString(*Builder.GetInsertBlock()->getModule(), CallSiteDescription);

  Value *CalledNameV = ConstantPointerNull::get(Builder.getPtrTy());
  if (CalledName) {
    CalledNameV =
        placeCString(*Builder.GetInsertBlock()->getModule(), *CalledName);
  }

  Builder.CreateCall(CheckStateAtExternalCall,
                     {CallSiteDescriptionGlobal, CalledNameV, CalledPtr});

  return true;
}

bool CallsCheckerPass::load(Module &M) {
  if (Loaded)
    return false;

  auto &Ctx = M.getContext();

  loadIgnoredFunctions(M);
  loadGoodFunctions(M);

  CheckStateAtExternalCall = M.getOrInsertFunction(
      "Kotlin_mm_checkStateAtExternalFunctionCall", Type::getVoidTy(Ctx),
      PointerType::getUnqual(Ctx), PointerType::getUnqual(Ctx),
      PointerType::getUnqual(Ctx));
  // Always ignore the checker function itself.
  IgnoredFunctions.insert(cast<Function>(CheckStateAtExternalCall.getCallee()));
  GetMethodImpl = M.getOrInsertFunction(
      "class_getMethodImplementation", PointerType::getUnqual(Ctx),
      PointerType::getUnqual(Ctx), PointerType::getUnqual(Ctx));
  GetClass =
      M.getOrInsertFunction("object_getClass", PointerType::getUnqual(Ctx),
                            PointerType::getUnqual(Ctx));
  GetSuperClass =
      M.getOrInsertFunction("class_getSuperclass", PointerType::getUnqual(Ctx),
                            PointerType::getUnqual(Ctx));

  Loaded = true;
  return true;
}

void CallsCheckerPass::loadIgnoredFunctions(Module &M) {
  auto *AllAnnotations = M.getNamedGlobal("llvm.global.annotations");
  if (!AllAnnotations)
    return;
  for (auto &Op :
       cast<ConstantArray>(AllAnnotations->getOperand(0))->operands()) {
    auto *AnnotationElement = cast<ConstantStruct>(&Op);
    auto *Target = dyn_cast<Function>(AnnotationElement->getOperand(0));
    if (!Target)
      continue;
    auto *Value = AnnotationElement->getOperand(1);
    StringRef ValueStr;
    if (!getConstantStringInfo(Value, ValueStr))
      continue;
    if (ValueStr != "no_external_calls_check")
      continue;
    IgnoredFunctions.insert(Target);
  }
}

void CallsCheckerPass::loadGoodFunctions(Module &M) {
  // Note: the code for `goodFunctions` assumes that runtime LLVM IR is included
  // in the current module, which is true only when the compiler caches are
  // disabled. But this is anyway only an optimization, so it is safe to use the
  // empty list as a fallback.
  auto *G = M.getNamedGlobal("Kotlin_callsCheckerGoodFunctionNames");
  if (!G)
    return;
  auto *Ini = G->hasInitializer() ? G->getInitializer() : nullptr;
  if (!Ini)
    return;
  for (const auto *Op : Ini->operand_values()) {
    auto Str =
        cast<ConstantDataSequential>(cast<GlobalVariable>(Op)->getInitializer())
            ->getAsCString();
    GoodFunctions.push_back(Str);
  }
  std::sort(GoodFunctions.begin(), GoodFunctions.end());
}

Value *CallsCheckerPass::placeCString(Module &M, StringRef S) {
  // TODO(KT-87596): built-in LLVM way?
  auto [It, New] = Strings.try_emplace(S, nullptr);
  if (New) {
    auto *V = ConstantDataArray::getString(M.getContext(), S);
    // TODO(KT-87596): private linkage
    It->second = new GlobalVariable(M, V->getType(), true,
                                    GlobalValue::InternalLinkage, V);
  }
  return It->second;
}

// Shared with KnownFunctionsChecker in the runtime.
bool CallsCheckerPass::isGoodFunction(StringRef Name) const {
  auto It = std::lower_bound(GoodFunctions.begin(), GoodFunctions.end(), Name);
  auto Check = [&](StringRef Banned) {
    if (Banned.back() != '*') {
      return Banned == Name;
    }
    return Name.substr(0, Banned.size() - 1) ==
           Banned.substr(0, Banned.size() - 1);
  };
  if (It != GoodFunctions.end() && Check(*It)) {
    return true;
  }
  if (It != GoodFunctions.begin() && Check(*std::prev(It))) {
    return true;
  }
  return false;
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
