/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.xcode

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.XcodeProvisioningSpec
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.provisionXcodeOrFail
import java.util.Properties

private data class XcodeSnapshot(
        override val additionalTools: String,
        override val appletvosSdk: String,
        override val appletvsimulatorSdk: String,
        override val iphoneosSdk: String,
        override val iphonesimulatorSdk: String,
        override val macosxSdk: String,
        override val simulatorRuntimes: String,
        override val toolchain: String,
        override val version: XcodeVersion,
        override val watchosSdk: String,
        override val watchsimulatorSdk: String,
) : Xcode {
    constructor(original: Xcode) : this(
            additionalTools = original.additionalTools,
            appletvosSdk = original.appletvosSdk,
            appletvsimulatorSdk = original.appletvsimulatorSdk,
            iphoneosSdk = original.iphoneosSdk,
            iphonesimulatorSdk = original.iphonesimulatorSdk,
            macosxSdk = original.macosxSdk,
            simulatorRuntimes = original.simulatorRuntimes,
            toolchain = original.toolchain,
            version = original.version,
            watchosSdk = original.watchosSdk,
            watchsimulatorSdk = original.watchsimulatorSdk,
    )
}

abstract class XcodeValueSource : ValueSource<Xcode, XcodeValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        /** When `true`, snapshot the whole provisioned Xcode instead of the system-selected one. */
        val wholeXcode: Property<Boolean>

        /**
         * The distribution's `konan.properties`, read for `xcodeVersion`/`xcodeBuild`/`xcodeArtifactUrl`.
         * Required iff [wholeXcode].
         */
        val konanProperties: RegularFileProperty

        /** `konan.data.dir` Gradle property, if set (dependencies live under `<konanDataDir>/dependencies`). */
        val konanDataDir: Property<String>

        /** Whether this is a TeamCity build (agents must ship the expected Xcode via their image). */
        val teamCity: Property<Boolean>

        /**
         * Whether the internal server is enabled. Only then may an Xcode that is neither provisioned nor installed
         * be downloaded; otherwise the build fails with an actionable message.
         */
        val serverEnabled: Property<Boolean>
    }

    override fun obtain(): Xcode {
        val xcode = if (parameters.wholeXcode.getOrElse(false)) provisionedXcode() else Xcode.defaultCurrent()
        return XcodeSnapshot(xcode)
    }

    private fun provisionedXcode(): Xcode {
        val properties = Properties().apply {
            parameters.konanProperties.get().asFile.inputStream().use { load(it) }
        }

        fun requiredProperty(name: String) = properties.getProperty(name)
                ?: error("whole-Xcode provisioning is on but '$name' is missing from konan.properties")

        val xcodeApp = provisionXcodeOrFail(
                XcodeProvisioningSpec(
                        konanDataDir = parameters.konanDataDir.orNull,
                        version = requiredProperty("xcodeVersion"),
                        build = requiredProperty("xcodeBuild"),
                        artifactUrl = requiredProperty("xcodeArtifactUrl"),
                        serverEnabled = parameters.serverEnabled.getOrElse(false),
                        isTeamCity = parameters.teamCity.getOrElse(false),
                )
        )
        return Xcode.forDeveloperDir(xcodeApp.resolve("Contents/Developer").path)
    }
}
