/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.utilities

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.llvm.coverage.CoverageManager
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.impl.javaFile
import java.io.File
import kotlin.random.Random

internal fun createTempFiles(config: KonanConfig, cacheDeserializationStrategy: CacheDeserializationStrategy? = null): TempFiles {
    val pathToTempDir = config.configuration.get(KonanConfigKeys.TEMPORARY_FILES_DIR)?.let {
        val singleFileStrategy = cacheDeserializationStrategy as? CacheDeserializationStrategy.SingleFile
        if (singleFileStrategy == null)
            it
        else org.jetbrains.kotlin.konan.file.File(it, CacheSupport.cacheFileId(singleFileStrategy.fqName, singleFileStrategy.filePath)).path
    }
    return TempFiles(pathToTempDir)
}

/**
 * This component-based approach is not really type-safe. On the other hand,
 * we don't know what will be use-cases of the dynamic driver yet, so it is better
 * to have more flexibility. For example, we might want to invoke only a part of pipeline.
 * Thus, we can create a different [CompilationFiles] with only necessary components and different files' lifetime,
 * e.g. non-temporary bitcode.
 *
 * The main difference between [CompilationFiles] and [OutputFiles] is artifact vs name based approach:
 * In [OutputFiles] everything was built around implicit...
 *
 * Note that this is driver-only entity, so it should not leak into phases to avoid coupling.
 */
internal class CompilationFiles(val components: Set<Component>) {
    /**
     * Output-specific parts of file I/O
     */
    sealed interface Component {
        /**
         * Instructs Linker to set -install_name
         */
        class InstallName(val value: String) : Component

        /**
         * Specifies output file for the binary linker.
         */
        class LinkerOutput(val value: File) : Component

        class FrameworkDirectory(val value: File): Component

        /**
         * Files required for generation of C interface.
         */
        class CExport(
                val cppAdapter: File,
                val bitcodeAdapter: File,
                val header: File,
                val def: File?,
        ) : Component

        /**
         * Directories required for storing compiler caches
         */
        class CacheDirectories(
                // We build cache in some temporary directory to avoid collisions
                val tempCacheDirectory: File,
                val outputDirectory: File,
        ) : Component

        class CodeCoverageFiles(
                val outputFileName: String
        ) : Component

        /**
         * File name for debug info
         */
        class DebugInfo(
                val debugInfoFileName: String
        ) : Component

        /**
         * File to store result of IR compilation
         */
        class ModuleBitcode(
                val file: () -> File
        ) : Component

        class ModuleObjectFile(
                val file: () -> File
        ) : Component

        class CachesPreLinkResult(
                val file: () -> File
        ) : Component
    }

    inline fun <reified T: Component> getComponentOrNull(): T? = components.filterIsInstance<T>().singleOrNull()

    inline fun <reified T: Component> getComponent(): T =
            getComponentOrNull<T>()!!

}

internal fun createCompilationFiles(
        config: KonanConfig,
        temporaryFiles: TempFiles,
        outputName: String,
        outputFiles: OutputFiles,
): CompilationFiles {
    val components = mutableSetOf<CompilationFiles.Component>()
    if (config.produce.isNativeLibrary) {
        components += CompilationFiles.Component.CExport(
                cppAdapter = temporaryFiles.create("api", ".cpp").javaFile(),
                bitcodeAdapter = temporaryFiles.create("api", ".bc").javaFile(),
                header = File("${outputName}_api.h"),
                def = if (config.target.family == Family.MINGW) File("${outputName}.def") else null,
        )
    }
    if (config.produce.isCache) {
        val tempCacheDirectory = File(outputName + Random.nextLong().toString())
        val outputDirectory = File(outputName)
        components += CompilationFiles.Component.CacheDirectories(
                tempCacheDirectory = tempCacheDirectory,
                outputDirectory = outputDirectory
        )
        // TODO: What if not per file?
        components += CompilationFiles.Component.LinkerOutput(
                tempCacheDirectory.resolve(CachedLibraries.PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).resolve(outputFiles.cacheFileName)
        )
        if (config.produce == CompilerOutputKind.DYNAMIC_CACHE && config.target.family.isAppleFamily) {
            components += CompilationFiles.Component.InstallName(
                    outputDirectory.resolve(CachedLibraries.PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).absolutePath
            )
        }
    }
    if (config.produce == CompilerOutputKind.FRAMEWORK) {
        val frameworkDirectory = File(config.outputPath)
        val dylibName = frameworkDirectory.name.removeSuffix(".framework")
        val dylibRelativePath = when (config.target.family) {
            Family.IOS,
            Family.TVOS,
            Family.WATCHOS -> dylibName
            Family.OSX -> "Versions/A/$dylibName"
            else -> error(config.target)
        }
        components += CompilationFiles.Component.InstallName("@rpath/${frameworkDirectory.name}/${dylibRelativePath}")
        components += CompilationFiles.Component.LinkerOutput(frameworkDirectory.resolve(dylibRelativePath))
        components += CompilationFiles.Component.FrameworkDirectory(frameworkDirectory)
    }

    if (CoverageManager.isCoverageEnabled(config)) {
        val coverageOutputFileName: String = config.configuration.get(KonanConfigKeys.PROFRAW_PATH)
                ?.let { File(it).absolutePath }
                ?: "${config.outputPath}.profraw"
        components += CompilationFiles.Component.CodeCoverageFiles(coverageOutputFileName)
    }

    if (config.cacheSupport.preLinkCaches) {
        components += CompilationFiles.Component.CachesPreLinkResult {
            temporaryFiles.create("withStaticCaches", ".o").javaFile()
        }
    }
    components += CompilationFiles.Component.DebugInfo(outputFiles.mainFileName)

    when (config.produce) {
        CompilerOutputKind.PROGRAM,
        CompilerOutputKind.STATIC,
        CompilerOutputKind.DYNAMIC -> components += CompilationFiles.Component.LinkerOutput(File(config.outputPath))
        else -> {}
    }
    return CompilationFiles(components)
}

/**
 * Setup [CompilationFiles] for compilation of a single Kotlin IR module to bitcode or object file.
 */
internal fun createModuleCompilationFiles(
        config: KonanConfig,
        temporaryFiles: TempFiles,
        rootFiles: CompilationFiles,
        llvmModuleName: String
): CompilationFiles {
    val components = rootFiles.components.toMutableSet()

    if (config.produce.involvesBitcodeGeneration) {
        components += CompilationFiles.Component.ModuleBitcode {
            temporaryFiles.create(llvmModuleName, ".bc").javaFile()
        }

        components += CompilationFiles.Component.ModuleObjectFile {
            temporaryFiles.create(llvmModuleName, ".o").javaFile()
        }
    }
    return CompilationFiles(components)
}

internal fun createObjCExportCompilationFiles(
        temporaryFiles: TempFiles,
        outputFiles: OutputFiles,
): CompilationFiles {
    val components = mutableSetOf<CompilationFiles.Component>()
    components += CompilationFiles.Component.ModuleBitcode {
        temporaryFiles.create("objcexport", ".bc").javaFile()
    }
    components += CompilationFiles.Component.DebugInfo(outputFiles.mainFileName)
    return CompilationFiles(components)
}