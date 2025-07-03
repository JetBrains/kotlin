//
// Created by Sergey.Bogolepov on 24.03.2022.
//

#include <CAPIExtensions.h>

#include <memory>

#include <llvm/Analysis/TargetLibraryInfo.h>
#include <llvm/IR/Constants.h>
#include <llvm/IR/Instructions.h>
#include <llvm/IR/DiagnosticPrinter.h>
#include <llvm/IR/LegacyPassManager.h>
#include <llvm/Linker/Linker.h>
#include <llvm/MC/TargetRegistry.h>
#include <llvm/IR/DiagnosticInfo.h>
#include <llvm/ProfileData/Coverage/CoverageMapping.h>
#include <llvm/Support/WithColor.h>
#include <llvm/Support/ThreadPool.h>
#include <llvm/Support/Error.h>
#include <llvm/Support/Timer.h>
#include <llvm/Transforms/Instrumentation/ThreadSanitizer.h>
#include <llvm/Transforms/ObjCARC.h>
#include <llvm/Transforms/Utils/Cloning.h>
#include <llvm/Bitcode/BitcodeWriter.h>
#include <llvm/Bitcode/BitcodeReader.h>
#include <llvm/Support/CBindingWrapping.h>
#include <llvm-c/Transforms/PassBuilder.h>
#include <condition_variable>
#include <mutex>
#include <queue>
#include "llvm/Transforms/Utils/SplitModule.h"

using namespace llvm;

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

namespace {
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
} // namespace

void LLVMRunPassesParallel(
        LLVMModuleRef M, const char *Passes, LLVMPassBuilderOptionsRef PBOptionsRef,
        const char *Triple, const LLVMTargetMachineOptionsRef TMOptions,
        unsigned SubModuleCount) {

    // Thread pool with one worker per available core
    DefaultThreadPool OptThreadPool(heavyweight_hardware_concurrency(0));

    LLVMTargetRef T;
    char *ErrorMessage = nullptr;
    if (LLVMGetTargetFromTriple(Triple, &T, &ErrorMessage)) {
        report_fatal_error("Unable to get target from triple: " + StringRef(ErrorMessage));
        LLVMDisposeMessage(ErrorMessage);
    }

    Module* Mod = unwrap(M);
    llvm::Linker L(*Mod);

    unsigned numPartitions = 0;
    ThreadSafeQueue<SmallString<0>> OptimizedSubModulesQueue;

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

                        if (LLVMOpaqueError *Err = LLVMRunPasses(wrap(SubM.get()), Passes, TM, PBOptionsRef)) {
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
                    },
                    std::move(BC));
        }, /*PreserveLocals=*/true);


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
    }
}
