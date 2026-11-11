/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArguments

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.ReturnValueCheckerMode
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.toCompilerValue
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class ReturnValueCheckerTestBase {
    protected fun Project.assertMode(taskName: String, mode: ReturnValueCheckerMode?) {
        val args = compileArguments(taskName)
        val matchingArgs = args.filter { it.contains("return-value-checker") }
        if (mode == null) {
            assertTrue(
                matchingArgs.isEmpty(),
                "Arguments for task '${taskName}' should not contain return-value-checker: $args"
            )
        } else {
            // Argument can come from either the task settings, or from the language settings where it is added as a free arg.
            assertTrue(
                matchingArgs.isNotEmpty(),
                "Arguments for task '${taskName}' should contain return-value-checker: $args"
            )
            assertTrue(
                matchingArgs.all { it.contains("return-value-checker=${mode.toCompilerValue()}") },
                "All return-value-checker arguments for task '${taskName}' should be in $mode mode: $matchingArgs"
            )
        }
    }

    protected fun Project.compileArguments(taskName: String): List<String> {
        val task = tasks.getByName(taskName) as KotlinCompilerArgumentsProducer
        return ArgumentUtils.convertArgumentsToStringList(task.createCompilerArguments(lenient))
    }
}
