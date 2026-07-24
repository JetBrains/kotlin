/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.asKotlinVersion
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Configuration-phase counterpart of the single-platform JVM scenarios of the former KGP `CompilerOptionsProjectIT`:
 * the project-level `kotlin { compilerOptions {} }` DSL, the `target.compilerOptions {}` override and `languageSettings`
 * interplay must end up in the compiler arguments of `compileKotlin`. Verified by computing the task's arguments at
 * configuration time, without running a build. The Android and multiplatform scenarios stay in the integration test /
 * are covered by [ProjectCompilerOptionsTests].
 */
class CompilerOptionsProjectTest {

    @Test
    fun jvmProjectLevelOptions() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.compilerOptions {
            javaParameters.set(true)
            verbose.set(false)
        }
        project.evaluate()

        val arguments = project.mainCompileArguments()
        assertTrue("-java-parameters" in arguments, "javaParameters=true should pass -java-parameters (arguments: $arguments)")
        assertFalse("-verbose" in arguments, "verbose=false should not pass -verbose (arguments: $arguments)")
    }

    @Test
    fun jvmTargetOptionsOverrideProjectOptions() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.apply {
            target.compilerOptions {
                javaParameters.set(true)
                verbose.set(true)
            }
            compilerOptions {
                javaParameters.set(false)
                verbose.set(false)
            }
        }
        project.evaluate()

        // Both options differ between the levels, so each passed flag proves the target value won over the project one.
        val arguments = project.mainCompileArguments()
        assertTrue("-java-parameters" in arguments, "target javaParameters=true should override the project value false (arguments: $arguments)")
        assertTrue("-verbose" in arguments, "target verbose=true should override the project value false (arguments: $arguments)")
    }

    @Test
    fun nonConfiguredLanguageSettingsDoNotOverrideProjectOptions() {
        // Any stable, non-deprecated version works here; the test only asserts the value propagates.
        val version = LanguageVersion.FIRST_NON_DEPRECATED
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.compilerOptions {
            languageVersion.set(version.asKotlinVersion())
            apiVersion.set(version.asKotlinVersion())
            progressiveMode.set(true)
            optIn.add("my.custom.OptInAnnotation")
            freeCompilerArgs.add("-Xdebug")
        }
        project.evaluate()

        val arguments = project.mainCompileArguments().joinToString(" ")
        val expected = listOf(
            "-language-version ${version.versionString}",
            "-api-version ${version.versionString}",
            "-progressive",
            "-opt-in my.custom.OptInAnnotation",
            "-Xdebug",
        )
        assertTrue(
            expected.all { it in arguments },
            "Missing ${expected.filterNot { it in arguments }} in $arguments",
        )
    }

    @Test
    fun configuredLanguageSettingsOverrideProjectOptions() {
        // Two distinct non-deprecated versions so the override is observable: languageSettings (lower) must win.
        val projectVersion = LanguageVersion.LATEST_STABLE
        val settingsVersion = LanguageVersion.FIRST_NON_DEPRECATED
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.apply {
            compilerOptions {
                languageVersion.set(projectVersion.asKotlinVersion())
                apiVersion.set(projectVersion.asKotlinVersion())
                progressiveMode.set(true)
                optIn.add("my.custom.OptInAnnotation")
                freeCompilerArgs.add("-Xdebug")
            }
            sourceSets.getByName("main").languageSettings {
                languageVersion = settingsVersion.versionString
                apiVersion = settingsVersion.versionString
                progressiveMode = false
                optIn("another.CustomOptInAnnotation")
                @Suppress("DEPRECATION_ERROR")
                enableLanguageFeature("UnitConversionsOnArbitraryExpressions")
            }
        }
        project.evaluate()

        val arguments = project.mainCompileArguments().joinToString(" ")
        val expected = listOf(
            "-language-version ${settingsVersion.versionString}",
            "-api-version ${settingsVersion.versionString}",
            "-Xdebug",
            "-opt-in my.custom.OptInAnnotation,another.CustomOptInAnnotation",
            "-XXLanguage:+UnitConversionsOnArbitraryExpressions",
        )
        assertTrue(
            expected.all { it in arguments },
            "Missing ${expected.filterNot { it in arguments }} in $arguments",
        )
        assertFalse(
            "-progressive" in arguments.split(" "),
            "languageSettings should disable progressive mode: $arguments",
        )
    }

    @Test
    fun moduleNameFromProjectOptions() {
        val project = buildProjectWithJvm()
        project.kotlinJvmExtension.compilerOptions {
            moduleName.set("customModule")
        }
        project.evaluate()

        val arguments = project.mainCompileArguments().joinToString(" ")
        assertTrue("-module-name customModule" in arguments, "project moduleName should be passed (arguments: $arguments)")
    }

    private fun Project.mainCompileArguments(): List<String> {
        val compileTask = kotlinJvmExtension.target.compilations.getByName("main")
            .compileTaskProvider.get() as KotlinCompile
        return ArgumentUtils.convertArgumentsToStringList(compileTask.createCompilerArguments(lenient))
    }
}
