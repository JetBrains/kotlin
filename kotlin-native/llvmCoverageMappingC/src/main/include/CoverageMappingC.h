/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef __COVERAGE_MAPPING_C_H__
# define __COVERAGE_MAPPING_C_H__

#include <llvm-c/Core.h>
#include <llvm-c/Target.h>


# ifdef __cplusplus
extern "C" {
# endif

/**
 * See org.jetbrains.kotlin.backend.konan.llvm.coverage.RegionKind.
 */
enum LLVMCoverageRegionKind {
    CODE,
    GAP,
    EXPANSION
};

/**
 * See org.jetbrains.kotlin.backend.konan.llvm.coverage.Region.
 */
struct LLVMCoverageRegion {
    int fileId;
    int lineStart;
    int columnStart;
    int lineEnd;
    int columnEnd;
    int counterId;
    int expandedFileId;
    enum LLVMCoverageRegionKind kind;
};

struct LLVMFunctionCoverage;

/**
 * Add record in the following format: https://llvm.org/docs/CoverageMappingFormat.html#function-record.
 */
LLVMValueRef
LLVMAddFunctionMappingRecord(LLVMContextRef context, const char *name, uint64_t hash, struct LLVMFunctionCoverage* coverageMapping);

/**
 * Wraps creation of coverage::CoverageMappingWriter and call to coverage::CoverageMappingWriter::write.
 */
struct LLVMFunctionCoverage* LLVMWriteCoverageRegionMapping(unsigned int *fileIdMapping, size_t fileIdMappingSize,
                                           struct LLVMCoverageRegion **mappingRegions, size_t mappingRegionsSize);

void LLVMFunctionCoverageDispose(struct LLVMFunctionCoverage* functionCoverage);

/**
 * Create __llvm_coverage_mapping global.
 */
LLVMValueRef LLVMCoverageEmit(LLVMModuleRef moduleRef, LLVMValueRef *records, size_t recordsSize,
        const char **filenames, int *filenamesIndices, size_t filenamesSize,
        struct LLVMFunctionCoverage** functionCoverages, size_t functionCoveragesSize);

/**
 * Wrapper for `llvm.instrprof.increment` declaration.
 */
LLVMValueRef LLVMInstrProfIncrement(LLVMModuleRef moduleRef);

/**
 * Wrapper for llvm::createPGOFuncNameVar.
 */
LLVMValueRef LLVMCreatePGOFunctionNameVar(LLVMValueRef llvmFunction, const char *pgoFunctionName);

void LLVMAddInstrProfPass(LLVMPassManagerRef passManagerRef, const char* outputFileName);

void LLVMKotlinAddTargetLibraryInfoWrapperPass(LLVMPassManagerRef passManagerRef, const char* targetTriple);

void LLVMKotlinInitializeTargets();

# ifdef __cplusplus
}
# endif
#endif
