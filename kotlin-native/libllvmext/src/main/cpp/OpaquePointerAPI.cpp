#include "OpaquePointerAPI.h"

#include <llvm/IR/IntrinsicInst.h>
#include <llvm/IR/Module.h>

using namespace llvm;

/* These are the opaque pointer functions that are missing in llvm-11, which
must be removed upon upgrading LLVM version.

Copied from llvm release/14.x.
*/

unsigned LLVMGetProgramAddressSpace(LLVMModuleRef moduleRef) {
    auto module = unwrap(moduleRef);
    return module ? module->getDataLayout().getProgramAddressSpace() : 0;
}

void LLVMSetOpaquePointers(LLVMContextRef contextRef, int value) {
    auto context = unwrap(contextRef);
    context->setOpaquePointers(value);
}
