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
import org.jetbrains.kotlin.gradle.utils.*
import java.io.Serializable
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNpmResolutionManager as wasmKotlinNpmResolutionManager

/**
 * See [KotlinNpmResolutionManager] for details about resolution process.
 */
class KotlinCompilationNpmResolver(
    val projectResolver: KotlinProjectNpmResolver,
    val compilation: KotlinJsIrCompilation,
) : Serializable {
    var rootResolver: KotlinRootNpmResolver = projectResolver.resolver

    val npmProject: NpmProject = compilation.npmProject

    val compilationDisambiguatedName: String = compilation.disambiguatedName

    val npmVersion: String by lazy {
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
        val nodeJsRoot = compilation.webTargetVariant(
            { project.rootProject.kotlinNodeJsRootExtension },
            { project.rootProject.wasmKotlinNodeJsRootExtension },
        )

        publicPackageJsonTaskHolder = project.registerTask<PublicPackageJsonTask>(
            npmProject.publicPackageJsonTaskName
        ) {
            it.dependsOn(packageJsonTaskHolder)

            it.compilationDisambiguatedName.set(compilation.disambiguatedName)
            it.packageJsonHandlers.set(compilation.packageJsonHandlers)

            it.npmResolutionManager.value(npmResolutionManager)
                .disallowChanges()

            it.jsIrCompilation.set(true)
            it.npmProjectName.set(npmProject.name)
            it.npmProjectMain.set(npmProject.main)
            it.extension.set(compilation.fileExtension)
        }

        project.dependencies.attributesSchema {
            it.attribute(publicPackageJsonAttribute)
        }

        nodeJsRoot.packageJsonUmbrellaTaskProvider.configure {
            it.dependsOn(publicPackageJsonTaskHolder)
        }

        if (compilation.isMain()) {
            project.tasks
                .withType(Zip::class.java)
                .matching { it.name == npmProject.target.artifactsTaskName }
                .configureEach {
                    it.dependsOn(publicPackageJsonTaskHolder)
                }

            val publicPackageJsonConfiguration = createPublicPackageJsonConfiguration()

            project.artifacts.add(publicPackageJsonConfiguration.name, publicPackageJsonTaskHolder.map { it.packageJsonFile }) {
                it.builtBy(publicPackageJsonTaskHolder)
            }
        }
    }

    override fun toString(): String = "KotlinCompilationNpmResolver(${npmProject.name})"

    val aggregatedConfiguration: Configuration = createAggregatedConfiguration()

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
        val all = project.configurations.createResolvable(compilation.npmAggregatedConfigurationName)

        all.usesPlatformOf(target)
        all.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
        all.attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        all.attributes.setAttribute(publicPackageJsonAttribute, PUBLIC_PACKAGE_JSON_ATTR_VALUE)
        all.isVisible = false
        all.description = "NPM configuration for $compilation."

        KotlinDependencyScope.values().forEach { scope ->
            val compilationConfiguration = project.compilationDependencyConfigurationByScope(
                compilation,
                scope
            )
            all.extendsFrom(compilationConfiguration)
            compilation.allKotlinSourceSets.forEach { sourceSet ->
                val sourceSetConfiguration = project.configurations.sourceSetDependencyConfigurationByScope(sourceSet, scope)
                all.extendsFrom(sourceSetConfiguration)
            }
        }

        return all
    }

    private fun createPublicPackageJsonConfiguration(): Configuration {
        val all = project.configurations.createConsumable(compilation.publicPackageJsonConfigurationName)

        all.usesPlatformOf(target)
        all.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
        all.attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        all.attributes.setAttribute(publicPackageJsonAttribute, PUBLIC_PACKAGE_JSON_ATTR_VALUE)
        all.isVisible = false

        return all
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
            artifacts: Set<ResolvedArtifact>,
        ) {
            artifacts.forEach { visitArtifact(dependency, it) }
        }

        private fun visitArtifact(
            dependency: ResolvedDependency,
            artifact: ResolvedArtifact,
        ) {
            val artifactId = artifact.id
            val componentIdentifier = artifactId.componentIdentifier

            // TODO update
            @Suppress("DEPRECATION")
            val isComposeProjectDep = artifactId `is` CompositeProjectComponentArtifactMetadata

            if (isComposeProjectDep) {
                visitCompositeProjectDependency(dependency, componentIdentifier as ProjectComponentIdentifier)
            }

            if (componentIdentifier is ProjectComponentIdentifier && !isComposeProjectDep) {
                visitProjectDependency(componentIdentifier)
                return
            }

            externalGradleDependencies.add(ExternalGradleDependency(dependency, artifact))
        }

        private fun visitCompositeProjectDependency(
            dependency: ResolvedDependency,
            componentIdentifier: ProjectComponentIdentifier,
        ) {
            componentIdentifier as DefaultProjectComponentIdentifier
            val includedBuild = project.gradle.includedBuild(componentIdentifier.identityPath.topRealPath().name!!)
            internalCompositeDependencies.add(
                CompositeDependency(dependency.moduleName, dependency.moduleVersion, includedBuild.projectDir, includedBuild)
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

        fun toPackageJsonProducer(): KotlinCompilationNpmResolution =
            KotlinCompilationNpmResolution(
                internalDependencies = internalDependencies,
                internalCompositeDependencies = internalCompositeDependencies,
                externalGradleDependencies = externalGradleDependencies.map {
                    FileExternalGradleDependency(
                        dependencyName = it.dependency.moduleName,
                        dependencyVersion = it.dependency.moduleVersion,
                        file = it.artifact.file,
                    )
                },
                externalNpmDependencies = externalNpmDependencies,
                fileCollectionDependencies = fileCollectionDependencies,
                projectPath = projectPath,
                compilationDisambiguatedName = compilationDisambiguatedName,
                npmProjectName = compilation.outputModuleName.get(),
                npmProjectVersion = npmVersion,
                tasksRequirements = rootResolver.tasksRequirements,
            )
    }

    companion object {
        val publicPackageJsonAttribute = Attribute.of(
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
