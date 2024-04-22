#include "OpaquePointerAPI.h"

#include <llvm/IR/IntrinsicInst.h>
#include <llvm/IR/Module.h>

using namespace llvm;

unsigned LLVMGetProgramAddressSpace(LLVMModuleRef moduleRef) {
    auto module = unwrap(moduleRef);
    return module ? module->getDataLayout().getProgramAddressSpace() : 0;
}
