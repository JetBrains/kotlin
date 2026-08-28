/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases.split

import kotlinx.cinterop.toCValues
import llvm.*
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.DependenciesTrackingResult
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ResolvedCacheBinaries
import org.jetbrains.kotlin.backend.konan.ResolvedLibraryCacheBinaries
import org.jetbrains.kotlin.backend.konan.driver.phases.*
import org.jetbrains.kotlin.backend.konan.resolveCacheBinaries
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.library.isImplicitlyLoadedFromKotlinNativeDistribution
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.packageFqName
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.io.path.*

private const val MANIFEST_MODULE_NAME: String = "manifest"
private const val SECTION_NAME: String = "__TEXT,__kaldo_boot"
private const val BOOTSTRAP_START_MANIFEST_DATA_NAME: String = "bootStartManifestData"
private const val BOOTSTRAP_START_MANIFEST_NAME: String = "bootStartManifest"
private const val FORMAT_IDENTIFIER: String = "KALD0"
private const val MANIFEST_HEADER_SIZE: Long = 5L + Long.SIZE_BYTES + Long.SIZE_BYTES + Int.SIZE_BYTES
private const val MANIFEST_ENTRY_SIZE: Long = 1L + Long.SIZE_BYTES + Long.SIZE_BYTES

internal data class BootstrapCompilationMetadata(
        val forceLoadCaches: List<ResolvedLibraryCacheBinaries>,
        val payloadsToLoadAtRuntime: List<String>,
        val resolvedCaches: ResolvedCacheBinaries
)

private enum class PayloadKind(val raw: Int) {
    OBJECT(0), ARCHIVE(1)
}

private fun Path.toPayloadKind() = when (this.extension) {
    "o" -> PayloadKind.OBJECT
    "a" -> PayloadKind.ARCHIVE
    else -> error("Unsupported payload kind for ${this.extension}.")
}

private data class ManifestHeader(
        val formatIdentifier: String,
        val manifestSize: Long,
        val bundleSize: Long,
        val entries: Int,
)

private data class ManifestEntry(
        val path: Path,
        val kind: PayloadKind,
        val offset: Long,
        val size: Long,
)

private data class Manifest(
        val header: ManifestHeader,
        val entries: List<ManifestEntry>
)

private fun BootstrapCompilationMetadata.toManifest(configuration: CompilerConfiguration): Manifest {
    val [existingPayloads, missingPayloads] = payloadsToLoadAtRuntime
            .map { Path(it) }
            .partition { it.exists() }

    for (missingPayload in missingPayloads) {
        configuration.report(
                CliDiagnostics.KONAN_ARGUMENT_STRONG_WARNING,
                "Bootstrap payload does not exist and will be omitted from the embedded manifest: $missingPayload." +
                        "Please report this issue in our tracker."
        )
    }

    val payloadsToSize = existingPayloads.associateWith { it.fileSize() }
    val bundleSize = payloadsToSize.values.sum()
    val manifestSize = MANIFEST_HEADER_SIZE + MANIFEST_ENTRY_SIZE * payloadsToSize.size + bundleSize

    var offset = 0L
    val entries = payloadsToSize.map { [path, size] ->
        ManifestEntry(
                path = path,
                kind = path.toPayloadKind(),
                offset = offset,
                size = size,
        ).also { offset += size }
    }

    val header = ManifestHeader(FORMAT_IDENTIFIER, manifestSize, bundleSize, entries.size)

    return Manifest(header, entries)
}

private fun Manifest.toByteArray(byteOrder: ByteOrder): ByteArray {
    require(header.manifestSize <= Int.MAX_VALUE) {
        "The bootstrap manifest is too large: ${header.manifestSize} bytes."
    }

    return ByteBuffer.allocate(header.manifestSize.toInt()).order(byteOrder).apply {
        put(header.formatIdentifier.toByteArray(Charsets.US_ASCII))
        putLong(header.manifestSize)
        putLong(header.bundleSize)
        putInt(header.entries)

        for ([_, kind, offset, size] in entries) {
            put(kind.raw.toByte())
            putLong(offset)
            putLong(size)
        }

        entries.forEach { put(it.path.readBytes()) }
    }.array()
}

/**
 * Write the bootstrap manifest into the *read-only* data section of the given module.
 *
 * At the time of writing, this function works mainly for Darwin (i.e., macOS, iOS, ...).
 */
private fun LLVMModuleRef.embedBootstrapManifest(
        context: LLVMContextRef,
        configuration: CompilerConfiguration,
        metadata: BootstrapCompilationMetadata,
) {
    val manifest = metadata.toManifest(configuration)
    val targetByteOrder = if (LLVMByteOrder(LLVMGetModuleDataLayout(this)) == LLVMByteOrdering.LLVMBigEndian) {
        ByteOrder.BIG_ENDIAN
    } else {
        ByteOrder.LITTLE_ENDIAN
    }
    val manifestBytes = manifest.toByteArray(targetByteOrder)
    val dataInitializer = LLVMConstStringInContext(context, manifestBytes.toCValues(), manifestBytes.size, 1)
    val dataGlobal = LLVMAddGlobal(this, LLVMTypeOf(dataInitializer), BOOTSTRAP_START_MANIFEST_DATA_NAME).apply {
        LLVMSetInitializer(this, dataInitializer)
        LLVMSetGlobalConstant(this, 1)
        LLVMSetLinkage(this, LLVMLinkage.LLVMInternalLinkage)
        LLVMSetAlignment(this, 8)
        LLVMSetSection(this, SECTION_NAME)
    }
    val ptrType = LLVMPointerType(LLVMInt8TypeInContext(context), 0)
    LLVMAddGlobal(this, ptrType, BOOTSTRAP_START_MANIFEST_NAME).apply {
        LLVMSetInitializer(this, LLVMConstBitCast(dataGlobal, ptrType))
        LLVMSetGlobalConstant(this, 1)
        LLVMSetLinkage(this, LLVMLinkage.LLVMExternalLinkage)
    }
}

internal fun PhaseEngine<NativeGenerationState>.resolveBootstrapMetadata(
        dependenciesTrackingResult: DependenciesTrackingResult,
        bootstrapObjectPath: Path,
): BootstrapCompilationMetadata {

    // Resolve cache binaries (stdlib, platform libs, etc.) that the host must link against
    val resolvedCaches = resolveCacheBinaries(context.config.cachedLibraries, dependenciesTrackingResult)
    val [forceLoadCaches, jitCaches] = resolvedCaches.staticLibraries.partition { [library, _] ->
        library.isNativeStdlib ||
                library.isImplicitlyLoadedFromKotlinNativeDistribution ||
                library.packageFqName?.let { it in context.config.splitCompilationForceLinkCachePackages } == true
    }
    return BootstrapCompilationMetadata(
            forceLoadCaches,
            listOf(bootstrapObjectPath.absolutePathString()) + jitCaches.flatMap { it.binaries },
            resolvedCaches,
    )
}

/**
 * The split-compilation manifest defines which object and archive payloads should be loaded
 * at the start of the host.
 */
internal fun PhaseEngine<NativeGenerationState>.generateManifestObject(
        manifest: BootstrapCompilationMetadata,
        temporaryFiles: TempFiles,
): Path {
    val manifestObjectPath = temporaryFiles.create(MANIFEST_MODULE_NAME, ".o")
    val manifestBitcodePath = temporaryFiles.createBitcodeFile(MANIFEST_MODULE_NAME)
    val manifestModule = LLVMModuleCreateWithNameInContext(MANIFEST_MODULE_NAME, context.llvmContext)!!.apply {
        LLVMSetDataLayout(this, context.runtime.dataLayout)
        embedBootstrapManifest(context.llvmContext, context.config.configuration, manifest)
    }
    runAndMeasurePhase(WriteBitcodeFilePhase, WriteBitcodeFileInput(manifestModule, manifestBitcodePath))
    runAndMeasurePhase(ObjectFilesPhase, ObjectFilesPhaseInput(manifestBitcodePath, manifestObjectPath))
    return manifestObjectPath
}
