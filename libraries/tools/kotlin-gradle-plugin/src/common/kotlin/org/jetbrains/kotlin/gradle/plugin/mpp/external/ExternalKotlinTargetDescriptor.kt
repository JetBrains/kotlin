/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetDescriptor.TargetFactory
import kotlin.properties.Delegates

/**
 * Descriptor used by the Kotlin Gradle Plugin to build a corresponding [ExternalKotlinTargetImpl] from.
 * This interface is *not stable for implementation* and will become sealed in the future!
 *
 * Use the `ExternalKotlinTargetDescriptor { }` factory function to create a new instance using a builder pattern
 */
@ExternalKotlinTargetApi
interface ExternalKotlinTargetDescriptor<T : DecoratedExternalKotlinTarget> {

    @ExternalKotlinTargetApi
    fun interface TargetFactory<T : DecoratedExternalKotlinTarget> {
        fun create(target: DecoratedExternalKotlinTarget.Delegate): T
    }

    val targetName: String
    val platformType: KotlinPlatformType
    val targetFactory: TargetFactory<T>

    val apiElements: ExternalKotlinTargetConfigurationDescriptor<T>
    val runtimeElements: ExternalKotlinTargetConfigurationDescriptor<T>
    val sourcesElements: ExternalKotlinTargetConfigurationDescriptor<T>
    val apiElementsPublished: ExternalKotlinTargetConfigurationDescriptor<T>
    val runtimeElementsPublished: ExternalKotlinTargetConfigurationDescriptor<T>

    val configure: ((T) -> Unit)?
    val configureIdeImport: (IdeMultiplatformImport.() -> Unit)?
}

/**
 * Creates a new [ExternalKotlinTargetDescriptor] using the builder pattern.
 * There are some required properties that have to be set.
 * Check [ExternalKotlinTargetDescriptorBuilder] for further details.
 *
 *  * The following properties have to be specified:
 *  * - [ExternalKotlinTargetDescriptorBuilder.targetName]
 *  * - [ExternalKotlinTargetDescriptorBuilder.platformType]
 *  * - [ExternalKotlinTargetDescriptorBuilder.targetFactory]
 *
 *  Not providing a required/necessary property will throw [IllegalStateException]
 */
@ExternalKotlinTargetApi
fun <T : DecoratedExternalKotlinTarget> ExternalKotlinTargetDescriptor(
    configure: ExternalKotlinTargetDescriptorBuilder<T>.() -> Unit,
): ExternalKotlinTargetDescriptor<T> {
    return ExternalKotlinTargetDescriptorBuilder<T>().also(configure).build()
}

/**
 * Builder for [ExternalKotlinTargetDescriptor]
 * The following properties have to be specified:
 * - [targetName]
 * - [platformType]
 * - [targetFactory]
 *
 * Properties added in future Kotlin Gradle Plugin releases will be added using a default value, but
 * a warning might be emitted if not specified.
 */
@ExternalKotlinTargetApi
class ExternalKotlinTargetDescriptorBuilder<T : DecoratedExternalKotlinTarget> internal constructor() {
    var targetName: String by Delegates.notNull()
    var platformType: KotlinPlatformType by Delegates.notNull()
    var targetFactory: TargetFactory<T> by Delegates.notNull()

    val apiElements: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    val runtimeElements: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    val sourcesElements: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    val apiElementsPublished: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    val runtimeElementsPublished: ExternalKotlinTargetConfigurationDescriptorBuilder<T> =
        ExternalKotlinTargetConfigurationDescriptorBuilder()

    private var configure: ((T) -> Unit)? = null

    /**
     * Generic configuration that will be invoked when building the target.
     * This configuration is called right after creating the instance and before
     * publishing the target to all subscribers of `kotlin.targets.all {}`
     */
    fun configure(action: (T) -> Unit) {
        val configure = this.configure
        if (configure == null) this.configure = action
        else this.configure = { configure(it); action(it) }
    }

    internal var configureIdeImport: (IdeMultiplatformImport.() -> Unit)? = null

    /**
     * Main entrance of configuring the ide import:
     * The [IdeMultiplatformImport] instance passed to this function shall
     * not be captured and used outside of this block.
     *
     * The [IdeMultiplatformImport] instance shall not be retrieved any other way than using this function.
     */
    fun configureIdeImport(action: IdeMultiplatformImport.() -> Unit) {
        val configureIdeImport = this.configureIdeImport
        if (configureIdeImport == null) this.configureIdeImport = action
        else this.configureIdeImport = { configureIdeImport(); action() }
    }

    internal fun build(): ExternalKotlinTargetDescriptor<T> = ExternalKotlinTargetDescriptorImpl(
        targetName = targetName,
        platformType = platformType,
        targetFactory = targetFactory,
        apiElements = apiElements.build(),
        runtimeElements = runtimeElements.build(),
        sourcesElements = sourcesElements.build(),
        apiElementsPublished = apiElementsPublished.build(),
        runtimeElementsPublished = runtimeElementsPublished.build(),
        configure = configure,
        configureIdeImport = configureIdeImport
    )
}

private data class ExternalKotlinTargetDescriptorImpl<T : DecoratedExternalKotlinTarget>(
    override val targetName: String,
    override val platformType: KotlinPlatformType,
    override val targetFactory: TargetFactory<T>,
    override val apiElements: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val runtimeElements: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val sourcesElements: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val apiElementsPublished: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val runtimeElementsPublished: ExternalKotlinTargetConfigurationDescriptor<T>,
    override val configure: ((T) -> Unit)?,
    override val configureIdeImport: (IdeMultiplatformImport.() -> Unit)?,
) : ExternalKotlinTargetDescriptor<T>


