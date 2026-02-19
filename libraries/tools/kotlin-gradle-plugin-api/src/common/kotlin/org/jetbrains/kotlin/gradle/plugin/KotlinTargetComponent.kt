/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.component.SoftwareComponent

/**
 * A special type of [SoftwareComponent] to define how and what to publish in maven repository for Kotlin projects.
 *
 * The [KotlinTargetComponent] is an internal representation/implementation of Gradle's [SoftwareComponent] in the Kotlin Gradle plugin
 * and is planned for removal from the public API.
 *
 * For more information about the plans for this API, see this issue in [YouTrack](https://youtrack.jetbrains.com/issue/KT-58830).
 */
interface KotlinTargetComponent : SoftwareComponent {

    /**
     * The [Kotlin target][KotlinTarget] that this component belongs to.
     */
    val target: KotlinTarget

    /**
     * Indicates whether this [KotlinTargetComponent] can be published.
     *
     * @see [KotlinTarget.publishable]
     */
    val publishable: Boolean

    /**
     * Indicates whether the current [KotlinTargetComponent] can be published on the current host.
     *
     * This value is determined based on the host-specific criteria and the characteristics
     * of the Kotlin target associated with the component.
     *
     * @see [KotlinTarget.publishable]
     */
    val publishableOnCurrentHost: Boolean

    /**
     * Defines the default artifact ID for the Kotlin target component.
     *
     * This property represents the identifier used to uniquely distinguish
     * the primary artifact associated with this component in a Maven repository.
     */
    val defaultArtifactId: String

    /**
     * @suppress
     */
    @Deprecated(
        message = "Scheduled for removal in Kotlin 2.3. Sources artifacts are now published as separate variant " +
                "use target.sourcesElementsConfigurationName to obtain necessary information",
        replaceWith = ReplaceWith("target.sourcesElementsConfigurationName"),
        level = DeprecationLevel.ERROR,
    )
    val sourcesArtifacts: Set<PublishArtifact>
}