// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#ifndef LIBLLVMEXT_C_API_EXTENSIONS_H
#define LIBLLVMEXT_C_API_EXTENSIONS_H

#include "PassesProfile.h"

#include "llvm-c/Error.h"
#include "llvm-c/ExternC.h"
#include "llvm-c/TargetMachine.h"
#include "llvm-c/Types.h"

LLVM_C_EXTERN_C_BEGIN

/// Initialize all needed targets.
///
/// Thread-safe. And makes sure that they are initialized at most once.
void LLVMKotlinInitializeTargets(void);

void LLVMKotlinSetNoTailCall(LLVMValueRef Call);

/// Returns true if code built for the `Actual` triple can be linked into a
/// module built for the `Expected` triple. On top of
/// `llvm::Triple::isCompatibleWith`, the environments must match exactly,
/// because it ignores differences that matter for Apple platforms (device vs.
/// simulator, Mac Catalyst vs. macOS).
LLVMBool LLVMKotlinIsTargetTripleCompatible(const char *Expected,
                                            const char *Actual);

/// Run `Passes` on module `M`.
/// When `Profile` is not `NULL` also collect profiling data and store the
/// result in it.
///
/// NOTE: This function is not thread-safe, because it may write
///       into global variables by modifying CLI-defined options.
///       Currently these arguments affect the global environment:
///       - SaveIRAfterPasses
///       - SaveIRDirectory
LLVMErrorRef LLVMKotlinRunPasses(LLVMModuleRef M, const char *Passes,
                                 LLVMTargetMachineRef TM, int InlinerThreshold,
                                 LLVMKotlinPassesProfileRef *Profile,
                                 const char *SaveIRAfterPasses,
                                 const char *SaveIRDirectory);

/// Instruction selector used by a target machine created with
/// `LLVMKotlinCreateTargetMachine`. The selection is explicit and overrides
/// the target's own per-optimization-level policy, with one exception:
/// `GlobalISel` is treated as `SelectionDAG` for non-Apple targets and for
/// configurations GlobalISel doesn't support (on AArch64: the ILP32 ABIs, such
/// as `arm64_32`, and the large code model on MachO).
/// With `GlobalISel`, functions it cannot select silently fall back to
/// SelectionDAG.
typedef enum {
  LLVMKotlinInstructionSelectorSelectionDAG = 0,
  LLVMKotlinInstructionSelectorGlobalISel = 1,
} LLVMKotlinInstructionSelector;

/// Create a target machine for `TargetTriple` (normalized first).
/// Returns `NULL` on failure and, if `ErrorMessage` is not `NULL`, stores the
/// reason in it; dispose it with `LLVMDisposeMessage`.
LLVMTargetMachineRef
LLVMKotlinCreateTargetMachine(const char *TargetTriple, const char *CPU,
                              const char *Features, LLVMRelocMode RM,
                              LLVMCodeModel CM, LLVMCodeGenOptLevel OL,
                              LLVMKotlinInstructionSelector Selector,
                              char **ErrorMessage);

LLVM_C_EXTERN_C_END

#endif
