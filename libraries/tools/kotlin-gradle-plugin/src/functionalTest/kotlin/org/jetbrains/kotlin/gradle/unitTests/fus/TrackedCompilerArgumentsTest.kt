/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.fus

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.gradle.plugin.statistics.CompilerArgumentMetrics
import org.jetbrains.kotlin.gradle.plugin.statistics.arguments.TrackedCompilerArgument
import org.jetbrains.kotlin.gradle.plugin.statistics.arguments.TrackedCompilerArguments
import org.jetbrains.kotlin.gradle.plugin.statistics.arguments.trackedCompilerArguments
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import kotlin.reflect.KClass
import kotlin.test.*

class TrackedCompilerArgumentsTest {

    private val logger = Logging.getLogger(TrackedCompilerArgumentsTest::class.java)

    private fun metricsFor(
        argumentsClass: KClass<out CommonToolArguments>,
        arguments: List<String>,
        rules: List<TrackedCompilerArgument<*>> = TrackedCompilerArguments.ALL,
    ): StubFUSConsumer {
        return StubFUSConsumer().also {
            val argumentsInstance = argumentsClass.java.getConstructor().newInstance()
            parseCommandLineArguments(arguments, argumentsInstance)
            CompilerArgumentMetrics.collectTrackedArguments(argumentsInstance, logger, it, rules)
        }
    }

    private fun jvmDefaults(vararg arguments: String): List<String>? =
        metricsFor(K2JVMCompilerArguments::class, arguments.toList())
            .stringListMetrics[StringListMetrics.JVM_DEFAULTS]

    // -jvm-default, set through the typed Gradle DSL: 'compilerOptions.jvmDefault' is serialized in the two-token form.

    @Test
    fun jvmDefaultIsReportedForTypedArgument() {
        assertEquals(listOf("enable"), jvmDefaults("-jvm-default", "enable"))
    }

    @Test
    fun jvmDefaultIsNotReportedWhenNotSpecified() {
        assertNull(jvmDefaults())
    }

    @Test
    fun malformedJvmDefaultIsNotReported() {
        // The compiler reports an error and resolves no mode, so neither should the metric invent one.
        assertNull(jvmDefaults("-jvm-default", "oops"))
    }

    // -jvm-default, set through freeCompilerArgs. 'toArgumentStrings' appends free args last, so they override the
    // value coming from the typed DSL - which is exactly what the compiler does.

    @Test
    fun jvmDefaultIsReportedForFreeCompilerArg() {
        assertEquals(listOf("no-compatibility"), jvmDefaults("-jvm-default=no-compatibility"))
    }

    @Test
    fun freeCompilerArgOverridesTypedJvmDefault() {
        assertEquals(
            listOf("no-compatibility"),
            jvmDefaults("-jvm-default", "enable", "-jvm-default=no-compatibility"),
        )
    }

    // The deprecated -Xjvm-default spelling, which only ever arrives through freeCompilerArgs, and whose value names
    // differ from the stable ones.

    @Test
    fun deprecatedJvmDefaultAllIsReportedAsNoCompatibility() {
        assertEquals(listOf("no-compatibility"), jvmDefaults("-Xjvm-default=all"))
    }

    @Test
    fun deprecatedJvmDefaultAllCompatibilityIsReportedAsEnable() {
        assertEquals(listOf("enable"), jvmDefaults("-Xjvm-default=all-compatibility"))
    }

    @Test
    fun deprecatedJvmDefaultDisableIsReportedAsDisable() {
        assertEquals(listOf("disable"), jvmDefaults("-Xjvm-default=disable"))
    }

    @Test
    fun stableJvmDefaultWinsOverDeprecatedOne() {
        assertEquals(listOf("disable"), jvmDefaults("-jvm-default", "disable", "-Xjvm-default=all"))
    }

    // Robustness of the shared re-parse.

    @Test
    fun unknownArgumentsDoNotPreventReporting() {
        assertEquals(listOf("enable"), jvmDefaults("-Xtotally-made-up", "-nonsense", "-jvm-default=enable"))
    }

    @Test
    fun jvmArgumentIsNotReportedForOtherCompilations() {
        val metrics = metricsFor(K2JSCompilerArguments::class, listOf("-jvm-default=enable"))
        assertTrue(metrics.stringListMetrics.isEmpty(), "A JVM-only rule must not fire for a JS compilation")
    }

    // The declaration shapes of the builder DSL. These use a local registry rather than the production one, so that
    // they keep testing the mechanism as production rules come and go.

    @Test
    fun everyDeclarationShapeReportsItsMetric() {
        val rules = trackedCompilerArguments {
            forArguments<K2JVMCompilerArguments> {
                booleanMetric(BooleanMetrics.ENABLED_KAPT, "-java-parameters", extract = { it.javaParameters })
                numberMetric(
                    NumericalMetrics.COMPILATIONS_COUNT, "-Xbackend-threads",
                    extract = { it.backendThreads.toLongOrNull() },
                )
                stringMetric(StringMetrics.KOTLIN_API_VERSION, "-api-version", extract = { it.apiVersion })
                stringListMetric(StringListMetrics.USE_FIR, "-language-version", extract = { it.languageVersion })
            }
        }

        val metrics = metricsFor(
            K2JVMCompilerArguments::class,
            listOf("-java-parameters", "-Xbackend-threads=4", "-api-version", "2.2", "-language-version", "2.3"),
            rules = rules,
        )

        assertEquals(true, metrics.booleanMetrics[BooleanMetrics.ENABLED_KAPT])
        assertEquals(4L, metrics.numericalMetrics[NumericalMetrics.COMPILATIONS_COUNT])
        assertEquals("2.2", metrics.stringMetrics[StringMetrics.KOTLIN_API_VERSION])
        assertEquals(listOf("2.3"), metrics.stringListMetrics[StringListMetrics.USE_FIR])
    }

    @Test
    fun nullExtractedValueReportsNothing() {
        val rules = trackedCompilerArguments {
            forArguments<K2JVMCompilerArguments> {
                stringMetric(StringMetrics.KOTLIN_API_VERSION, "-api-version", extract = { it.apiVersion })
            }
        }

        assertTrue(metricsFor(K2JVMCompilerArguments::class, emptyList(), rules = rules).stringMetrics.isEmpty())
    }

    @Test
    fun nullConvertedValueReportsNothing() {
        val rules = trackedCompilerArguments {
            forArguments<K2JVMCompilerArguments> {
                stringMetric(
                    StringMetrics.KOTLIN_API_VERSION, "-api-version",
                    extract = { it.apiVersion },
                    convert = { null },
                )
            }
        }

        val metrics = metricsFor(K2JVMCompilerArguments::class, listOf("-api-version", "2.2"), rules = rules)
        assertTrue(metrics.stringMetrics.isEmpty())
    }
}
