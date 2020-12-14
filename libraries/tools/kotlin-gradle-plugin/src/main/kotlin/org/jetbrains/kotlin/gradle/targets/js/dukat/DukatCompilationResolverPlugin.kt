/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.internal.project.ProjectInternal
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

internal class DukatCompilationResolverPlugin(
    private val resolver: KotlinCompilationNpmResolver
) : CompilationResolverPlugin {
    val project get() = resolver.project
    val nodeJs get() = resolver.nodeJs
    val npmProject get() = resolver.npmProject
    val compilation get() = npmProject.compilation
    val integratedTaskName = npmProject.compilation.disambiguateName("generateExternalsIntegrated")
    val separateTaskName = npmProject.compilation.disambiguateName("generateExternals")

    init {
        val externalsOutputFormat = compilation.externalsOutputFormat

        gradleModelPostProcess(externalsOutputFormat, npmProject)

        val integratedTask = project.registerTask<IntegratedDukatTask>(
            integratedTaskName,
            listOf(compilation)
        ) {
            it.group = DUKAT_TASK_GROUP
            it.description = "Integrated generation Kotlin/JS external declarations for .d.ts files in ${compilation}"
            it.externalsOutputFormat = externalsOutputFormat
            it.dependsOn(nodeJs.npmInstallTaskProvider, npmProject.packageJsonTask)
        }

        val target = compilation.target

        val legacyTargetNotReuseIrTask =
            target is KotlinJsTarget && (target.irTarget == null || externalsOutputFormat != ExternalsOutputFormat.SOURCE)
        if (target is KotlinJsIrTarget || legacyTargetNotReuseIrTask) {
            compilation.compileKotlinTaskProvider.dependsOn(integratedTask)
        }

        if (target is KotlinJsIrTarget && target.legacyTarget != null) {
            target.legacyTarget?.compilations?.named(compilation.name) {
                if (it.externalsOutputFormat == ExternalsOutputFormat.SOURCE) {
                    it.compileKotlinTaskProvider.dependsOn(integratedTask)
                }
            }
        }

        project.registerTask<SeparateDukatTask>(
            separateTaskName,
            listOf(compilation)
        ) {
            it.group = DUKAT_TASK_GROUP
            it.description = "Generate Kotlin/JS external declarations for .d.ts files of all NPM dependencies in ${compilation}"
            it.dependsOn(nodeJs.npmInstallTaskProvider, npmProject.packageJsonTask)
        }
    }

    override fun hookDependencies(
        internalDependencies: Set<KotlinCompilationNpmResolver>,
        internalCompositeDependencies: Set<KotlinCompilationNpmResolver.CompositeDependency>,
        externalGradleDependencies: Set<KotlinCompilationNpmResolver.ExternalGradleDependency>,
        externalNpmDependencies: Set<NpmDependency>,
        fileCollectionDependencies: Set<FileCollectionDependency>
    ) {
        if (nodeJs.experimental.discoverTypes) {
            // todo: discoverTypes
        }
    }

    fun executeDukatIfNeeded(
        packageJsonIsUpdated: Boolean,
        resolution: KotlinRootNpmResolution
    ) {
        val externalNpmDependencies = resolution[project.path][compilation].externalNpmDependencies

        val target = compilation.target
        val externalsOutputFormat = compilation.externalsOutputFormat
        val legacyTargetReuseIrTask =
            target is KotlinJsTarget && (target.irTarget != null && externalsOutputFormat == ExternalsOutputFormat.SOURCE)
        if (legacyTargetReuseIrTask) {
            return
        }

        DukatExecutor(
            nodeJs,
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
            npmProject.externalsDir
                .listFiles()
                ?.filter { it.isCompatibleArchive }
                ?.forEach {
                    project.dependencies.add(
                        compilation.compileDependencyConfigurationName,
                        project.files(it)
                    )
                }

        }
    }
}