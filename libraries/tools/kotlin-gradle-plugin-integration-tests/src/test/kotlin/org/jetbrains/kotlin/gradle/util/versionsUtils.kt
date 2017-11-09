package org.jetbrains.kotlin.gradle.util

import org.gradle.util.VersionNumber

fun isLegacyAndroidGradleVersion(androidGradlePluginVersion: String): Boolean =
        VersionNumber.parse(androidGradlePluginVersion) < VersionNumber.parse("3.0.0-alpha1")