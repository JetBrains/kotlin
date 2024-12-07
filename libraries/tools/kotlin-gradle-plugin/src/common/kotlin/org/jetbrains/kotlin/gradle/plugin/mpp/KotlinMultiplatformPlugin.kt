/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.registerKotlinPluginExtensions

/**
 * This class is not needed anymore;
 *
 * ### Previous Usage:
 * Previously the Kotlin Gradle Plugin used to distinguish between two kinds of 'plugins'
 * The 'wrapper' (see 'KotlinMultiplatformPluginWrapper') and the plugin.
 *
 * The Wrappers were the plugins that were actually applied to the project when users requested Kotlin:
 * ```kotlin
 * plugins {
 *     kotlin("multiplatform")
 * }
 * ```
 *
 * The code above would have applied the 'KotlinMultiplatformPluginWrapper'.
 * Those 'wrapper plugins' then did run the 'main configuration code' and at some point created this 'wrapped' plugins manually
 * and called the [apply] function.
 *
 * Its quite unfortunate that both entities implemented [Plugin] of [Project] and therefore the differentiation was not clear.
 *
 * ### Replaced by: Kotlin Gradle Plugin Extension Points:
 *  See [registerKotlinPluginExtensions]:
 *  Kotlin Multiplatform will register corresponding extension points to customise its behavior.
 *  At the time of writing this comment: We take heavy usage of the [KotlinProjectSetupAction.extensionPoint] to provide
 *  additional project configuration for multiplatform.
 */
@Deprecated("Scheduled for removal in Kotlin 2.1")
class KotlinMultiplatformPlugin : Plugin<Project> {
    override fun apply(project: Project) = Unit

    companion object {
        @Deprecated(
            "Scheduled for removal in Kotlin 2.1",
            replaceWith = ReplaceWith(
                "KotlinMetadataTarget.METADATA_TARGET_NAME",
                imports = ["org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget"]
            )
        )
        const val METADATA_TARGET_NAME = KotlinMetadataTarget.METADATA_TARGET_NAME
    }
}
