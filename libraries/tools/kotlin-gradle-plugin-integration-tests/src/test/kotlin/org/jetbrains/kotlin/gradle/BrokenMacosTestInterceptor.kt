/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.util.text.SemVer
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleAndroidTest
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
    val failureExpectation: BrokenOnMacosTestFailureExpectation = BrokenOnMacosTestFailureExpectation.AFTER_GRADLE_8,
)

enum class BrokenOnMacosTestFailureExpectation {
    ALWAYS,
    // This is most likely to happen because target project is not configuration cache compatible and the test runs with CC
    AFTER_GRADLE_8,
    AFTER_AGP_8_5_0,
}

class BrokenMacosTestInterceptor : InvocationInterceptor {
    override fun interceptTestMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) = runTest(invocation, extensionContext, emptyList())

    override fun interceptDynamicTest(
        invocation: Invocation<Void>,
        invocationContext: DynamicTestInvocationContext,
        extensionContext: ExtensionContext,
    ) = runTest(invocation, extensionContext, emptyList())

    override fun interceptTestTemplateMethod(
        invocation: Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        runTest(
            invocation,
            extensionContext,
            invocationContext.arguments,
        )
    }

    private fun runTest(
        invocation: Invocation<Void>,
        extensionContext: ExtensionContext,
        arguments: List<Any>,
    ) {
        if (!HostManager.hostIsMac) {
            invocation.proceed()
            return
        }

        val brokenOnMacosTest = extensionContext.element.get().getAnnotation(BrokenOnMacosTest::class.java)
        val isTestMarkedBrokenOnMacos = brokenOnMacosTest != null
        if (!isTestMarkedBrokenOnMacos) {
            invocation.proceed()
            return
        }

        val isTestExpectedToBeBroken: Boolean = when (brokenOnMacosTest.failureExpectation) {
            BrokenOnMacosTestFailureExpectation.ALWAYS -> true
            BrokenOnMacosTestFailureExpectation.AFTER_GRADLE_8 -> {
                val gradleVersion = (arguments.singleOrNull { it is GradleVersion } as? GradleVersion) ?: error("Missing Gradle version")
                gradleVersion >= GradleVersion.version("8.0.0")
            }
            BrokenOnMacosTestFailureExpectation.AFTER_AGP_8_5_0 -> {
                extensionContext.element.get().getAnnotation(GradleAndroidTest::class.java) ?: error("Not a GradleAndroidTest")
                val agpVersion = (arguments.singleOrNull { it is String } as? String) ?: error(
                    "Couldn't find single Android version argument in GradleAndroidTest: $arguments"
                )
                (SemVer.parseFromText(agpVersion) ?: error("Couldn't parse AGP version: $agpVersion")) >= SemVer.parseFromText("8.5.0")!!
            }
        }
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
                appendLine("Test succeeded, but was expected to fail on a macOS host. Please remove \"@${BrokenOnMacosTest::class.simpleName}\" from the test or adjust \"${BrokenOnMacosTest::failureExpectation.name}\" parameter")
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