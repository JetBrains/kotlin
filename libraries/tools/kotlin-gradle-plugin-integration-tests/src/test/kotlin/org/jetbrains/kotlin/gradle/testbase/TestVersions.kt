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
        const val G_6_8 = "6.8.3"
        const val G_6_9 = "6.9.2"
        const val G_7_0 = "7.0.2"
        const val G_7_1 = "7.1.1"
        const val G_7_2 = "7.2"
        const val G_7_3 = "7.3.3"
        const val G_7_4 = "7.4.2"
        const val G_7_5 = "7.5.1"
        const val G_7_6 = "7.6"
        // https://gradle.org/nightly/
        // Retention policy is 3 months
        const val G_8_0 = "8.0-rc-1"
        const val MIN_SUPPORTED = minSupportedGradleVersion
        const val MIN_SUPPORTED_KPM = G_7_0
        const val MAX_SUPPORTED = G_7_6
    }

    object Kotlin {
        const val STABLE_RELEASE = "1.6.21"

        // Copied from KOTLIN_VERSION.kt file
        val CURRENT
            get() = System.getProperty("kotlinVersion") ?: error("Required to specify kotlinVersion system property for tests")
    }

    object AGP {
        const val AGP_42 = "4.2.2"
        const val AGP_70 = "7.0.4"
        const val AGP_71 = "7.1.3"
        const val AGP_72 = "7.2.2"
        const val AGP_73 = "7.3.1"
        const val AGP_74 = "7.4.0"
        const val AGP_80 = "8.0.0-alpha11"

        const val MIN_SUPPORTED = AGP_42 // KotlinAndroidPlugin.minimalSupportedAgpVersion
        const val MAX_SUPPORTED = AGP_74 // Update once Gradle MAX_SUPPORTED version will be bumped
    }

    enum class AgpCompatibilityMatrix(
        val version: String,
        val minSupportedGradleVersion: GradleVersion,
        val maxSupportedGradleVersion: GradleVersion,
        val requiredJdkVersion: JavaVersion
    ) {
        AGP_42(AGP.AGP_42, GradleVersion.version(Gradle.MIN_SUPPORTED), GradleVersion.version(Gradle.G_6_9), JavaVersion.VERSION_1_8),
        AGP_70(AGP.AGP_70, GradleVersion.version(Gradle.G_7_0), GradleVersion.version(Gradle.G_7_4), JavaVersion.VERSION_11),
        AGP_71(AGP.AGP_71, GradleVersion.version(Gradle.G_7_2), GradleVersion.version(Gradle.G_7_4), JavaVersion.VERSION_11),
        AGP_72(AGP.AGP_72, GradleVersion.version(Gradle.G_7_3), GradleVersion.version(Gradle.G_7_4), JavaVersion.VERSION_11),
        AGP_73(AGP.AGP_73, GradleVersion.version(Gradle.G_7_4), GradleVersion.version(Gradle.G_7_5), JavaVersion.VERSION_11),
        AGP_74(AGP.AGP_74, GradleVersion.version(Gradle.G_7_5), GradleVersion.version(Gradle.G_7_6), JavaVersion.VERSION_11),
        AGP_80(AGP.AGP_80, GradleVersion.version(Gradle.G_8_0), GradleVersion.version(Gradle.G_8_0), JavaVersion.VERSION_17),
        ;
    }
}
