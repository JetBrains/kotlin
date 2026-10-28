/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.BasicIrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.backend.konan.ir.BridgesPolicy
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.backend.konan.getFilesWithFqNames
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.util.concurrent.Executors
import java.util.concurrent.Future

private const val MAX_REPORTED_FACT_MISMATCHES = 128

/**
 * Computes the facts the files of a klib define without compiling it, so that they can be compared against
 * the ones stored by the previous compilation (see [CacheBuilderFactBased]).
 *
 * Only the defined facts are computed: the used ones are only known when compiling.
 */
interface KlibFactsGenerator {

    companion object {
        fun from(config: NativeSecondStageCompilationConfig): KlibFactsGenerator =
                KlibFactsGeneratorImpl(config.threadsCount, config.bridgesPolicy)
    }

    fun generateFactsFrom(kotlinLibrary: KotlinLibrary): FactsTracker
}

sealed interface FactVerificationResult {
    data object Success : FactVerificationResult
    data class Failure(val message: String) : FactVerificationResult
}

/**
 * Deserializes the IR of the klib alone, like `klib dump-ir`, and runs [FactsCollector] on its files,
 * as the compiler does before lowering.
 *
 * The dependencies of the klib are not loaded: the symbols they provide stay unbound and no fake override is built.
 * [bridgesPolicy] must be the one of the compilation the facts are compared against (see [Fact.VtableLayout]).
 */
class KlibFactsGeneratorImpl internal constructor(
        private val threadsCount: Int,
        private val bridgesPolicy: BridgesPolicy
) : KlibFactsGenerator {
    override fun generateFactsFrom(kotlinLibrary: KotlinLibrary): FactsTracker {
        val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)
        val linker = FactsIrLinker(symbolTable)
        val irModule = linker.deserializeFullModule(createFakeModuleDescriptor(kotlinLibrary), kotlinLibrary)
        linker.modulesWithReachableTopLevels.forEach(IrModuleDeserializer::deserializeReachableDeclarations)

        if (threadsCount == 1) {
            return FactsTrackerImpl().apply {
                irModule.files.forEach { irFile -> FactsCollector.collectDefsFrom(irFile, this, bridgesPolicy) }
            }
        }

        val executors = Executors.newFixedThreadPool(threadsCount)
        val futures: List<Future<FactsTrackerImpl>> = irModule.files.map { irFile ->
            executors.submit<FactsTrackerImpl> {
                FactsTrackerImpl().apply {
                    FactsCollector.collectDefsFrom(irFile, this, bridgesPolicy)
                }
            }
        }

        return try {
            val collectedDefs = futures.flatMap { it.get().definedFacts.entries }.associate { it.toPair() }
            FactsTrackerImpl(initialDefinedFacts = collectedDefs)
        } finally {
            executors.shutdownNow()
        }
    }

    private fun createFakeModuleDescriptor(library: KotlinLibrary): ModuleDescriptorImpl {
        val builtIns = object : KotlinBuiltIns(LockBasedStorageManager.NO_LOCKS) {}
        val moduleDescriptor = ModuleDescriptorImpl(
                moduleName = Name.special("<${library.uniqueName}>"),
                storageManager = LockBasedStorageManager.NO_LOCKS,
                builtIns = builtIns,
                capabilities = mapOf(KlibModuleOrigin.CAPABILITY to DeserializedKlibModuleOrigin(library)),
        )
        moduleDescriptor.setDependencies(moduleDescriptor)
        builtIns.builtInsModule = moduleDescriptor
        return moduleDescriptor
    }
}

/** A copy of `KlibToolIrLinker` from the klib tool. */
private class FactsIrLinker(symbolTable: SymbolTable) : KotlinIrLinker(symbolTable, errorCallback = { error(it) }) {
    override val irMangler: KotlinMangler.IrMangler = KonanManglerIr

    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
            linker = this,
            symbolTable = symbolTable,
            mangler = irMangler,
            friendModules = emptyMap(),
            partialLinkageSupport = PartialLinkageSupportForLinker.DISABLED,
    )

    override val returnUnboundSymbolsIfSignatureNotFound get() = true

    override fun createModuleDeserializer(
            moduleFragment: IrModuleFragment,
            klib: KotlinLibrary?,
            strategyResolver: (String) -> DeserializationStrategy,
    ): IrModuleDeserializer = FactsModuleDeserializer(
            moduleFragment = moduleFragment,
            klib = klib ?: error("Expecting kotlin library for $moduleFragment"),
            strategyResolver = strategyResolver,
    )

    override fun isBuiltInModule(module: IrModuleFragment) = false

    private inner class FactsModuleDeserializer(
            moduleFragment: IrModuleFragment,
            klib: KotlinLibrary,
            strategyResolver: (String) -> DeserializationStrategy,
    ) : BasicIrModuleDeserializer(
            linker = this,
            moduleFragment = moduleFragment,
            strategyResolver = strategyResolver,
            klib = klib,
            libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT,
            allowErrorNodes = true,
            deserializeTypeAliases = true,
    )
}

internal fun checkKlibFacts(
        kotlinLibrary: KotlinLibrary,
        compilerFacts: DefsAssociation,
        filesToCheck: Set<String>?,
        threadsCount: Int,
        bridgesPolicy: BridgesPolicy,
): FactVerificationResult {
    val libraryFiles = kotlinLibrary.getFilesWithFqNames().map { it.filePath }.toSet()
    val files = (filesToCheck?.let(libraryFiles::intersect) ?: libraryFiles).sorted()
    if (files.isEmpty()) return FactVerificationResult.Success

    val klibFacts = KlibFactsGeneratorImpl(threadsCount, bridgesPolicy)
            .generateFactsFrom(kotlinLibrary)
            .definedFacts
    val mismatches = mutableListOf<String>()
    var mismatchCount = 0

    fun addMismatch(message: String) {
        mismatchCount++
        if (mismatches.size < MAX_REPORTED_FACT_MISMATCHES) mismatches += message
    }

    for (file in files) {
        val expected = klibFacts[file].orEmpty()
        val actual = compilerFacts[file].orEmpty()
        (expected.keys - actual.keys).forEach { key ->
            addMismatch("Missing from compiler in $file: ${key.kind} ${key.representation ?: key.key.signatureHash} (klib=${expected.getValue(key)})")
        }
        (actual.keys - expected.keys).forEach { key ->
            addMismatch("Extra in compiler in $file: ${key.kind} ${key.representation ?: key.key.signatureHash} (compiler=${actual.getValue(key)})")
        }
        (expected.keys intersect actual.keys).filter { expected.getValue(it) != actual.getValue(it) }.forEach { key ->
            addMismatch("Different hash in $file: ${key.kind} ${key.representation ?: key.key.signatureHash} (klib=${expected.getValue(key)}, compiler=${actual.getValue(key)})")
        }
    }

    if (mismatchCount == 0) return FactVerificationResult.Success

    val message = buildString {
        appendLine("Klib facts mismatch for ${kotlinLibrary.uniqueName}:")
        mismatches.forEach { appendLine("  $it") }
        if (mismatchCount > mismatches.size) {
            appendLine("  ... ${mismatchCount - mismatches.size} more mismatches omitted")
        }
    }.trimEnd()

    return FactVerificationResult.Failure(message)
}
