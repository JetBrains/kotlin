/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.xcode

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.isWholeXcodeProvisioningEnabled
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.nativeDistribution.nativeProtoDistribution

open class XcodeOverridePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Only for macOS hosts.
        if (!HostManager.hostIsMac)
            return
        val wholeXcodeOn = project.isWholeXcodeProvisioningEnabled()
        // We snapshot the Xcode into Xcode.xcodeOverride (inside a ValueSource, so configuration-cache-safe) so that
        // nothing resolves the Apple toolchain live via xcrun at configuration time (which would break the
        // configuration cache — e.g. libclangext's host `clangForJni`). The cases:
        //  - whole-Xcode on                         -> snapshot the provisioned Xcode (host build tools use it too);
        //  - whole-Xcode off, no internal server    -> snapshot the system-selected Xcode (the original behavior);
        //  - whole-Xcode off, internal server       -> nothing to do: the toolchain resolves from the downloaded
        //                                              dependency paths (the InternalServer branch), with no xcrun.
        if (!wholeXcodeOn && DependencyProcessor.isInternalSeverAvailable)
            return
        val xcodeProvider: Provider<Xcode> = project.providers.of(XcodeValueSource::class) {
            parameters.wholeXcode.set(wholeXcodeOn)
            if (wholeXcodeOn) {
                parameters.konanProperties.set(project.nativeProtoDistribution.konanProperties)
                project.providers.gradleProperty("konan.data.dir").orNull?.let { parameters.konanDataDir.set(it) }
                parameters.teamCity.set(project.providers.environmentVariable("TEAMCITY_VERSION").isPresent)
                val forceServer = project.providers.gradleProperty("kotlin.native.useInternalServer")
                        .map { it.toBoolean() }.getOrElse(false)
                parameters.serverEnabled.set(DependencyProcessor.isInternalSeverAvailable || forceServer)
            }
        }
        Xcode.xcodeOverride = xcodeProvider.get()
    }
}
