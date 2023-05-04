/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import kotlin.test.Test
import kotlin.test.assertEquals

class KT58427ResolveJSCompilerArguments {
    @Suppress("DEPRECATION_ERROR")
    @Test
    fun `test - resolve js compiler arguments with CompilerArgumentsAware`() {
        val project = buildProjectWithMPP()
        project.repositories.mavenLocal()
        project.repositories.mavenCentralCacheRedirector()
        val kotlin = project.multiplatformExtension
        val js = kotlin.js(KotlinJsCompilerType.IR) { nodejs() }

        kotlin.sourceSets.all { sourceSet ->
            sourceSet.languageSettings.languageVersion = "1.7"
            sourceSet.languageSettings.apiVersion = "1.6"
        }

        project.evaluate()

        val jsCompileTask = js.compilations.main.compileTaskProvider.get()
        val args = jsCompileTask.createCompilerArgs()

        /*
        Regression: Compiler Arguments used RegularImmutableList for freeArgs, so the 'copyBeanTo' function failed copying the list.

        org.gradle.internal.impldep.com.google.common.collect.RegularImmutableList
        java.lang.InstantiationException: org.gradle.internal.impldep.com.google.common.collect.RegularImmutableList
	        at java.base/java.lang.Class.newInstance(Class.java:571)
	        at org.jetbrains.kotlin.cli.common.arguments.ArgumentUtilsKt.copyValueIfNeeded(argumentUtils.kt:88)
	        at org.jetbrains.kotlin.cli.common.arguments.ArgumentUtilsKt.copyProperties(argumentUtils.kt:66)
	        at org.jetbrains.kotlin.cli.common.arguments.ArgumentUtilsKt.copyBeanTo(argumentUtils.kt:33)
	        at org.jetbrains.kotlin.cli.common.arguments.ArgumentUtilsKt.copyBeanTo$default(argumentUtils.kt:32)
	        at org.jetbrains.kotlin.gradle.internal.CompilerArgumentAware$DefaultImpls.setupCompilerArgs(CompilerArgumentAware.kt:65)
	        at org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool.setupCompilerArgs(AbstractKotlinCompileTool.kt:31)
	        at org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile.setupCompilerArgs(Kotlin2JsCompile.kt:133)
	        at org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile.setupCompilerArgs(Kotlin2JsCompile.kt:53)
	        at org.jetbrains.kotlin.gradle.internal.CompilerArgumentAware$DefaultImpls.setupCompilerArgs$default(CompilerArgumentAware.kt:58)

        Caused by: java.lang.NoSuchMethodException: org.gradle.internal.impldep.com.google.common.collect.RegularImmutableList.<init>()
         */
        jsCompileTask.setupCompilerArgs(args)

        assertEquals("1.7", args.languageVersion)
        assertEquals("1.6", args.apiVersion)
    }
}
