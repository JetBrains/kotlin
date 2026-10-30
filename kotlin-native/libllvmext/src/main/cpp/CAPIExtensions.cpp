// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "CAPIExtensions.h"

#include "KotlinPlugin.h"
#include "PassesProfileHandler.h"

#include "llvm-c/Core.h"
#include "llvm-c/TargetMachine.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/Type.h"
#include "llvm/MC/TargetRegistry.h"
#include "llvm/Passes/PassBuilder.h"
#include "llvm/Passes/StandardInstrumentations.h"
#include "llvm/Support/CodeGen.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Target/CodeGenCWrappers.h"
#include "llvm/Target/TargetMachine.h"
#include "llvm/Target/TargetOptions.h"
#include "llvm/TargetParser/Triple.h"

#include <cstddef>
#include <mutex>
#include <optional>

using namespace llvm::kotlin;

namespace llvm {
inline TargetMachine *unwrap(LLVMTargetMachineRef TM) {
  return reinterpret_cast<llvm::TargetMachine *>(TM);
}

inline CodeGenOptLevel unwrap(LLVMCodeGenOptLevel CGO) {
  switch (CGO) {
  case LLVMCodeGenLevelNone:
    return CodeGenOptLevel::None;
  case LLVMCodeGenLevelDefault:
    return CodeGenOptLevel::Default;
  case LLVMCodeGenLevelLess:
    return CodeGenOptLevel::Less;
  case LLVMCodeGenLevelAggressive:
    return CodeGenOptLevel::Aggressive;
  }
  llvm_unreachable("Unknown LLVMCodeGenOptLevel");
}

std::optional<Reloc::Model> unwrap(LLVMRelocMode RM) {
  switch (RM) {
  case LLVMRelocDefault:
    return std::nullopt;
  case LLVMRelocStatic:
    return Reloc::Model::Static;
  case LLVMRelocPIC:
    return Reloc::Model::PIC_;
  case LLVMRelocDynamicNoPic:
    return Reloc::Model::DynamicNoPIC;
  case LLVMRelocROPI:
    return Reloc::Model::ROPI;
  case LLVMRelocRWPI:
    return Reloc::Model::RWPI;
  case LLVMRelocROPI_RWPI:
    return Reloc::Model::ROPI_RWPI;
  }
  llvm_unreachable("Unknown LLVMRelocMode");
}
} // namespace llvm

static std::once_flag TargetsInitializedFlag;

void LLVMKotlinInitializeTargets() {
  std::call_once(TargetsInitializedFlag, [] {
// AsmPrinter and AsmParser are required for in-process object file emission.
#define INIT_LLVM_TARGET(TargetName)                                           \
  LLVMInitialize##TargetName##TargetInfo();                                    \
  LLVMInitialize##TargetName##Target();                                        \
  LLVMInitialize##TargetName##TargetMC();                                      \
  LLVMInitialize##TargetName##AsmPrinter();                                    \
  LLVMInitialize##TargetName##AsmParser();
    INIT_LLVM_TARGET(AArch64)
    INIT_LLVM_TARGET(ARM)
    INIT_LLVM_TARGET(X86)
#undef INIT_LLVM_TARGET
  });
}

void LLVMKotlinSetNoTailCall(LLVMValueRef Call) {
  llvm::unwrap<llvm::CallInst>(Call)->setTailCallKind(
      llvm::CallInst::TCK_NoTail);
}

namespace {

class KotlinRunPassesCommandLineHolder {
public:
  KotlinRunPassesCommandLineHolder() {
    auto &Opts = llvm::cl::getRegisteredOptions();
    PrintAfter = Opts["print-after"];
    IrDumpDirectory = Opts["ir-dump-directory"];
  }

  // NOTE: Avoid adding new CLI arguments overrides as much as possible, as this
  //       makes `LLVMKotlinRunPasses` thread unsafe.
  //       When adding new ones, update `LLVMKotlinRunPasses` documentation in
  //       the header.
  llvm::Error Parse(const char *SaveIRAfterPasses,
                    const char *SaveIRDirectory) {
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
    return llvm::Error::success();
  }

  ~KotlinRunPassesCommandLineHolder() {
    for (auto *Opt : ModifiedOptions) {
      Opt->setDefault();
    }
  }

private:
  llvm::Error SetOption(unsigned OptPos, llvm::cl::Option *Opt,
                        llvm::StringRef Val) {
    ModifiedOptions.push_back(Opt);
    if (Opt->getMiscFlags() & llvm::cl::MiscFlags::CommaSeparated) {
      llvm::StringRef::size_type Pos = Val.find(',');
      while (Pos != llvm::StringRef::npos) {
        auto SingleVal = Val.substr(0, Pos);
        if (Opt->addOccurrence(OptPos, Opt->ArgStr, SingleVal)) {
          return createStringError(llvm::Twine("Failed to parse value of ") +
                                   Opt->ArgStr + " :" + SingleVal);
        }
        // Erase the portion before the comma, AND the comma.
        Val = Val.substr(Pos + 1);
        // Check for another comma.
        Pos = Val.find(',');
      }
    }
    if (Opt->addOccurrence(OptPos, Opt->ArgStr, Val)) {
      return createStringError(llvm::Twine("Failed to parse value of ") +
                               Opt->ArgStr + " :" + Val);
    }
    return llvm::Error::success();
  }

  llvm::cl::Option *PrintAfter;
  llvm::cl::Option *IrDumpDirectory;
  llvm::SmallVector<llvm::cl::Option *> ModifiedOptions;
};

} // namespace

LLVMBool LLVMKotlinIsTargetTripleCompatible(const char *Expected,
                                            const char *Actual) {
  // `isCompatibleWith` ignores the environment for Apple triples, so a
  // simulator or Mac Catalyst module would be accepted for a device or macOS
  // target (and vice versa).
  llvm::Triple E{llvm::Triple::normalize(Expected)};
  llvm::Triple A{llvm::Triple::normalize(Actual)};
  return E.isCompatibleWith(A) && E.getEnvironment() == A.getEnvironment();
}

// GlobalISel is only used for Apple targets: elsewhere it is not reliable
// enough (on Linux x86_64 we experience some huge memory peaks).
static bool isGlobalISelSupported(const llvm::Triple &TT,
                                  std::optional<llvm::CodeModel::Model> CM) {
  if (!TT.isOSDarwin())
    return false;
  if (!TT.isAArch64())
    return true;
  return TT.getArch() != llvm::Triple::aarch64_32 &&
         TT.getEnvironment() != llvm::Triple::GNUILP32 &&
         !(CM == llvm::CodeModel::Large && TT.isOSBinFormatMachO());
}

LLVMTargetMachineRef LLVMKotlinCreateTargetMachine(
    const char *TargetTriple, const char *CPU, const char *Features,
    LLVMRelocMode RM, LLVMCodeModel CM, LLVMCodeGenOptLevel OL,
    LLVMKotlinInstructionSelector Selector, char **ErrorMessage) {
  std::string NormalizedTriple =
      llvm::Triple::normalize(TargetTriple ? TargetTriple : "");

  std::string Error{};
  auto Target = llvm::TargetRegistry::lookupTarget(NormalizedTriple, Error);
  if (!Target) {
    *ErrorMessage = LLVMCreateMessage(Error.c_str());
    return nullptr;
  }

  bool JIT = false;
  std::optional<llvm::Reloc::Model> RelocModel = llvm::unwrap(RM);
  std::optional<llvm::CodeModel::Model> CodeModel = llvm::unwrap(CM, JIT);
  llvm::CodeGenOptLevel OptLevel = llvm::unwrap(OL);

  llvm::Triple TT{NormalizedTriple};
  bool UseGlobalISel = Selector == LLVMKotlinInstructionSelectorGlobalISel &&
                       isGlobalISelSupported(TT, CodeModel);

  // Inherit the compiler flags from clang contained in konan.properties.
  llvm::TargetOptions TO{};
  TO.UseInitArray = true;

  if (UseGlobalISel) {
    TO.EnableGlobalISel = true;
    // Fall back to SelectionDAG if GlobalISel fails.
    TO.GlobalISelAbort = llvm::GlobalISelAbortMode::Disable;
  }

  if (TT.isOSWindows() || TT.isOSLinux()) {
    TO.FunctionSections = true;
    TO.DataSections = true;
  }
  if (TT.isOSWindows() || TT.isAndroid()) {
    TO.EmulatedTLS = true;
  }

  llvm::TargetMachine *TM =
      Target->createTargetMachine(TT, CPU ? CPU : "", Features ? Features : "",
                                  TO, RelocModel, CodeModel, OptLevel, JIT);
  if (!TM) {
    *ErrorMessage = LLVMCreateMessage(Error.c_str());
    return nullptr;
  }

  // Some targets (e.g. AArch64 at -O0) enable GlobalISel in their constructor
  // regardless of `TO`, so the selection is enforced after creation.
  TM->setGlobalISel(UseGlobalISel);
  if (TT.isOSDarwin()) {
      TM->setO0WantsFastISel(false);
  }

  return reinterpret_cast<LLVMTargetMachineRef>(TM);
}

LLVMErrorRef LLVMKotlinRunPasses(LLVMModuleRef M, const char *Passes,
                                 LLVMTargetMachineRef TM, int InlinerThreshold,
                                 LLVMKotlinPassesProfileRef *Profile,
                                 const char *SaveIRAfterPasses,
                                 const char *SaveIRDirectory) {
  // Implementation is taken from
  // https://github.com/Kotlin/llvm-project/blob/0fa53d5183ec3c0654631d719dd6dfa7a270ca98/llvm/lib/Passes/PassBuilderBindings.cpp#L47
  llvm::TargetMachine *Machine = llvm::unwrap(TM);
  llvm::Module *Mod = llvm::unwrap(M);

  KotlinRunPassesCommandLineHolder CommandLineHolder;
  if (auto Err = CommandLineHolder.Parse(SaveIRAfterPasses, SaveIRDirectory)) {
    return llvm::wrap(std::move(Err));
  }

  llvm::PipelineTuningOptions PTO;
  PTO.InlinerThreshold = InlinerThreshold;
  PTO.MaxDevirtIterations = 0;
  llvm::PassInstrumentationCallbacks PIC;
  llvm::PassBuilder PB(Machine, PTO, std::nullopt, &PIC);

  // Register all Kotlin passes.
  getKotlinPluginInfo().RegisterPassBuilderCallbacks(PB);

  llvm::LoopAnalysisManager LAM;
  llvm::FunctionAnalysisManager FAM;
  llvm::CGSCCAnalysisManager CGAM;
  llvm::ModuleAnalysisManager MAM;
  PB.registerLoopAnalyses(LAM);
  PB.registerFunctionAnalyses(FAM);
  PB.registerCGSCCAnalyses(CGAM);
  PB.registerModuleAnalyses(MAM);
  PB.crossRegisterProxies(LAM, FAM, CGAM, MAM);

  llvm::StandardInstrumentations SI(Mod->getContext(), false, false);
  SI.registerCallbacks(PIC, &MAM);

  PassesProfileHandler PPH(Profile != nullptr);
  // Putting last to make this the last callback for before* events;
  // the handler will additionally make sure its after* events are handled
  // before anything else. This makes it so the profile tracks phases only,
  // ignoring other callbacks.
  PPH.registerCallbacks(PIC);

  llvm::ModulePassManager MPM;
  if (auto Err = PB.parsePassPipeline(MPM, Passes))
    return wrap(std::move(Err));
  MPM.run(*Mod, MAM);

  if (Profile != nullptr) {
    *Profile = wrap(new PassesProfile(PPH.serialize()));
  }

  return LLVMErrorSuccess;
}
