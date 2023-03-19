/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.backend.konan.driver.DynamicCompilerDriver
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.createKonanLibrary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

// Extracted from KonanTarget class to avoid problems with kotlin-native-shared.
private val deprecatedTargets = setOf(
        KonanTarget.WATCHOS_X86,
        KonanTarget.IOS_ARM32,
        KonanTarget.LINUX_ARM32_HFP,
        KonanTarget.MINGW_X86,
        KonanTarget.LINUX_MIPS32,
        KonanTarget.LINUX_MIPSEL32,
        KonanTarget.WASM32
)

private const val DEPRECATION_LINK = "https://kotl.in/native-targets-tiers"

class KonanDriver(
        val project: Project,
        val environment: KotlinCoreEnvironment,
        val configuration: CompilerConfiguration,
        val spawnCompilation: (List<String>, CompilerConfiguration.() -> Unit) -> Unit
) {
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
        var konanConfig = KonanConfig(project, configuration)

        if (configuration.get(KonanConfigKeys.LIST_TARGETS) == true) {
            konanConfig.targetManager.list()
        }
        if (konanConfig.infoArgsOnly) return

        // Avoid showing warning twice in 2-phase compilation.
        if (konanConfig.produce != CompilerOutputKind.LIBRARY && konanConfig.target in deprecatedTargets) {
            configuration.report(CompilerMessageSeverity.STRONG_WARNING,
                    "target ${konanConfig.target} is deprecated and will be removed soon. See: $DEPRECATION_LINK")
        }

        ensureModuleName(konanConfig)

        val cacheBuilder = CacheBuilder(konanConfig, spawnCompilation)
        if (cacheBuilder.needToBuild()) {
            cacheBuilder.build()
            konanConfig = KonanConfig(project, configuration) // TODO: Just set freshly built caches.
        }

        DynamicCompilerDriver().run(konanConfig, environment)
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