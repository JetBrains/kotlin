/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.xcode

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.XcodeProvisioner
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

/**
 * Snapshots an [Xcode] inside a Gradle [ValueSource]. The toolchain/SDK paths are resolved by running `xcrun`;
 * doing that directly at configuration time breaks the configuration cache, so it is done here (a ValueSource is
 * the sanctioned way to run external processes at configuration time) and the resulting plain-value snapshot is
 * stored in the configuration cache.
 *
 * When [Parameters.wholeXcode] is set, the whole Xcode expected by the distribution (`konan.properties`
 * `xcodeVersion`/`xcodeBuild`) is snapshotted instead of the system-selected (ambient) one. Provisioning has to
 * happen here rather than in a task: the snapshot below freezes the Apple toolchain/SDK paths, so the Xcode must
 * already exist by the time this runs. A ValueSource is the sanctioned place for that work, and its result goes
 * into the configuration cache, so a download happens at most once. See [XcodeProvisioner.provisionXcode] for
 * when a download is actually allowed — never on TeamCity, and only with the internal server enabled.
 */
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
        val version = requiredProperty("xcodeVersion")
        val build = requiredProperty("xcodeBuild")
        val xcodeApp = XcodeProvisioner.provisionXcode(
                konanDataDir = parameters.konanDataDir.orNull,
                version = version,
                build = build,
                artifactUrl = requiredProperty("xcodeArtifactUrl"),
                serverEnabled = parameters.serverEnabled.getOrElse(false),
                isTeamCity = parameters.teamCity.getOrElse(false),
        ) ?: error(
                "Whole Xcode $version (build $build) is not provisioned under the Kotlin/Native dependencies, and the " +
                        "currently selected Xcode has a different build. Install and select Xcode $version " +
                        "(build $build), or set KONAN_USE_INTERNAL_SERVER=1 to have it downloaded."
        )
        return Xcode.forDeveloperDir(xcodeApp.resolve("Contents/Developer").path)
    }
}
