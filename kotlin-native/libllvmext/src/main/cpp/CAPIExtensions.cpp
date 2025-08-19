//
// Created by Sergey.Bogolepov on 24.03.2022.
//

#include <CAPIExtensions.h>
#include <llvm/IR/Module.h>
#include <llvm/Passes/PassBuilder.h>
#include <llvm/Passes/StandardInstrumentations.h>
#include <llvm/Transforms/Utils/Cloning.h>

#include "PassesProfileHandler.h"

using namespace llvm;

namespace {

TargetMachine *unwrap(LLVMTargetMachineRef P) {
    return reinterpret_cast<TargetMachine *>(P);
}

}

void LLVMKotlinInitializeTargets() {
#define INIT_LLVM_TARGET(TargetName) \
    LLVMInitialize##TargetName##TargetInfo();\
    LLVMInitialize##TargetName##Target();\
    LLVMInitialize##TargetName##TargetMC();

    INIT_LLVM_TARGET(AArch64)
    INIT_LLVM_TARGET(ARM)
    INIT_LLVM_TARGET(X86)

#undef INIT_LLVM_TARGET
}

void LLVMSetNoTailCall(LLVMValueRef Call) {
    unwrap<CallInst>(Call)->setTailCallKind(CallInst::TCK_NoTail);
}

int LLVMInlineCall(LLVMValueRef call) {
  InlineFunctionInfo IFI;
  return InlineFunction(*unwrap<CallBase>(call), IFI).isSuccess();
}

extern "C" LLVMErrorRef LLVMKotlinRunPasses(
        LLVMModuleRef M,
        const char *Passes,
        LLVMTargetMachineRef TM,
        int InlinerThreshold,
        LLVMKotlinPassesProfileRef* Profile
) {
    // Implementation is taken from https://github.com/Kotlin/llvm-project/blob/0fa53d5183ec3c0654631d719dd6dfa7a270ca98/llvm/lib/Passes/PassBuilderBindings.cpp#L47
    TargetMachine *Machine = unwrap(TM);
    Module *Mod = unwrap(M);

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
    // the handler will additionally make sure its after* events are handled before anything else.
    // This makes it so the profile tracks phases only, ignoring other callbacks.
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