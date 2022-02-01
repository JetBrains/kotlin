/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import groovy.lang.Closure
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePluginConvention
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

class KotlinMappedJvmCompilationFactory(
    target: KotlinJvmTarget
) : KotlinJvmCompilationFactory(target) {
    override fun create(name: String): KotlinJvmCompilation {
        val module = target.project.kpmModules.maybeCreate(name)
        val variant = module.fragments.create(target.name, KotlinJvmVariant::class.java)

        return KotlinJvmCompilation(
            VariantMappedCompilationDetailsWithRuntime(variant, target),
        )
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
                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                    .compilationDetails.let { it as VariantMappedCompilationDetails<*> }
                    .variant.let { it as KotlinJvmVariant }
                    .outputsJarTaskName
            )
        )
}