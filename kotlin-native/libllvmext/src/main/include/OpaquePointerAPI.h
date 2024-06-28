
#ifndef LIBLLVMEXT_OPAQUE_POINTER_API_H
#define LIBLLVMEXT_OPAQUE_POINTER_API_H

#include <llvm-c/Core.h>
#include <llvm-c/Target.h>

# ifdef __cplusplus
extern "C" {
# endif

/* These are the opaque pointer functions that are missing in llvm-11, which
must be removed upon upgrading LLVM version.

Copied from llvm release/14.x.
*/

// LLVMConstGEP2 and LLVMConstInBoundsGEP2 are already forward declared in Core.h
LLVMValueRef LLVMConstGEP2(LLVMTypeRef Ty, LLVMValueRef ConstantVal,
                           LLVMValueRef *ConstantIndices, unsigned NumIndices);

LLVMValueRef LLVMConstInBoundsGEP2(LLVMTypeRef Ty, LLVMValueRef ConstantVal,
                                   LLVMValueRef *ConstantIndices,
                                   unsigned NumIndices);

LLVMValueRef LLVMAddAlias2(LLVMModuleRef M, LLVMTypeRef ValueTy,
                           unsigned AddrSpace, LLVMValueRef Aliasee,
                           const char *Name);

unsigned LLVMGetProgramAddressSpace(LLVMModuleRef moduleRef);

# ifdef __cplusplus
}
# endif

#endif // LIBLLVMEXT_OPAQUE_POINTER_API_H

