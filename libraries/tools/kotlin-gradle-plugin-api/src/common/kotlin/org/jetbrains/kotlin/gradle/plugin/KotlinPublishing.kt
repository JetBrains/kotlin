/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.component.AdhocComponentWithVariants
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * Kotlin Plugin DSL extension to configure Kotlin publishing settings.
 *
 * @since 2.1.20
 */
@ExperimentalKotlinGradlePluginApi
interface KotlinPublishing {
    /**
     * Returns [AdhocComponentWithVariants] that can be used to add additional variants to root Kotlin Multiplatform Publication
     *
     * It is not possible to modify or replace default Kotlin Multiplatform publication variants.
     * Attributes of added variants should not match default Kotlin Multiplatform publication variants.
     * Check [gradle outgoing variants](https://kotl.in/gradle/outgoing-variants) for more details.
     *
     * If you want to update existing variants of the root Kotlin Multiplatform Publication, consider creating
     * a new gradle component and publishing it separately.
     *
     * Example:
     * ```kotlin
     * val userManualElements = configurations.consumable("userManualElements") {
     *     attributes {
     *         attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
     *         attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.USER_MANUAL))
     *     }
     * }
     *
     * kotlin.publishing.adhocSoftwareComponent.addVariantsFromConfiguration(userManualElements.get()) {}
     * // variants from userManualElements will be published with "kotlinMultiplatform" publication
     * ```
     *
     * Search for more details on [AdhocComponentWithVariants] in Gradle's documentation.
     *
     * @since 2.1.20
     */
    @ExperimentalKotlinGradlePluginApi
    val adhocSoftwareComponent: AdhocComponentWithVariants

    /**
     * Configures the [adhocSoftwareComponent] with the provided [configure] block in Kotlin DSL context.
     *
     * @since 2.1.20
     */
    @ExperimentalKotlinGradlePluginApi
    fun adhocSoftwareComponent(configure: AdhocComponentWithVariants.() -> Unit) {
        adhocSoftwareComponent.configure()
    }

    /**
     * Configures the [adhocSoftwareComponent] with the provided [configure] block in Groovy DSL context.
     *
     * @since 2.1.20
     */
    @ExperimentalKotlinGradlePluginApi
    fun adhocSoftwareComponent(configure: Action<AdhocComponentWithVariants>) {
        configure.execute(adhocSoftwareComponent)
    }
}