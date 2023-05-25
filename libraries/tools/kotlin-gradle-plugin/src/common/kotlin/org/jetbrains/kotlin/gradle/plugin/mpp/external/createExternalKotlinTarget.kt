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
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.configureSourcesPublicationAttributes
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.markConsumable
import org.jetbrains.kotlin.gradle.utils.named

/**
 * Creates an adhoc/external Kotlin Target which can be maintained and evolved outside the kotlin.git repository.
 * The target will be created adhering to the configuration provided by the [descriptor].
 * The instance will be backed by an internal implementation of [KotlinTarget]
 * The instance will be created using the [ExternalKotlinTargetDescriptor.targetFactory] which will have to inject the backing
 * internal implementation using the [DecoratedExternalKotlinTarget.Delegate] into [DecoratedExternalKotlinTarget]
 */
@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinTarget> KotlinMultiplatformExtension.createExternalKotlinTarget(
    descriptor: ExternalKotlinTargetDescriptor<T>
): T {
    val apiElementsConfiguration = project.configurations.maybeCreate(lowerCamelCaseName(descriptor.targetName, "apiElements"))
    val runtimeElementsConfiguration = project.configurations.maybeCreate(lowerCamelCaseName(descriptor.targetName, "runtimeElements"))
    val sourcesElementsConfiguration = project.configurations.maybeCreate(lowerCamelCaseName(descriptor.targetName, "sourcesElements"))

    val apiElementsPublishedConfiguration =
        project.configurations.maybeCreate(lowerCamelCaseName(descriptor.targetName, "apiElements-published"))

    val runtimeElementsPublishedConfiguration =
        project.configurations.maybeCreate(lowerCamelCaseName(descriptor.targetName, "runtimeElements-published"))

    val kotlinTargetComponent = ExternalKotlinTargetComponent(
        ExternalKotlinTargetComponent.TargetProvider.byTargetName(this, descriptor.targetName)
    )

    val artifactsTaskLocator = ExternalKotlinTargetImpl.ArtifactsTaskLocator { target ->
        target.project.locateOrRegisterTask<Jar>(lowerCamelCaseName(descriptor.targetName, "jar"))
    }

    val target = ExternalKotlinTargetImpl(
        project = project,
        targetName = descriptor.targetName,
        platformType = descriptor.platformType,
        publishable = true,
        apiElementsConfiguration = apiElementsConfiguration,
        runtimeElementsConfiguration = runtimeElementsConfiguration,
        sourcesElementsConfiguration = sourcesElementsConfiguration,
        apiElementsPublishedConfiguration = apiElementsPublishedConfiguration,
        runtimeElementsPublishedConfiguration = runtimeElementsPublishedConfiguration,
        kotlinTargetComponent = kotlinTargetComponent,
        artifactsTaskLocator = artifactsTaskLocator
    )

    target.setupApiElements(apiElementsConfiguration)
    target.setupApiElements(apiElementsPublishedConfiguration)
    target.setupRuntimeElements(runtimeElementsConfiguration)
    target.setupRuntimeElements(runtimeElementsPublishedConfiguration)
    target.setupSourcesElements(sourcesElementsConfiguration)
    apiElementsConfiguration.markConsumable()
    runtimeElementsConfiguration.markConsumable()
    sourcesElementsConfiguration.markConsumable()

    /* Those configurations can not be resolved but also not consumed (not suitable for project to project dependencies) */
    apiElementsPublishedConfiguration.isCanBeConsumed = false
    apiElementsPublishedConfiguration.isCanBeResolved = false
    runtimeElementsPublishedConfiguration.isCanBeResolved = false
    runtimeElementsPublishedConfiguration.isCanBeConsumed = false

    val decorated = descriptor.targetFactory.create(DecoratedExternalKotlinTarget.Delegate(target))
    target.onCreated()

    descriptor.configure?.invoke(decorated)
    descriptor.apiElements.configure?.invoke(decorated, apiElementsConfiguration)
    descriptor.runtimeElements.configure?.invoke(decorated, runtimeElementsConfiguration)
    descriptor.sourcesElements.configure?.invoke(decorated, sourcesElementsConfiguration)
    descriptor.apiElementsPublished.configure?.invoke(decorated, apiElementsPublishedConfiguration)
    descriptor.runtimeElementsPublished.configure?.invoke(decorated, runtimeElementsPublishedConfiguration)
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
    descriptor: ExternalKotlinTargetDescriptorBuilder<T>.() -> Unit
): T {
    return createExternalKotlinTarget(ExternalKotlinTargetDescriptor(descriptor))
}

private fun ExternalKotlinTargetImpl.setupApiElements(configuration: Configuration) {
    configuration.usesPlatformOf(this)
    configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(this))
    configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
}

private fun ExternalKotlinTargetImpl.setupRuntimeElements(configuration: Configuration) {
    configuration.usesPlatformOf(this)
    configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(this))
    configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
}

private fun ExternalKotlinTargetImpl.setupSourcesElements(configuration: Configuration) {
    configuration.configureSourcesPublicationAttributes(this)
}