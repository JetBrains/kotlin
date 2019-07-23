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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.dukat.DukatCompilationResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fixSemver
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency.Scope.*
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.CompilationResolverPlugin
import java.io.File
import java.io.Serializable

/**
 * See [KotlinNpmResolutionManager] for details about resolution process.
 */
internal class KotlinCompilationNpmResolver(
    val projectResolver: KotlinProjectNpmResolver,
    val compilation: KotlinJsCompilation
) {
    val resolver = projectResolver.resolver
    val npmProject = compilation.npmProject
    val nodeJs get() = resolver.nodeJs
    val target get() = compilation.target
    val project get() = target.project
    val packageJsonTaskHolder = KotlinPackageJsonTask.create(compilation)
    val plugins: List<CompilationResolverPlugin> = projectResolver.resolver.plugins.flatMap {
        it.createCompilationResolverPlugins(this)
    }

    override fun toString(): String = "KotlinCompilationNpmResolver(${npmProject.name})"

    val aggregatedConfiguration: Configuration by lazy {
        createAggregatedConfiguration()
    }

    val packageJsonProducer: PackageJsonProducer by lazy {
        val visitor = ConfigurationVisitor()
        visitor.visit(aggregatedConfiguration)
        visitor.toPackageJsonProducer()
    }

    private var closed = false
    private var resolution: KotlinCompilationNpmResolution? = null

    @Synchronized
    fun resolve(skipWriting: Boolean = false): KotlinCompilationNpmResolution {
        check(!closed) { "$this already closed" }
        check(resolution == null) { "$this already resolved" }

        return packageJsonProducer.createPackageJson(skipWriting).also {
            resolution = it
        }
    }

    @Synchronized
    fun getResolutionOrResolveIfForced(): KotlinCompilationNpmResolution? {
        if (resolution != null) return resolution
        if (packageJsonTaskHolder.doGetTask().state.upToDate) return resolve(skipWriting = true)
        if (resolver.forceFullResolve && resolution == null) return resolve()
        return null
    }

    @Synchronized
    fun close(): KotlinCompilationNpmResolution? {
        check(!closed) { "$this already closed" }
        val resolution = getResolutionOrResolveIfForced()
        closed = true
        return resolution
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
        val taskRequirements = projectResolver.taskRequirements.getTaskRequirements(compilation)
        if (taskRequirements.isEmpty()) return null

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

    data class ExternalGradleDependency(
        val dependency: ResolvedDependency,
        val artifact: ResolvedArtifact
    )

    inner class ConfigurationVisitor {
        private val internalDependencies = mutableSetOf<KotlinCompilationNpmResolver>()
        private val externalGradleDependencies = mutableSetOf<ExternalGradleDependency>()
        private val externalNpmDependencies = mutableSetOf<NpmDependency>()

        fun visit(configuration: Configuration) {
            configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                visitDependency(it)
            }

            configuration.allDependencies.forEach { dependency ->
                when (dependency) {
                    is NpmDependency -> externalNpmDependencies.add(dependency)
                }
            }

            //TODO: rewrite when we get general way to have inter compilation dependencies
            if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
                val main = compilation.target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJsCompilation
                internalDependencies.add(projectResolver[main])
            }

            plugins.forEach {
                it.hookDependencies(
                    internalDependencies,
                    externalGradleDependencies,
                    externalNpmDependencies
                )
            }
        }

        private fun visitDependency(dependency: ResolvedDependency) {
            visitArtifacts(dependency, dependency.moduleArtifacts)

            dependency.children.forEach {
                visitDependency(it)
            }
        }

        private fun visitArtifacts(
            dependency: ResolvedDependency,
            artifacts: MutableSet<ResolvedArtifact>
        ) {
            artifacts.forEach { artifact ->
                val componentIdentifier = artifact.id.componentIdentifier
                if (componentIdentifier is ProjectComponentIdentifier) {
                    val dependentProject = project.findProject(componentIdentifier.projectPath)
                        ?: error("Cannot find project ${componentIdentifier.projectPath}")

                    val dependentResolver = resolver.findDependentResolver(project, dependentProject)
                    if (dependentResolver != null) {
                        internalDependencies.add(dependentResolver)
                    }
                } else {
                    externalGradleDependencies.add(ExternalGradleDependency(dependency, artifact))
                }
            }
        }

        fun toPackageJsonProducer() = PackageJsonProducer(internalDependencies, externalGradleDependencies, externalNpmDependencies)
    }

    class PackageJsonProducerInputs(
        @get:Input
        val internalDependencies: Collection<String>,

        @get:InputFiles
        val externalGradleDependencies: Collection<File>,

        @get:Input
        val externalDependencies: Collection<String>
    ) : Serializable

    @Suppress("MemberVisibilityCanBePrivate")
    inner class PackageJsonProducer(
        val internalDependencies: Collection<KotlinCompilationNpmResolver>,
        val externalGradleDependencies: Collection<ExternalGradleDependency>,
        val externalNpmDependencies: Collection<NpmDependency>
    ) {
        val inputs: PackageJsonProducerInputs
            get() = PackageJsonProducerInputs(
                internalDependencies.map { it.npmProject.name },
                externalGradleDependencies.map { it.artifact.file },
                externalNpmDependencies.map { "${it.scope} ${it.key}:${it.version}" }
            )

        fun createPackageJson(skipWriting: Boolean): KotlinCompilationNpmResolution {
            val resolvedInternalDependencies = internalDependencies.map {
                it.getResolutionOrResolveIfForced()
                    ?: error("Unresolved dependent npm package: ${this@KotlinCompilationNpmResolver} -> $it")
            }
            val importedExternalGradleDependencies = externalGradleDependencies.mapNotNull {
                resolver.gradleNodeModules.get(it.dependency, it.artifact)
            }

            val packageJson = PackageJson(
                npmProject.name,
                fixSemver(project.version.toString())
            )

            packageJson.main = npmProject.main

            resolvedInternalDependencies.forEach {
                packageJson.dependencies[it.packageJson.name] = it.packageJson.version
            }

            importedExternalGradleDependencies.forEach {
                packageJson.dependencies[it.name] = it.version
            }

            externalNpmDependencies.forEach {
                when (it.scope) {
                    NORMAL -> packageJson.dependencies[it.key] = chooseVersion(packageJson.dependencies[it.key], it.version)
                    DEV -> packageJson.devDependencies[it.key] = chooseVersion(packageJson.devDependencies[it.key], it.version)
                    OPTIONAL -> packageJson.optionalDependencies[it.key] = chooseVersion(packageJson.optionalDependencies[it.key], it.version)
                    PEER -> packageJson.peerDependencies[it.key] = chooseVersion(packageJson.peerDependencies[it.key], it.version)
                }
            }

            compilation.packageJsonHandlers.forEach {
                it(packageJson)
            }

            if (!skipWriting) {
                packageJson.saveTo(npmProject.packageJsonFile)
            }

            return KotlinCompilationNpmResolution(
                project,
                npmProject,
                resolvedInternalDependencies,
                importedExternalGradleDependencies,
                externalNpmDependencies,
                packageJson
            )
        }

        private fun chooseVersion(oldVersion: String?, newVersion: String): String =
            oldVersion ?: newVersion // todo: real versions conflict resolution
    }
}