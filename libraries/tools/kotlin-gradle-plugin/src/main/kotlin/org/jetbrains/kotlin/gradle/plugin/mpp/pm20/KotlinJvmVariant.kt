/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import groovy.lang.Closure
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.unambiguousNameInProject
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTargetConfigurator
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.project.model.refinesClosure
import java.util.concurrent.Callable

open class KotlinJvmVariant(
    containingModule: KotlinGradleModule,
    fragmentName: String,
    dependencyConfigurations: KotlinFragmentDependencyConfigurations,
    compileDependenciesConfiguration: Configuration,
    apiElementsConfiguration: Configuration,
    runtimeDependenciesConfiguration: Configuration,
    runtimeElementsConfiguration: Configuration
) : KotlinGradlePublishedVariantWithRuntime(
    containingModule = containingModule,
    fragmentName = fragmentName,
    dependencyConfigurations = dependencyConfigurations,
    compileDependencyConfiguration = compileDependenciesConfiguration,
    apiElementsConfiguration = apiElementsConfiguration,
    runtimeDependencyConfiguration = runtimeDependenciesConfiguration,
    runtimeElementsConfiguration = runtimeElementsConfiguration
) {
    override val compilationData: KotlinJvmVariantCompilationData by lazy { KotlinJvmVariantCompilationData(this) }

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm
}

class KotlinJvmVariantCompilationData(val variant: KotlinJvmVariant) : KotlinVariantCompilationDataInternal<KotlinJvmOptions> {
    override val owner: KotlinJvmVariant get() = variant

    // TODO pull out to the variant
    override val kotlinOptions: KotlinJvmOptions = KotlinJvmOptionsImpl()
}

internal fun KotlinGradleVariant.ownModuleName(): String {
    val project = containingModule.project
    val baseName = project.extensions.getByType(BasePluginExtension::class.java).archivesName.orNull
        ?: project.name
    val suffix = if (containingModule.moduleClassifier == null) "" else "_${containingModule.moduleClassifier}"
    return filterModuleName("$baseName$suffix")
}

open class KotlinVariantMappedJvmCompilation(
    override val target: KotlinJvmTarget,
    internal val variant: KotlinJvmVariant,
) : KotlinJvmCompilation(target, variant.containingModule.name) {
    // TODO: in the legacy model, it used to be a KotlinCompilationWithResources

    override val runtimeDependencyConfigurationName: String
        get() = variant.runtimeDependenciesConfiguration.name

    override var runtimeDependencyFiles: FileCollection
        get() = variant.runtimeDependencyFiles
        set(value) {
            variant.runtimeDependencyFiles = value
        }

    // TODO: compilation data implements compileDependencyFiles as a val, not var, fix this?
    override val compileDependencyConfigurationName: String
        get() = variant.compileDependenciesConfiguration.name

    override var compileDependencyFiles: FileCollection
        get() = compilationData.compileDependencyFiles
        set(value) {
            variant.compileDependencyFiles = value
        }

    private val compilationData: KotlinJvmVariantCompilationData
        get() = variant.compilationData

    override val kotlinOptions: KotlinJvmOptions = compilationData.kotlinOptions

    override val compilationName: String
        get() = compilationData.compilationPurpose

    // FIXME: mutating this set should not be allowed
    override val kotlinSourceSets: MutableSet<KotlinSourceSet>
        get() = super.kotlinSourceSets

    override val allKotlinSourceSets: Set<KotlinSourceSet>
        get() {
            val allMappedSourceSets = target.project.kotlinExtension.sourceSets
                .filterIsInstance<FragmentMappedKotlinSourceSet>()
                .associateBy { it.underlyingFragment }
            return variant.refinesClosure.mapNotNull { allMappedSourceSets[it] }.toSet()
        }

    override val defaultSourceSetName: String
        get() = variant.unambiguousNameInProject

    override fun source(sourceSet: KotlinSourceSet) {
        defaultSourceSet.dependsOn(sourceSet)
    }

    override fun associateWith(other: KotlinCompilation<*>) {
        throw UnsupportedOperationException("not supported in the mapped model")
    }

    override val associateWith: List<KotlinCompilation<*>> get() = emptyList()

    override fun getAttributes(): AttributeContainer {
        // TODO: not implemented?
        return HierarchyAttributeContainer(target.attributes)
    }

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) {
        variant.dependencies(configure)
    }

    override fun dependencies(configureClosure: Closure<Any?>) {
        variant.dependencies(configureClosure)
    }

    override val apiConfigurationName: String
        get() = variant.apiConfigurationName
    override val implementationConfigurationName: String
        get() = variant.implementationConfigurationName
    override val compileOnlyConfigurationName: String
        get() = variant.compileOnlyConfigurationName
    override val runtimeOnlyConfigurationName: String
        get() = variant.runtimeOnlyConfigurationName

    override val project: Project
        get() = variant.project

    override val output: KotlinCompilationOutput
        get() = variant.compilationOutputs

    override val compileKotlinTaskName: String
        get() = compilationData.compileKotlinTaskName

    override val compileAllTaskName: String
        get() = compilationData.compileAllTaskName

    override val moduleName: String
        get() = compilationData.moduleName
}

class KotlinMappedJvmCompilationFactory(
    target: KotlinJvmTarget
) : KotlinJvmCompilationFactory(target) {
    override fun create(name: String): KotlinVariantMappedJvmCompilation {
        val module = target.project.kpmModules.maybeCreate(name)
        val variant = module.fragments.create(target.name, KotlinJvmVariant::class.java)
        return KotlinVariantMappedJvmCompilation(target, variant)
    }
}

class KotlinMappedJvmTargetConfigurator : KotlinJvmTargetConfigurator() {
    override fun defineConfigurationsForTarget(target: KotlinJvmTarget) = Unit // done in KPM
    override fun configureCompilationDefaults(target: KotlinJvmTarget) {
        // everything else is done in KPM, but KPM doesn't have resources processing yet
        target.compilations.all { compilation ->
            configureResourceProcessing(
                compilation,
                target.project.files(Callable { compilation.allKotlinSourceSets.map { it.resources } })
            )
        }
    }

    override fun configureCompilations(target: KotlinJvmTarget) {
        target.compilations.create(KotlinCompilation.MAIN_COMPILATION_NAME)
        target.compilations.create(KotlinCompilation.TEST_COMPILATION_NAME)
    }

    override fun configureArchivesAndComponent(target: KotlinJvmTarget) = Unit // done in KPM

    override fun createArchiveTasks(target: KotlinJvmTarget): TaskProvider<out Zip> =
        checkNotNull(
            target.project.locateTask<Jar>(
                (target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) as KotlinVariantMappedJvmCompilation).variant.outputsJarTaskName
            )
        )
}
