//
// Created by Sergey.Bogolepov on 24.03.2022.
//

#include <CAPIExtensions.h>
#include <llvm/Bitcode/BitcodeWriter.h>
#include <llvm/Bitcode/BitcodeReader.h>
#include <llvm/IR/Module.h>
#include <llvm/Linker/Linker.h>
#include <llvm/Passes/PassBuilder.h>
#include <llvm/Passes/StandardInstrumentations.h>
#include <llvm/Support/ThreadPool.h>
#include <llvm/Transforms/Utils/Cloning.h>
#include <llvm/Transforms/Utils/SplitModule.h>

#include "PassesProfileHandler.h"

using namespace llvm;

namespace {

TargetMachine *unwrap(LLVMTargetMachineRef P) {
    return reinterpret_cast<TargetMachine *>(P);
}

LLVMErrorRef runPasses(
        LLVMModuleRef M,
        const char *Passes,
        LLVMTargetMachineRef TM,
        int InlinerThreshold,
        PassesProfile* Profile
) {
    // Implementation is taken from https://github.com/Kotlin/llvm-project/blob/0fa53d5183ec3c0654631d719dd6dfa7a270ca98/llvm/lib/Passes/PassBuilderBindings.cpp#L47
    TargetMachine *Machine = unwrap(TM);
    Module *Mod = llvm::unwrap(M);

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
        *Profile = PPH.serialize();
    }

    return LLVMErrorSuccess;
}

template <typename T>
class ThreadSafeQueue {
public:
    void push(T value) {
        std::lock_guard<std::mutex> lock(mutex_);
        queue_.push(std::move(value));
        cond_.notify_one();
    }

    T pop() {
        std::unique_lock<std::mutex> lock(mutex_);
        cond_.wait(lock, [this] { return !queue_.empty(); });
        T value = std::move(queue_.front());
        queue_.pop();
        return value;
    }

private:
    std::queue<T> queue_;
    std::mutex mutex_;
    std::condition_variable cond_;
};

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
        const char *Triple,
        const LLVMTargetMachineOptionsRef TMOptions,
        int InlinerThreshold,
        LLVMKotlinPassesProfileRef* Profile,
        unsigned SubModuleCount
) {
    LLVMTargetRef T;
    char *ErrorMessage = nullptr;
    if (LLVMGetTargetFromTriple(Triple, &T, &ErrorMessage)) {
        report_fatal_error("Unable to get target from triple: " + StringRef(ErrorMessage));
        LLVMDisposeMessage(ErrorMessage);
    }

    if (SubModuleCount == 1) {
        LLVMTargetMachineRef TM = LLVMCreateTargetMachineWithOptions(T, Triple, TMOptions);
        if (!TM) {
            report_fatal_error("Failed to create target machine");
        }
        PassesProfile SingleProfile;
        if (auto err = runPasses(M, Passes, TM, InlinerThreshold, Profile ? &SingleProfile : nullptr)) {
            return err;
        }
        if (Profile) {
            *Profile = wrap(new PassesProfile(SingleProfile));
        }
        return LLVMErrorSuccess;
    }

    // Thread pool with one worker per available core
    DefaultThreadPool OptThreadPool(heavyweight_hardware_concurrency(0));

    Module* Mod = unwrap(M);
    llvm::Linker L(*Mod);

    unsigned numPartitions = 0;
    ThreadSafeQueue<SmallString<0>> OptimizedSubModulesQueue;
    ThreadSafeQueue<PassesProfile> ProfilesQueue;

    SplitModule(*Mod, SubModuleCount,
            [&](std::unique_ptr<Module> MPart) {
            // SplitModule might split into fewer modules than SubModuleCount,
            // so we need to know the exact number of modules that will get
            // enqueued.
            numPartitions++;

            // All the MParts share the same Context as the source module. This
            // is not thread safe, so we need to serialize and deserialize into
            // individual LLVMContexts before starting to optimize the module.
            // We need to do the serialization sequentially, because of the
            // shared LLVMContext, but the deserialization can be done by each
            // worker thread.
            SmallString<0> BC;
            raw_svector_ostream BCOS(BC);
            WriteBitcodeToFile(*MPart, BCOS);

            OptThreadPool.async(
                    [&](const SmallString<0> &BC) {
                        LLVMContext Ctx;
                        Expected<std::unique_ptr<Module>> MOrErr =
                            parseBitcodeFile(MemoryBufferRef(BC.str(), "opt-temp"), Ctx);
                        if (auto E = MOrErr.takeError()) {
                            logAllUnhandledErrors(std::move(E), errs(), "Error parsing bitcode in thread: ");
                            report_fatal_error("Failed to read bitcode");
                        }

                        std::unique_ptr<Module> SubM = std::move(*MOrErr);

                        LLVMTargetMachineRef TM = LLVMCreateTargetMachineWithOptions(T, Triple, TMOptions);
                        if (!TM) {
                            report_fatal_error("Failed to create target machine");
                        }

                        PassesProfile SingleProfile;
                        if (LLVMOpaqueError *Err = runPasses(wrap(SubM.get()), Passes, TM, InlinerThreshold, Profile ? &SingleProfile : nullptr)) {
                            logAllUnhandledErrors(unwrap(Err), errs(), "LLVMRunPasses failed: ");
                            report_fatal_error("Passes failed");
                        }

                        LLVMDisposeTargetMachine(TM);

                        // We again need to serialize the module, because
                        // modules need to share context again before linking.
                        
                        SmallString<0> OptimizedBC;
                        raw_svector_ostream OptimizedBCOS(OptimizedBC);
                        WriteBitcodeToFile(*SubM, OptimizedBCOS);

                        OptimizedSubModulesQueue.push(std::move(OptimizedBC));
                        if (Profile) {
                            ProfilesQueue.push(std::move(SingleProfile));
                        }
                    },
                    std::move(BC));
        }, /*PreserveLocals=*/true);

    PassesProfile TotalProfile;
    // Link all the optimized partitions back into the original module.
    for (unsigned i = 0; i < numPartitions; ++i) {
        SmallString<0> SubBC = OptimizedSubModulesQueue.pop();
        Expected<std::unique_ptr<Module>> MOrErr =
            parseBitcodeFile(MemoryBufferRef(SubBC.str(), "opt-temp"), Mod->getContext());
        if (auto E = MOrErr.takeError()) {
            logAllUnhandledErrors(std::move(E), errs(), "Error parsing bitcode for linking: ");
            report_fatal_error("Failed to read bitcode for linking");
        }
        if (L.linkInModule(std::move(*MOrErr), Linker::Flags::OverrideFromSrc)) {
            report_fatal_error("Failed to link bitcode");
        }
        if (Profile) {
            auto SingleProfile = ProfilesQueue.pop();
            TotalProfile.SerializedProfile += "\n";
            TotalProfile.SerializedProfile += SingleProfile.SerializedProfile;
        }
    }
    if (Profile) {
        *Profile = wrap(new PassesProfile(TotalProfile));
    }
    return LLVMErrorSuccess;
}
