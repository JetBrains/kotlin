// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "CAPIExtensions.h"

#include "PassesProfileHandler.h"

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

  INIT_LLVM_TARGET(AArch64)
  INIT_LLVM_TARGET(ARM)
  INIT_LLVM_TARGET(X86)

#undef INIT_LLVM_TARGET
}

void LLVMKotlinSetNoTailCall(LLVMValueRef Call) {
  unwrap<CallInst>(Call)->setTailCallKind(CallInst::TCK_NoTail);
}

int LLVMKotlinInlineCall(LLVMValueRef Call) {
  InlineFunctionInfo IFI;
  return InlineFunction(*unwrap<CallBase>(Call), IFI).isSuccess();
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
