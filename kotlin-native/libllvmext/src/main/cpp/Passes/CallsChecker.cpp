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

#include <algorithm>

using namespace llvm;
using namespace llvm::kotlin;

// clang-format off
static constexpr std::array GoodFunctionNames = {
  "\x01_mprotect",
  "mprotect",
  "posix_memalign",

  "_ZL15_objc_terminatev", // _objc_terminate()
  "_ZNKSt8__detail20_Prime_rehash_policy14_M_need_rehashEmmm", // std::__detail::_Prime_rehash_policy::_M_need_rehash(unsigned long, unsigned long, unsigned long) const
  "_ZNKSt8__detail20_Prime_rehash_policy14_M_need_rehashEyyy", // std::__detail::_Prime_rehash_policy::_M_need_rehash(unsigned long long, unsigned long long, unsigned long long) const
  "_ZNSaIcED2Ev", // std::allocator<char>::~allocator()
  "_ZNSt13exception_ptrC1ERKS_", // std::exception_ptr::exception_ptr(std::exception_ptr const&)
  "_ZNSt13exception_ptrD1Ev", // std::exception_ptr::~exception_ptr()
  "_ZNSt15__exception_ptr13exception_ptrC1ERKS0_", // std::__exception_ptr::exception_ptr::exception_ptr(std::__exception_ptr::exception_ptr const&)
  "_ZNSt15__exception_ptr13exception_ptrD1Ev", // std::__exception_ptr::exception_ptr::~exception_ptr()
  "_ZNSt18condition_variableD1Ev", // std::condition_variable::~condition_variable()
  "_ZNSt3__112__next_primeEm", // std::__1::__next_prime(unsigned long)
  "_ZNSt3__112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEE7reserveEm", // std::__1::basic_string<char, std::__1::char_traits<char>, std::__1::allocator<char>>::reserve(unsigned long)
  "_ZNSt3__112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEE9push_backEc", // std::__1::basic_string<char, std::__1::char_traits<char>, std::__1::allocator<char>>::push_back(char)
  "_ZNSt3__112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEED1Ev", // std::__1::basic_string<char, std::__1::char_traits<char>, std::__1::allocator<char> >::~basic_string()
  "_ZNSt3__16chrono12steady_clock3nowEv", // std::__1::chrono::steady_clock::now()
  "_ZNSt3__19to_stringEi", // std::__1::to_string(int)
  "_ZNSt6chrono3_V212steady_clock3nowEv", // std::chrono::_V2::steady_clock::now()
  "_ZNSt8__detail15_List_node_base7_M_hookEPS0_", // std::__detail::_List_node_base::_M_hook(std::__detail::_List_node_base*)
  "_ZNSt8__detail15_List_node_base9_M_unhookEv", // std::__detail::_List_node_base::_M_unhook()
  "_ZNSt8__detail15_List_node_base11_M_transferEPS0_S1_", // std::__detail::_List_node_base::_M_transfer(std::__detail::_List_node_base*, std::__detail::_List_node_base*)
  "_ZNSt9exceptionD2Ev", // std::exception::~exception()
  "_ZSt17current_exceptionv", // std::current_exception()
  "_ZSt17rethrow_exceptionSt13exception_ptr", // std::rethrow_exception(std::exception_ptr)
  "_ZSt29_Rb_tree_insert_and_rebalancebPSt18_Rb_tree_node_baseS0_RS_", // std::_Rb_tree_insert_and_rebalance(bool, std::_Rb_tree_node_base*, std::_Rb_tree_node_base*, std::_Rb_tree_node_base&)
  "_ZSt9terminatev", // std::terminate()
  "_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev", // std::__cxx11::basic_string<char, std::char_traits<char>, std::allocator<char> >::~basic_string()
  "_ZSt17rethrow_exceptionNSt15__exception_ptr13exception_ptrE", // std::rethrow_exception(std::__exception_ptr::exception_ptr)
  "_ZSt28_Rb_tree_rebalance_for_erasePSt18_Rb_tree_node_baseRS_", // std::_Rb_tree_rebalance_for_erase(std::_Rb_tree_node_base*, std::_Rb_tree_node_base&)
  "_ZN9__gnu_cxx27__verbose_terminate_handlerEv", // __gnu_cxx::__verbose_terminate_handler()
  "_Znwm", // new
  "_Znwy", // operator new(unsigned long long)
  "_ZdlPv", // delete
  "_ZdlPvm", // operator delete(void*, unsigned long)
  "_ZNSt3__16thread20hardware_concurrencyEv", // std::__1::thread::hardware_concurrency()
  "_ZNSt6thread20hardware_concurrencyEv", // std::thread::hardware_concurrency()
  "__mingw_vsnprintf",
  "__cxa_allocate_exception",
  "__cxa_begin_catch",
  "__cxa_end_catch",
  "__cxa_throw",
  "__cxa_rethrow",
  "__memset_chk",

  "abort",
  "acos",
  "acosf",
  "acosh",
  "acoshf",
  "asin",
  "asinf",
  "asinh",
  "asinhf",
  "atan",
  "atanf",
  "atan2",
  "atan2f",
  "atanf",
  "atanh",
  "atanhf",
  "calloc",
  "clock_gettime",
  "cos",
  "cosf",
  "cosh",
  "cosh",
  "coshf",
  "coshf",
  "cbrt",
  "cbrtf",
  "exit",
  "exp",
  "expf",
  "expm1",
  "expm1f",
  "exp10",
  "exp10f",
  "__exp10",
  "__exp10f",
  "free",
  "getrusage",
  "gettimeofday",
  "hypot",
  "hypotf",
  "isinf",
  "isnan",
  "log",
  "logf",
  "log1p",
  "log1pf",
  "log10",
  "log10f",
  "log2",
  "log2f",
  "malloc",
  "memcmp",
  "memmem",
  "mmap",
  "\x01_mmap",
  "munmap",
  "\x01_munmap",
  "nextafter",
  "nextafterf",
  "pow",
  "powf",
  "remainder",
  "remainderf",
  "sin",
  "sinf",
  "sinh",
  "sinhf",
  "snprintf",
  "sqrt",
  "sqrtf",
  "strcmp",
  "strlen",
  "strnlen",
  "tan",
  "tanf",
  "tanh",
  "tanhf",
  "vsnprintf",
  "bcmp",

  "gettid",

  "getenv",
  "setenv",
  "unsetenv",

  "dispatch_async_f",
  "dispatch_once",
  "pthread_equal",
  "pthread_key_create",
  "pthread_once",
  "pthread_main_np",
  "pthread_self",

  "+[NSMethodSignature signatureWithObjCTypes:]",
  "+[NSNull null]",
  "+[NSObject allocWithZone:]",
  "+[NSObject class]",
  "+[NSObject conformsToProtocol:]",
  "+[NSObject isKindOfClass:]",
  "+[NSObject isSubclassOfClass:]",
  "+[NSObject new]",
  "+[NSString stringWithFormat:]",
  "+[NSString stringWithUTF8String:]",
  "-[NSPlaceholderValue initWithBytes:objCType:]",
  "-[NSException name]",
  "-[NSException reason]",
  "-[NSMethodSignature getArgumentTypeAtIndex:]",
  "-[NSMethodSignature methodReturnType]",
  "-[NSMethodSignature numberOfArguments]",
  "-[NSObject class]",
  "-[NSObject conformsToProtocol:]",
  "-[NSObject init]",
  "-[NSObject isKindOfClass:]",
  "-[NSPlaceholderString initWithBytes:length:encoding:]",
  "-[NSPlaceholderString initWithBytesNoCopy:length:encoding:freeWhenDone:]",
  "-[NSValue init]",
  "-[NSValue pointerValue]",
  "-[__NSCFBoolean boolValue]",
  "-[__NSCFNumber doubleValue]",
  "-[__NSCFNumber floatValue]",
  "-[__NSCFNumber intValue]",
  "-[__NSCFNumber longLongValue]",
  "-[__NSCFNumber objCType]",
  "-[__NSCFString isEqual:]",
  "CFStringCreateCopy",
  "CFStringGetCharacters",
  "CFStringGetLength",
  "CFStringGetFastestEncoding",
  "_Block_copy",
  "_Block_object_assign",
  "class_getName",
  "class_getSuperclass",
  "class_isMetaClass",
  "ivar_getOffset",
  "method_getName",
  "method_getTypeEncoding",
  "objc_alloc",
  "objc_alloc_init",
  "objc_autorelease",
  "objc_autoreleasePoolPush",
  "objc_autoreleaseReturnValue",
  "objc_getAssociatedObject",
  "objc_getClass",
  "objc_getProtocol",
  "objc_lookUpClass",
  "object_getClass",
  "object_isClass",
  "_os_signpost_emit_with_name_impl",
  "os_signpost_enabled",
  "os_signpost_id_make_with_pointer",
  "protocol_getName",

  "llvm.abs.*",
  "llvm.assume",
  "llvm.ceil.*",
  "llvm.copysign.*",
  "llvm.cos.*",
  "llvm.ctlz.*",
  "llvm.ctpop.*",
  "llvm.cttz.*",
  "llvm.dbg.*",
  "llvm.eh.typeid.for",
  "llvm.eh.typeid.for.p0",
  "llvm.exp.*",
  "llvm.exp10.*",
  "llvm.experimental.noalias.scope.decl",
  "llvm.fabs.*",
  "llvm.fabs.*",
  "llvm.floor.*",
  "llvm.fmuladd.*",
  "llvm.instrprof.*",
  "llvm.lifetime.*",
  "llvm.log.*",
  "llvm.log10.*",
  "llvm.log2.*",
  "llvm.memcpy.*",
  "llvm.memmove.*",
  "llvm.memset.*",
  "llvm.objc.autorelease",
  "llvm.objc.autoreleaseReturnValue",
  "llvm.vector.*",
  "llvm.objectsize.*",
  "llvm.pow.*",
  "llvm.rint.*",
  "llvm.sin.*",
  "llvm.sinh.*",
  "llvm.cosh.*",
  "llvm.asin.*",
  "llvm.acos.*",
  "llvm.tan.*",
  "llvm.tanh.*",
  "llvm.atan.*",
  "llvm.atan2.*",
  "llvm.smax.*",
  "llvm.smin.*",
  "llvm.sqrt.*",
  "llvm.threadlocal.address*",
  "llvm.umax.*",
  "llvm.umin.*",
  "llvm.umul.*",
  "llvm.va_end",
  "llvm.va_start",
  "llvm.x86.avx2.*",
  "llvm.x86.ssse3.*",
  "llvm.x86.sse2.*",
  "llvm.uadd.sat.*",
  "llvm.aarch64.neon.*",

  "SetConsoleOutputCP",
  "SetConsoleCP",
  "QueryPerformanceCounter",
  "VirtualAlloc",
  "FlsSetValue",
  "GetCurrentProcess",
  "GetCurrentThreadId",
  "GetLastError",
  "FlsFree",
  "K32GetProcessMemoryInfo",
  "VirtualFree",
  "madvise",
  "_aligned_free",
  "_aligned_malloc",
};
// clang-format on

static std::vector<StringRef> goodFunctionNamesSorted() {
  std::vector<StringRef> result;
  std::transform(GoodFunctionNames.begin(), GoodFunctionNames.end(),
                 std::back_inserter(result),
                 [](const char *str) { return str; });
  std::sort(result.begin(), result.end());
  return result;
}

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
  if (auto *F = dyn_cast_or_null<Function>(V)) {
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
  if (auto *Cast = dyn_cast_or_null<CastInst>(V)) {
    return getPossiblyExternalCalledFunction(Cast->getOperand(0));
  }
  if (isIndirectCallArgument(V)) {
    return ExternalCallInfo(std::nullopt, V);
  }
  if (isa<InlineAsm>(V)) {
    return std::nullopt;
  }
  if (auto *Expr = dyn_cast_or_null<ConstantExpr>(V)) {
    switch (Expr->getOpcode()) {
    case Instruction::BitCast:
      return getPossiblyExternalCalledFunction(Expr->getOperand(0));
    default:
      reportFatalInternalError(
          formatv("Not implemented constant type {0}", Expr->getOpcodeName()));
    }
  }
  if (auto *A = dyn_cast_or_null<GlobalAlias>(V)) {
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
    return false;

  for (auto &BB : F) {
    Changed = run(BB) || Changed;
  }

  return Changed;
}

bool CallsCheckerPass::run(BasicBlock &BB) {
  // First collect all the calls and only then start modifying the instructions.
  SmallVector<CallBase *> Calls;
  for (auto &I : BB) {
    if (auto *C = dyn_cast_or_null<CallBase>(&I)) {
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

  // TODO: Good function check.

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
  // TODO: why?
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
    auto *SuperStructType = StructType::get(Builder.getPtrTy(), Builder.getPtrTy());
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

  auto *CallSiteDescriptionGlobal = placeCString(*Builder.GetInsertBlock()->getModule(), CallSiteDescription);

  Value *CalledNameV = ConstantPointerNull::get(Builder.getPtrTy());
  if (CalledName) {
    CalledNameV = placeCString(*Builder.GetInsertBlock()->getModule(), *CalledName);
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
    auto *Target = dyn_cast_or_null<Function>(AnnotationElement->getOperand(0));
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

Value *CallsCheckerPass::placeCString(Module &M, StringRef S) {
  // TODO: built-in LLVM way?
  auto [It, New] = Strings.try_emplace(S, nullptr);
  if (New) {
    auto *V = ConstantDataArray::getString(M.getContext(), S);
    // TODO: private linkage
    It->second = new GlobalVariable(M, V->getType(), true, GlobalValue::InternalLinkage, V);
  }
  return It->second;
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

  auto &Ctx = M.getContext();

  std::vector<Constant *> GoodFunctionNamesSorted;
  for (auto Str : goodFunctionNamesSorted()) {
    auto *ConstStr = ConstantDataArray::getString(Ctx, Str);
    GoodFunctionNamesSorted.push_back(new GlobalVariable(
        M, ConstStr->getType(), true, GlobalValue::PrivateLinkage, ConstStr));
  }
  auto *GoodFunctionNamesSortedArrValue = ConstantArray::get(
      ArrayType::get(PointerType::getUnqual(Ctx), GoodFunctionNames.size()),
      GoodFunctionNamesSorted);
  auto *GoodFunctionNamesSortedArr = new GlobalVariable(
      M, GoodFunctionNamesSortedArrValue->getType(), true,
      GlobalValue::PrivateLinkage, GoodFunctionNamesSortedArrValue,
      "kotlin.callsChecker.goodFunctionNamesSorted");

  auto *GoodFunctionNamesSize =
      ConstantInt::get(Type::getInt64Ty(Ctx), GoodFunctionNamesSorted.size());

  auto *GoodFunctionNamesSortedOverride =
      M.getOrInsertGlobal("Kotlin_callsChecker_goodFunctionNamesSorted",
                          PointerType::getUnqual(Ctx));
  GoodFunctionNamesSortedOverride->setConstant(true);
  GoodFunctionNamesSortedOverride->setLinkage(GlobalValue::LinkOnceODRLinkage);
  GoodFunctionNamesSortedOverride->setInitializer(GoodFunctionNamesSortedArr);

  auto *GoodFunctionNamesSizeOverride =
      M.getOrInsertGlobal("Kotlin_callsChecker_goodFunctionNamesSize",
                          GoodFunctionNamesSize->getType());
  GoodFunctionNamesSizeOverride->setConstant(true);
  GoodFunctionNamesSizeOverride->setLinkage(GlobalValue::LinkOnceODRLinkage);
  GoodFunctionNamesSizeOverride->setInitializer(GoodFunctionNamesSize);

  SmallVector<Constant *> KnownFunctions;
  for (auto &F : M) {
    if (isAKnownFunction(F)) {
      KnownFunctions.push_back(&F);
    }
  }

  // If there are no known functions, no need to register this module with the
  // runtime.
  if (KnownFunctions.empty())
    return true;

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
