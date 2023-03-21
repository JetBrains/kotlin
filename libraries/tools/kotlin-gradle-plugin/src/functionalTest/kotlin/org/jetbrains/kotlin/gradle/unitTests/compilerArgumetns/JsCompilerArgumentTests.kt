/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.prepareCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import kotlin.test.Test
import kotlin.test.assertEquals

class JsCompilerArgumentTests {
    @Suppress("DEPRECATION")
    @Test
    fun `test - simple project`() {
        val project = buildProjectWithMPP {
            repositories {
                mavenLocal()
                mavenCentralCacheRedirector()
            }
        }

        val kotlin = project.multiplatformExtension
        val jsTarget = kotlin.js(KotlinJsCompilerType.IR)
        val jsMainCompilation = jsTarget.compilations.main
        project.evaluate()

        val jsMainCompileTask = jsMainCompilation.compileTaskProvider.get()
        val argumentsFromCompilerArgumentsProducer = jsMainCompileTask.createCompilerArguments(
            CreateCompilerArgumentsContext(
                isLenient = true,
                includeArgumentTypes = ArgumentType.all - ArgumentType.DependencyClasspath
            )
        )
        val argumentsFromCompilerArgumentsAware = jsMainCompileTask.prepareCompilerArguments(true)

        assertEquals(
            ArgumentUtils.convertArgumentsToStringList(argumentsFromCompilerArgumentsAware),
            ArgumentUtils.convertArgumentsToStringList(argumentsFromCompilerArgumentsProducer),
        )
    }
}