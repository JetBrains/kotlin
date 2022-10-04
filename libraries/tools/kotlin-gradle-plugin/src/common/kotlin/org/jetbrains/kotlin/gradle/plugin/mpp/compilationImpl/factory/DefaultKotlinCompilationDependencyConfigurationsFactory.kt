/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationDependencyConfigurationsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationDependencyConfigurationsContainer
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.*

internal sealed class DefaultKotlinCompilationDependencyConfigurationsFactory :
    KotlinCompilationImplFactory.KotlinCompilationDependencyConfigurationsFactory {

    object WithRuntime : DefaultKotlinCompilationDependencyConfigurationsFactory() {
        override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationDependencyConfigurationsContainer {
            return KotlinCompilationDependencyConfigurationsContainer(target, compilationName, withRuntime = true)
        }
    }

    object WithoutRuntime : DefaultKotlinCompilationDependencyConfigurationsFactory() {
        override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationDependencyConfigurationsContainer {
            return KotlinCompilationDependencyConfigurationsContainer(target, compilationName, withRuntime = false)
        }
    }
}

internal object NativeKotlinCompilationDependencyConfigurationsFactory :
    KotlinCompilationImplFactory.KotlinCompilationDependencyConfigurationsFactory {

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationDependencyConfigurationsContainer {
        return KotlinCompilationDependencyConfigurationsContainer(
            target, compilationName, withRuntime = false, compileClasspathConfigurationSuffix = "compileKlibraries"
        )
    }
}

internal object JsKotlinCompilationDependencyConfigurationsFactory :
    KotlinCompilationImplFactory.KotlinCompilationDependencyConfigurationsFactory {

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationDependencyConfigurationsContainer {
        return KotlinCompilationDependencyConfigurationsContainer(
            target, compilationName, withRuntime = true,
            configurationNamePrefix = lowerCamelCaseName(
                target.disambiguationClassifierInPlatform, compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }
            )
        )
    }

    private val KotlinTarget.disambiguationClassifierInPlatform: String?
        get() = when (this) {
            is KotlinJsTarget -> disambiguationClassifierInPlatform
            is KotlinJsIrTarget -> disambiguationClassifierInPlatform
            else -> error("Unexpected target type of $this")
        }
}

private fun KotlinCompilationDependencyConfigurationsContainer(
    target: KotlinTarget, compilationName: String, withRuntime: Boolean,
    configurationNamePrefix: String = lowerCamelCaseName(
        target.disambiguationClassifier, compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
    ),
    compileClasspathConfigurationSuffix: String = "compileClasspath",
    runtimeClasspathConfigurationSuffix: String = "runtimeClasspath"
): KotlinCompilationDependencyConfigurationsContainer {
    val compilationCoordinates = "${target.disambiguationClassifier}/$compilationName"
    val compilation = "compilation"

    fun name(vararg parts: String) = lowerCamelCaseName(configurationNamePrefix, *parts)

    val apiConfiguration = target.project.configurations.maybeCreate(name(compilation, API)).apply {
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = false
        description = "API dependencies for $compilationCoordinates"
    }

    val implementationConfiguration = target.project.configurations.maybeCreate(name(compilation, IMPLEMENTATION)).apply {
        extendsFrom(apiConfiguration)
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = false
        description = "Implementation only dependencies for $compilationCoordinates."
    }

    val compileOnlyConfiguration = target.project.configurations.maybeCreate(name(compilation, COMPILE_ONLY)).apply {
        isCanBeConsumed = false
        setupAsLocalTargetSpecificConfigurationIfSupported(target)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))
        isVisible = false
        isCanBeResolved = false
        description = "Compile only dependencies for $compilationCoordinates."
    }

    val runtimeOnlyConfiguration = target.project.configurations.maybeCreate(name(compilation, RUNTIME_ONLY)).apply {
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = false
        description = "Runtime only dependencies for $compilationCoordinates."
    }

    val compileDependencyConfiguration = target.project.configurations.maybeCreate(name(compileClasspathConfigurationSuffix)).apply {
        extendsFrom(compileOnlyConfiguration, implementationConfiguration)
        usesPlatformOf(target)
        isVisible = false
        isCanBeConsumed = false
        attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
        if (target.platformType != KotlinPlatformType.androidJvm) {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))
        }
        description = "Compile classpath for $compilationCoordinates."
    }

    val runtimeDependencyConfiguration = if (withRuntime) target.project.configurations.maybeCreate(
        name(runtimeClasspathConfigurationSuffix)
    ).apply {
        extendsFrom(runtimeOnlyConfiguration, implementationConfiguration)
        usesPlatformOf(target)
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
        if (target.platformType != KotlinPlatformType.androidJvm) {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))
        }
        description = "Runtime classpath of $compilationCoordinates."
    } else null

    return DefaultKotlinCompilationDependencyConfigurationsContainer(
        apiConfiguration = apiConfiguration,
        implementationConfiguration = implementationConfiguration,
        compileOnlyConfiguration = compileOnlyConfiguration,
        runtimeOnlyConfiguration = runtimeOnlyConfiguration,
        compileDependencyConfiguration = compileDependencyConfiguration,
        runtimeDependencyConfiguration = runtimeDependencyConfiguration
    )
}