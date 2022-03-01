/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.filterModuleName
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

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
    val baseName = project.archivesName
        ?: project.name
    val suffix = if (containingModule.moduleClassifier == null) "" else "_${containingModule.moduleClassifier}"
    return filterModuleName("$baseName$suffix")
}

internal class KotlinMappedJvmCompilationFactory(
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