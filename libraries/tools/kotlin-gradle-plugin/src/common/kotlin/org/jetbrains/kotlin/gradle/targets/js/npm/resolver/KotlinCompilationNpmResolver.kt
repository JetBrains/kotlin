/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.fileExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.compilationDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinResolveDependenciesTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import java.io.Serializable

/**
 * See [KotlinNpmResolutionManager] for details about resolution process.
 */
internal class KotlinCompilationNpmResolver(
    projectResolver: KotlinProjectNpmResolver,
    val compilation: KotlinJsIrCompilation,
) : Serializable {
    var rootResolver = projectResolver.resolver

    val npmProject = compilation.npmProject

    val compilationDisambiguatedName = compilation.publicPackageJsonConfigurationName
    val packageJsonFileProvider = npmProject.publicPackageJsonFile

    val npmVersion by lazy {
        project.version.toString()
    }

    val target get() = compilation.target

    val project get() = target.project

    val projectPath: String = project.path
    val buildPath: String = project.currentBuildId().buildPathCompat

    val aggregatedConfiguration: Configuration = run {
        createAggregatedConfiguration()
    }

    internal val resolvedAggregatedConfiguration: LazyResolvedConfiguration =
        LazyResolvedConfiguration(aggregatedConfiguration)

    val externalNpmDependencies: DomainObjectSet<NpmDependency> = aggregatedConfiguration.allDependencies
        .withType(NpmDependency::class.java)


    override fun toString(): String = "KotlinCompilationNpmResolver(${npmProject.name})"

    private fun createAggregatedConfiguration(): Configuration {
        val all = project.configurations.createResolvable(compilation.npmAggregatedConfigurationName)

        all.usesPlatformOf(target)
        all.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
        all.attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        all.attributes.setAttribute(publicPackageJsonAttribute, PUBLIC_PACKAGE_JSON_ATTR_VALUE)
        all.isVisible = false
        all.description = "NPM configuration for $compilation."

        KotlinDependencyScope.values()
            .filter { it != KotlinDependencyScope.RUNTIME_ONLY_SCOPE }
            .forEach { scope ->
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

        // We don't have `kotlin-js-test-runner` in NPM yet
        all.dependencies.add(rootResolver.versions.kotlinJsTestRunner.createDependency(project))

        return all
    }

    val compilationNpmResolution: Provider<KotlinCompilationNpmResolution> = project.provider {
        val all = aggregatedConfiguration

        mutableSetOf<FileCollectionExternalGradleDependency>()

        val fileCollectionDependencies = all.allDependencies
            .withType<FileCollectionDependency>()
            .map {
                FileCollectionExternalGradleDependency(
                    it.files.files,
                    it.version
                )
            }
            .toSet()

        KotlinCompilationNpmResolution(
            buildPath,
            externalNpmDependencies.map { it.toDeclaration() }.toSet(),
            fileCollectionDependencies,
            projectPath,
            compilation.disambiguatedName,
            compilationDisambiguatedName,
            npmProject.name,
            npmVersion,
            rootResolver.tasksRequirements
        )
    }

    val resolveDependenciesTask: TaskProvider<KotlinResolveDependenciesTask> =
        KotlinResolveDependenciesTask.create(compilation, aggregatedConfiguration)

    val packageJsonTaskHolder: TaskProvider<KotlinPackageJsonTask> =
        KotlinPackageJsonTask.create(compilation, compilationDisambiguatedName, compilationNpmResolution).apply {
            this.configure {
                it.dependsOn(resolveDependenciesTask)
            }
        }

    val publicPackageJsonTaskHolder: TaskProvider<PublicPackageJsonTask> = run {
        val npmResolutionManager = project.kotlinNpmResolutionManager
        val nodeJsTaskProviders = project.rootProject.kotlinNodeJsExtension
        project.registerTask<PublicPackageJsonTask>(
            npmProject.publicPackageJsonTaskName
        ) {
            it.dependsOn(packageJsonTaskHolder)

            it.packageJson.set(packageJsonFileProvider)
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

            nodeJsTaskProviders.packageJsonUmbrellaTaskProvider.configure {
                it.dependsOn(packageJsonTask)
            }

            if (compilation.isMain()) {
                // wait for artifact task registering
                project.launch {
                    project.tasks
                        .withType(Zip::class.java)
                        .named(npmProject.target.artifactsTaskName) {
                            it.from(
                                compilationNpmResolution.zip(packageJsonTask.flatMap { it.packageJson }) { resolution, file ->
                                    if (resolution.npmDependencies.isNotEmpty()) {
                                        file
                                    } else emptySet<Any>()
                                }
                            )
                            it.dependsOn(packageJsonTask)
                        }

                    val publicPackageJsonConfiguration = createPublicPackageJsonConfiguration()

                    target.project.artifacts.add(publicPackageJsonConfiguration.name, packageJsonTask.flatMap { it.packageJson }) {
                        it.builtBy(packageJsonTask)
                    }
                }
            }
        }
    }

    @Synchronized
    fun close(): KotlinCompilationNpmResolution? {
        return compilationNpmResolution.get()
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

    companion object {
        val publicPackageJsonAttribute = Attribute.of(
            "org.jetbrains.kotlin.js.public.package.json",
            String::class.java
        )

        const val PUBLIC_PACKAGE_JSON_ATTR_VALUE = "public-package-json"
    }
}