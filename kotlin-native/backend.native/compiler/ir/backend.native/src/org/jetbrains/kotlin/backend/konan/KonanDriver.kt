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
import org.jetbrains.kotlin.backend.konan.driver.CompilerDriver
import org.jetbrains.kotlin.backend.konan.driver.DynamicCompilerDriver
import org.jetbrains.kotlin.backend.konan.driver.StaticCompilerDriver
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
                configuration.get(KonanConfigKeys.FILES_TO_CACHE)
            else {
                val lib = createKonanLibrary(File(libPath), "default", null, true)
                (0 until lib.fileCount()).map { fileIndex ->
                    val proto = IrFile.parseFrom(lib.file(fileIndex).codedInputStream, ExtensionRegistryLite.newInstance())
                    proto.fileEntry.name
                }
            }
        }
        if (fileNames != null) {
            configuration.put(KonanConfigKeys.MAKE_PER_FILE_CACHE, true)
            configuration.put(KonanConfigKeys.FILES_TO_CACHE, fileNames)
        }

        val config = KonanConfig(project, configuration)
        ensureModuleName(config)

        pickCompilerDriver()?.run(config, environment)
                ?: error("Kotlin/Native compiler does not support given configuration")
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

    private fun pickCompilerDriver(): CompilerDriver? = when {
        DynamicCompilerDriver.supportsConfig() -> DynamicCompilerDriver()
        StaticCompilerDriver.supportsConfig() -> StaticCompilerDriver()
        else -> null
    }
}
