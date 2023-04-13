/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.regressionTests.KTIJ25227CompilerArgumentsIdeCompatibilityTest.IdePluginCodeMock.IdeReflectionCodeMock.get
import org.jetbrains.kotlin.gradle.regressionTests.KTIJ25227CompilerArgumentsIdeCompatibilityTest.IdePluginCodeMock.IdeReflectionCodeMock.getMethodOrNull
import org.jetbrains.kotlin.gradle.regressionTests.KTIJ25227CompilerArgumentsIdeCompatibilityTest.IdePluginCodeMock.checkExtractCompilerArgumentsFromTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.main
import org.junit.Test
import java.lang.reflect.Method
import kotlin.test.fail

class KTIJ25227CompilerArgumentsIdeCompatibilityTest {

    @Test
    fun `test - jvm compile task - is compatible with old CompilerArgumentsExtractor in older IDEs`() {
        val project = buildProjectWithJvm()
        project.evaluate()

        val kotlin = project.kotlinJvmExtension
        val mainCompilation = kotlin.target.compilations.main
        val mainCompileTask = mainCompilation.compileTaskProvider.get() as KotlinCompile
        checkExtractCompilerArgumentsFromTask(mainCompileTask)
    }


    /*
    Code from CompilerArgumentsExtractor inside intellij.git with injected asssertions
     */
    private object IdePluginCodeMock {
        const val CREATE_COMPILER_ARGS = "createCompilerArgs"
        const val SETUP_COMPILER_ARGS = "setupCompilerArgs"

        fun checkExtractCompilerArgumentsFromTask(compileTask: Task, defaultsOnly: Boolean = false): Any {
            val compileTaskClass = compileTask.javaClass
            val compilerArguments = compileTask[CREATE_COMPILER_ARGS] ?: fail("Missing $CREATE_COMPILER_ARGS method")
            compileTaskClass.getMethodOrNull(SETUP_COMPILER_ARGS, compilerArguments::class.java, Boolean::class.java, Boolean::class.java)
                ?.doSetupCompilerArgs(compileTask, compilerArguments, defaultsOnly, false) ?: compileTaskClass
                .getMethodOrNull(SETUP_COMPILER_ARGS, compilerArguments::class.java, Boolean::class.java)
                ?.doSetupCompilerArgs(compileTask, compilerArguments, defaultsOnly)
            ?: fail("Missing $SETUP_COMPILER_ARGS method")
            return compilerArguments
        }

        private fun Method.doSetupCompilerArgs(
            compileTask: Task,
            compilerArgs: Any,
            defaultsOnly: Boolean,
            ignoreClasspathIssues: Boolean? = null
        ) {
            try {
                ignoreClasspathIssues?.also { invoke(compileTask, compilerArgs, defaultsOnly, it) }
                    ?: invoke(compileTask, compilerArgs, defaultsOnly)
            } catch (e: Exception) {
                ignoreClasspathIssues?.also { if (!it) doSetupCompilerArgs(compileTask, compilerArgs, defaultsOnly, true) }
            }
        }

        private object IdeReflectionCodeMock {
            operator fun Any?.get(methodName: String, vararg params: Any): Any? {
                return this[methodName, params.map { it.javaClass }, params.toList()]
            }

            operator fun Any?.get(methodName: String, paramTypes: List<Class<*>>, params: List<Any?>): Any? {
                if (this == null) return null
                return this::class.java.getMethodOrNull(methodName, *paramTypes.toTypedArray())
                    ?.invoke(this, *params.toTypedArray())
            }

            fun Class<*>.getMethodOrNull(name: String, vararg parameterTypes: Class<*>) =
                try {
                    getMethod(name, *parameterTypes)
                } catch (e: Exception) {
                    null
                }
        }
    }
}

