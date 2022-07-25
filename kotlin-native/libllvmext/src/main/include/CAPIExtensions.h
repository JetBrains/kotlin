//
// Created by Sergey.Bogolepov on 24.03.2022.
//

#ifndef LIBLLVMEXT_EXTENSIONS_H
#define LIBLLVMEXT_EXTENSIONS_H

#endif //LIBLLVMEXT_EXTENSIONS_H

#include <llvm-c/Core.h>
#include <llvm-c/Target.h>


# ifdef __cplusplus
extern "C" {
# endif

void LLVMKotlinAddTargetLibraryInfoWrapperPass(LLVMPassManagerRef passManagerRef, const char* targetTriple);

void LLVMAddObjCARCContractPass(LLVMPassManagerRef passManagerRef);

void LLVMKotlinInitializeTargets();

void LLVMSetNoTailCall(LLVMValueRef Call);

int LLVMInlineCall(LLVMValueRef call);

void LLVMAddThreadSanitizerPass(LLVMPassManagerRef PM);

# ifdef __cplusplus
}
# endif