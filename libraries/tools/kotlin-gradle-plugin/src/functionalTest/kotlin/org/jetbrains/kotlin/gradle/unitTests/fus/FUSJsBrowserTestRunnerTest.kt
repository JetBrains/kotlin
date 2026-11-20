/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalJsTestDsl::class)

package org.jetbrains.kotlin.gradle.unitTests.fus

import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinBrowserTestRunnerDsl
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import javax.inject.Inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FUSJsBrowserTestRunnerTest {

    // KT-90087
    @Test
    fun defaultChromiumRunnerIsReported() {
        val project = buildProjectWithMPP(preApplyCode = enableFusOnCI) {
            with(multiplatformExtension) {
                js { browser { test {} } }
            }
        }

        project.evaluate()

        val metrics = project.collectedFusConfigurationTimeMetrics.stringListMetrics
        assertEquals(
            listOf("chromium"),
            metrics[StringListMetrics.JS_TEST_BROWSER_TYPE],
            "JS_TEST_BROWSER_TYPE is not reported for the default Chromium runner",
        )
        assertEquals(
            emptyList(),
            metrics[StringListMetrics.JS_TEST_BROWSER_CHANGED_OPTION],
            "JS_TEST_BROWSER_CHANGED_OPTION is not reported for the default Chromium runner",
        )
    }

    @Test
    fun explicitRunnerIsReported() {
        val project = buildProjectWithMPP(preApplyCode = enableFusOnCI) {
            with(multiplatformExtension) {
                js {
                    browser {
                        test.apply {
                            firefox("firefox") {
                                it.headless.set(false)
                            }
                        }
                    }
                }
            }
        }

        project.evaluate()

        val metrics = project.collectedFusConfigurationTimeMetrics.stringListMetrics
        assertEquals(
            listOf("firefox"),
            metrics[StringListMetrics.JS_TEST_BROWSER_TYPE],
        )
        assertEquals(
            listOf("headless"),
            metrics[StringListMetrics.JS_TEST_BROWSER_CHANGED_OPTION],
        )
    }

    internal abstract class UnsupportedBrowserTestRunner @Inject constructor() : KotlinBrowserTestRunnerDsl

    @Test
    fun `unsupported browser test runner does not break configuration time metrics`() {
        val project = buildProjectWithMPP(preApplyCode = enableFusOnCI) {
            with(multiplatformExtension) {
                js {
                    browser {
                        test {
                            it.firefox("ff")
                            it.browserRunners.add(project.objects.newInstance(UnsupportedBrowserTestRunner::class.java, "custom"))
                        }
                    }
                }
            }
        }

        assertFails { project.evaluate() }

        val metrics = project.collectedFusConfigurationTimeMetrics
        assertEquals(listOf("firefox"), metrics.stringListMetrics[StringListMetrics.JS_TEST_BROWSER_TYPE])
    }
}
