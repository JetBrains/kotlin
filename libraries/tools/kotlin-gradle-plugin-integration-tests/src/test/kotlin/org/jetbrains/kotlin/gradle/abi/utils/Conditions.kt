/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.abi.utils

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.nio.file.Files

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(CaseSensitiveFsCondition::class)
annotation class CaseSensitiveCondition

internal class CaseSensitiveFsCondition : ExecutionCondition {

    private val disabled = "Disabled for case insensitive file system: ${System.getProperty("os.name")}"
    private val caseSensitive = "File system is case sensitive"

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val f = Files.createTempFile("UPPER", "UPPER").toFile()
        f.deleteOnExit()
        return try {
            val lower = File(f.absolutePath.lowercase())
            val insensitive = lower.exists()
            if (insensitive) {
                ConditionEvaluationResult.disabled(disabled)
            } else {
                ConditionEvaluationResult.enabled(caseSensitive)
            }
        } finally {
            f.delete()
        }
    }
}
