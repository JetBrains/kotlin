/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.CompilationResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

internal class DukatCompilationResolverPlugin(
    private val resolver: KotlinCompilationNpmResolver
) : CompilationResolverPlugin {
    val project get() = resolver.project
    val nodeJs get() = resolver.nodeJs
    val npmProject get() = resolver.npmProject
    val compilation get() = npmProject.compilation
    val taskName = npmProject.compilation.disambiguateName("generateExternals")

    init {
        compilation.defaultSourceSet.kotlin.srcDir(npmProject.externalsDir)

        val task = project.createOrRegisterTask<PackageJsonDukatTask>(taskName) {
            it.compilation = compilation
            it.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            it.description = "Generate Kotlin/JS external declarations for .d.ts files in ${compilation}"
            it.dependsOn(nodeJs.npmInstallTask, npmProject.packageJsonTask)
        }

        compilation.compileKotlinTask.dependsOn(task.getTaskOrProvider())
    }

    override fun hookDependencies(
        internalDependencies: MutableSet<KotlinCompilationNpmResolver>,
        externalGradleDependencies: MutableSet<KotlinCompilationNpmResolver.ExternalGradleDependency>,
        externalNpmDependencies: MutableSet<NpmDependency>
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

        PackageJsonDukatExecutor(
            nodeJs,
            DtsResolver(npmProject).getAllDts(externalNpmDependencies),
            npmProject,
            packageJsonIsUpdated,
            operation = compilation.name + " > " + PackageJsonDukatExecutor.OPERATION,
            compareInputs = true
        ).execute()
    }

    companion object {
        const val VERSION = "2"
    }
}