
#ifndef LIBLLVMEXT_OPAQUE_POINTER_API_H
#define LIBLLVMEXT_OPAQUE_POINTER_API_H

#include <llvm-c/Core.h>
#include <llvm-c/Target.h>

# ifdef __cplusplus
extern "C" {
# endif

unsigned LLVMGetProgramAddressSpace(LLVMModuleRef moduleRef);

# ifdef __cplusplus
}
# endif

#endif // LIBLLVMEXT_OPAQUE_POINTER_API_H

