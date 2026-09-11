/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.jetbrains.kotlin.konan.target.Family
import java.io.Serializable

/**
 * The SwiftPM platform names of the Apple families.
 */
internal val swiftPackagePlatformNames: Map<Family, String> = mapOf(
    Family.IOS to "iOS",
    Family.OSX to "macOS",
    Family.TVOS to "tvOS",
    Family.WATCHOS to "watchOS",
)

/** A `platforms:` entry of the generated manifest: `.name("minimumVersion")`. */
internal data class SwiftPackagePlatform(val name: String, val minimumVersion: String) : Serializable

/**
 * The `platforms:` of the generated manifest: the platform of each of the Apple [families] with the minimum
 * version Swift Export requires for it. Sorted, so that the manifest doesn't depend on the order of the targets.
 */
internal fun swiftPackagePlatforms(families: Iterable<Family>): List<SwiftPackagePlatform> = families
    .distinct()
    .map { SwiftPackagePlatform(swiftPackagePlatformNames.getValue(it), SwiftExportConstants.minimumDeploymentTargets.getValue(it)) }
    .sortedBy { it.name }
