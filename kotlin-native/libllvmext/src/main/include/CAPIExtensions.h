//
// Created by Sergey.Bogolepov on 24.03.2022.
//

#ifndef LIBLLVMEXT_EXTENSIONS_H
#define LIBLLVMEXT_EXTENSIONS_H

#include <llvm-c/Core.h>
#include <llvm-c/Error.h>
#include <llvm-c/Target.h>
#include <llvm-c/TargetMachine.h>
#include <llvm-c/Transforms/PassBuilder.h>


# ifdef __cplusplus
extern "C" {
# endif

void LLVMKotlinInitializeTargets(void);

void LLVMSetNoTailCall(LLVMValueRef Call);

int LLVMInlineCall(LLVMValueRef call);

/// Control LLVM -time-passes flag.
void LLVMSetTimePasses(int enabled);

/// Print timing results. Useful in combination with LLVMSetTimePasses.
void LLVMPrintAllTimersToStdOut(void);

/// Clear all LLVM timers. Allows avoiding automatic printing on shutdown
void LLVMClearAllTimers(void);

void LLVMRunPassesParallel(
        LLVMModuleRef M, const char *Passes, LLVMPassBuilderOptionsRef PBOptions,
        const char *Triple, const LLVMTargetMachineOptionsRef TMOptions,
        unsigned SubModuleCount);

# ifdef __cplusplus
}
# endif

#endif //LIBLLVMEXT_EXTENSIONS_H
