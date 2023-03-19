/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.llvm.coverage

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.llvm.name
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.konan.file.File

private fun RegionKind.toLLVMCoverageRegionKind(): LLVMCoverageRegionKind = when (this) {
    RegionKind.Code -> LLVMCoverageRegionKind.CODE
    RegionKind.Gap -> LLVMCoverageRegionKind.GAP
    is RegionKind.Expansion -> LLVMCoverageRegionKind.EXPANSION
}

private fun LLVMCoverageRegion.populateFrom(region: Region, regionId: Int, filesIndex: Map<IrFile, Int>) = apply {
    fileId = filesIndex.getValue(region.file)
    lineStart = region.startLine
    columnStart = region.startColumn
    lineEnd = region.endLine
    columnEnd = region.endColumn
    counterId = regionId
    kind = region.kind.toLLVMCoverageRegionKind()
    expandedFileId = if (region.kind is RegionKind.Expansion) filesIndex.getValue(region.kind.expandedFile) else 0
}

/**
 * Writes all of the coverage information to the [org.jetbrains.kotlin.backend.konan.NativeGenerationState.llvm.module].
 * See http://llvm.org/docs/CoverageMappingFormat.html for the format description.
 */
internal class LLVMCoverageWriter(
        private val generationState: NativeGenerationState,
        private val filesRegionsInfo: List<FileRegionInfo>
) {
    fun write() {
        if (filesRegionsInfo.isEmpty()) return

        val module = generationState.llvm.module
        val filesIndex = filesRegionsInfo.mapIndexed { index, fileRegionInfo -> fileRegionInfo.file to index }.toMap()

        val coverageGlobal = memScoped {
            val (functionMappingRecords, functionCoverages) = filesRegionsInfo.flatMap { it.functions }.map { functionRegions ->
                val regions = (functionRegions.regions.values).map { region ->
                    alloc<LLVMCoverageRegion>().populateFrom(region, functionRegions.regionEnumeration.getValue(region), filesIndex).ptr
                }
                val fileIds = functionRegions.regions.map { filesIndex.getValue(it.value.file) }.toSet().toIntArray()
                val functionCoverage = LLVMWriteCoverageRegionMapping(
                        fileIds.toCValues(), fileIds.size.signExtend(),
                        regions.toCValues(), regions.size.signExtend())

                val functionName = generationState.llvmDeclarations.forFunction(functionRegions.function).name
                val functionMappingRecord = LLVMAddFunctionMappingRecord(LLVMGetModuleContext(generationState.llvm.module),
                        functionName, functionRegions.structuralHash, functionCoverage)!!

                Pair(functionMappingRecord, functionCoverage)
            }.unzip()
            val (filenames, fileIds) = filesIndex.entries.toList().map { File(it.key.path).absolutePath to it.value }.unzip()
            val retval = LLVMCoverageEmit(module, functionMappingRecords.toCValues(), functionMappingRecords.size.signExtend(),
                    filenames.toCStringArray(this), fileIds.toIntArray().toCValues(), fileIds.size.signExtend(),
                    functionCoverages.map { it }.toCValues(), functionCoverages.size.signExtend())!!

            // TODO: Is there a better way to cleanup fields of T* type in `memScoped`?
            functionCoverages.forEach { LLVMFunctionCoverageDispose(it) }

            retval
        }
        generationState.llvm.usedGlobals.add(coverageGlobal)
    }
}
