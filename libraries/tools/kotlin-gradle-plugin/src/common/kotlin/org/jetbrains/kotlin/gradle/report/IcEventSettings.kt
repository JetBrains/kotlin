/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import java.io.File
import java.io.Serializable

data class IcEventSettings (
    val buildReportLabel: String? = null,
    val singleOutputFile: File? = null,
    val jsonOutputDir: File? = null,
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0L
    }
}
