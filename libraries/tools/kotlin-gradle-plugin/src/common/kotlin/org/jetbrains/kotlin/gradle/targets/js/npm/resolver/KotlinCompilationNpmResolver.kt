/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.artifacts.DefaultProjectComponentIdentifier
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.Path
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.fileExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.compilationDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.currentBuild
import java.io.Serializable
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNpmResolutionManager as wasmKotlinNpmResolutionManager

/**
 * _This is an internal KGP utility and should not be used in user buildscripts._
 *
 * Extracts transitive npm dependencies from dependencies used by a Kotlin Compilation.
 *
 * KGP JS uses these dependencies to manually extract transitive npm dependencies.
 * The npm dependencies are not used directly, but instead
 * to configure task inputs for Gradle's up-to-date checks
 * and manually configuring task dependencies.
 *
 * See [KotlinNpmResolutionManager] for details about resolution process.
 */
class KotlinCompilationNpmResolver(
    val projectResolver: KotlinProjectNpmResolver,
    val compilation: KotlinJsIrCompilation,
) : Serializable {
    var rootResolver = projectResolver.resolver

    val npmProject = compilation.npmProject

    val compilationDisambiguatedName = compilation.disambiguatedName

    val npmVersion by lazy {
        project.version.toString()
    }

    val target: KotlinJsIrTarget get() = compilation.target

    val project: Project get() = target.project

    val projectPath: String = project.path

    val packageJsonTaskHolder: TaskProvider<KotlinPackageJsonTask> =
        KotlinPackageJsonTask.create(compilation)

    val publicPackageJsonTaskHolder: TaskProvider<PublicPackageJsonTask>

    init {
        val npmResolutionManager = compilation.webTargetVariant(
            { project.kotlinNpmResolutionManager },
            { project.wasmKotlinNpmResolutionManager },
        )

        publicPackageJsonTaskHolder = project.registerTask<PublicPackageJsonTask>(
            npmProject.publicPackageJsonTaskName
        ) {
            it.dependsOn(packageJsonTaskHolder)

            it.compilationDisambiguatedName.set(compilation.disambiguatedName)
            it.packageJsonHandlers.set(compilation.packageJsonHandlers)

            it.npmResolutionManager.value(npmResolutionManager)
                .disallowChanges()

            @Suppress("DEPRECATION_ERROR")
            it.jsIrCompilation.set(true)
            it.npmProjectName.set(npmProject.name)
            it.npmProjectMain.set(npmProject.main)
            it.npmProjectTypes.set(npmProject.typesFileName)
            it.extension.set(compilation.fileExtension)
        }

        project.dependencies.attributesSchema {
            it.attribute(publicPackageJsonAttribute)
        }

        val nodeJsRoot = compilation.webTargetVariant(
            { project.rootProject.kotlinNodeJsRootExtension },
            { project.rootProject.wasmKotlinNodeJsRootExtension },
        )

        nodeJsRoot.packageJsonUmbrellaTaskProvider.configure {
            it.dependsOn(publicPackageJsonTaskHolder)
        }

        if (compilation.isMain()) {
            project.tasks
                .withType(Zip::class.java)
                .configureEach {
                    if (it.name == npmProject.target.artifactsTaskName) {
                        it.dependsOn(publicPackageJsonTaskHolder)
                    }
                }

            val publicPackageJsonConfiguration = createPublicPackageJsonConfiguration()

            target.project.artifacts.add(
                publicPackageJsonConfiguration.name,
                publicPackageJsonTaskHolder.map { it.packageJsonFile },
            )
        }
    }

    override fun toString(): String = "KotlinCompilationNpmResolver(${npmProject.name})"

    val aggregatedConfiguration: Configuration =
        createAggregatedConfiguration()

    private var _compilationNpmResolution: KotlinCompilationNpmResolution? = null

    val compilationNpmResolution: KotlinCompilationNpmResolution
        get() {
            return _compilationNpmResolution ?: run {
                val visitor = ConfigurationVisitor()
                visitor.visit(aggregatedConfiguration)
                visitor.toPackageJsonProducer()
            }.also {
                _compilationNpmResolution = it
            }
        }

    @Synchronized
    fun close(): KotlinCompilationNpmResolution? {
        return _compilationNpmResolution
    }

    private fun createAggregatedConfiguration(): Configuration {
        return project.configurations.createResolvable(compilation.npmAggregatedConfigurationName) {
            usesPlatformOf(target)
            attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            attributes.attribute(publicPackageJsonAttribute, PUBLIC_PACKAGE_JSON_ATTR_VALUE)
            @Suppress("DEPRECATION")
            isVisible = false
            description = "NPM configuration for $compilation."

            /**
             * [KotlinDependencyScope.COMPILE_ONLY_SCOPE] is not valid for non-JVM projects,
             * so it is not included here.
             * See [org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.IncorrectCompileOnlyDependenciesChecker].
             */
            val extendableScopes = setOf(
                KotlinDependencyScope.API_SCOPE,
                KotlinDependencyScope.IMPLEMENTATION_SCOPE,
                KotlinDependencyScope.RUNTIME_ONLY_SCOPE,
            )

            extendableScopes.forEach { scope ->
                val compilationConfiguration = project.compilationDependencyConfigurationByScope(
                    compilation,
                    scope
                )
                extendsFrom(compilationConfiguration)
                compilation.allKotlinSourceSets.forEach { sourceSet ->
                    val sourceSetConfiguration = project.configurations.sourceSetDependencyConfigurationByScope(sourceSet, scope)
                    extendsFrom(sourceSetConfiguration)
                }
            }
        }
    }

    private fun createPublicPackageJsonConfiguration(): Configuration {
        return project.configurations.createConsumable(compilation.publicPackageJsonConfigurationName) {
            usesPlatformOf(target)
            attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            attributes.attribute(publicPackageJsonAttribute, PUBLIC_PACKAGE_JSON_ATTR_VALUE)
            @Suppress("DEPRECATION")
            isVisible = false
        }
    }

    inner class ConfigurationVisitor {
        private val internalDependencies = mutableSetOf<InternalDependency>()
        private val internalCompositeDependencies = mutableSetOf<CompositeDependency>()
        private val externalGradleDependencies = mutableSetOf<ExternalGradleDependency>()
        private val externalNpmDependencies = mutableSetOf<NpmDependencyDeclaration>()
        private val fileCollectionDependencies = mutableSetOf<FileCollectionExternalGradleDependency>()

        private val visitedDependencies = mutableSetOf<ResolvedDependency>()

        fun visit(configuration: Configuration) {
            configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach {
                visitDependency(it)
            }

            configuration.allDependencies.forEach { dependency ->
                when (dependency) {
                    is NpmDependency -> externalNpmDependencies.add(dependency.toDeclaration())
                    is FileCollectionDependency -> fileCollectionDependencies.add(
                        FileCollectionExternalGradleDependency(
                            dependency.files.files,
                            dependency.version
                        )
                    )
                }
            }

            //TODO: rewrite when we get general way to have inter compilation dependencies
            if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
                val main = compilation.target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinJsIrCompilation
                internalDependencies.add(
                    InternalDependency(
                        projectResolver.projectPath,
                        main.disambiguatedName,
                        projectResolver[main].compilation.outputModuleName.get()
                    )
                )
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
        }

        private fun visitDependency(dependency: ResolvedDependency) {
            if (dependency in visitedDependencies) return
            visitedDependencies.add(dependency)
            visitArtifacts(dependency, dependency.moduleArtifacts)

            dependency.children.forEach {
                visitDependency(it)
            }
        }

        private fun visitArtifacts(
            dependency: ResolvedDependency,
            artifacts: MutableSet<ResolvedArtifact>,
        ) {
            artifacts.forEach { visitArtifact(dependency, it) }
        }

        private fun visitArtifact(
            dependency: ResolvedDependency,
            artifact: ResolvedArtifact,
        ) {
            val artifactId = artifact.id
            val componentIdentifier = artifactId.componentIdentifier

            if (
                componentIdentifier is ProjectComponentIdentifier
                &&
                componentIdentifier.isCompositeBuildId()
            ) {
                visitCompositeProjectDependency(componentIdentifier)
            }

            if (
                componentIdentifier is ProjectComponentIdentifier
                &&
                !componentIdentifier.isCompositeBuildId()
            ) {
                visitProjectDependency(componentIdentifier)
            } else {
                externalGradleDependencies.add(ExternalGradleDependency(dependency, artifact))
            }
        }

        private fun visitCompositeProjectDependency(
            identifier: ProjectComponentIdentifier,
        ) {
            require(identifier is DefaultProjectComponentIdentifier) {
                "Only DefaultProjectComponentIdentifier supported as composite dependency, got $identifier"
            }
            val includedBuild = project.gradle.includedBuild(identifier.identityPath.topRealPath().name!!)
            internalCompositeDependencies.add(
                CompositeDependency(
                    dependencyName = "", // deprecated, no longer used
                    dependencyVersion = "",  // deprecated, no longer used
                    includedBuildDir = includedBuild.projectDir,
                    includedBuild = includedBuild,
                )
            )
        }

        private fun visitProjectDependency(
            componentIdentifier: ProjectComponentIdentifier,
        ) {
            val dependentProject = project.findProject(componentIdentifier.projectPath)
                ?: error("Cannot find project ${componentIdentifier.projectPath}")

            rootResolver.findDependentResolver(project, dependentProject)
                ?.forEach { dependentResolver ->
                    internalDependencies.add(
                        InternalDependency(
                            dependentResolver.projectPath,
                            dependentResolver.compilationDisambiguatedName,
                            dependentResolver.compilation.outputModuleName.get()
                        )
                    )
                }
        }

        fun toPackageJsonProducer() = KotlinCompilationNpmResolution(
            internalDependencies = internalDependencies,
            internalCompositeDependencies = internalCompositeDependencies,
            externalGradleDependencies = externalGradleDependencies.map {
                FileExternalGradleDependency(
                    it.dependency.moduleName,
                    it.dependency.moduleVersion,
                    it.artifact.file
                )
            },
            externalNpmDependencies = externalNpmDependencies,
            fileCollectionDependencies = fileCollectionDependencies,
            projectPath = projectPath,
            compilationDisambiguatedName = compilationDisambiguatedName,
            npmProjectName = compilation.outputModuleName.get(),
            npmProjectVersion = npmVersion,
            tasksRequirements = rootResolver.tasksRequirements
        )
    }

    /**
     * Determine if a [ProjectComponentIdentifier] is a project dependency from an included composite build.
     */
    private fun ProjectComponentIdentifier.isCompositeBuildId(): Boolean =
        this !in project.currentBuild

    companion object {
        val publicPackageJsonAttribute: Attribute<String> = Attribute.of(
            "org.jetbrains.kotlin.js.public.package.json",
            String::class.java
        )

        const val PUBLIC_PACKAGE_JSON_ATTR_VALUE = "public-package-json"

        private tailrec fun Path.topRealPath(): Path {
            val parent = parent
            parent?.parent ?: return this
            return parent.topRealPath()
        }
    }
}
