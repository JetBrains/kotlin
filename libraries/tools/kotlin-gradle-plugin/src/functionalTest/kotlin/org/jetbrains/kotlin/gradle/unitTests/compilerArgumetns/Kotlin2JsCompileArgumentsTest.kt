/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.default
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class Kotlin2JsCompileArgumentsTest {
    @Test
    fun `test - simple project - old CompilerArgumentsAware and new CompilerArgumentsProducer - return same arguments`() {
        val project = buildProjectWithMPP()
        project.repositories.mavenLocal()

        val kotlin = project.multiplatformExtension
        val jsTarget = kotlin.js(KotlinJsCompilerType.IR)
        val jsMainCompilation = jsTarget.compilations.main
        project.evaluate()

        val jsMainCompileTask = jsMainCompilation.compileTaskProvider.get()
        val argumentsFromCompilerArgumentsProducer = jsMainCompileTask.createCompilerArguments(lenient)

        @Suppress("DEPRECATION_ERROR")
        assertEquals(
            jsMainCompileTask.serializedCompilerArgumentsIgnoreClasspathIssues,
            ArgumentUtils.convertArgumentsToStringList(argumentsFromCompilerArgumentsProducer),
        )
    }

    @Test
    fun `test - simple project - failing dependency - lenient`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        val jsTarget = kotlin.js()
        kotlin.sourceSets.getByName("commonMain").dependencies { implementation("not-a:dependency:1.0.0") }
        project.evaluate()

        val jsMainCompileTask = jsTarget.compilations.main.compileTaskProvider.get()
        assertNull(jsMainCompileTask.createCompilerArguments(lenient).libraries)

        assertFails { jsMainCompileTask.createCompilerArguments(default) }
    }
}