/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.utils.minSupportedGradleVersion

interface TestVersions {

    // https://gradle.org/nightly/
    // Gradle nightly releases retention policy is 3 months
    object Gradle {
        const val G_6_8 = "6.8.3"
        const val G_6_9 = "6.9.4"
        const val G_7_0 = "7.0.2"
        const val G_7_1 = "7.1.1"
        const val G_7_2 = "7.2"
        const val G_7_3 = "7.3.3"
        const val G_7_4 = "7.4.2"
        const val G_7_5 = "7.5.1"
        const val G_7_6 = "7.6.2"
        const val G_8_0 = "8.0.2"
        const val G_8_1 = "8.1.1"
        const val G_8_2 = "8.2.1"
        const val G_8_3 = "8.3-rc-1"
        const val MIN_SUPPORTED = minSupportedGradleVersion
        const val MIN_SUPPORTED_KPM = G_7_0
        const val MAX_SUPPORTED = G_8_1
    }

    object Kotlin {
        const val STABLE_RELEASE = "1.8.22"

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
        const val AGP_74 = "7.4.2"
        const val AGP_80 = "8.0.2"
        const val AGP_81 = "8.1.0-rc01"
        const val AGP_82 = "8.2.0-alpha13"

        const val MIN_SUPPORTED = AGP_42 // KotlinAndroidPlugin.minimalSupportedAgpVersion
        const val MAX_SUPPORTED = AGP_81 // Update once Gradle MAX_SUPPORTED version will be bumped
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
        AGP_81(AGP.AGP_81, GradleVersion.version(Gradle.G_8_1), GradleVersion.version(Gradle.G_8_1), JavaVersion.VERSION_17),
        AGP_82(AGP.AGP_82, GradleVersion.version(Gradle.G_8_1), GradleVersion.version(Gradle.G_8_3), JavaVersion.VERSION_17),
        ;
    }

    object COCOAPODS {
        const val VERSION = "1.11.0"
    }

    object AppleGradlePlugin {
        const val V222_0_21 = "222.4550-0.21"
    }

    object ThirdPartyDependencies {
        const val SHADOW_PLUGIN_VERSION = "8.1.1"
        const val GRADLE_ENTERPRISE_PLUGIN_VERSION = "3.13.4"
    }
}
