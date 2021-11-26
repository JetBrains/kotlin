/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.utils.minSupportedGradleVersion

interface TestVersions {
    object Gradle {
        const val G_6_1 = "6.1.1"
        const val G_6_2 = "6.2.2"
        const val G_6_3 = "6.3"
        const val G_6_4 = "6.4.1"
        const val G_6_5 = "6.5.1"
        const val G_6_6 = "6.6.1"
        const val G_6_7 = "6.7.1"
        const val G_6_8 = "6.8.3"
        const val G_6_9 = "6.9.1"
        const val G_7_0 = "7.0.2"
        const val G_7_1 = "7.1.1"
        const val G_7_2 = "7.2"
        const val G_7_3 = "7.3-rc-2"
        const val MIN_SUPPORTED = minSupportedGradleVersion
        const val MAX_SUPPORTED = G_7_0
    }

    object Kotlin {
        const val STABLE_RELEASE = "1.4.32"

        // Copied from KOTLIN_VERSION.kt file
        val CURRENT
            get() = System.getProperty("kotlinVersion") ?: error("Required to specify kotlinVersion system property for tests")
    }

    object AGP {
        const val AGP_34 = "3.4.3"
        const val AGP_36 = "3.6.4"
        const val AGP_41 = "4.1.3"
        const val AGP_42 = "4.2.2"
        const val AGP_70 = "7.0.2"
    }
}