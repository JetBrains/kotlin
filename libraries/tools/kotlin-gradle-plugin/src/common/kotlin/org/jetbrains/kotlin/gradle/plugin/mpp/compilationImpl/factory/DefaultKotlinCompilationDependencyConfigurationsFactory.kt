/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.DefaultKotlinCompilationDependencyConfigurationsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationDependencyConfigurationsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.javaSourceSets
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
        val naming = ConfigurationNaming.Default(target, compilationName)
        return KotlinCompilationDependencyConfigurationsContainer(
            target = target,
            compilationName = compilationName,
            naming = naming,
            withRuntime = false,
            compileClasspathConfigurationName = naming.name("compileKlibraries")
        )
    }
}

internal object JsKotlinCompilationDependencyConfigurationsFactory :
    KotlinCompilationImplFactory.KotlinCompilationDependencyConfigurationsFactory {

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationDependencyConfigurationsContainer {
        return KotlinCompilationDependencyConfigurationsContainer(
            target, compilationName, withRuntime = true,
            naming = ConfigurationNaming.Js(target, compilationName)
        )
    }
}

internal class JvmWithJavaCompilationDependencyConfigurationsFactory(private val target: KotlinWithJavaTarget<*, *>) :
    KotlinCompilationImplFactory.KotlinCompilationDependencyConfigurationsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationDependencyConfigurationsContainer {
        val javaSourceSet = this.target.javaSourceSets.maybeCreate(compilationName)
        return KotlinCompilationDependencyConfigurationsContainer(
            target = target, compilationName = compilationName, withRuntime = true,
            apiConfigurationName = javaSourceSet.apiConfigurationName,
            implementationConfigurationName = javaSourceSet.implementationConfigurationName,
            compileOnlyConfigurationName = javaSourceSet.compileOnlyConfigurationName,
            runtimeOnlyConfigurationName = javaSourceSet.runtimeOnlyConfigurationName,
            compileClasspathConfigurationName = javaSourceSet.compileClasspathConfigurationName,
            runtimeClasspathConfigurationName = javaSourceSet.runtimeClasspathConfigurationName
        )
    }
}

private fun interface ConfigurationNaming {
    fun name(vararg parts: String): String

    class Default(
        private val target: KotlinTarget,
        private val compilationName: String,
    ) : ConfigurationNaming {
        override fun name(vararg parts: String): String = lowerCamelCaseName(
            target.disambiguationClassifier, compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }, *parts
        )
    }

    class Js(
        private val target: KotlinTarget,
        private val compilationName: String
    ) : ConfigurationNaming {
        override fun name(vararg parts: String): String = lowerCamelCaseName(
            target.disambiguationClassifierInPlatform, compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }, *parts
        )

        private val KotlinTarget.disambiguationClassifierInPlatform: String?
            get() = when (this) {
                is KotlinJsTarget -> disambiguationClassifierInPlatform
                is KotlinJsIrTarget -> disambiguationClassifierInPlatform
                else -> error("Unexpected target type of $this")
            }
    }
}

private const val compilation = "compilation"

private fun KotlinCompilationDependencyConfigurationsContainer(
    target: KotlinTarget, compilationName: String, withRuntime: Boolean,
    naming: ConfigurationNaming = ConfigurationNaming.Default(target, compilationName),
    apiConfigurationName: String = naming.name(compilation, API),
    implementationConfigurationName: String = naming.name(compilation, IMPLEMENTATION),
    compileOnlyConfigurationName: String = naming.name(compilation, COMPILE_ONLY),
    runtimeOnlyConfigurationName: String = naming.name(compilation, RUNTIME_ONLY),
    compileClasspathConfigurationName: String = naming.name("compileClasspath"),
    runtimeClasspathConfigurationName: String = naming.name("runtimeClasspath")
): KotlinCompilationDependencyConfigurationsContainer {
    val compilationCoordinates = "${target.disambiguationClassifier}/$compilationName"

    val apiConfiguration = target.project.configurations.maybeCreate(apiConfigurationName).apply {
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = false
        description = "API dependencies for $compilationCoordinates"
    }

    val implementationConfiguration = target.project.configurations.maybeCreate(implementationConfigurationName).apply {
        extendsFrom(apiConfiguration)
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = false
        description = "Implementation only dependencies for $compilationCoordinates."
    }

    val compileOnlyConfiguration = target.project.configurations.maybeCreate(compileOnlyConfigurationName).apply {
        isCanBeConsumed = false
        setupAsLocalTargetSpecificConfigurationIfSupported(target)
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))
        isVisible = false
        isCanBeResolved = false
        description = "Compile only dependencies for $compilationCoordinates."
    }

    val runtimeOnlyConfiguration = target.project.configurations.maybeCreate(runtimeOnlyConfigurationName).apply {
        isVisible = false
        isCanBeConsumed = false
        isCanBeResolved = false
        description = "Runtime only dependencies for $compilationCoordinates."
    }

    val compileDependencyConfiguration = target.project.configurations.maybeCreate(compileClasspathConfigurationName).apply {
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

    val runtimeDependencyConfiguration =
        if (withRuntime) target.project.configurations.maybeCreate(runtimeClasspathConfigurationName).apply {
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