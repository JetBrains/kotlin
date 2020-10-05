/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
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
        val dukatMode = compilation.dukatMode

        val integratedTask = project.registerTask<IntegratedDukatTask>(
            integratedTaskName,
            listOf(compilation)
        ) {
            it.group = DUKAT_TASK_GROUP
            it.description = "Integrated generation Kotlin/JS external declarations for .d.ts files in ${compilation}"
            it.dukatMode = dukatMode
            it.dependsOn(nodeJs.npmInstallTaskProvider, npmProject.packageJsonTask)
        }

        val target = compilation.target

        if (target is KotlinJsIrTarget || target is KotlinJsTarget && dukatMode != DukatMode.SOURCE) {
            compilation.compileKotlinTaskProvider.dependsOn(integratedTask)
        }

        if (target is KotlinJsIrTarget && target.legacyTarget != null) {
            target.legacyTarget?.compilations?.named(compilation.name) {
                if (it.dukatMode == DukatMode.SOURCE) {
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
        externalNpmDependencies: Set<NpmDependency>
    ) {
        if (nodeJs.experimental.discoverTypes) {
            // todo: discoverTypes
        }
    }

    fun executeDukatIfNeeded(
        packageJsonIsUpdated: Boolean,
        resolution: KotlinRootNpmResolution
    ) {
        val externalNpmDependencies = resolution[project][compilation].externalNpmDependencies

        DukatExecutor(
            nodeJs,
            DtsResolver(npmProject).getAllDts(externalNpmDependencies),
            compilation.dukatMode,
            npmProject,
            packageJsonIsUpdated,
            operation = compilation.name + " > " + DukatExecutor.OPERATION,
            compareInputs = true
        ).execute()
    }

    companion object {
        const val VERSION = "2"
    }
}