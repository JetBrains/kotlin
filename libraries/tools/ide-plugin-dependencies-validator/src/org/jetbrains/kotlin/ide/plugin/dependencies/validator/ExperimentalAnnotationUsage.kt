/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.plugin.dependencies.validator

import java.nio.file.Path

data class ExperimentalAnnotationUsage(
    val file: Path,
    val lineNumber: Int,
    val usedExperimentalAnnotation: String,
) {
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + file.toAbsolutePath().hashCode()
        result = 31 * result + lineNumber
        result = 31 * result + usedExperimentalAnnotation.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExperimentalAnnotationUsage

        if (file.toAbsolutePath() != other.file.toAbsolutePath()) return false
        if (lineNumber != other.lineNumber) return false
        if (usedExperimentalAnnotation != other.usedExperimentalAnnotation) return false

        return true
    }
}
