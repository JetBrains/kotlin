/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.configureSourcesPublicationAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isSourcesPublishableFuture
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.maybeCreateDependencyScope
import org.jetbrains.kotlin.gradle.utils.setAttribute

internal val CreateTargetConfigurationsSideEffect = KotlinTargetSideEffect { target ->
    val project = target.project

    val configurations = project.configurations

    val mainCompilation = target.compilations.maybeCreate(KotlinCompilation.MAIN_COMPILATION_NAME)

    val compileConfiguration = mainCompilation.internal.configurations.deprecatedCompileConfiguration
    val implementationConfiguration = configurations.maybeCreateDependencyScope(mainCompilation.implementationConfigurationName)

    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
    val runtimeOnlyConfiguration = when (mainCompilation) {
        is DeprecatedKotlinCompilationToRunnableFiles<*> -> configurations.maybeCreateDependencyScope(mainCompilation.runtimeOnlyConfigurationName)
        else -> null
    }

    configurations.maybeCreateConsumable(target.apiElementsConfigurationName).apply {
        description = "API elements for main."
        isVisible = false
        attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
        attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        extendsFrom(configurations.maybeCreateDependencyScope(mainCompilation.apiConfigurationName))
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
        if (mainCompilation is DeprecatedKotlinCompilationToRunnableFiles) {
            val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
            runtimeConfiguration?.let { extendsFrom(it) }
        }
        usesPlatformOf(target)
    }

    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
    if (mainCompilation is DeprecatedKotlinCompilationToRunnableFiles<*>) {
        configurations.maybeCreateConsumable(target.runtimeElementsConfigurationName).apply {
            description = "Elements of runtime for main."
            isVisible = false
            attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(target))
            attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
            extendsFrom(implementationConfiguration)
            extendsFrom(runtimeOnlyConfiguration)
            runtimeConfiguration?.let { extendsFrom(it) }
            usesPlatformOf(target)
        }
    }

    configurations.maybeCreateConsumable(target.sourcesElementsConfigurationName).apply {
        description = "Source files of main compilation of ${target.name}."
        isVisible = false
        configureSourcesPublicationAttributes(target)
        project.launch { isCanBeConsumed = target.internal.isSourcesPublishableFuture.await() }
    }

    if (target !is KotlinMetadataTarget) {
        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
        val compileTestsConfiguration = testCompilation.internal.configurations.deprecatedCompileConfiguration
        val testImplementationConfiguration = configurations.maybeCreateDependencyScope(testCompilation.implementationConfigurationName)

        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
        val testRuntimeOnlyConfiguration = when (testCompilation) {
            is DeprecatedKotlinCompilationToRunnableFiles<*> -> configurations.maybeCreateDependencyScope(testCompilation.runtimeOnlyConfigurationName)
            else -> null
        }

        compileConfiguration?.let { compileTestsConfiguration?.extendsFrom(it) }
        testImplementationConfiguration.extendsFrom(implementationConfiguration)
        testRuntimeOnlyConfiguration?.extendsFrom(runtimeOnlyConfiguration)

        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
        if (mainCompilation is DeprecatedKotlinCompilationToRunnableFiles &&
            testCompilation is DeprecatedKotlinCompilationToRunnableFiles
        ) {
            val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
            val testRuntimeConfiguration = testCompilation.internal.configurations.deprecatedRuntimeConfiguration
            runtimeConfiguration?.let { testRuntimeConfiguration?.extendsFrom(it) }
        }
    }

    if (target is KotlinJsIrTarget && !target.isMpp!!) {
        target.project.configurations.maybeCreateConsumable(
            target.commonFakeApiElementsConfigurationName
        ).apply {
            description = "Common Fake API elements for main."
            isVisible = false
            attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
            attributes.setAttribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        }
    }
}