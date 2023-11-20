/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
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

    @Deprecated(TOOLCHAIN_DSL_WRONG_USAGE_ERROR, level = DeprecationLevel.ERROR)
    fun jvmToolchain(action: Action<JavaToolchainSpec>): Unit = error(TOOLCHAIN_DSL_WRONG_USAGE_ERROR)

    @Deprecated(TOOLCHAIN_DSL_WRONG_USAGE_ERROR, level = DeprecationLevel.ERROR)
    fun jvmToolchain(jdkVersion: Int): Unit = error(TOOLCHAIN_DSL_WRONG_USAGE_ERROR)
}

private const val TOOLCHAIN_DSL_WRONG_USAGE_ERROR =
    "Configuring JVM toolchain in the Kotlin target level DSL is prohibited. " +
            "JVM toolchain feature should be configured in the extension scope as it affects all JVM targets (JVM, Android)."

internal val KotlinTarget.internal: InternalKotlinTarget
    get() = (this as? InternalKotlinTarget) ?: throw IllegalArgumentException(
        "KotlinTarget($name) ${this::class} does not implement ${InternalKotlinTarget::class}"
    )

internal val InternalKotlinTarget.isSourcesPublishableFuture by extrasStoredFuture {
    KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()
    isSourcesPublishable
}