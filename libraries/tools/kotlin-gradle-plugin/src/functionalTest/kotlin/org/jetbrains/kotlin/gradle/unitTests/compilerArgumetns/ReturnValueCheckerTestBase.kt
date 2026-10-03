/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.ReturnValueCheckerMode
import org.jetbrains.kotlin.gradle.dsl.toCompilerValue
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class ReturnValueCheckerTestBase {
    protected fun Project.assertMode(taskName: String, mode: ReturnValueCheckerMode?) {
        val args = compileArguments(taskName)
        if (mode == null) {
            assertTrue(
                args.none { it.contains("return-value-checker") },
                "Arguments for task '${taskName}' should not contain return-value-checker: $args"
            )
        } else {
            val args = args.filter { it.contains("return-value-checker") }
            assertEquals(
                1,
                args.size,
                "A single return-value-checker argument is expected for '${taskName}': $args"
            )
            assertTrue(
                args.any { it.contains("return-value-checker=${mode.toCompilerValue()}") },
                "Arguments for task '${taskName}' should contain return-value-checker in $mode mode: $args"
            )
        }
    }

    protected fun Project.compileArguments(taskName: String): List<String> {
        val task = tasks.getByName(taskName) as KotlinCompilerArgumentsProducer
        return ArgumentUtils.convertArgumentsToStringList(task.createCompilerArguments(lenient))
    }
}
