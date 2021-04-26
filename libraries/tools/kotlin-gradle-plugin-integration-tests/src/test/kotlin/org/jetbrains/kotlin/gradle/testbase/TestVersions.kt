/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.utils.minSupportedGradleVersion

interface TestVersions {
    object Gradle {
        const val MIN_SUPPORTED = minSupportedGradleVersion
        const val MAX_SUPPORTED = "7.0"
    }

    object Kotlin {
        const val STABLE_RELEASE = "1.4.32"

        // Copied from KOTLIN_VERSION.kt file
        val CURRENT
            get() = System.getProperty("kotlinVersion") ?: error("Required to specify kotlinVersion system property for tests")
    }
}