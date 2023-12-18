/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetImpl
import org.jetbrains.kotlin.gradle.utils.extrasStoredFuture
import org.jetbrains.kotlin.gradle.utils.getByType
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

internal interface InternalKotlinTarget : KotlinTarget, HasMutableExtras {
    var isSourcesPublishable: Boolean
    val kotlinComponents: Set<KotlinTargetComponent>

    @InternalKotlinGradlePluginApi
    override val components: Set<KotlinTargetSoftwareComponent>
    fun onPublicationCreated(publication: MavenPublication)

    @Deprecated(
        "Accessing 'sourceSets' container on the Kotlin target level DSL is deprecated. " +
                "Consider configuring 'sourceSets' on the Kotlin extension level.",
        level = DeprecationLevel.WARNING
    )
    override val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
        get() = project.extensions.getByType<KotlinProjectExtension>().sourceSets
}

internal val KotlinTarget.internal: InternalKotlinTarget
    get() = (this as? InternalKotlinTarget) ?: throw IllegalArgumentException(
        "KotlinTarget($name) ${this::class} does not implement ${InternalKotlinTarget::class}"
    )

internal val InternalKotlinTarget.compilerOptions: KotlinCommonCompilerOptions
    get() = when (this) {
        is AbstractKotlinTarget -> compilerOptions
        is ExternalKotlinTargetImpl -> compilerOptions
        is DecoratedExternalKotlinTarget -> delegate.compilerOptions
        else -> throw IllegalStateException("Unexpected 'KotlinTarget' type: ${this.javaClass}")
    }

internal val InternalKotlinTarget.isSourcesPublishableFuture by extrasStoredFuture {
    KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()
    isSourcesPublishable
}