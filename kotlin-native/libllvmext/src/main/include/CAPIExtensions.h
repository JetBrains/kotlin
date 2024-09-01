//
// Created by Sergey.Bogolepov on 24.03.2022.
//

#ifndef LIBLLVMEXT_EXTENSIONS_H
#define LIBLLVMEXT_EXTENSIONS_H

#include <llvm-c/Core.h>
#include <llvm-c/Target.h>


# ifdef __cplusplus
extern "C" {
# endif

void LLVMKotlinInitializeTargets();

void LLVMSetNoTailCall(LLVMValueRef Call);

int LLVMInlineCall(LLVMValueRef call);

/// Control LLVM -time-passes flag.
void LLVMSetTimePasses(int enabled);

/// Print timing results. Useful in combination with LLVMSetTimePasses.
void LLVMPrintAllTimersToStdOut();

/// Clear all LLVM timers. Allows avoiding automatic printing on shutdown
void LLVMClearAllTimers();

# ifdef __cplusplus
}
# endif

#endif //LIBLLVMEXT_EXTENSIONS_H