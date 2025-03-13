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
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
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
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.createResolvable
import org.jetbrains.kotlin.gradle.utils.setAttribute
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
    var rootResolver = projectResolver.resolver

    val npmProject = compilation.npmProject

    val compilationDisambiguatedName = compilation.disambiguatedName

    val npmVersion by lazy {
        project.version.toString()
    }

    val target get() = compilation.target

    val project get() = target.project

    val projectPath: String = project.path

    val packageJsonTaskHolder: TaskProvider<KotlinPackageJsonTask> =
        KotlinPackageJsonTask.create(compilation)

    val publicPackageJsonTaskHolder: TaskProvider<PublicPackageJsonTask> = run {
        val npmResolutionManager = compilation.webTargetVariant(
            { project.kotlinNpmResolutionManager },
            { project.wasmKotlinNpmResolutionManager },
        )

        project.registerTask<PublicPackageJsonTask>(
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
        }.also { packageJsonTask ->
            project.dependencies.attributesSchema {
                it.attribute(publicPackageJsonAttribute)
            }

            val nodeJsRoot = compilation.webTargetVariant(
                { project.rootProject.kotlinNodeJsRootExtension },
                { project.rootProject.wasmKotlinNodeJsRootExtension },
            )

            nodeJsRoot.packageJsonUmbrellaTaskProvider.configure {
                it.dependsOn(packageJsonTask)
            }

            if (compilation.isMain()) {
                project.tasks
                    .withType(Zip::class.java)
                    .configureEach {
                        if (it.name == npmProject.target.artifactsTaskName) {
                            it.dependsOn(packageJsonTask)
                        }
                    }

                val publicPackageJsonConfiguration = createPublicPackageJsonConfiguration()

                target.project.artifacts.add(
                    publicPackageJsonConfiguration.name,
                    packageJsonTask.map { it.packageJsonFile },
                )
            }
        }
    }

    override fun toString(): String = "KotlinCompilationNpmResolver(${npmProject.name})"

    val aggregatedConfiguration: Configuration = run {
        createAggregatedConfiguration()
    }

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

            /**
             * Check GAV of `dependantProject != componentIdentifier` to filter out
             * composite build projects with the same path as a local project, but different coords.
             *
             * We must verify that `dependentProject` is _not_ a local subproject.
             *
             * A local subproject could coincidentally have the same path that of an included build.
             * E.g. this build could have a subproject `:lib`, and the included build could _also_ have a `:lib` subproject.
             *
             * The only way Gradle can differentiate them is by comparing the GAV coordinates.
             *
             * If the GAV coords are different, then `dependentProject` and `dependency` cannot refer to the same project,
             * therefore `dependentProject` is a composite build.
             *
             * If the GAV coords are the same, then `dependentProject` and `dependency` could still refer to two separate projects if,
             * coincidentally, they both have the same `projectPath` _and_ GAV coordinates!
             * In this case, we cannot differentiate them. However, Gradle will not be able to differentiate them,
             * so the user will have a heck of a lot more problems than we can deal with.
             * So, we can just assume it's _not_ a composite build if the GAV coords are the same.
             */
            fun ProjectComponentIdentifier.isCompositeBuildDependency(): Boolean {
                val dependentProject = project.findProject(projectPath)
                    ?: return true // if there's no local project with the same path the identifier must be for an included build project.

                val dependencyCoords = dependency.run { "${moduleGroup}:${moduleName}:${moduleVersion}" }
                val projectCoords = dependentProject.run { "${group}:${name}:${version}" }

                return dependencyCoords != projectCoords
            }

            if (componentIdentifier is ProjectComponentIdentifier && !componentIdentifier.isCompositeBuildDependency()) {
                visitProjectDependency(componentIdentifier)
            } else {
                externalGradleDependencies.add(ExternalGradleDependency(dependency, artifact))
            }
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
            internalCompositeDependencies = emptyList(),
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

    companion object {
        val publicPackageJsonAttribute: Attribute<String> = Attribute.of(
            "org.jetbrains.kotlin.js.public.package.json",
            String::class.java
        )

        const val PUBLIC_PACKAGE_JSON_ATTR_VALUE = "public-package-json"
    }
}
