/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.configureSourcesPublicationAttributes
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isSourcesPublishableFuture
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal val CreateTargetConfigurationsSideEffect = KotlinTargetSideEffect { target ->
    val project = target.project

    val configurations = project.configurations

    val mainCompilation = target.compilations.maybeCreate(KotlinCompilation.MAIN_COMPILATION_NAME)

    val compileConfiguration = mainCompilation.internal.configurations.deprecatedCompileConfiguration
    val implementationConfiguration = configurations.maybeCreate(mainCompilation.implementationConfigurationName)

    val runtimeOnlyConfiguration = when (mainCompilation) {
        is KotlinCompilationToRunnableFiles<*> -> configurations.maybeCreate(mainCompilation.runtimeOnlyConfigurationName)
        else -> null
    }

    configurations.maybeCreate(target.apiElementsConfigurationName).apply {
        description = "API elements for main."
        isVisible = false
        isCanBeResolved = false
        isCanBeConsumed = true
        attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        extendsFrom(configurations.maybeCreate(mainCompilation.apiConfigurationName))
        if (mainCompilation is KotlinCompilationToRunnableFiles) {
            val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
            runtimeConfiguration?.let { extendsFrom(it) }
        }
        usesPlatformOf(target)
    }

    if (mainCompilation is KotlinCompilationToRunnableFiles<*>) {
        configurations.maybeCreate(target.runtimeElementsConfigurationName).apply {
            description = "Elements of runtime for main."
            isVisible = false
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(target))
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
            extendsFrom(implementationConfiguration)
            if (runtimeOnlyConfiguration != null)
                extendsFrom(runtimeOnlyConfiguration)
            runtimeConfiguration?.let { extendsFrom(it) }
            usesPlatformOf(target)
        }
    }

    configurations.maybeCreate(target.sourcesElementsConfigurationName).apply {
        description = "Source files of main compilation of ${target.name}."
        isVisible = false
        isCanBeResolved = false
        isCanBeConsumed = true
        configureSourcesPublicationAttributes(target)
        project.launch { isCanBeConsumed = target.internal.isSourcesPublishableFuture.await() }
    }

    if (target !is KotlinMetadataTarget) {
        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
        val compileTestsConfiguration = testCompilation.internal.configurations.deprecatedCompileConfiguration
        val testImplementationConfiguration = configurations.maybeCreate(testCompilation.implementationConfigurationName)
        val testRuntimeOnlyConfiguration = when (testCompilation) {
            is KotlinCompilationToRunnableFiles<*> -> configurations.maybeCreate(testCompilation.runtimeOnlyConfigurationName)
            else -> null
        }

        compileConfiguration?.let { compileTestsConfiguration?.extendsFrom(it) }
        testImplementationConfiguration.extendsFrom(implementationConfiguration)
        testRuntimeOnlyConfiguration?.extendsFrom(runtimeOnlyConfiguration)

        if (mainCompilation is KotlinCompilationToRunnableFiles && testCompilation is KotlinCompilationToRunnableFiles) {
            val runtimeConfiguration = mainCompilation.internal.configurations.deprecatedRuntimeConfiguration
            val testRuntimeConfiguration = testCompilation.internal.configurations.deprecatedRuntimeConfiguration
            runtimeConfiguration?.let { testRuntimeConfiguration?.extendsFrom(it) }
        }
    }

    if (target is KotlinJsIrTarget && !target.isMpp!!) {
        target.project.configurations.maybeCreate(
            target.commonFakeApiElementsConfigurationName
        ).apply {
            description = "Common Fake API elements for main."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute<Usage>(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(target))
            attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
        }
    }
}