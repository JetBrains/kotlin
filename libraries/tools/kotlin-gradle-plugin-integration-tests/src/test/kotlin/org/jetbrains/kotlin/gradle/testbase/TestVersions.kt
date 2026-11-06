/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion

// Must be in sync with gradleVersions in libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts KTI-1612
interface TestVersions {
    // FIXME: Find a way to unhack this?
    @Suppress("UNSUPPORTED_FEATURE")
    typealias Gradle = TestGradleVersion

    object Kotlin {
        const val STABLE_RELEASE = "2.4.20"

        // Copied from KOTLIN_VERSION.kt file
        val CURRENT
            get() = System.getProperty("kotlinVersion") ?: error("Required to specify kotlinVersion system property for tests")
    }

    object AGP {
        const val AGP_88 = "8.8.2"
        const val AGP_89 = "8.9.3"
        const val AGP_810 = "8.10.1"
        const val AGP_811 = "8.11.2"
        const val AGP_812 = "8.12.3"
        const val AGP_813 = "8.13.2"
        const val AGP_90 = "9.0.1"
        const val AGP_91 = "9.1.1"
        const val AGP_92 = "9.2.1"
        const val AGP_93 = "9.3.2"
        const val AGP_94 = "9.4.1"

        // Should be in sync with KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck
        const val MIN_SUPPORTED = AGP_88 // AgpCompatibilityCheck.minimalSupportedAgpVersion
        const val MAX_SUPPORTED = AGP_94 // Update once the Gradle MAX_SUPPORTED version is bumped
    }

    enum class AgpCompatibilityMatrix(
        val version: String,
        val minSupportedGradleVersion: GradleVersion,
        val maxSupportedGradleVersion: GradleVersion,
        val requiredJdkVersion: JavaVersion,
    ) {
        AGP_88(AGP.AGP_88, GradleVersion.version(TestGradleVersion.G_8_14), GradleVersion.version(TestGradleVersion.G_8_14), JavaVersion.VERSION_17),
        AGP_89(AGP.AGP_89, GradleVersion.version(TestGradleVersion.G_8_14), GradleVersion.version(TestGradleVersion.G_8_14), JavaVersion.VERSION_17),
        AGP_810(AGP.AGP_810, GradleVersion.version(TestGradleVersion.G_8_14), GradleVersion.version(TestGradleVersion.G_8_14), JavaVersion.VERSION_17),
        AGP_811(AGP.AGP_811, GradleVersion.version(TestGradleVersion.G_8_14), GradleVersion.version(TestGradleVersion.G_9_1), JavaVersion.VERSION_17),
        AGP_812(AGP.AGP_812, GradleVersion.version(TestGradleVersion.G_8_14), GradleVersion.version(TestGradleVersion.G_9_1), JavaVersion.VERSION_17),
        AGP_813(AGP.AGP_813, GradleVersion.version(TestGradleVersion.G_8_14), GradleVersion.version(TestGradleVersion.G_9_1), JavaVersion.VERSION_17),
        AGP_90(AGP.AGP_90, GradleVersion.version(TestGradleVersion.G_9_1), GradleVersion.version(TestGradleVersion.G_9_4), JavaVersion.VERSION_17),
        AGP_91(AGP.AGP_91, GradleVersion.version(TestGradleVersion.G_9_3), GradleVersion.version(TestGradleVersion.G_9_5), JavaVersion.VERSION_17),
        AGP_92(AGP.AGP_92, GradleVersion.version(TestGradleVersion.G_9_4), GradleVersion.version(TestGradleVersion.G_9_5), JavaVersion.VERSION_17),
        AGP_93(AGP.AGP_93, GradleVersion.version(TestGradleVersion.G_9_5), GradleVersion.version(TestGradleVersion.G_9_7), JavaVersion.VERSION_17),
        AGP_94(AGP.AGP_94, GradleVersion.version(TestGradleVersion.G_9_6), GradleVersion.version(TestGradleVersion.G_9_8), JavaVersion.VERSION_17),
        ;

        companion object {
            fun fromVersion(
                agpVersion: String
            ): AgpCompatibilityMatrix = AgpCompatibilityMatrix.entries.first { it.version == agpVersion }
        }
    }

    object COCOAPODS {
        const val VERSION = "1.16.2"
    }

    object ThirdPartyDependencies {
        const val SHADOW_PLUGIN_VERSION = "8.3.9"
        const val GOOGLE_DAGGER = "2.24"
        const val GRADLE_ENTERPRISE_PLUGIN_VERSION = "3.13.4"
        const val GRADLE_DEVELOCITY_PLUGIN_VERSION = "4.4.3"
        const val KOTLINX_ATOMICFU = "0.31.0"
        const val KOTLINX_KOVER = "0.9.8"
        const val KOTLINX_BINARY_COMPATIBILITY_VALIDATOR = "0.17.0"
        const val DOKKA = "1.8.10"
        const val DOKKA_V2 = "2.2.0"

        const val KSP = "2.3.12"
    }

    object Compose {
        val composeSnapshotId = System.getProperty("composeSnapshotId")
        val composeVersion = System.getProperty("composeVersion")
    }
}
