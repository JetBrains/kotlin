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

void LLVMKotlinInitializeTargets(void);

void LLVMKotlinSetNoTailCall(LLVMValueRef Call);

int LLVMKotlinInlineCall(LLVMValueRef Call);

/// Emit `call void @llvm.assume(i1 true) [ "align"(Ptr, i64 Align) ]` at the
/// builder's current insertion point.
///
/// Reason this exists: `AlignmentFromAssumptions` (invoked as part of `default<O3>`) is
/// the only supported channel in current LLVM for propagating pointer-alignment info
/// across `addrspacecast`. Older ptrtoint/and/icmp-based assume patterns were dropped in
/// LLVM 15+. The operand-bundle-form intrinsic isn't exposed by the LLVM C API bindings
/// currently regenerated for K/N (`LLVMBuildCallWithOperandBundles` and
/// `LLVMCreateOperandBundle`), so we go through this small C++ shim instead of adding a
/// new cinterop surface.
///
/// K/N uses this in `generateCudaSharedMemoryRef` to preserve the 16-byte alignment of
/// `__shared__` NVPTX arrays across the addrspacecast to the generic pointer space —
/// otherwise `LoadStoreVectorizer` can't collapse consecutive scalar `ld.shared.b32`
/// loads into a single `ld.shared.v4.b32`.
void LLVMKotlinBuildAlignAssume(LLVMBuilderRef Builder, LLVMValueRef Ptr,
                                uint64_t Align);

/// Set the `contract` fast-math flag on every `fmul`/`fadd`/`fsub` instruction in `M`.
///
/// Reason this exists: K/N's IR emitter produces plain `fmul`/`fadd` without any FMF, and
/// NVPTX's DAG combiner only fuses `a*b + c` into `fma.rn.f32` when either (a) both
/// instructions carry the `contract` FMF, or (b) the function has `unsafe-fp-math=true`.
/// Option (b) is too broad — it also permits reassociation, which reorders summation and
/// diverges from host references beyond the per-op fma-vs-mul+add difference (breaks the
/// naive matmul test at N=4096 with |delta|≈2^-6). Setting `contract` per instruction is
/// the narrowest possible enable: only fusion, no reassociation, no other relaxations.
/// Matches nvcc's `-ffp-contract=fast -fno-fast-math` default for device code.
///
/// InstCombine then folds `contract fmul %a, %b` + `contract fadd %sum, %m` into
/// `@llvm.fmuladd.f32`, which the NVPTX backend lowers to `fma.rn.f32`.
///
/// K/N calls this from `StripDeadDeviceIrPhase` before the O3 pipeline runs.
void LLVMKotlinEnableFPContractInModule(LLVMModuleRef M);

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

LLVM_C_EXTERN_C_END

#endif
