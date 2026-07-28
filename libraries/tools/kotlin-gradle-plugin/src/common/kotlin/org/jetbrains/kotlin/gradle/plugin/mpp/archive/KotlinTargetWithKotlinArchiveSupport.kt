/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.HasProject

internal interface KotlinTargetWithKotlinArchiveSupport : HasProject {
    val isStoredInKotlinArchive: Boolean
    val requiresPlatformComponent: Boolean
    val requiresPlatformComponentCompatibilityCapability: Boolean
}

internal interface KotlinTargetWithKlibsInKotlinArchiveSupport : KotlinTargetWithKotlinArchiveSupport {
    val pathInKotlinArchive: String
}

internal val KotlinTargetWithKlibsInKotlinArchiveSupport.kotlinArchivePlatformKlibPath: Provider<String>
    get() = project.provider { "platform/$pathInKotlinArchive" }

internal val KotlinTargetWithKlibsInKotlinArchiveSupport.kotlinArchiveResourcesPath: Provider<String>
    get() = project.provider { "resources/$pathInKotlinArchive" }

internal fun KotlinTargetWithKlibsInKotlinArchiveSupport.kotlinArchiveCinteropKlibPath(fileName: Provider<String>): Provider<String> {
    return fileName.map { name -> "platform/$pathInKotlinArchive/$name" }
}
