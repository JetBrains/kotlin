/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.MINIMALLY_SUPPORTED_GRADLE_VERSION

// Must be in sync with gradleVersions in libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts KTI-1612
interface TestVersions {

    // https://gradle.org/nightly/
    // Gradle nightly releases retention policy is 3 months
    object Gradle {
        const val G_7_6 = "7.6.3"
        const val G_8_0 = "8.0.2"
        const val G_8_1 = "8.1.1"
        const val G_8_2 = "8.2.1"
        const val G_8_3 = "8.3"
        const val G_8_4 = "8.4"
        const val G_8_5 = "8.5"
        const val G_8_6 = "8.6"
        const val G_8_7 = "8.7"
        const val G_8_8 = "8.8"
        const val G_8_9 = "8.9"

        // Should be the same as GradleCompatibilityCheck.minSupportedGradleVersion
        const val MIN_SUPPORTED = MINIMALLY_SUPPORTED_GRADLE_VERSION
        const val MAX_SUPPORTED = G_8_9
    }

    object Kotlin {
        const val STABLE_RELEASE = "2.0.0"

        // Copied from KOTLIN_VERSION.kt file
        val CURRENT
            get() = System.getProperty("kotlinVersion") ?: error("Required to specify kotlinVersion system property for tests")
    }

    object AGP {
        const val AGP_73 = "7.3.1"
        const val AGP_74 = "7.4.2"
        const val AGP_80 = "8.0.2"
        const val AGP_81 = "8.1.4"
        const val AGP_82 = "8.2.2"
        const val AGP_83 = "8.3.2"
        const val AGP_84 = "8.4.0"
        const val AGP_85 = "8.5.0"
        const val AGP_86 = "8.6.0-alpha08"

        // Should be in sync with KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck
        const val MIN_SUPPORTED = AGP_73 // AgpCompatibilityCheck.minimalSupportedAgpVersion
        const val MAX_SUPPORTED = AGP_85 // Update once Gradle MAX_SUPPORTED version will be bumped
    }

    enum class AgpCompatibilityMatrix(
        val version: String,
        val minSupportedGradleVersion: GradleVersion,
        val maxSupportedGradleVersion: GradleVersion,
        val requiredJdkVersion: JavaVersion,
    ) {
        AGP_73(AGP.AGP_73, GradleVersion.version(Gradle.G_7_6), GradleVersion.version(Gradle.G_7_6), JavaVersion.VERSION_11),
        AGP_74(AGP.AGP_74, GradleVersion.version(Gradle.G_7_6), GradleVersion.version(Gradle.G_7_6), JavaVersion.VERSION_11),
        AGP_80(AGP.AGP_80, GradleVersion.version(Gradle.G_8_0), GradleVersion.version(Gradle.G_8_0), JavaVersion.VERSION_17),
        AGP_81(AGP.AGP_81, GradleVersion.version(Gradle.G_8_1), GradleVersion.version(Gradle.G_8_4), JavaVersion.VERSION_17),
        AGP_82(AGP.AGP_82, GradleVersion.version(Gradle.G_8_2), GradleVersion.version(Gradle.G_8_4), JavaVersion.VERSION_17),
        AGP_83(AGP.AGP_83, GradleVersion.version(Gradle.G_8_4), GradleVersion.version(Gradle.G_8_8), JavaVersion.VERSION_17),
        AGP_84(AGP.AGP_84, GradleVersion.version(Gradle.G_8_6), GradleVersion.version(Gradle.G_8_8), JavaVersion.VERSION_17),
        AGP_85(AGP.AGP_85, GradleVersion.version(Gradle.G_8_7), GradleVersion.version(Gradle.G_8_9), JavaVersion.VERSION_17),
        AGP_86(AGP.AGP_86, GradleVersion.version(Gradle.G_8_7), GradleVersion.version(Gradle.G_8_9), JavaVersion.VERSION_17),
        ;

        companion object {
            fun fromVersion(
                agpVersion: String
            ): AgpCompatibilityMatrix = AgpCompatibilityMatrix.entries.first { it.version == agpVersion }
        }
    }

    object COCOAPODS {
        const val VERSION = "1.11.0"
    }

    object AppleGradlePlugin {
        const val V222_0_21 = "222.4550-0.21"
    }

    object ThirdPartyDependencies {
        const val SHADOW_PLUGIN_VERSION = "8.3.0"
        const val GOOGLE_DAGGER = "2.24"
        const val GRADLE_ENTERPRISE_PLUGIN_VERSION = "3.13.4"
        const val KOTLINX_ATOMICFU = "0.23.2"
    }
}
