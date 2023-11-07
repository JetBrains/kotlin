/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.MissingXcodeException
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File

data class XcodeVersion(val major: Int, val minor: Int) : Comparable<XcodeVersion> {
    override fun compareTo(other: XcodeVersion): Int {
        return when (val majorComparison = major.compareTo(other.major)) {
            0 -> minor.compareTo(other.minor)
            else -> majorComparison
        }
    }

    override fun toString(): String {
        return "$major.$minor"
    }

    companion object {
        fun parse(version: String): XcodeVersion? {
            val split = version.split("(\\s+|\\.|-)".toRegex())
            return XcodeVersion(
                major = split[0].toIntOrNull() ?: return null,
                minor = split.getOrNull(1)?.toIntOrNull() ?: return null,
            )
        }
    }
}

interface Xcode {
    val toolchain: String
    val macosxSdk: String
    val iphoneosSdk: String
    val iphonesimulatorSdk: String
    val version: XcodeVersion
    val appletvosSdk: String
    val appletvsimulatorSdk: String
    val watchosSdk: String
    val watchsimulatorSdk: String
    // Xcode.app/Contents/Developer/usr
    val additionalTools: String
    val simulatorRuntimes: String

    /**
     * TODO: `toLowerCase` is deprecated and should be replaced with `lowercase`, but
     * this code used in buildSrc which depends on bootstrap version of stdlib, so right version
     * of this function isn't available, please replace warning suppression with right function
     * when compatible version of bootstrap will be available.
     */
    @Suppress("DEPRECATION")
    fun pathToPlatformSdk(platformName: String): String = when (platformName.toLowerCase()) {
        "macosx" -> macosxSdk
        "iphoneos" -> iphoneosSdk
        "iphonesimulator" -> iphonesimulatorSdk
        "appletvos" -> appletvosSdk
        "appletvsimulator" -> appletvsimulatorSdk
        "watchos" -> watchosSdk
        "watchsimulator" -> watchsimulatorSdk
        else -> error("Unknown Apple platform: $platformName")
    }

    companion object {
        // Don't cache the instance: the compiler might be executed in a Gradle daemon process,
        // so current Xcode might actually change between different invocations.
        @Deprecated("", ReplaceWith("this.findCurrent()"), DeprecationLevel.WARNING)
        val current: Xcode
            get() = findCurrent()

        fun findCurrent(): Xcode = CurrentXcode()
    }
}

internal class CurrentXcode : Xcode {

    override val toolchain by lazy {
        val ldPath = xcrun("-f", "ld") // = $toolchain/usr/bin/ld
        File(ldPath).parentFile.parentFile.absolutePath
    }

    override val additionalTools: String by lazy {
        val bitcodeBuildToolPath = xcrun("-f", "bitcode-build-tool")
        File(bitcodeBuildToolPath).parentFile.parentFile.absolutePath
    }

    override val simulatorRuntimes: String by lazy {
        Command("/usr/bin/xcrun", "simctl", "list", "runtimes", "-j").getOutputLines().joinToString(separator = "\n")
    }
    override val macosxSdk by lazy { getSdkPath("macosx") }
    override val iphoneosSdk by lazy { getSdkPath("iphoneos") }
    override val iphonesimulatorSdk by lazy { getSdkPath("iphonesimulator") }
    override val appletvosSdk by lazy { getSdkPath("appletvos") }
    override val appletvsimulatorSdk by lazy { getSdkPath("appletvsimulator") }
    override val watchosSdk: String by lazy { getSdkPath("watchos") }
    override val watchsimulatorSdk: String by lazy { getSdkPath("watchsimulator") }

    internal val xcodebuildVersion: XcodeVersion
        get() = xcrun("xcodebuild", "-version")
            .removePrefix("Xcode ")
            .parseXcodeVersion()

    internal val bundleVersion: XcodeVersion
        get() = bash("""/usr/libexec/PlistBuddy "$(xcode-select -print-path)/../Info.plist" -c "Print :CFBundleShortVersionString"""")
            .parseXcodeVersion()

    override val version by lazy {
        try {
            bundleVersion
        } catch (e: KonanExternalToolFailure) {
            xcodebuildVersion
        }
    }

    private fun xcrun(vararg args: String): String = try {
        Command("/usr/bin/xcrun", *args).getOutputLines().first()
    } catch (e: KonanExternalToolFailure) {
        // TODO: we should make the message below even more clear and actionable.
        //  Maybe add a link to the documentation.
        //  See https://youtrack.jetbrains.com/issue/KT-50923.
        val message = """
                An error occurred during an xcrun execution. Make sure that Xcode and its command line tools are properly installed.
                Failed command: /usr/bin/xcrun ${args.joinToString(" ")}
                Try running this command in Terminal and fix the errors by making Xcode (and its command line tools) configuration correct.
            """.trimIndent()
        throw MissingXcodeException(message, e)
    }

    private fun bash(command: String): String = Command("/bin/bash", "-c", command).getOutputLines().joinToString("\n")

    private fun getSdkPath(sdk: String) = xcrun("--sdk", sdk, "--show-sdk-path")

    private fun String.parseXcodeVersion(): XcodeVersion {
        return XcodeVersion.parse(this) ?: throw MissingXcodeException("Couldn't parse Xcode version from '$this'")
    }
}
