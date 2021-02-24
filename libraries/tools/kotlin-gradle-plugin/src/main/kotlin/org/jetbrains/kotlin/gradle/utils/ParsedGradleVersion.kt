/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.invocation.Gradle
import org.gradle.util.GradleVersion

internal data class ParsedGradleVersion(val major: Int, val minor: Int) : Comparable<ParsedGradleVersion> {
    override fun compareTo(other: ParsedGradleVersion): Int {
        val majorCompare = major.compareTo(other.major)
        if (majorCompare != 0) return majorCompare

        return minor.compareTo(other.minor)
    }

    companion object {
        private fun String.parseIntOrNull(): Int? =
            try {
                toInt()
            } catch (e: NumberFormatException) {
                null
            }

        fun parse(version: String): ParsedGradleVersion? {
            val matches = "(\\d+)\\.(\\d+).*"
                .toRegex()
                .find(version)
                ?.groups
                ?.drop(1)?.take(2)
                // checking if two subexpression groups are found and length of each is >0 and <4
                ?.let { if (it.all { (it?.value?.length ?: 0).let { it > 0 && it < 4 } }) it else null }

            val versions = matches?.mapNotNull { it?.value?.parseIntOrNull() } ?: emptyList()
            if (versions.size == 2 && versions.all { it >= 0 }) {
                val (major, minor) = versions
                return ParsedGradleVersion(major, minor)
            }

            return null
        }
    }
}

fun isGradleVersionAtLeast(major: Int, minor: Int) =
    ParsedGradleVersion.parse(GradleVersion.current().version)
        ?.let { it >= ParsedGradleVersion(major, minor) } ?: false
