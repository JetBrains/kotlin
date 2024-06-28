/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.component.SoftwareComponent

/**
 * @suppress TODO: KT-58858 add documentation
 */
interface KotlinTargetComponent : SoftwareComponent {
    val target: KotlinTarget
    val publishable: Boolean
    val publishableOnCurrentHost: Boolean
    val defaultArtifactId: String

    @Deprecated(
        message = "Sources artifacts are now published as separate variant " +
                "use target.sourcesElementsConfigurationName to obtain necessary information",
        replaceWith = ReplaceWith("target.sourcesElementsConfigurationName")
    )
    val sourcesArtifacts: Set<PublishArtifact>
}