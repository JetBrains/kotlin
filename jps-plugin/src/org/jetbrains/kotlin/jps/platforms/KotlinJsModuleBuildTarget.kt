/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

import com.intellij.util.containers.MultiMap
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.jps.JpsKotlinCompilerSettings
import org.jetbrains.kotlin.jps.productionOutputFilePath
import org.jetbrains.kotlin.jps.testOutputFilePath
import org.jetbrains.kotlin.utils.JsLibraryUtils
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils.JS_EXT
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils.META_JS_SUFFIX
import java.io.File
import java.net.URI

class KotlinJsModuleBuildTarget(jpsModuleBuildTarget: ModuleBuildTarget) : KotlinModuleBuilderTarget(jpsModuleBuildTarget) {
    val jsCompilerArguments
        get() = JpsKotlinCompilerSettings.getK2JsCompilerArguments(module)

    val outputFile
        get() = explicitOutputPath?.let { File(it) } ?: implicitOutputFile

    val explicitOutputPath
        get() = if (isTests) module.testOutputFilePath else module.productionOutputFilePath

    val implicitOutputFile: File
        get() {
            val suffix = if (isTests) "_test" else ""

            return File(outputDir, module.name + suffix + JS_EXT)
        }

    val outputFileBaseName: String
        get() = outputFile.path.substringBeforeLast(".")

    val outputMetaFile: File
        get() = File(outputFileBaseName + META_JS_SUFFIX)

    val libraryFiles: List<String>
        get() = mutableListOf<String>().also { result ->
            for (library in allDependencies.libraries) {
                for (root in library.getRoots(JpsOrderRootType.COMPILED)) {
                    result.add(JpsPathUtil.urlToPath(root.url))
                }
            }
        }

    val dependenciesMetaFiles: List<String>
        get() = mutableListOf<String>().also { result ->
            allDependencies.processModules { module ->
                if (isTests) addDependencyMetaFile(module, result, isTests = true)

                // production targets should be also added as dependency to test targets
                addDependencyMetaFile(module, result, isTests = false)
            }
        }

    private fun addDependencyMetaFile(
        module: JpsModule,
        result: MutableList<String>,
        isTests: Boolean
    ) {
        val dependencyBuildTarget = ModuleBuildTarget(module, isTests).kotlinData

        if (dependencyBuildTarget != this@KotlinJsModuleBuildTarget &&
            dependencyBuildTarget is KotlinJsModuleBuildTarget &&
            dependencyBuildTarget.sources.isNotEmpty()
        ) {
            val metaFile = dependencyBuildTarget.outputMetaFile
            if (metaFile.exists()) {
                result.add(metaFile.absolutePath)
            }
        }
    }

    override fun compileModuleChunk(
        allCompiledFiles: MutableSet<File>,
        chunk: ModuleChunk,
        commonArguments: CommonCompilerArguments,
        context: CompileContext,
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        environment: JpsCompilerEnvironment,
        filesToCompile: MultiMap<ModuleBuildTarget, File>
    ): Boolean {
        require(chunk.representativeTarget() == jpsModuleBuildTarget)
        if (reportAndSkipCircular(chunk, environment)) return false

        val sources = sources
        if (sources.isEmpty()) return false

        // Compiler starts to produce path relative to base dirs in source maps if at least one statement is true:
        // 1) base dirs are specified;
        // 2) prefix is specified (i.e. non-empty)
        // Otherwise compiler produces paths relative to source maps location.
        // We don't have UI to configure base dirs, but we have UI to configure prefix.
        // If prefix is not specified (empty) in UI, we want to produce paths relative to source maps location
        val sourceRoots = if (jsCompilerArguments.sourceMapPrefix.isNullOrBlank()) {
            emptyList()
        } else {
            module.contentRootsList.urls
                .map { URI.create(it) }
                .filter { it.scheme == "file" }
                .map { File(it.path) }
        }

        val friendPaths = friendBuildTargets.mapNotNull {
            (it as? KotlinJsModuleBuildTarget)?.outputMetaFile?.absoluteFile?.toString()
        }

        val libraries = libraryFiles + dependenciesMetaFiles

        val compilerRunner = JpsKotlinCompilerRunner()
        compilerRunner.runK2JsCompiler(
            commonArguments,
            jsCompilerArguments,
            compilerSettings,
            environment,
            sources,
            sourceRoots,
            libraries,
            friendPaths,
            outputFile
        )

        return true
    }

    override fun doAfterBuild() {
        copyJsLibraryFilesIfNeeded()
    }

    private fun copyJsLibraryFilesIfNeeded() {
        if (compilerSettings.copyJsLibraryFiles) {
            val outputLibraryRuntimeDirectory = File(outputDir, compilerSettings.outputDirectoryForJsLibraryFiles).absolutePath
            JsLibraryUtils.copyJsFilesFromLibraries(
                libraryFiles, outputLibraryRuntimeDirectory,
                copySourceMap = jsCompilerArguments.sourceMap
            )
        }
    }
}