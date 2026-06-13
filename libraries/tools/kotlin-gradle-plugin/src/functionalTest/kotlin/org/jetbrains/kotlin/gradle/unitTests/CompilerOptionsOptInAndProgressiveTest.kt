/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertContains

/**
 * Configuration-phase counterpart of the opt-in / progressive arg-passing scenarios of the former KGP
 * `CompilerOptionsIT`: the `optIn` / `progressiveMode` options configured via `compilerOptions {}` or
 * `languageSettings {}` must end up in the compile task's computed compiler arguments. Verified by computing the
 * arguments after `evaluate()`, without a real build.
 */
class CompilerOptionsOptInAndProgressiveTest {

    @Test
    fun optInFromCompilerOptionsDslIsPassed() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.compilerOptions.optIn.addAll("kotlin.RequiresOptIn", "my.CustomOptIn")
        project.evaluate()

        assertContains(project.compileArgumentsString("compileKotlin"), "-opt-in kotlin.RequiresOptIn,my.CustomOptIn")
    }

    @Test
    fun progressiveModeFromLanguageSettingsIsPassed() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.sourceSets.configureEach { it.languageSettings.progressiveMode = true }
        project.evaluate()

        assertContains(project.compileArgumentsString("compileKotlin").split(" "), "-progressive")
    }

    @Test
    fun optInFromLanguageSettingsAndCompilerOptionsAreCombined() {
        val project = buildProjectWithMPP {
            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                it.compilerOptions.optIn.add("another.custom.UnderOptIn")
            }
            with(multiplatformExtension) {
                jvm()
                sourceSets.getByName("jvmMain").languageSettings.optIn("my.custom.OptInAnnotation")
            }
        }
        project.evaluate()

        assertContains(
            project.compileArgumentsString("compileKotlinJvm"),
            "-opt-in my.custom.OptInAnnotation,another.custom.UnderOptIn",
        )
    }

    private fun Project.compileArgumentsString(taskName: String): String {
        val task = tasks.getByName(taskName) as KotlinCompilerArgumentsProducer
        return ArgumentUtils.convertArgumentsToStringList(task.createCompilerArguments(lenient)).joinToString(" ")
    }
}
