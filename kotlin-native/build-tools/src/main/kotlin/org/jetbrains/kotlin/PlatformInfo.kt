package org.jetbrains.kotlin

import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.*
import org.gradle.api.Project

object PlatformInfo {
    @JvmStatic
    fun isMac() = HostManager.hostIsMac
    @JvmStatic
    fun isWindows() = HostManager.hostIsMingw
    @JvmStatic
    fun isLinux() = HostManager.hostIsLinux

    @JvmStatic
    val hostName: String
        get() = HostManager.hostName

    @JvmStatic
    fun checkXcodeVersion(project: Project) {
        val requiredMajorVersion = project.findProperty("xcodeMajorVersion")?.toString()
        val checkXcodeVersion = project.findProperty("checkXcodeVersion")?.toString() == "true"

        if (!DependencyProcessor.isInternalSeverAvailable
                && checkXcodeVersion
                && requiredMajorVersion != null
        ) {
            val currentXcodeVersion = Xcode.findCurrent().version.toString()
            val currentMajorVersion = currentXcodeVersion.splitToSequence('.').first()
            if (currentMajorVersion != requiredMajorVersion) {
                throw IllegalStateException(
                        "Incorrect Xcode version: ${currentXcodeVersion}. Required major Xcode version is ${requiredMajorVersion}."
                                + "\nYou can try '-PcheckXcodeVersion=false' to suppress this error, the result might be wrong."
                )
            }
        }
    }
}
