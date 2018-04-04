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
import org.jetbrains.jps.incremental.ProjectBuildException
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaClasspathKind
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compilerRunner.JpsCompilerEnvironment
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.jps.build.FSOperationsHelper
import org.jetbrains.kotlin.jps.build.KotlinSourceFileCollector
import org.jetbrains.kotlin.jps.model.kotlinFacet
import org.jetbrains.kotlin.jps.model.productionOutputFilePath
import org.jetbrains.kotlin.jps.model.testOutputFilePath
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

/**
 * Properties and actions for Kotlin test / production module build target.
 */
abstract class KotlinModuleBuilderTarget(val context: CompileContext, val jpsModuleBuildTarget: ModuleBuildTarget) {
    val sourceRootTypes: Collection<JpsModuleSourceRootType<*>>
        get() = if (isTests) {
            listOf(JavaSourceRootType.TEST_SOURCE, KotlinSourceRootType.TestSource)
        } else {
            listOf(JavaSourceRootType.SOURCE, KotlinSourceRootType.Source)
        }

    val module: JpsModule
        get() = jpsModuleBuildTarget.module

    val isTests: Boolean
        get() = jpsModuleBuildTarget.isTests

    val targetId: TargetId
        get() {
            // Since IDEA 2016 each gradle source root is imported as a separate module.
            // One gradle module X is imported as two JPS modules:
            // 1. X-production with one production target;
            // 2. X-test with one test target.
            // This breaks kotlin code since internal members' names are mangled using module name.
            // For example, a declaration of a function 'f' in 'X-production' becomes 'fXProduction', but a call 'f' in 'X-test' becomes 'fXTest()'.
            // The workaround is to replace a name of such test target with the name of corresponding production module.
            // See KT-11993.
            val name = relatedProductionModule?.name ?: jpsModuleBuildTarget.id
            return TargetId(name, jpsModuleBuildTarget.targetType.typeId)
        }

    val outputDir by lazy {
        val explicitOutputPath = if (isTests) module.testOutputFilePath else module.productionOutputFilePath
        val explicitOutputDir = explicitOutputPath?.let { File(it).absoluteFile.parentFile }
        return@lazy explicitOutputDir
                ?: jpsModuleBuildTarget.outputDir
                ?: throw ProjectBuildException("No output directory found for " + this)
    }

    val friendBuildTargets: List<KotlinModuleBuilderTarget>
        get() {
            val result = mutableListOf<KotlinModuleBuilderTarget>()

            if (isTests) {
                result.addIfNotNull(context.kotlinBuildTargets[module.productionBuildTarget])
                result.addIfNotNull(context.kotlinBuildTargets[relatedProductionModule?.productionBuildTarget])
            }

            return result.filter { it.sources.isNotEmpty() }
        }

    val friendOutputDirs: List<File>
        get() = friendBuildTargets.mapNotNull {
            JpsJavaExtensionService.getInstance().getOutputDirectory(it.module, false)
        }

    private val relatedProductionModule: JpsModule?
        get() = JpsJavaExtensionService.getInstance().getTestModuleProperties(module)?.productionModule

    val allDependencies by lazy {
        JpsJavaExtensionService.dependencies(module).recursively().exportedOnly()
            .includedIn(JpsJavaClasspathKind.compile(isTests))
    }

    val expectedBy by lazy(::findExpectedBy)

    private fun findExpectedBy(): List<KotlinModuleBuilderTarget> {
        val kotlinFacetExtension = module.kotlinFacet
        val implementedModuleNames = kotlinFacetExtension?.settings?.implementedModuleNames ?: return listOf()
        if (implementedModuleNames.isEmpty()) return listOf()

        return allDependencies.modules
            .filter { it.name in implementedModuleNames }
            .map { context.kotlinBuildTargets[ModuleBuildTarget(it, isTests)]!! }
    }

    val sources by lazy {
        mutableListOf<File>().also { result ->
            // add all common libs sources
            expectedBy.forEach {
                it.collectSources(result)
            }

            collectSources(result)
        }
    }

    private fun collectSources(receiver: MutableList<File>) {
        val moduleExcludes = module.excludeRootsList.urls.mapTo(java.util.HashSet(), JpsPathUtil::urlToFile)

        val compilerExcludes = JpsJavaExtensionService.getInstance()
            .getOrCreateCompilerConfiguration(module.project)
            .compilerExcludes

        module
            .sourceRoots
            .filter { it.rootType in sourceRootTypes }
            .forEach {
                it.file.walkTopDown()
                    .onEnter { it !in moduleExcludes }
                    .filterTo(receiver) {
                        !compilerExcludes.isExcluded(it) &&
                                it.isFile &&
                                KotlinSourceFileCollector.isKotlinSourceFile(it)
                    }
            }
    }

    override fun toString() = jpsModuleBuildTarget.toString()

    abstract fun compileModuleChunk(
        allCompiledFiles: MutableSet<File>,
        chunk: ModuleChunk,
        commonArguments: CommonCompilerArguments,
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        environment: JpsCompilerEnvironment,
        filesToCompile: MultiMap<ModuleBuildTarget, File>,
        fsOperations: FSOperationsHelper
    ): Boolean

    protected fun reportAndSkipCircular(
        chunk: ModuleChunk,
        environment: JpsCompilerEnvironment
    ): Boolean {
        if (chunk.modules.size > 1) {
            // We do not support circular dependencies, but if they are present, we do our best should not break the build,
            // so we simply yield a warning and report NOTHING_DONE
            environment.messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Circular dependencies are not supported. The following modules depend on each other: "
                        + chunk.modules.joinToString(", ") { it.name } + " "
                        + "Kotlin is not compiled for these modules"
            )

            return true
        }

        return false
    }

    open fun doAfterBuild() {
    }
}