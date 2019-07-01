/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.NpmProjectPackage

internal class KotlinCompilationNpmResolver(
    val projectResolver: KotlinProjectNpmResolver,
    val compilation: KotlinJsCompilation
) {
    val resolver = projectResolver.resolver
    val target get() = compilation.target
    val project get() = target.project

    val aggregatedConfiguration: Configuration by lazy {
        createAggregatedConfiguration()
    }

    val projectPackage by lazy {
        createPackageJson(aggregatedConfiguration)
    }

    private fun createAggregatedConfiguration(): Configuration {
        val all = project.configurations.create("${compilation.name}Npm")

        all.usesPlatformOf(target)
        all.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
        all.isVisible = false
        all.isCanBeConsumed = false
        all.isCanBeResolved = true
        all.description = "NPM configuration for $compilation."

        compilation.allKotlinSourceSets.forEach { sourceSet ->
            sourceSet.relatedConfigurationNames.forEach { configurationName ->
                val configuration = project.configurations.getByName(configurationName)
                all.extendsFrom(configuration)
            }
        }

        createNpmToolsConfiguration()?.let { tools ->
            all.extendsFrom(tools)
        }

        return all
    }

    private fun createNpmToolsConfiguration(): Configuration? {
        val taskRequirements = projectResolver.requiredFromTasksByCompilation[compilation]
            ?.takeIf { it.isNotEmpty() } ?: return null

        val toolsConfiguration = project.configurations.create("${compilation.name}NpmTools")

        toolsConfiguration.isVisible = false
        toolsConfiguration.isCanBeConsumed = false
        toolsConfiguration.isCanBeResolved = true
        toolsConfiguration.description = "NPM Tools configuration for $compilation."

        taskRequirements.forEach { requirement ->
            requirement.requiredNpmDependencies.forEach { requiredNpmDependency ->
                toolsConfiguration.dependencies.add(requiredNpmDependency.createDependency(project))
            }
        }

        return toolsConfiguration
    }

    private fun createPackageJson(configuration: Configuration): NpmProjectPackage {
        val npmProject = compilation.npmProject
        val name = npmProject.name
        val packageJson = PackageJson(
            name,
            fixSemver(project.version.toString())
        )
        val npmDependencies = mutableSetOf<NpmDependency>()
        val gradleDependencies = NpmGradleDependencies()

        packageJson.main = npmProject.main

        collectDependenciesFromConfiguration(configuration, gradleDependencies)

        configuration.allDependencies.forEach { dependency ->
            when (dependency) {
                is NpmDependency -> npmDependencies.add(dependency)
            }
        }

        npmDependencies.forEach {
            packageJson.dependencies[it.key] = chooseVersion(packageJson.dependencies[it.key], it.version)
        }

        gradleDependencies.externalModules.forEach {
            packageJson.dependencies[it.name] = it.version
        }

        gradleDependencies.internalModules.forEach { target ->
            val targetResolver = resolver.findDependentResolver(project, target)
            if (targetResolver != null) {
                val targetPackageJson = targetResolver.projectPackage.packageJson
                packageJson.dependencies[targetPackageJson.name] = targetPackageJson.version
            }
        }

        project.nodeJs.packageJsonHandlers.forEach {
            it(packageJson)
        }

        val npmPackage = NpmProjectPackage(
            project,
            npmProject,
            npmDependencies,
            gradleDependencies,
            packageJson
        )
        npmPackage.packageJson.saveTo(npmProject.packageJsonFile)

        return npmPackage
    }

    fun chooseVersion(oldVersion: String?, newVersion: String): String =
        oldVersion ?: newVersion // todo: real versions conflict resolution


    private fun collectDependenciesFromConfiguration(configuration: Configuration, result: NpmGradleDependencies) {
        if (configuration.isCanBeResolved) {
            configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                visitDependency(it, result)
            }
        }
    }

    private fun visitDependency(dependency: ResolvedDependency, result: NpmGradleDependencies) {
        visitArtifacts(dependency, dependency.moduleArtifacts, result)

        dependency.children.forEach {
            visitDependency(it, result)
        }
    }

    private fun visitArtifacts(
        dependency: ResolvedDependency,
        artifacts: MutableSet<ResolvedArtifact>,
        result: NpmGradleDependencies
    ) {
        artifacts.forEach { artifact ->
            val componentIdentifier = artifact.id.componentIdentifier
            if (componentIdentifier is ProjectComponentIdentifier) {
                val dependentProject = project.findProject(componentIdentifier.projectPath)
                    ?: error("Cannot find project ${componentIdentifier.projectPath}")

                result.internalModules.add(dependentProject)
            } else {
                resolver.gradleNodeModules.ensureImported(artifact, dependency, artifacts, result)
            }
        }
    }
}