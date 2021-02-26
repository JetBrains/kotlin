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

        val v3_4_1 = fromString("3.4.1")
        val v3_6_0 = fromString("3.6.0")
        val v4_1_0 = fromString("4.1.0-beta02")
        val v4_2_0 = fromString("4.2.0-alpha10")
        val v7_0_0 = fromString("7.0.0-alpha03")
    }
}