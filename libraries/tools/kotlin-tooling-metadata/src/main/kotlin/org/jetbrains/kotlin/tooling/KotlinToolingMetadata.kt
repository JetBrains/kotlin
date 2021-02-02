/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling


data class KotlinToolingMetadata(
    /**
     * Build System used (e.g. Gradle, Maven, ...)
     */
    val buildSystem: String,
    val buildSystemVersion: String,

    /**
     * Plugin used to build (e.g.
     *  - org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
     *  - org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlugin
     *  - ...
     */
    val buildPlugin: String,
    val buildPluginVersion: String,

    val projectSettings: ProjectSettings,
    val projectTargets: List<ProjectTargetMetadata>,
) {

    data class ProjectSettings(
        val isHmppEnabled: Boolean,
        val isCompatibilityMetadataVariantEnabled: Boolean,
    )

    data class ProjectTargetMetadata(
        val target: String,
        val platformType: String,
        val extras: Map<String, String>
    )

    companion object
}


