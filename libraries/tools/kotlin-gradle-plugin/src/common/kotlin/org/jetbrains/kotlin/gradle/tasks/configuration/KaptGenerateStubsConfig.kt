/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.KAPT_SUBPLUGIN_ID
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.isIncludeCompileClasspath
import org.jetbrains.kotlin.gradle.internal.KotlinJvmCompilerArgumentsContributor
import org.jetbrains.kotlin.gradle.internal.buildKaptSubpluginOptions
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompilerArgumentsProvider
import org.jetbrains.kotlin.gradle.utils.isParentOf
import java.io.File
import java.nio.file.Files

internal class KaptGenerateStubsConfig : BaseKotlinCompileConfig<KaptGenerateStubsTask> {

    constructor(compilation: KotlinCompilationData<*>, kotlinTaskProvider: TaskProvider<KotlinCompile>) : super(compilation) {
        configureFromExtension(project.extensions.getByType(KaptExtension::class.java))
        configureTask { task ->
            val kotlinCompileTask = kotlinTaskProvider.get()
            task.useModuleDetection.value(kotlinCompileTask.useModuleDetection).disallowChanges()
            task.moduleName.value(kotlinCompileTask.moduleName).disallowChanges()
            task.libraries.from({ kotlinCompileTask.libraries })
            task.compileKotlinArgumentsContributor.set(providers.provider { kotlinCompileTask.compilerArgumentsContributor })
            task.pluginOptions.addAll(kotlinCompileTask.pluginOptions)
            // KotlinCompile will also have as input output from KaptGenerateStubTask and KaptTask
            // We are filtering them to avoid failed UP-TO-DATE checks
            val kaptJavaSourcesDir = Kapt3GradleSubplugin.getKaptGeneratedSourcesDir(
                project,
                compilation.compilationPurpose
            )
            val kaptKotlinSourcesDir = Kapt3GradleSubplugin.getKaptGeneratedKotlinSourcesDir(
                project,
                compilation.compilationPurpose
            )
            val destinationDirectory = task.destinationDirectory
            val stubsDir = task.stubsDir
            task.source(
                kotlinCompileTask
                    .javaSources
                    .filter(KaptFilterSpec(destinationDirectory, stubsDir, kaptJavaSourcesDir)),
                kotlinCompileTask
                    .sources
                    .filter(KaptFilterSpec(destinationDirectory, stubsDir, kaptKotlinSourcesDir))
            )
        }
    }

    constructor(project: Project, ext: KotlinTopLevelExtension, kaptExtension: KaptExtension) : super(project, ext) {
        configureFromExtension(kaptExtension)
        configureTask { task ->
            task.compileKotlinArgumentsContributor.set(
                providers.provider {
                    KotlinJvmCompilerArgumentsContributor(KotlinJvmCompilerArgumentsProvider(task))
                }
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

    private fun isIncludeCompileClasspath(kaptExtension: KaptExtension) = kaptExtension.includeCompileClasspath ?: project.isIncludeCompileClasspath()

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

    // Drop `isEmptyDirectory` check after min supported Gradle version will be bumped to 6.8
    // It will be covered by '@IgnoreEmptyDirectories' input annotation
    private class KaptFilterSpec(
        private val destinationDirectory: DirectoryProperty,
        private val stubsDir: DirectoryProperty,
        private val additionalParentToCheck: File
    ) : Spec<File> {
        override fun isSatisfiedBy(element: File): Boolean {
            return !element.isEmptyDirectory &&
                    element.isSourceRootAllowed()
        }

        private val File.isEmptyDirectory: Boolean
            get() = with(toPath()) { Files.isDirectory(this) && !Files.list(this).use { it.findFirst().isPresent } }

        private fun File.isSourceRootAllowed(): Boolean =
            !destinationDirectory.get().asFile.isParentOf(this) &&
                    !stubsDir.asFile.get().isParentOf(this) &&
                    !additionalParentToCheck.isParentOf(this)
    }
}