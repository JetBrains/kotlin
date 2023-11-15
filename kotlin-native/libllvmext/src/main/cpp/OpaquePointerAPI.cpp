#include "OpaquePointerAPI.h"

#include <llvm/IR/IntrinsicInst.h>
#include <llvm/IR/Module.h>

using namespace llvm;

/* These are the opaque pointer functions that are missing in llvm-11, which
must be removed upon upgrading LLVM version.

Copied from llvm release/14.x.
*/

LLVMValueRef LLVMAddAlias2(LLVMModuleRef M, LLVMTypeRef ValueTy,
                           unsigned AddrSpace, LLVMValueRef Aliasee,
                           const char *Name) {
  return wrap(GlobalAlias::create(unwrap(ValueTy), AddrSpace,
                                  GlobalValue::ExternalLinkage, Name,
                                  unwrap<Constant>(Aliasee), unwrap(M)));
}

LLVMValueRef LLVMConstGEP2(LLVMTypeRef Ty, LLVMValueRef ConstantVal,
                           LLVMValueRef *ConstantIndices, unsigned NumIndices) {
  ArrayRef<Constant *> IdxList(unwrap<Constant>(ConstantIndices, NumIndices),
                               NumIndices);
  Constant *Val = unwrap<Constant>(ConstantVal);
  return wrap(ConstantExpr::getGetElementPtr(unwrap(Ty), Val, IdxList));
}

LLVMValueRef LLVMConstInBoundsGEP2(LLVMTypeRef Ty, LLVMValueRef ConstantVal,
                                   LLVMValueRef *ConstantIndices,
                                   unsigned NumIndices) {
  ArrayRef<Constant *> IdxList(unwrap<Constant>(ConstantIndices, NumIndices),
                               NumIndices);
  Constant *Val = unwrap<Constant>(ConstantVal);
  return wrap(ConstantExpr::getInBoundsGetElementPtr(unwrap(Ty), Val, IdxList));
}

unsigned LLVMGetProgramAddressSpace(LLVMModuleRef moduleRef) {
    auto module = unwrap(moduleRef);
    return module ? module->getDataLayout().getProgramAddressSpace() : 0;
}
