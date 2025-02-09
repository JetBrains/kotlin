/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.AssumptionViolatedException
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation
import java.lang.reflect.Method

/**
 * This annotation is used to mark tests that are known to be broken on macOS. Test failure will be wrapped with an exception that the test
 * is expected to be broken. If you are working on the test marked with this annotation, please fix the test and remove the annotation.
 */
annotation class BrokenOnMacosTest(
    // This is most likely to happen because target project is not configuration cache compatible.
    val expectedToFailOnlyAfterGradle8: Boolean = true,
)

class BrokenMacosTestInterceptor : InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) = runTest(invocation, extensionContext, null)

    override fun interceptDynamicTest(
        invocation: Invocation<Void>,
        invocationContext: DynamicTestInvocationContext,
        extensionContext: ExtensionContext,
    ) = runTest(invocation, extensionContext, null)

    override fun interceptTestTemplateMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) = runTest(invocation, extensionContext, invocationContext.arguments.singleOrNull { it is GradleVersion } as? GradleVersion)

    private fun runTest(
        invocation: Invocation<Void>,
        extensionContext: ExtensionContext,
        gradleVersion: GradleVersion?,
    ) {
        val brokenOnMacosTest = extensionContext.element.get().getAnnotation(BrokenOnMacosTest::class.java)
        val isTestMarkedBrokenOnMacos = HostManager.hostIsMac && brokenOnMacosTest != null
        if (!isTestMarkedBrokenOnMacos) {
            invocation.proceed()
            return
        }

        val isTestExpectedToBeBroken = if (brokenOnMacosTest.expectedToFailOnlyAfterGradle8) {
            gradleVersion?.let { it >= GradleVersion.version("8.0.0") } ?: true
        } else true
        if (!isTestExpectedToBeBroken) {
            invocation.proceed()
            return
        }

        try {
            invocation.proceed()
        } catch (exception: Throwable) {
            if (System.getProperty(SKIP_BROKEN_INTEGRATION_TESTS_PROPERTY).toBoolean()) {
                throw AssumptionViolatedException(
                    "Test is broken and will be skipped",
                    exception,
                )
            } else {
                throw WarnAboutBrokenTest(
                    "This test is marked \"@${BrokenOnMacosTest::class.simpleName}\" and is known to fail on a macOS host. Please fix the test and remove the annotation.",
                    exception,
                )
            }
        }

        throw MarkedTestSucceeded(
            buildString {
                appendLine("Test succeeded, but was expected to fail on a macOS host. Please remove \"@${BrokenOnMacosTest::class.simpleName}\" from the test or adjust \"${BrokenOnMacosTest::expectedToFailOnlyAfterGradle8.name}\" parameter")
            }
        )
    }

    class WarnAboutBrokenTest(message: String, cause: Throwable) : AssertionError(message, cause)
    class MarkedTestSucceeded(message: String) : IllegalStateException(message)

    companion object {
        // On CI we run all macOS tests weekly to check that the list of broken tests is up to date
        private const val SKIP_BROKEN_INTEGRATION_TESTS_PROPERTY = "skipIntegrationTestsMarkedBroken"
    }
}