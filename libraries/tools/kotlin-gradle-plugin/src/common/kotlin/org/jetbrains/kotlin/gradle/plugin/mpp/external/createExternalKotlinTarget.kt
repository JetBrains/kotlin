/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExternalKotlinTargetApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.configureSourcesPublicationAttributes
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.newInstance

/**
 * Creates an adhoc/external Kotlin Target which can be maintained and evolved outside the kotlin.git repository.
 * The target will be created adhering to the configuration provided by the [descriptor].
 * The instance will be backed by an internal implementation of [KotlinTarget]
 * The instance will be created using the [ExternalKotlinTargetDescriptor.targetFactory] which will have to inject the backing
 * internal implementation using the [DecoratedExternalKotlinTarget.Delegate] into [DecoratedExternalKotlinTarget]
 */
@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinTarget> KotlinMultiplatformExtension.createExternalKotlinTarget(
    descriptor: ExternalKotlinTargetDescriptor<T>,
): T {
    val apiElementsConfiguration = project.configurations
        .maybeCreateConsumable(lowerCamelCaseName(descriptor.targetName, "apiElements"))
    val runtimeElementsConfiguration = project.configurations
        .maybeCreateConsumable(lowerCamelCaseName(descriptor.targetName, "runtimeElements"))
    val sourcesElementsConfiguration = project.configurations
        .maybeCreateConsumable(lowerCamelCaseName(descriptor.targetName, "sourcesElements"))

    fun Configuration.notVisible() = apply { isVisible = false }
    val apiElementsPublishedConfiguration = project.configurations
        .maybeCreateDependencyScope(lowerCamelCaseName(descriptor.targetName, "apiElements-published"))
        .notVisible()

    val runtimeElementsPublishedConfiguration = project.configurations
        .maybeCreateDependencyScope(lowerCamelCaseName(descriptor.targetName, "runtimeElements-published"))
        .notVisible()

    val sourcesElementsPublishedConfiguration = project.configurations
        .maybeCreateDependencyScope(lowerCamelCaseName(descriptor.targetName, "sourcesElements-published"))
        .notVisible()

    val kotlinTargetComponent = ExternalKotlinTargetComponent(
        ExternalKotlinTargetComponent.TargetProvider.byTargetName(this, descriptor.targetName)
    )

    val artifactsTaskLocator = ExternalKotlinTargetImpl.ArtifactsTaskLocator { target ->
        target.project.locateOrRegisterTask<Jar>(lowerCamelCaseName(descriptor.targetName, "jar"))
    }

    val compilerOptions = when (descriptor.platformType) {
        KotlinPlatformType.androidJvm,
        KotlinPlatformType.jvm -> project.objects.newInstance<KotlinJvmCompilerOptionsDefault>()
        KotlinPlatformType.wasm,
        KotlinPlatformType.js -> project.objects.newInstance<KotlinJsCompilerOptionsDefault>()
        KotlinPlatformType.common -> project.objects.newInstance<KotlinCommonCompilerOptionsDefault>()
        KotlinPlatformType.native -> project.objects.newInstance<KotlinNativeCompilerOptionsDefault>()
    }

    val target = ExternalKotlinTargetImpl(
        project = project,
        targetName = descriptor.targetName,
        platformType = descriptor.platformType,
        publishable = true,
        compilerOptions = compilerOptions,
        apiElementsConfiguration = apiElementsConfiguration,
        runtimeElementsConfiguration = runtimeElementsConfiguration,
        sourcesElementsConfiguration = sourcesElementsConfiguration,
        apiElementsPublishedConfiguration = apiElementsPublishedConfiguration,
        runtimeElementsPublishedConfiguration = runtimeElementsPublishedConfiguration,
        sourcesElementsPublishedConfiguration = sourcesElementsPublishedConfiguration,
        kotlinTargetComponent = kotlinTargetComponent,
        artifactsTaskLocator = artifactsTaskLocator
    )

    target.setupApiElements(apiElementsConfiguration)
    target.setupApiElements(apiElementsPublishedConfiguration)
    target.setupRuntimeElements(runtimeElementsConfiguration)
    target.setupRuntimeElements(runtimeElementsPublishedConfiguration)
    target.setupSourcesElements(sourcesElementsConfiguration)
    target.setupSourcesElements(sourcesElementsPublishedConfiguration)

    val decorated = descriptor.targetFactory.create(DecoratedExternalKotlinTarget.Delegate(target))
    target.onCreated()

    descriptor.configure?.invoke(decorated)
    descriptor.apiElements.configure?.invoke(decorated, apiElementsConfiguration)
    descriptor.runtimeElements.configure?.invoke(decorated, runtimeElementsConfiguration)
    descriptor.sourcesElements.configure?.invoke(decorated, sourcesElementsConfiguration)
    descriptor.apiElementsPublished.configure?.invoke(decorated, apiElementsPublishedConfiguration)
    descriptor.runtimeElementsPublished.configure?.invoke(decorated, runtimeElementsPublishedConfiguration)
    descriptor.sourcesElementsPublished.configure?.invoke(decorated, sourcesElementsPublishedConfiguration)
    descriptor.configureIdeImport?.invoke(project.kotlinIdeMultiplatformImport)

    targets.add(decorated)
    decorated.logger.info("Created ${descriptor.platformType} target")
    return decorated
}

/**
 * @see createExternalKotlinTarget
 */
@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinTarget> KotlinMultiplatformExtension.createExternalKotlinTarget(
    descriptor: ExternalKotlinTargetDescriptorBuilder<T>.() -> Unit,
): T {
    return createExternalKotlinTarget(ExternalKotlinTargetDescriptor(descriptor))
}

private fun ExternalKotlinTargetImpl.setupApiElements(configuration: Configuration) {
    configuration.usesPlatformOf(this)
    configuration.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(this))
    configuration.attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
}

private fun ExternalKotlinTargetImpl.setupRuntimeElements(configuration: Configuration) {
    configuration.usesPlatformOf(this)
    configuration.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(this))
    configuration.attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
}

private fun ExternalKotlinTargetImpl.setupSourcesElements(configuration: Configuration) {
    configuration.configureSourcesPublicationAttributes(this)
}
