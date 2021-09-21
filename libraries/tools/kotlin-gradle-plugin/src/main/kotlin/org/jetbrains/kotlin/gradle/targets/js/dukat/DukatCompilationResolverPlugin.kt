/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.isCompatibleArchive
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.CompilationResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.unavailableValueError

internal class DukatCompilationResolverPlugin(
    @Transient
    private val resolver: KotlinCompilationNpmResolver
) : CompilationResolverPlugin {
    val project get() = resolver.project
    val nodeJs get() = resolver.nodeJs
    private val nodeJs_ get() = nodeJs ?: unavailableValueError("nodeJs")
    val versions by lazy { nodeJs_.versions }
    val npmProject by lazy { resolver.npmProject }
    val compilation get() = npmProject.compilation
    val compilationName by lazy {
        compilation.disambiguatedName
    }
    val legacyTargetReuseIrTask by lazy {
        val target = compilation.target
        target is KotlinJsTarget && (target.irTarget != null && externalsOutputFormat == ExternalsOutputFormat.SOURCE)
    }
    val externalsOutputFormat by lazy {
        compilation.externalsOutputFormat
    }
    val integratedTaskName = npmProject.compilation.disambiguateName(GENERATE_EXTERNALS_INTEGRATED_TASK_SIMPLE_NAME)
    val separateTaskName = npmProject.compilation.disambiguateName(GENERATE_EXTERNALS_TASK_SIMPLE_NAME)

    private fun registerIntegratedTask(): TaskProvider<IntegratedDukatTask> {
        return project.registerTask(
            integratedTaskName,
            listOf(compilation)
        ) {
            it.group = DUKAT_TASK_GROUP
            it.description = "Integrated generation Kotlin/JS external declarations for .d.ts files in $compilation"
            it.externalsOutputFormat = externalsOutputFormat
            it.dependsOn(nodeJs_.npmInstallTaskProvider, npmProject.packageJsonTask)
        }
    }

    init {
        val externalsOutputFormat = compilation.externalsOutputFormat

        gradleModelPostProcess(externalsOutputFormat, npmProject)

        var integratedTask: TaskProvider<IntegratedDukatTask>? = null
        if (compilation.shouldDependOnDukatIntegrationTask()) {
            val task = integratedTask ?: registerIntegratedTask().also { integratedTask = it }
            compilation.compileKotlinTaskProvider.dependsOn(task)
        }

        if (compilation.shouldLegacyUseIrTargetDukatIntegrationTask()) {
            (compilation.target as KotlinJsIrTarget).legacyTarget?.compilations?.named(compilation.name) {
                val task = integratedTask ?: registerIntegratedTask()
                if (it.externalsOutputFormat == ExternalsOutputFormat.SOURCE) {
                    it.compileKotlinTaskProvider.dependsOn(task)
                }
            }
        }

        project.registerTask<SeparateDukatTask>(
            separateTaskName,
            listOf(compilation)
        ) {
            it.group = DUKAT_TASK_GROUP
            it.description = "Generate Kotlin/JS external declarations for .d.ts files of all NPM dependencies in ${compilation}"
            it.dependsOn(nodeJs_.npmInstallTaskProvider, npmProject.packageJsonTask)
        }
    }

    override fun hookDependencies(
        internalDependencies: Set<KotlinCompilationNpmResolver.InternalDependency>,
        internalCompositeDependencies: Set<KotlinCompilationNpmResolver.CompositeDependency>,
        externalGradleDependencies: Set<KotlinCompilationNpmResolver.ExternalGradleDependency>,
        externalNpmDependencies: Set<NpmDependency>,
        fileCollectionDependencies: Set<KotlinCompilationNpmResolver.FileCollectionExternalGradleDependency>
    ) {
        if (nodeJs_.experimental.discoverTypes) {
            // todo: discoverTypes
        }
    }

    fun executeDukatIfNeeded(
        packageJsonIsUpdated: Boolean,
        resolution: KotlinRootNpmResolution
    ) {
        val externalNpmDependencies = resolution[project.path][compilationName].externalNpmDependencies



        if (legacyTargetReuseIrTask) {
            return
        }

        DukatExecutor(
            versions,
            DtsResolver(npmProject).getAllDts(externalNpmDependencies),
            externalsOutputFormat,
            npmProject,
            packageJsonIsUpdated,
            operation = compilation.name + " > " + DukatExecutor.OPERATION,
            compareInputs = true
        ).execute((project as ProjectInternal).services)
    }

    companion object {
        const val VERSION = "3"
        internal const val GENERATE_EXTERNALS_INTEGRATED_TASK_SIMPLE_NAME = "generateExternalsIntegrated"
        internal const val GENERATE_EXTERNALS_TASK_SIMPLE_NAME = "generateExternals"

        internal fun KotlinJsCompilation.shouldDependOnDukatIntegrationTask(): Boolean = with(target) {
            this is KotlinJsIrTarget ||
                (this is KotlinJsTarget &&
                    (irTarget == null || externalsOutputFormat != ExternalsOutputFormat.SOURCE)
                    )
        }

        internal fun KotlinJsCompilation.shouldLegacyUseIrTargetDukatIntegrationTask(): Boolean =
            with(target) {
                this is KotlinJsIrTarget && legacyTarget != null
            }
    }
}

internal fun gradleModelPostProcess(
    externalsOutputFormat: ExternalsOutputFormat,
    npmProject: NpmProject
) {
    val compilation = npmProject.compilation
    val project = npmProject.project
    when (externalsOutputFormat) {
        ExternalsOutputFormat.SOURCE -> compilation.defaultSourceSet.kotlin.srcDir(npmProject.externalsDir)
        ExternalsOutputFormat.BINARY -> {
            project.dependencies.add(
                compilation.compileDependencyConfigurationName,
                project.fileTree(npmProject.externalsDir).include {
                    it.file.isCompatibleArchive
                }
            )
        }
    }
}