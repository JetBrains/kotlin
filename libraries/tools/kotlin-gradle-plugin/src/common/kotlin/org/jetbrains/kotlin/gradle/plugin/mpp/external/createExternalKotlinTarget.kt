/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExternalKotlinTargetApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinTarget> KotlinMultiplatformExtension.createExternalKotlinTarget(
    descriptor: ExternalKotlinTargetDescriptor<T>
): T {
    val defaultConfiguration = project.configurations.maybeCreate(lowerCamelCaseName(descriptor.targetName, "default"))
    val apiElementsConfiguration = project.configurations.maybeCreate(lowerCamelCaseName(descriptor.targetName, "apiElements"))
    val runtimeElementsConfiguration = project.configurations.maybeCreate(lowerCamelCaseName(descriptor.targetName, "runtimeElements"))
    val artifactsTaskLocator = ExternalKotlinTargetImpl.ArtifactsTaskLocator { target ->
        target.project.locateOrRegisterTask<Jar>(lowerCamelCaseName(descriptor.targetName, "jar"))
    }

    val target = ExternalKotlinTargetImpl(
        project = project,
        targetName = descriptor.targetName,
        platformType = descriptor.platformType,
        defaultConfiguration = defaultConfiguration,
        apiElementsConfiguration = apiElementsConfiguration,
        runtimeElementsConfiguration = runtimeElementsConfiguration,
        publishable = true,
        kotlinComponents = emptySet(),
        artifactsTaskLocator = artifactsTaskLocator
    )

    val decorated = descriptor.targetFactory.create(DecoratedExternalKotlinTarget.Delegate(target))
    target.onCreated()
    descriptor.configure?.invoke(decorated)
    targets.add(decorated)
    decorated.logger.info("Created ${descriptor.platformType} target")
    return decorated
}

@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinTarget> KotlinMultiplatformExtension.createExternalKotlinTarget(
    descriptor: ExternalKotlinTargetDescriptorBuilder<T>.() -> Unit
): T {
    return createExternalKotlinTarget(ExternalKotlinTargetDescriptor(descriptor))
}
