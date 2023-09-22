/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.services

internal data class Metric(val name: String, val projectHash: String?) : Comparable<Metric> {
    override fun compareTo(other: Metric): Int {
        val compareNames = name.compareTo(other.name)
        return when {
            compareNames != 0 -> compareNames
            projectHash == other.projectHash -> 0
            else -> (projectHash ?: "").compareTo(other.projectHash ?: "")
        }
    }

    override fun toString(): String {
        val suffix = if (projectHash == null) "" else ".${projectHash}"
        return name + suffix
    }
}