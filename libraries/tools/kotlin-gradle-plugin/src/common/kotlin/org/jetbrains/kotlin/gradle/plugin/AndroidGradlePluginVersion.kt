/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode", "FunctionName")

package org.jetbrains.kotlin.gradle.plugin

import com.android.Version
import java.io.Serializable
import java.util.*

internal fun AndroidGradlePluginVersion(versionString: String): AndroidGradlePluginVersion {
    return AndroidGradlePluginVersionOrNull(versionString)
        ?: throw IllegalArgumentException("Invalid Android Gradle Plugin version: $versionString")
}

internal fun AndroidGradlePluginVersionOrNull(versionString: String): AndroidGradlePluginVersion? {
    val baseVersion = versionString.split("-", limit = 2)[0]
    val classifier = versionString.split("-", limit = 2).getOrNull(1)

    val baseVersionSplit = baseVersion.split(".")
    if (!(baseVersionSplit.size == 2 || baseVersionSplit.size == 3)) return null

    return AndroidGradlePluginVersion(
        major = baseVersionSplit[0].toIntOrNull() ?: return null,
        minor = baseVersionSplit[1].toIntOrNull() ?: return null,
        patch = baseVersionSplit.getOrNull(2)?.let { it.toIntOrNull() ?: return null } ?: 0,
        classifier = classifier
    )
}

internal data class AndroidGradlePluginVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0,
    val classifier: String? = null
) : Comparable<AndroidGradlePluginVersion>, Serializable {
    override fun compareTo(other: AndroidGradlePluginVersion): Int {
        if (this === other) return 0
        (this.major - other.major).takeIf { it != 0 }?.let { return it }
        (this.minor - other.minor).takeIf { it != 0 }?.let { return it }
        (this.patch - other.patch).takeIf { it != 0 }?.let { return it }

        if (this.classifier == null && other.classifier == null) return 0
        if (this.classifier == null) return 1
        if (other.classifier == null) return -1

        val thisClassifierLowercase = this.classifier.toLowerCase(Locale.ROOT)
        val otherClassifierLowercase = other.classifier.toLowerCase(Locale.ROOT)
        if (thisClassifierLowercase == otherClassifierLowercase) return 0
        return thisClassifierLowercase.compareTo(otherClassifierLowercase)
    }

    override fun toString(): String {
        return "$major.$minor.$patch" + if (classifier != null) "-$classifier" else ""
    }

    companion object {
        val currentOrNull: AndroidGradlePluginVersion? = try {
            AndroidGradlePluginVersion(Version.ANDROID_GRADLE_PLUGIN_VERSION)
        } catch (_: LinkageError) {
            null
        }

        /**
         * The currently applied/accessible Android Gradle Plugin version
         */
        val current: AndroidGradlePluginVersion
            get() = currentOrNull ?: throw IllegalStateException(
                "Can't infer current AndroidGradlePluginVersion: Is the Android plugin applied?"
            )
    }
}

internal operator fun AndroidGradlePluginVersion.compareTo(versionString: String): Int {
    return this.compareTo(AndroidGradlePluginVersion(versionString))
}

internal fun AndroidGradlePluginVersion?.isAtLeast(versionString: String): Boolean {
    if (this == null) return false
    return this >= AndroidGradlePluginVersion(versionString)
}

internal fun AndroidGradlePluginVersion?.isAtLeast(version: AndroidGradlePluginVersion): Boolean {
    if (this == null) return false
    return this >= version
}
