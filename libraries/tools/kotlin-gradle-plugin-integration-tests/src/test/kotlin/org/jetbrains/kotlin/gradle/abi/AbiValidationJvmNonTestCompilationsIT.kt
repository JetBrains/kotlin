/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.abi.utils.abiValidation
import org.jetbrains.kotlin.gradle.abi.utils.referenceJvmDumpFile
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*
import kotlin.test.assertContains
import kotlin.test.assertFalse

@JvmGradlePluginTests
class AbiValidationJvmNonTestCompilationsIT : KGPBaseTest() {

    @GradleTest
    fun testNonTestCompilationsExcludesTestCompilation(
        gradleVersion: GradleVersion,
    ) {
        project(
            "base-kotlin-jvm-library",
            gradleVersion,
        ) {
            abiValidation {
                binariesSource.set(BinariesSource.NON_TEST_COMPILATIONS)
            }

            buildScriptInjection {
                project.plugins.apply("jvm-test-suite")
                val testing = project.extensions.getByType(org.gradle.testing.base.TestingExtension::class.java)
                testing.suites.register("integrationTest", org.gradle.api.plugins.jvm.JvmTestSuite::class.java)
            }

            kotlinSourcesDir().source("PublicAbi.kt") { MAIN_SOURCE }
            kotlinSourcesDir("test").source("TestOnlyClass.kt") { TEST_SOURCE }
            kotlinSourcesDir("integrationTest").source("IntegrationTestOnlyClass.kt") { INTEGRATION_TEST_SOURCE }

            build("updateKotlinAbi")

            val dump = referenceJvmDumpFile().readText()
            assertContains(dump, "class PublicAbi")
            assertFalse(dump.contains("class TestOnlyClass"))
            assertFalse(dump.contains("class IntegrationTestOnlyClass"))
        }
    }
}

private val MAIN_SOURCE = """
    class PublicAbi {
        fun greet(): String = "Hello"
    }
""".trimIndent()

private val TEST_SOURCE = """
    class TestOnlyClass {
        fun testHelper(): String = "test"
    }
""".trimIndent()

private val INTEGRATION_TEST_SOURCE = """
    class IntegrationTestOnlyClass {
        fun integrationHelper(): String = "integration test"
    }
""".trimIndent()
