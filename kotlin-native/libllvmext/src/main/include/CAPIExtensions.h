//
// Created by Sergey.Bogolepov on 24.03.2022.
//

#ifndef LIBLLVMEXT_EXTENSIONS_H
#define LIBLLVMEXT_EXTENSIONS_H

#include <PassesProfile.h>
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

/// Run `Passes` on module `M`.
/// When `Profile` is not `NULL` also collect profiling data and store the result in it.
LLVMErrorRef LLVMKotlinRunPasses(
        LLVMModuleRef M,
        const char *Passes,
        LLVMTargetMachineRef TM,
        int InlinerThreshold,
        LLVMKotlinPassesProfileRef* Profile
);

# ifdef __cplusplus
}
# endif

#endif //LIBLLVMEXT_EXTENSIONS_H
