
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


unsigned LLVMGetProgramAddressSpace(LLVMModuleRef moduleRef);

void LLVMSetOpaquePointers(LLVMContextRef contextRef, int value);

# ifdef __cplusplus
}
# endif

#endif // LIBLLVMEXT_OPAQUE_POINTER_API_H

