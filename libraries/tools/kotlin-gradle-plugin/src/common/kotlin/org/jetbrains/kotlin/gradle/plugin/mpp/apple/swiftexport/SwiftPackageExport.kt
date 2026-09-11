/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.jetbrains.kotlin.konan.target.Family

/**
 * The SwiftPM platform names of the Apple families.
 */
internal val swiftPackagePlatformNames: Map<Family, String> = mapOf(
    Family.IOS to "iOS",
    Family.OSX to "macOS",
    Family.TVOS to "tvOS",
    Family.WATCHOS to "watchOS",
)

/**
 * The `platforms:` of the generated manifest: the platform name of each of the Apple [families] and the minimum
 * version Swift Export requires for it. Sorted, so that the manifest doesn't depend on the order of the targets.
 */
internal fun swiftPackagePlatforms(families: Iterable<Family>): Map<String, String> = families
    .associate { swiftPackagePlatformNames.getValue(it) to SwiftExportConstants.minimumDeploymentTargets.getValue(it) }
    .toSortedMap()
