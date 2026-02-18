/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

interface TestVersions {
    enum class Java(
        val numericVersion: Int
    ) {
        JDK_1_8(8),
        JDK_11(11),
        JDK_17(17),
        JDK_21(21)
    }

    class Maven(
        val version: String
    ) {
        override fun toString(): String = "Maven[$version]"

        companion object {
            const val MAVEN_3_9_12 = "3.9.12"
            const val MAVEN_3_8_9 = "3.8.9"
            const val MAVEN_3_6_3 = "3.6.3"

            const val MIN_SUPPORTED = MAVEN_3_6_3
            const val MAX_SUPPORTED = MAVEN_3_9_12
        }
    }
}