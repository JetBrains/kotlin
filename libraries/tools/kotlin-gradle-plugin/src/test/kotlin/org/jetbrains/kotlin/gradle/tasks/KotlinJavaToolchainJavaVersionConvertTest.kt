/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.JavaVersion
import org.gradle.api.internal.provider.AbstractProperty.PropertyQueryException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.jvm.Jvm
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.testhelpers.StubLogger
import org.junit.Before
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class KotlinJavaToolchainJavaVersionConvertTest {
    private lateinit var objects: ObjectFactory
    private lateinit var providers: ProviderFactory
    private val logger = StubLogger("JavaToolchain")
    private val jvmCompilerOptions by lazy {
        objects.newInstance<KotlinJvmCompilerOptionsDefault>()
    }

    @Before
    fun setUp() {
        val project = ProjectBuilder.builder().build()
        objects = project.objects
        providers = project.providers
    }

    @Test
    fun wiresExpectedTarget() {
        val jvm = jvm(JavaVersion.VERSION_19)

        DefaultKotlinJavaToolchain.wireJvmTargetToJvm(jvmCompilerOptions, providers.provider { jvm }, logger)

        assertEquals(JvmTarget.JVM_19, jvmCompilerOptions.jvmTarget.orNull)
        assertTrue(logger.loggedWarnings.isEmpty())
    }

    @Test
    fun throwsExceptionOnUnsupportedLowerTarget() {
        val jvm = jvm(JavaVersion.VERSION_1_7)

        DefaultKotlinJavaToolchain.wireJvmTargetToJvm(jvmCompilerOptions, providers.provider { jvm }, logger)

        val exception = assertThrows<PropertyQueryException> {
            jvmCompilerOptions.jvmTarget.orNull
        }
        assertTrue(exception.cause is IllegalArgumentException)
    }

    @Test
    fun fallbacksToHighestSupportedVersionWithLogMessage() {
        val jvm = jvm(JavaVersion.VERSION_24)

        DefaultKotlinJavaToolchain.wireJvmTargetToJvm(jvmCompilerOptions, providers.provider { jvm }, logger)

        val expectedKotlinTarget = JvmTarget.values().last()
        assertEquals(expectedKotlinTarget, jvmCompilerOptions.jvmTarget.orNull)
        assertTrue(logger.loggedWarnings.isNotEmpty())
        assertEquals(
            "Kotlin does not yet support ${jvm.javaVersion} JDK target, falling back to Kotlin $expectedKotlinTarget JVM target",
            logger.loggedWarnings.single()
        )
    }

    private fun jvm(javaVersion: JavaVersion) = Jvm.discovered(File("/tmp"), "any", javaVersion)
}