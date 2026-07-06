// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "CAPIExtensions.h"

#include "KotlinPlugin.h"
#include "PassesProfileHandler.h"

#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/Intrinsics.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"
#include "llvm/Passes/PassBuilder.h"
#include "llvm/Passes/StandardInstrumentations.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/Error.h"
#include "llvm/Transforms/Utils/Cloning.h"

using namespace llvm;
using namespace llvm::kotlin;

static TargetMachine *unwrap(LLVMTargetMachineRef P) {
  return reinterpret_cast<TargetMachine *>(P);
}

void LLVMKotlinInitializeTargets() {
#define INIT_LLVM_TARGET(TargetName)                                           \
  LLVMInitialize##TargetName##TargetInfo();                                    \
  LLVMInitialize##TargetName##Target();                                        \
  LLVMInitialize##TargetName##TargetMC();

#define INIT_LLVM_TARGET_WITH_ASM_PRINTER(TargetName)                          \
  INIT_LLVM_TARGET(TargetName)                                                 \
  LLVMInitialize##TargetName##AsmPrinter();

  INIT_LLVM_TARGET(AArch64)
  INIT_LLVM_TARGET(ARM)
  INIT_LLVM_TARGET(X86)
  // NVPTX needs the AsmPrinter to emit `.ptx` text via `LLVMTargetMachineEmitToFile`
  // with `LLVMAssemblyFile`. The CUDA device fragment lowers its LLVM module via the
  // NVPTX backend; without these registrations `LLVMGetTargetFromTriple("nvptx64-…")`
  // returns "Target not registered".
  INIT_LLVM_TARGET_WITH_ASM_PRINTER(NVPTX)

#undef INIT_LLVM_TARGET_WITH_ASM_PRINTER
#undef INIT_LLVM_TARGET
}

void LLVMKotlinSetNoTailCall(LLVMValueRef Call) {
  unwrap<CallInst>(Call)->setTailCallKind(CallInst::TCK_NoTail);
}

int LLVMKotlinInlineCall(LLVMValueRef Call) {
  InlineFunctionInfo IFI;
  return InlineFunction(*unwrap<CallBase>(Call), IFI).isSuccess();
}

void LLVMKotlinBuildAlignAssume(LLVMBuilderRef Builder, LLVMValueRef Ptr,
                                uint64_t Align) {
  IRBuilder<> *B = unwrap(Builder);
  Module *M = B->GetInsertBlock()->getModule();
  Function *AssumeFn =
      Intrinsic::getOrInsertDeclaration(M, Intrinsic::assume);
  Value *True = ConstantInt::getTrue(B->getContext());
  OperandBundleDef AlignBundle(
      "align",
      ArrayRef<Value *>{
          unwrap(Ptr),
          ConstantInt::get(Type::getInt64Ty(B->getContext()), Align),
      });
  B->CreateCall(AssumeFn, {True}, {AlignBundle});
}

namespace {

// Owns the cl::Option overrides raised by LLVMKotlinBeginAggressiveLoopUnroll; the
// destructor restores each touched option to its default value. Same pattern as
// KotlinRunPassesCommandLineHolder below; kept separate because that class is scoped to a
// single LLVMKotlinRunPasses invocation whereas this one has an externally controlled
// lifetime (Kotlin-side try/finally).
class AggressiveUnrollScope {
public:
  AggressiveUnrollScope() {
    auto &Opts = cl::getRegisteredOptions();
    // If a value fails to parse (e.g. LLVM renamed an option in a future rebase), the
    // Set() helper reports it via errs() and skips — we deliberately don't fail the
    // compile: PTX would still be correct, just with default LLVM unroll heuristics.
    Set(Opts, "unroll-threshold", "5000");
    Set(Opts, "unroll-partial-threshold", "5000");
    Set(Opts, "unroll-full-max-count", "10000");
    Set(Opts, "unroll-max-iteration-count-to-analyze", "1000");
  }

  ~AggressiveUnrollScope() {
    for (auto *Opt : Modified) {
      Opt->setDefault();
    }
  }

private:
  void Set(StringMap<cl::Option *> &Opts, const char *Name, const char *Val) {
    auto It = Opts.find(Name);
    if (It == Opts.end()) {
      errs() << "LLVMKotlinBeginAggressiveLoopUnroll: option '" << Name
             << "' not registered; skipping.\n";
      return;
    }
    cl::Option *Opt = It->second;
    if (Opt->addOccurrence(0, Opt->ArgStr, Val)) {
      errs() << "LLVMKotlinBeginAggressiveLoopUnroll: failed to set '" << Name
             << "=" << Val << "'; skipping.\n";
      return;
    }
    Modified.push_back(Opt);
  }

  SmallVector<cl::Option *> Modified;
};

} // namespace

LLVMKotlinUnrollScopeRef LLVMKotlinBeginAggressiveLoopUnroll(void) {
  return reinterpret_cast<LLVMKotlinUnrollScopeRef>(new AggressiveUnrollScope());
}

void LLVMKotlinEndAggressiveLoopUnroll(LLVMKotlinUnrollScopeRef scope) {
  delete reinterpret_cast<AggressiveUnrollScope *>(scope);
}

void LLVMKotlinEnableFPContractInModule(LLVMModuleRef M) {
  Module *Mod = unwrap(M);
  for (Function &F : *Mod) {
    for (BasicBlock &BB : F) {
      for (Instruction &I : BB) {
        unsigned Op = I.getOpcode();
        if (Op == Instruction::FMul || Op == Instruction::FAdd ||
            Op == Instruction::FSub) {
          FastMathFlags FMF = I.getFastMathFlags();
          FMF.setAllowContract(true);
          I.setFastMathFlags(FMF);
        }
      }
    }
  }
}

namespace {

class KotlinRunPassesCommandLineHolder {
public:
  KotlinRunPassesCommandLineHolder() {
    auto &Opts = cl::getRegisteredOptions();
    PrintAfter = Opts["print-after"];
    IrDumpDirectory = Opts["ir-dump-directory"];
  }

  // NOTE: Avoid adding new CLI arguments overrides as much as possible, as this
  //       makes `LLVMKotlinRunPasses` thread unsafe.
  //       When adding new ones, update `LLVMKotlinRunPasses` documentation in
  //       the header.
  Error Parse(const char *SaveIRAfterPasses, const char *SaveIRDirectory) {
    unsigned OptPos = 0;
    if (SaveIRAfterPasses != nullptr) {
      if (auto Err = SetOption(OptPos++, PrintAfter, SaveIRAfterPasses)) {
        return Err;
      }
    }
    if (SaveIRDirectory != nullptr) {
      if (auto Err = SetOption(OptPos++, IrDumpDirectory, SaveIRDirectory)) {
        return Err;
      }
    }
    return Error::success();
  }

  ~KotlinRunPassesCommandLineHolder() {
    for (auto *Opt : ModifiedOptions) {
      Opt->setDefault();
    }
  }

private:
  Error SetOption(unsigned OptPos, cl::Option *Opt, StringRef Val) {
    ModifiedOptions.push_back(Opt);
    if (Opt->getMiscFlags() & cl::MiscFlags::CommaSeparated) {
      StringRef::size_type Pos = Val.find(',');
      while (Pos != StringRef::npos) {
        auto SingleVal = Val.substr(0, Pos);
        if (Opt->addOccurrence(OptPos, Opt->ArgStr, SingleVal)) {
          return createStringError(Twine("Failed to parse value of ") +
                                   Opt->ArgStr + " :" + SingleVal);
        }
        // Erase the portion before the comma, AND the comma.
        Val = Val.substr(Pos + 1);
        // Check for another comma.
        Pos = Val.find(',');
      }
    }
    if (Opt->addOccurrence(OptPos, Opt->ArgStr, Val)) {
      return createStringError(Twine("Failed to parse value of ") +
                               Opt->ArgStr + " :" + Val);
    }
    return Error::success();
  }

  cl::Option *PrintAfter;
  cl::Option *IrDumpDirectory;
  SmallVector<cl::Option *> ModifiedOptions;
};

} // namespace

LLVMErrorRef LLVMKotlinRunPasses(LLVMModuleRef M, const char *Passes,
                                 LLVMTargetMachineRef TM, int InlinerThreshold,
                                 LLVMKotlinPassesProfileRef *Profile,
                                 const char *SaveIRAfterPasses,
                                 const char *SaveIRDirectory) {
  // Implementation is taken from
  // https://github.com/Kotlin/llvm-project/blob/0fa53d5183ec3c0654631d719dd6dfa7a270ca98/llvm/lib/Passes/PassBuilderBindings.cpp#L47
  TargetMachine *Machine = unwrap(TM);
  Module *Mod = unwrap(M);

  KotlinRunPassesCommandLineHolder CommandLineHolder;
  if (auto Err = CommandLineHolder.Parse(SaveIRAfterPasses, SaveIRDirectory)) {
    return wrap(std::move(Err));
  }

  PipelineTuningOptions PTO;
  PTO.InlinerThreshold = InlinerThreshold;
  PTO.MaxDevirtIterations = 0;
  PassInstrumentationCallbacks PIC;
  PassBuilder PB(Machine, PTO, std::nullopt, &PIC);

  // Register all Kotlin passes.
  getKotlinPluginInfo().RegisterPassBuilderCallbacks(PB);

  LoopAnalysisManager LAM;
  FunctionAnalysisManager FAM;
  CGSCCAnalysisManager CGAM;
  ModuleAnalysisManager MAM;
  PB.registerLoopAnalyses(LAM);
  PB.registerFunctionAnalyses(FAM);
  PB.registerCGSCCAnalyses(CGAM);
  PB.registerModuleAnalyses(MAM);
  PB.crossRegisterProxies(LAM, FAM, CGAM, MAM);

  StandardInstrumentations SI(Mod->getContext(), false, false);
  SI.registerCallbacks(PIC, &MAM);

  PassesProfileHandler PPH(Profile != nullptr);
  // Putting last to make this the last callback for before* events;
  // the handler will additionally make sure its after* events are handled
  // before anything else. This makes it so the profile tracks phases only,
  // ignoring other callbacks.
  PPH.registerCallbacks(PIC);

  ModulePassManager MPM;
  if (auto Err = PB.parsePassPipeline(MPM, Passes))
    return wrap(std::move(Err));
  MPM.run(*Mod, MAM);

  if (Profile != nullptr) {
    *Profile = wrap(new PassesProfile(PPH.serialize()));
  }

  return LLVMErrorSuccess;
}
