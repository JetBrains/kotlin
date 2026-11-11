/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_JVM_CLASSPATH_METADATA
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.kotlin
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Checks that the KMP JVM incremental compilation properties are translated into `-Xuse-metadata-on-incremental-classpath`.
 * The compiler behaviour behind the flag is covered by `ClasspathMetadataIncrementalTest` in `kotlin-build-tools-api-tests`.
 */
class JvmClasspathMetadataCompilerArgumentTest {

    @Test
    fun disabledByDefault() {
        assertUseMetadataOnIncrementalClasspath(properties = emptyMap(), expected = false)
    }

    @Test
    fun enabledByIncrementalCompilationOfCommonSources() {
        assertUseMetadataOnIncrementalClasspath(
            properties = mapOf(KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES to "true"),
            expected = true,
        )
    }

    @Test
    fun disabledWhenClasspathMetadataIsTurnedOff() {
        assertUseMetadataOnIncrementalClasspath(
            properties = mapOf(
                KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES to "true",
                KOTLIN_INTERNAL_JVM_CLASSPATH_METADATA to "false",
            ),
            expected = false,
        )
    }

    @Test
    fun classpathMetadataAloneDoesNotEnableTheFlag() {
        assertUseMetadataOnIncrementalClasspath(
            properties = mapOf(KOTLIN_INTERNAL_JVM_CLASSPATH_METADATA to "true"),
            expected = false,
        )
    }

    @Test
    fun disabledWithoutIncrementalCompilation() {
        assertUseMetadataOnIncrementalClasspath(
            properties = mapOf(
                KOTLIN_JVM_INCREMENTAL_COMPILATION_OF_COMMON_SOURCES to "true",
                KOTLIN_INTERNAL_JVM_CLASSPATH_METADATA to "true",
                "kotlin.incremental" to "false",
            ),
            expected = false,
        )
    }

    private fun assertUseMetadataOnIncrementalClasspath(properties: Map<String, String>, expected: Boolean) {
        val project = buildProjectWithMPP(
            preApplyCode = {
                enableDefaultStdlibDependency(false)
                properties.forEach { (name, value) -> extraProperties.set(name, value) }
            },
            code = {
                kotlin { jvm() }
            },
        ).evaluate()

        val compileTask = project.tasks.getByName("compileKotlinJvm") as KotlinCompile
        val arguments = compileTask.createCompilerArguments(lenient)

        assertEquals(expected, arguments.useMetadataOnIncrementalClasspath)
    }
}
