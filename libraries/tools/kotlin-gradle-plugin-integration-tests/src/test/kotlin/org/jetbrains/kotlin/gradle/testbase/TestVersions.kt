/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.utils.minSupportedGradleVersion

interface TestVersions {
    object Gradle {
        const val G_6_7 = "6.7.1"
        const val G_6_8 = "6.8.3"
        const val G_6_9 = "6.9.2"
        const val G_7_0 = "7.0.2"
        const val G_7_1 = "7.1.1"
        const val G_7_2 = "7.2"
        const val G_7_3 = "7.3.3"
        const val G_7_4 = "7.4.2"
        const val G_7_5 = "7.5"
        const val MIN_SUPPORTED = minSupportedGradleVersion
        const val MIN_SUPPORTED_KPM = G_7_0
        const val MAX_SUPPORTED = G_7_2
    }

    object Kotlin {
        const val STABLE_RELEASE = "1.5.32"

        // Copied from KOTLIN_VERSION.kt file
        val CURRENT
            get() = System.getProperty("kotlinVersion") ?: error("Required to specify kotlinVersion system property for tests")
    }

    enum class AGP(
        val version: String,
        val minSupportedGradleVersion: GradleVersion,
        val maxSupportedGradleVersion: GradleVersion,
        val requiredJdkVersion: JavaVersion
    ) {
        AGP_36("3.6.4", GradleVersion.version(Gradle.MIN_SUPPORTED), GradleVersion.version(Gradle.G_6_9), JavaVersion.VERSION_1_8),
        AGP_40("4.0.2", GradleVersion.version(Gradle.MIN_SUPPORTED), GradleVersion.version(Gradle.G_6_9), JavaVersion.VERSION_1_8),
        AGP_41("4.1.3", GradleVersion.version(Gradle.MIN_SUPPORTED), GradleVersion.version(Gradle.G_6_9), JavaVersion.VERSION_1_8),
        AGP_42("4.2.2", GradleVersion.version(Gradle.MIN_SUPPORTED), GradleVersion.version(Gradle.G_6_9), JavaVersion.VERSION_1_8),
        AGP_70("7.0.4", GradleVersion.version(Gradle.G_7_0), GradleVersion.version(Gradle.G_7_4), JavaVersion.VERSION_11),
        AGP_71("7.1.3", GradleVersion.version(Gradle.G_7_2), GradleVersion.version(Gradle.G_7_4), JavaVersion.VERSION_11),
        AGP_72("7.2.1", GradleVersion.version(Gradle.G_7_3), GradleVersion.version(Gradle.G_7_4), JavaVersion.VERSION_11),
        ;

        companion object {
            const val MIN_SUPPORTED = "3.6.4" // AGP_36 - KotlinAndroidPlugin.MINIMAL_SUPPORTED_AGP_VERSION
            const val MAX_SUPPORTED = "7.0.4" // AGP_70 - Update once Gradle MAX_SUPPORTED version will be bumped to 7.2+
        }
    }
}
