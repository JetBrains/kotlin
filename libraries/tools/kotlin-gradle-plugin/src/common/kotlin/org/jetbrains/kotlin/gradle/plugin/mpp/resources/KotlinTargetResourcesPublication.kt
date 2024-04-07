/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import java.io.File

@ComposeKotlinGradlePluginApi
interface KotlinTargetResourcesPublication {

    data class ResourceRoot(
        val resourcesBaseDirectory: Provider<File>,
        val includes: List<String>,
        val excludes: List<String>,
    )

    fun canPublishResources(target: KotlinTarget): Boolean

    fun publishResourcesAsKotlinComponent(
        target: KotlinTarget,
        resourcePathForSourceSet: (KotlinSourceSet) -> (ResourceRoot),
        relativeResourcePlacement: Provider<File>,
    )

    fun publishInAndroidAssets(
        target: KotlinAndroidTarget,
        resourcePathForSourceSet: (KotlinSourceSet) -> (ResourceRoot),
        relativeResourcePlacement: Provider<File>,
    )
    
    fun canResolveResources(target: KotlinTarget): Boolean

    fun resolveResources(target: KotlinTarget): Provider<File>

    companion object {
        const val EXTENSION_NAME = "multiplatformResourcesPublication"
    }

}