package org.jetbrains.kotlin.idea

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.util.PlatformUtils

data class PlatformVersion(val platform: Platform, val version: String /* 3.1 or 2017.3 */) {
    companion object {
        fun parse(platformString: String): PlatformVersion? {
            for (platform in Platform.values()) {
                if (platformString.startsWith(platform.qualifier)) {
                    return PlatformVersion(platform, platformString.drop(platform.qualifier.length))
                }
            }

            return null
        }

        fun getCurrent(): PlatformVersion? {
            val platform = when (PlatformUtils.getPlatformPrefix()) {
                PlatformUtils.IDEA_CE_PREFIX, PlatformUtils.IDEA_PREFIX -> Platform.IDEA
                "AndroidStudio" -> Platform.ANDROID_STUDIO // from 'com.android.tools.idea.IdeInfo'
                else -> return null
            }

            val version = ApplicationInfo.getInstance().run { majorVersion + "." + minorVersion.substringBefore(".") }
            return PlatformVersion(platform, version)
        }
    }

    enum class Platform(val qualifier: String, val presentableText: String) {
        IDEA("IJ", "IDEA"), ANDROID_STUDIO("Studio", "Android Studio")
    }

    override fun toString() = platform.presentableText + " " + version
}