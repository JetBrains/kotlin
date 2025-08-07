//
// Created by Sergey.Bogolepov on 24.03.2022.
//

#include <CAPIExtensions.h>
#include <llvm/IR/Module.h>
#include <llvm/Passes/PassBuilder.h>
#include <llvm/Passes/StandardInstrumentations.h>
#include <llvm/Support/Timer.h>
#include <llvm/Transforms/Utils/Cloning.h>

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

void LLVMSetTimePasses(int enabled) {
    llvm::TimePassesIsEnabled = static_cast<bool>(enabled);
}

void LLVMPrintAllTimersToStdOut() {
    llvm::TimerGroup::printAll(llvm::outs());
}

void LLVMClearAllTimers() {
    llvm::TimerGroup::clearAll();
}

extern "C" LLVMErrorRef LLVMKotlinRunPasses(
        LLVMModuleRef M,
        const char *Passes,
        LLVMTargetMachineRef TM,
        int InlinerThreshold
) {
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

    ModulePassManager MPM;
    if (auto Err = PB.parsePassPipeline(MPM, Passes))
        return wrap(std::move(Err));
    MPM.run(*Mod, MAM);

    return LLVMErrorSuccess;
}