/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import kotlinx.serialization.Serializable

@Serializable
data class KotlinToolingMetadata(
    val schemaVersion: String,
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
     *  )
     */
    val buildPlugin: String,
    val buildPluginVersion: String,

    val projectSettings: ProjectSettings,
    val projectTargets: List<ProjectTargetMetadata>,
) {

    @Serializable
    data class ProjectSettings(
        val isHmppEnabled: Boolean,
        val isCompatibilityMetadataVariantEnabled: Boolean,
        val isKPMEnabled: Boolean = false,
    )

    @Serializable(with = ProjectTargetMetadataSerializer::class)
    data class ProjectTargetMetadata(
        val target: String,
        val platformType: String,
        val extras: Extras = Extras()
    ) {
        @Serializable
        data class Extras(
            val jvm: JvmExtras? = null,
            val android: AndroidExtras? = null,
            val js: JsExtras? = null,
            val native: NativeExtras? = null
        )

        @Serializable
        data class JvmExtras(
            val jvmTarget: String? = null,
            val withJavaEnabled: Boolean
        )

        @Serializable
        data class AndroidExtras(
            val sourceCompatibility: String,
            val targetCompatibility: String,
        )

        @Serializable
        data class JsExtras(
            val isBrowserConfigured: Boolean,
            val isNodejsConfigured: Boolean,
        )

        @Serializable
        data class NativeExtras(
            val konanTarget: String,
            val konanVersion: String,
            val konanAbiVersion: String
        )
    }

    companion object {
        const val currentSchemaVersion: String = "1.1.0"
    }
}
