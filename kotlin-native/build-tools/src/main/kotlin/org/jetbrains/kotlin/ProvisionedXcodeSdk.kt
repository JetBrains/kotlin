/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.konan.properties.KonanPropertiesLoader
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.konan.util.XcodeProvisioner

/**
 * Whether the whole-Xcode provisioning is opted in for this build (Gradle property
 * `kotlin.native.internalServer.wholeXcode`). Only meaningful on macOS hosts.
 *
 * The helpers here compute the provisioning spec and trigger provisioning. The runtime clang/linker pick up the
 * provisioned sysroot automatically via PlatformManager's `konanPropertiesOverride` (which flips
 * `useProvisionedXcode`); the compiler-side counterpart is `useProvisionedXcode`/`ProvisionedXcode` in
 * `AppleConfigurablesImpl`.
 */
fun Project.isWholeXcodeProvisioningEnabled(): Boolean =
        HostManager.hostIsMac &&
                providers.gradleProperty("kotlin.native.internalServer.wholeXcode").map { it.toBoolean() }.getOrElse(false)

/**
 * Configuration-time snapshot of everything [XcodeProvisioner.provisionXcode] needs. All fields are plain
 * serializable values so a task can capture it and provision in a `doFirst` (configuration-cache safe).
 */
class XcodeProvisioningSpec(
        val konanDataDir: String?,
        val version: String,
        val build: String,
        val artifactUrl: String,
        val serverEnabled: Boolean,
        val isTeamCity: Boolean,
)

/**
 * The provisioning spec for [target] when whole-Xcode provisioning is enabled for an Apple target, else `null`.
 * Computed at configuration time.
 */
fun Project.xcodeProvisioningSpec(platformManager: PlatformManager, target: KonanTarget): XcodeProvisioningSpec? {
    if (!isWholeXcodeProvisioningEnabled()) return null
    if (!target.family.isAppleFamily) return null
    val configurables = platformManager.platform(target).configurables as? AppleConfigurables ?: return null
    val properties = (configurables as KonanPropertiesLoader).properties
    val version = properties.getProperty("xcodeVersion") ?: return null
    val build = properties.getProperty("xcodeBuild") ?: return null
    val artifactUrl = properties.getProperty("xcodeArtifactUrl") ?: return null
    val konanDataDir = providers.gradleProperty("konan.data.dir").orNull
    val forceServer = providers.gradleProperty("kotlin.native.useInternalServer").map { it.toBoolean() }.getOrElse(false)
    val serverEnabled = DependencyProcessor.isInternalSeverAvailable || forceServer
    val isTeamCity = providers.environmentVariable("TEAMCITY_VERSION").isPresent
    return XcodeProvisioningSpec(konanDataDir, version, build, artifactUrl, serverEnabled, isTeamCity)
}

/**
 * Adds a `doFirst` that provisions the whole Xcode before this task runs, when [spec] is non-null (i.e. the
 * feature is on for an Apple target). On CI the symlink already exists so it is a fast no-op.
 */
fun Task.provisionXcodeBeforeRun(spec: XcodeProvisioningSpec?) {
    val provisioningSpec = spec ?: return
    doFirst {
        XcodeProvisioner.provisionXcode(
                konanDataDir = provisioningSpec.konanDataDir,
                version = provisioningSpec.version,
                build = provisioningSpec.build,
                artifactUrl = provisioningSpec.artifactUrl,
                serverEnabled = provisioningSpec.serverEnabled,
                isTeamCity = provisioningSpec.isTeamCity,
        )
    }
}
