/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import kotlinx.cinterop.usingJvmCInteropCallbacks
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.createKonanLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.usingNativeMemoryAllocator
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.utils.addToStdlib.cast

class KonanDriver(val project: Project, val environment: KotlinCoreEnvironment, val configuration: CompilerConfiguration) {
    fun run() {
        val fileNames = configuration.get(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE)?.let { libPath ->
            if (configuration.get(KonanConfigKeys.MAKE_PER_FILE_CACHE) != true)
                null
            else {
                val lib = createKonanLibrary(File(libPath), "default", null, true)
                (0 until lib.fileCount()).map { fileIndex ->
                    val proto = IrFile.parseFrom(lib.file(fileIndex).codedInputStream, ExtensionRegistryLite.newInstance())
                    proto.fileEntry.name
                }
            }
        }

        if (fileNames == null) {
            KonanConfig(project, configuration).runTopLevelPhases()
        } else {
            fileNames.forEach { buildFileCache(it, CompilerOutputKind.PRELIMINARY_CACHE) }
            fileNames.forEach { buildFileCache(it, configuration.get(KonanConfigKeys.PRODUCE)!!) }
        }
    }

    private fun buildFileCache(fileName: String, cacheKind: CompilerOutputKind) {
        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)!!
        val subConfiguration = configuration.copy()
        subConfiguration.put(KonanConfigKeys.PRODUCE, cacheKind)
        subConfiguration.put(KonanConfigKeys.FILE_TO_CACHE, fileName)
        subConfiguration.put(KonanConfigKeys.MAKE_PER_FILE_CACHE, false)
        subConfiguration.put(CLIConfigurationKeys.PHASE_CONFIG, phaseConfig.toBuilder().build())
        KonanConfig(project, subConfiguration).runTopLevelPhases()
    }

    private fun KonanConfig.runTopLevelPhases() {
        try {
            ensureModuleName(this)
            runTopLevelPhases(this, environment)
        } finally {
            dispose()
        }
    }

    private fun ensureModuleName(config: KonanConfig) {
        if (environment.getSourceFiles().isEmpty()) {
            val libraries = config.resolvedLibraries.getFullList()
            val moduleName = config.moduleId
            if (libraries.any { it.uniqueName == moduleName }) {
                val kexeModuleName = "${moduleName}_kexe"
                config.configuration.put(KonanConfigKeys.MODULE_NAME, kexeModuleName)
                assert(libraries.none { it.uniqueName == kexeModuleName })
            }
        }
    }
}

private fun runTopLevelPhases(konanConfig: KonanConfig, environment: KotlinCoreEnvironment) {

    val config = konanConfig.configuration

    val targets = konanConfig.targetManager
    if (config.get(KonanConfigKeys.LIST_TARGETS) ?: false) {
        targets.list()
    }

    val context = Context(konanConfig)
    context.environment = environment
    context.phaseConfig.konanPhasesConfig(konanConfig) // TODO: Wrong place to call it

    if (konanConfig.infoArgsOnly) return

    if (!context.frontendPhase()) return

    usingNativeMemoryAllocator {
        usingJvmCInteropCallbacks {
            try {
                toplevelPhase.cast<CompilerPhase<Context, Unit, Unit>>().invokeToplevel(context.phaseConfig, context, Unit)
            } finally {
                context.disposeLlvm()
            }
        }
    }
}

// returns true if should generate code.
internal fun Context.frontendPhase(): Boolean {
    lateinit var analysisResult: AnalysisResult

    do {
        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(
                messageCollector,
                environment.configuration.languageVersionSettings,
                environment.configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )

        // Build AST and binding info.
        analyzerWithCompilerReport.analyzeAndReport(environment.getSourceFiles()) {
            TopDownAnalyzerFacadeForKonan.analyzeFiles(environment.getSourceFiles(), this)
        }
        if (analyzerWithCompilerReport.hasErrors()) {
            throw KonanCompilationException()
        }
        analysisResult = analyzerWithCompilerReport.analysisResult
        if (analysisResult is AnalysisResult.RetryWithAdditionalRoots) {
            environment.addKotlinSourceRoots(analysisResult.additionalKotlinRoots)
        }
    } while(analysisResult is AnalysisResult.RetryWithAdditionalRoots)

    moduleDescriptor = analysisResult.moduleDescriptor
    bindingContext = analysisResult.bindingContext

    return analysisResult.shouldGenerateCode
}
