/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.KAPT_SUBPLUGIN_ID
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.isIncludeCompileClasspath
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompilerArgumentsProvider
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.isParentOf
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

internal class KaptGenerateStubsConfig : BaseKotlinCompileConfig<KaptGenerateStubsTask> {

    constructor(
        compilation: KotlinCompilation<*>,
        kotlinTaskProvider: TaskProvider<KotlinCompile>,
        kaptClassesDir: File
    ) : super(KotlinCompilationInfo(compilation)) {
        configureFromExtension(project.extensions.getByType(KaptExtension::class.java))
        configureTask { task ->
            val kotlinCompileTask = kotlinTaskProvider.get()
            task.useModuleDetection.value(kotlinCompileTask.useModuleDetection).disallowChanges()
            @Suppress("DEPRECATION")
            task.moduleName.value(kotlinCompileTask.moduleName).disallowChanges()
            task.libraries.from({ kotlinCompileTask.libraries - project.files(kaptClassesDir) })
            task.compileTaskCompilerOptions.set(providers.provider { kotlinCompileTask.compilerOptions })
            task.pluginOptions.addAll(kotlinCompileTask.pluginOptions)
            task.compilerOptions.moduleName.convention(kotlinCompileTask.compilerOptions.moduleName)
            task.compilerOptions.freeCompilerArgs.convention(kotlinCompileTask.compilerOptions.freeCompilerArgs)
            // KotlinCompile will also have as input output from KaptGenerateStubTask and KaptTask
            // We are filtering them to avoid failed UP-TO-DATE checks
            val kaptJavaSourcesDir = Kapt3GradleSubplugin.getKaptGeneratedSourcesDir(
                project,
                compilation.compilationName
            )
            val kaptKotlinSourcesDir = Kapt3GradleSubplugin.getKaptGeneratedKotlinSourcesDir(
                project,
                compilation.compilationName
            )
            val destinationDirectory = task.destinationDirectory
            val stubsDir = task.stubsDir
            val kaptFilterSpec = KaptFilterSpec(destinationDirectory, stubsDir, kaptJavaSourcesDir, kaptKotlinSourcesDir)
            // FileTree filtering approach fails with configuration cache until Gradle 7.5 leading to failed UP-TO-DATE checks
            val kaptFilter = if (shouldUseFileTreeKaptFilter) {
                FileTreeKaptInputsFilter(kaptFilterSpec)
            } else {
                FileCollectionKaptInputsFilter(kaptFilterSpec)
            }
            task.source(
                kaptFilter.filtered(kotlinCompileTask.javaSources),
                kaptFilter.filtered(kotlinCompileTask.sources),
            )
        }
    }

    constructor(project: Project, ext: KotlinTopLevelExtension, kaptExtension: KaptExtension) : super(project, ext) {
        configureFromExtension(kaptExtension)
        configureTask { task ->
            task.compileTaskCompilerOptions.set(
                providers.provider { task.compilerOptions }
            )
        }
    }

    private fun configureFromExtension(kaptExtension: KaptExtension) {
        configureTask { task ->
            task.verbose.set(KaptTask.queryKaptVerboseProperty(project))
            task.pluginOptions.add(buildOptions(kaptExtension, task))

            if (!isIncludeCompileClasspath(kaptExtension)) {
                task.onlyIf {
                    !(it as KaptGenerateStubsTask).kaptClasspath.isEmpty
                }
            }
        }
    }

    private fun isIncludeCompileClasspath(kaptExtension: KaptExtension) =
        kaptExtension.includeCompileClasspath ?: project.isIncludeCompileClasspath()

    private fun buildOptions(kaptExtension: KaptExtension, task: KaptGenerateStubsTask): Provider<CompilerPluginOptions> {
        val javacOptions = project.provider { kaptExtension.getJavacOptions() }
        return project.provider {
            val compilerPluginOptions = CompilerPluginOptions()
            buildKaptSubpluginOptions(
                kaptExtension,
                project,
                javacOptions.get(),
                aptMode = "stubs",
                generatedSourcesDir = objectFactory.fileCollection().from(task.destinationDirectory.asFile),
                generatedClassesDir = objectFactory.fileCollection().from(task.destinationDirectory.asFile),
                incrementalDataDir = objectFactory.fileCollection().from(task.destinationDirectory.asFile),
                includeCompileClasspath = isIncludeCompileClasspath(kaptExtension),
                kaptStubsDir = objectFactory.fileCollection().from(task.stubsDir.asFile)
            ).forEach {
                compilerPluginOptions.addPluginArgument(KAPT_SUBPLUGIN_ID, it)
            }
            return@provider compilerPluginOptions
        }
    }

    private abstract class CachingKaptInputsFilter {
        private val filterCache = ConcurrentHashMap<File, Boolean>()
        abstract fun filtered(fileCollection: FileCollection): FileCollection

        protected fun isSatisfiedBy(file: File) = filterCache[file] ?: predicate(file).also { filterCache[file] = it }

        abstract fun predicate(file: File): Boolean
    }

    // Drop `isEmptyDirectory` check after min supported Gradle version will be bumped to 6.8
    // It will be covered by '@IgnoreEmptyDirectories' input annotation
    private class FileCollectionKaptInputsFilter(val spec: KaptFilterSpec) : CachingKaptInputsFilter() {
        override fun filtered(fileCollection: FileCollection): FileCollection {
            return fileCollection.filter(::isSatisfiedBy)
        }

        override fun predicate(file: File) = !file.isEmptyDirectory && spec.isSatisfiedBy(file)

        private val File.isEmptyDirectory: Boolean
            get() = with(toPath()) {
                Files.isDirectory(this) && !Files.list(this).use { it.findFirst().isPresent }
            }
    }

    private val shouldUseFileTreeKaptFilter
        get() = isGradleVersionAtLeast(7, 5) || !isConfigurationCacheAvailable(project.gradle)

    private class FileTreeKaptInputsFilter(val spec: KaptFilterSpec) : CachingKaptInputsFilter() {
        override fun filtered(fileCollection: FileCollection): FileCollection {
            return fileCollection.asFileTree.matching { it.include { elem -> isSatisfiedBy(elem.file) } }
        }

        override fun predicate(file: File) = spec.isSatisfiedBy(file)
    }

    private class KaptFilterSpec(
        private val destinationDirectory: DirectoryProperty,
        private val stubsDir: DirectoryProperty,
        private val kaptJavaSourcesDir: File,
        private val kaptKotlinSourcesDir: File,
    ) : Spec<File> {
        override fun isSatisfiedBy(element: File) = element.isSourceRootAllowed()

        private fun File.isSourceRootAllowed(): Boolean =
            !destinationDirectory.get().asFile.isParentOf(this) &&
                    !stubsDir.asFile.get().isParentOf(this) &&
                    !kaptJavaSourcesDir.isParentOf(this) &&
                    !kaptKotlinSourcesDir.isParentOf(this)
    }
}