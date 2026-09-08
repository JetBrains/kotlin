/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import kotlinBuildProperties
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.konan.util.XcodeProvisioner
import java.io.File

fun Project.isWholeXcodeProvisioningEnabled(): Boolean =
        HostManager.hostIsMac && kotlinBuildProperties.booleanProperty(WHOLE_XCODE_PROPERTY, false).get()

fun Project.isInternalServerEnabled(): Boolean =
        DependencyProcessor.isInternalSeverAvailable ||
                kotlinBuildProperties.booleanProperty(USE_INTERNAL_SERVER_PROPERTY, false).get()

private const val WHOLE_XCODE_PROPERTY = "kotlin.native.internalServer.wholeXcode"

private const val USE_INTERNAL_SERVER_PROPERTY = "kotlin.native.useInternalServer"

internal fun isAppleTargetName(targetName: String): Boolean =
        KonanTarget.predefinedTargets[targetName]?.family?.isAppleFamily == true

class XcodeProvisioningSpec(
        val konanDataDir: String?,
        val version: String,
        val build: String,
        val artifactUrl: String,
        val serverEnabled: Boolean,
        val isTeamCity: Boolean,
)

fun provisionXcodeOrFail(spec: XcodeProvisioningSpec): File = XcodeProvisioner.provisionXcode(
        konanDataDir = spec.konanDataDir,
        version = spec.version,
        build = spec.build,
        artifactUrl = spec.artifactUrl,
        serverEnabled = spec.serverEnabled,
        isTeamCity = spec.isTeamCity,
) ?: error(
        "Whole Xcode ${spec.version} (build ${spec.build}) is not provisioned under the Kotlin/Native dependencies, " +
                "and the currently selected Xcode has a different build. Install and select Xcode ${spec.version} " +
                "(build ${spec.build}), or set KONAN_USE_INTERNAL_SERVER=1 to have it downloaded."
)
