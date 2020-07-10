/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.initialization.IncludedBuild
import org.gradle.api.internal.artifacts.DefaultProjectComponentIdentifier
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.CompilationResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinCompilationNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.CompositeProjectComponentArtifactMetadata
import org.jetbrains.kotlin.gradle.utils.`is`
import org.jetbrains.kotlin.gradle.utils.topRealPath
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

    val publicPackageJsonTaskHolder: TaskProvider<PublicPackageJsonTask> =
        project.registerTask<PublicPackageJsonTask>(
            npmProject.publicPackageJsonTaskName,
            listOf(compilation)
        ) {
            it.dependsOn(nodeJs.npmInstallTaskProvider)
            it.dependsOn(packageJsonTaskHolder)
        }.also { packageJsonTask ->
            if (compilation.isMain()) {
                project.tasks
                    .withType(Zip::class.java)
                    .named(npmProject.target.artifactsTaskName)
                    .configure {
                        it.dependsOn(packageJsonTask)
                    }
            }
        }

    val plugins: List<CompilationResolverPlugin> = projectResolver.resolver.plugins
        .flatMap {
            if (compilation.isMain()) {
                it.createCompilationResolverPlugins(this)
            } else {
                emptyList()
            }
        }

    override fun toString(): String = "KotlinCompilationNpmResolver(${npmProject.name})"

    private val aggregatedConfiguration: Configuration by lazy {
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
        if (packageJsonTaskHolder.get().state.upToDate) return resolve(skipWriting = true)
        if (resolver.forceFullResolve && resolution == null) {
            // need to force all NPM tasks to be configured in IDEA import
            project.tasks.implementing(RequiresNpmDependencies::class).all {}
            return resolve()
        }
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
        val all = project.configurations.create(compilation.disambiguateName("npm"))

        all.usesPlatformOf(target)
        all.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
        all.isVisible = false
        all.isCanBeConsumed = false
        all.isCanBeResolved = true
        all.description = "NPM configuration for $compilation."

        compilation.allKotlinSourceSets.forEach { sourceSet ->
            KotlinDependencyScope.values().forEach { scope ->
                val configuration = project.sourceSetDependencyConfigurationByScope(sourceSet, scope)
                all.extendsFrom(configuration)
            }
        }

        // We don't have `kotlin-js-test-runner` in NPM yet
        all.dependencies.add(nodeJs.versions.kotlinJsTestRunner.createDependency(project))

        return all
    }

    data class ExternalGradleDependency(
        val dependency: ResolvedDependency,
        val artifact: ResolvedArtifact
    )

    data class CompositeDependency(
        val dependency: ResolvedDependency,
        val includedBuild: IncludedBuild
    )

    inner class ConfigurationVisitor {
        private val internalDependencies = mutableSetOf<KotlinCompilationNpmResolver>()
        private val internalCompositeDependencies = mutableSetOf<CompositeDependency>()
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

            val hasPublicNpmDependencies = externalNpmDependencies
                .any { it.isPublicDependency() }

            if (compilation.isMain() && hasPublicNpmDependencies) {
                project.tasks
                    .withType(Zip::class.java)
                    .named(npmProject.target.artifactsTaskName)
                    .configure { task ->
                        task.from(publicPackageJsonTaskHolder)
                    }
            }

            plugins.forEach {
                it.hookDependencies(
                    internalDependencies,
                    internalCompositeDependencies,
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
            artifacts.forEach { visitArtifact(dependency, it) }
        }

        private fun visitArtifact(
            dependency: ResolvedDependency,
            artifact: ResolvedArtifact
        ) {
            val artifactId = artifact.id
            val componentIdentifier = artifactId.componentIdentifier

            if (artifactId `is` CompositeProjectComponentArtifactMetadata) {
                visitCompositeProjectDependency(dependency, componentIdentifier as ProjectComponentIdentifier)
                return
            }

            if (componentIdentifier is ProjectComponentIdentifier) {
                visitProjectDependency(componentIdentifier)
                return
            }

            externalGradleDependencies.add(ExternalGradleDependency(dependency, artifact))
        }

        private fun visitCompositeProjectDependency(
            dependency: ResolvedDependency,
            componentIdentifier: ProjectComponentIdentifier
        ) {
            check(target is KotlinJsIrTarget) {
                """
                Composite builds for Kotlin/JS are supported only for IR compiler.
                Use kotlin.js.compiler=ir in gradle.properties or
                js(IR) {
                ...
                }
                """.trimIndent()
            }

            (componentIdentifier as DefaultProjectComponentIdentifier).let { identifier ->
                val includedBuild = project.gradle.includedBuild(identifier.identityPath.topRealPath().name!!)
                internalCompositeDependencies.add(CompositeDependency(dependency, includedBuild))
            }
        }

        private fun visitProjectDependency(
            componentIdentifier: ProjectComponentIdentifier
        ) {
            val dependentProject = project.findProject(componentIdentifier.projectPath)
                ?: error("Cannot find project ${componentIdentifier.projectPath}")

            resolver.findDependentResolver(project, dependentProject)
                ?.forEach { dependentResolver ->
                    internalDependencies.add(dependentResolver)
                }
        }

        fun toPackageJsonProducer() = PackageJsonProducer(
            internalDependencies,
            internalCompositeDependencies,
            externalGradleDependencies,
            externalNpmDependencies
        )
    }

    class PackageJsonProducerInputs(
        @get:Input
        val internalDependencies: Collection<String>,

        @get:InputFiles
        val internalCompositeDependencies: Collection<File>,

        @get:InputFiles
        val externalGradleDependencies: Collection<File>,

        @get:Input
        val externalDependencies: Collection<String>
    ) : Serializable

    @Suppress("MemberVisibilityCanBePrivate")
    inner class PackageJsonProducer(
        val internalDependencies: Collection<KotlinCompilationNpmResolver>,
        val internalCompositeDependencies: Collection<CompositeDependency>,
        val externalGradleDependencies: Collection<ExternalGradleDependency>,
        val externalNpmDependencies: Collection<NpmDependency>
    ) {
        val inputs: PackageJsonProducerInputs
            get() = PackageJsonProducerInputs(
                internalDependencies.map { it.npmProject.name },
                internalCompositeDependencies.flatMap { it.getPackages() },
                externalGradleDependencies.map { it.artifact.file },
                externalNpmDependencies.map { it.uniqueRepresentation() }
            )

        fun createPackageJson(skipWriting: Boolean): KotlinCompilationNpmResolution {
            val resolvedInternalDependencies = internalDependencies.map {
                it.getResolutionOrResolveIfForced()
                    ?: error("Unresolved dependent npm package: ${this@KotlinCompilationNpmResolver} -> $it")
            }
            val importedExternalGradleDependencies = externalGradleDependencies.mapNotNull {
                resolver.gradleNodeModules.get(it.dependency, it.artifact.file)
            }

            val compositeDependencies = internalCompositeDependencies.flatMap { dependency ->
                dependency.getPackages()
                    .map { file ->
                        resolver.compositeNodeModules.get(
                            dependency.dependency,
                            file
                        )
                    }
            }
                .filterNotNull()

            val toolsNpmDependencies = nodeJs.taskRequirements
                .getCompilationNpmRequirements(compilation)

            val allNpmDependencies = externalNpmDependencies + toolsNpmDependencies

            val packageJson = packageJson(
                npmProject,
                allNpmDependencies
            )

            compositeDependencies.forEach {
                packageJson.dependencies[it.name] = it.version
            }

            resolvedInternalDependencies.forEach {
                packageJson.dependencies[it.packageJson.name] = it.packageJson.version
            }

            importedExternalGradleDependencies.forEach {
                packageJson.dependencies[it.name] = fileVersion(it.path)
            }

            compilation.packageJsonHandlers.forEach {
                it(packageJson)
            }

            if (!skipWriting) {
                packageJson.saveTo(npmProject.prePackageJsonFile)
            }

            return KotlinCompilationNpmResolution(
                project,
                npmProject,
                resolvedInternalDependencies,
                compositeDependencies,
                importedExternalGradleDependencies,
                allNpmDependencies,
                packageJson
            )
        }

        private fun CompositeDependency.getPackages(): List<File> {
            val packages = includedBuild
                .projectDir
                .resolve(nodeJs.projectPackagesDir.relativeTo(nodeJs.rootProject.rootDir))
            return packages
                .list()
                ?.map { packages.resolve(it) }
                ?.map { it.resolve(PACKAGE_JSON) }
                ?: emptyList()
        }
    }
}