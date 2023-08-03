/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.utils.futureExtension
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

internal interface InternalKotlinTarget : KotlinTarget, HasMutableExtras {
    var isSourcesPublishable: Boolean
    val kotlinComponents: Set<KotlinTargetComponent>

    @InternalKotlinGradlePluginApi
    override val components: Set<KotlinTargetSoftwareComponent>
    fun onPublicationCreated(publication: MavenPublication)
}

internal val KotlinTarget.internal: InternalKotlinTarget
    get() = (this as? InternalKotlinTarget) ?: throw IllegalArgumentException(
        "KotlinTarget($name) ${this::class} does not implement ${InternalKotlinTarget::class}"
    )


internal val InternalKotlinTarget.isSourcesPublishableFuture by futureExtension("isSourcesPublishableFuture") {
    KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()
    isSourcesPublishable
}