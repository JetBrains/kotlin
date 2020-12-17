/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
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
import org.jetbrains.kotlin.gradle.plugin.sources.compilationDependencyConfigurationByScope
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
    @Transient
    val projectResolver: KotlinProjectNpmResolver,
    @Transient
    val compilation: KotlinJsCompilation
) {
    @Transient
    val resolver = projectResolver.resolver

    private val gradleNodeModules by lazy {
        resolver.gradleNodeModules
    }

    private val compositeNodeModules by lazy {
        resolver.compositeNodeModules
    }

    val npmProject = compilation.npmProject

    val compilationName = compilation.name

    val npmName by lazy {
        npmProject.name
    }

    val npmVersion by lazy {
        project.version.toString()
    }

    val npmMain by lazy {
        npmProject.main
    }

    val prePackageJsonFile by lazy {
        npmProject.prePackageJsonFile
    }

    val nodeJs get() = resolver.nodeJs

    val taskRequirements by lazy {
        nodeJs.taskRequirements
    }

    val target get() = compilation.target

    val project get() = target.project

    @Transient
    val packageJsonTaskHolder: TaskProvider<KotlinPackageJsonTask>? =
        KotlinPackageJsonTask.create(compilation)

    @Transient
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

    @Transient
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
        if (packageJsonTaskHolder == null || packageJsonTaskHolder.get().state.upToDate) return resolve(skipWriting = true)
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

        KotlinDependencyScope.values().forEach { scope ->
            val compilationConfiguration = project.compilationDependencyConfigurationByScope(
                compilation,
                scope
            )
            all.extendsFrom(compilationConfiguration)
            compilation.allKotlinSourceSets.forEach { sourceSet ->
                val sourceSetConfiguration = project.sourceSetDependencyConfigurationByScope(sourceSet, scope)
                all.extendsFrom(sourceSetConfiguration)
            }
        }

        // We don't have `kotlin-js-test-runner` in NPM yet
        all.dependencies.add(nodeJs.versions.kotlinJsTestRunner.createDependency(project))

        npmProject.externalsDir
            .listFiles()
            ?.filter { it.isCompatibleArchive }
            ?.forEach {
                project.dependencies.add(
                    all.name,
                    project.files(it)
                )
            }

        return all
    }

    data class ExternalGradleDependency(
        val dependency: ResolvedDependency,
        val artifact: ResolvedArtifact
    )

    data class FileExternalGradleDependency(
        val dependencyName: String,
        val dependencyVersion: String,
        val file: File
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
        private val fileCollectionDependencies = mutableSetOf<FileCollectionDependency>()

        fun visit(configuration: Configuration) {
            configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                visitDependency(it)
            }

            configuration.allDependencies.forEach { dependency ->
                when (dependency) {
                    is NpmDependency -> externalNpmDependencies.add(dependency)
                    is FileCollectionDependency -> fileCollectionDependencies.add(dependency)
                }
            }

            //TODO: rewrite when we get general way to have inter compilation dependencies
            if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
                val main = compilation.target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJsCompilation
                internalDependencies.add(projectResolver[main])
            }

            val hasPublicNpmDependencies = externalNpmDependencies.isNotEmpty()

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
                    externalNpmDependencies,
                    fileCollectionDependencies
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
            externalNpmDependencies,
            fileCollectionDependencies
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
        val externalDependencies: Collection<String>,

        @get:Input
        val fileCollectionDependencies: Collection<File>
    ) : Serializable

    @Suppress("MemberVisibilityCanBePrivate")
    inner class PackageJsonProducer(
        val internalDependencies: Collection<KotlinCompilationNpmResolver>,
        val internalCompositeDependencies: Collection<CompositeDependency>,
        @Transient
        val externalGradleDependencies: Collection<ExternalGradleDependency>,
        @Transient
        val externalNpmDependencies: Collection<NpmDependency>,
        val fileCollectionDependencies: Collection<FileCollectionDependency>
    ) {
        val externalNpmDependencyDeclarations by lazy {
            externalNpmDependencies.map {
                it.toDeclaration()
            }
        }

        val fileExternalGradleDependencies by lazy {
            externalGradleDependencies.map {
                FileExternalGradleDependency(
                    it.dependency.moduleName,
                    it.dependency.moduleVersion,
                    it.artifact.file
                )
            }
        }

        val inputs: PackageJsonProducerInputs
            get() = PackageJsonProducerInputs(
                internalDependencies.map { it.npmProject.name },
                internalCompositeDependencies.flatMap { it.getPackages() },
                fileExternalGradleDependencies.map { it.file },
                externalNpmDependencyDeclarations.map { it.uniqueRepresentation() },
                fileCollectionDependencies.map { it.files }.flatMap { it.files }
            )

        fun createPackageJson(skipWriting: Boolean): KotlinCompilationNpmResolution {
            val resolvedInternalDependencies = internalDependencies.map {
                it.getResolutionOrResolveIfForced()
                    ?: error("Unresolved dependent npm package: ${this@KotlinCompilationNpmResolver} -> $it")
            }
            val importedExternalGradleDependencies = fileExternalGradleDependencies.mapNotNull {
                gradleNodeModules.get(it.dependencyName, it.dependencyVersion, it.file)
            } + fileCollectionDependencies.flatMap { dependency ->
                dependency.files
                    // Gradle can hash with FileHasher only files and only existed files
                    .filter { it.isFile }
                    .map { file ->
                        gradleNodeModules.get(
                            file.name,
                            dependency.version ?: "0.0.1",
                            file
                        )
                    }
            }.filterNotNull()

            val compositeDependencies = internalCompositeDependencies.flatMap { dependency ->
                dependency.getPackages()
                    .map { file ->
                        compositeNodeModules.get(
                            dependency.dependency.moduleName,
                            dependency.dependency.moduleVersion,
                            file
                        )
                    }
            }
                .filterNotNull()

            val toolsNpmDependencies = taskRequirements
                .getCompilationNpmRequirements(compilationName)

            val allNpmDependencies = externalNpmDependencyDeclarations + toolsNpmDependencies

            val packageJson = packageJson(
                npmName,
                npmVersion,
                npmMain,
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

//            compilation.packageJsonHandlers.forEach {
//                it(packageJson)
//            }

            if (!skipWriting) {
                packageJson.saveTo(prePackageJsonFile)
            }

            return KotlinCompilationNpmResolution(
                if (compilation != null) project else null,
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