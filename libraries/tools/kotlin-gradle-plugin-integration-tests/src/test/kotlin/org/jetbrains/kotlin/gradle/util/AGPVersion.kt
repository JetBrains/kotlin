/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.util.VersionNumber

class AGPVersion private constructor(private val versionNumber: VersionNumber) {
    operator fun compareTo(other: AGPVersion): Int =
        versionNumber.compareTo(other.versionNumber)

    override fun toString(): String =
        versionNumber.toString()

    companion object {
        fun fromString(versionString: String): AGPVersion =
            AGPVersion(VersionNumber.parse(versionString))

        val v3_0_0 = fromString("3.0.0")
        val v3_1_0 = fromString("3.1.0")
        val v3_2_0 = fromString("3.2.0")
        val v3_3_2 = fromString("3.3.2")
    }
}