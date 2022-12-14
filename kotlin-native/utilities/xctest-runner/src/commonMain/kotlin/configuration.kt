/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlinx.cinterop.*
import kotlin.native.internal.test.*
import platform.Foundation.NSInvocation
import platform.Foundation.NSBundle
import platform.Foundation.NSStringFromSelector
import platform.XCTest.*
import platform.objc.*

internal const val TOP_LEVEL_SUITE = "Kotlin/Native test suite"

// Name of the key that contains arguments used to set [TestSettings]
private const val TEST_ARGUMENTS_KEY = "KotlinNativeTestArgs"

// Test settings should be initialized by the setup method
// It stores settings with the filtered test suites, loggers and listeners.
private var testSettings: TestSettings? = null

internal fun testMethodsNames(): List<String> = testSettings?.testSuites
    ?.toList()
    ?.flatMap { testSuite ->
        testSuite.testCases.values.map { "$testSuite.${it.name}" }
    } ?: error("TestSettings isn't initialized")

@Suppress("unused")
@kotlin.native.internal.ExportForCppRuntime("Konan_create_testSuite")
internal fun setupXCTestSuite(): XCTestSuite {
    val nativeTestSuite = XCTestSuite.testSuiteWithName(TOP_LEVEL_SUITE)

    // Get test arguments from the Info.plist to create test settings
    val plistTestArgs = NSBundle.allBundles.mapNotNull {
        (it as? NSBundle)?.infoDictionary?.get(TEST_ARGUMENTS_KEY)
    }.singleOrNull() as? String
    val args = plistTestArgs?.split(" ")?.toTypedArray() ?: emptyArray<String>()

    // Initialize settings with the given args
    testSettings = TestProcessor(GeneratedSuites.suites, args).process()

    checkNotNull(testSettings) {
        "Test settings wasn't set. Check provided arguments and suites"
    }

    // Set test observer that will log test execution
    testSettings?.let {
        XCTestObservationCenter.sharedTestObservationCenter.addTestObserver(NativeTestObserver(it))
    }

    if (testSettings?.runTests == true) {
        // Generate and add tests to the main suite
        testSettings?.testSuites?.generate()?.forEach {
            nativeTestSuite.addTest(it)
        }

        // Tests created (self-check)
        @Suppress("UNCHECKED_CAST")
        check(testSettings?.testSuites?.size == (nativeTestSuite.tests as List<XCTest>).size) {
            "The amount of generated XCTest suites should be equal to Kotlin test suites"
        }
    }

    return nativeTestSuite
}

private fun Collection<TestSuite>.generate(): List<XCTestSuite> {
    val testInvocations = XCTestCaseRunner.testInvocations()
    return this.map { suite ->
        val xcSuite = XCTestSuiteRunner(suite)
        suite.testCases.values.map { testCase ->
            testInvocations.filter {
                it.selectorString() == "${suite.name}.${testCase.name}"
            }.map { invocation ->
                XCTestCaseRunner(
                    invocation = invocation,
                    testName = "${suite.name}.${testCase.name}",
                    testCase = testCase
                )
            }.single()
        }.forEach {
            xcSuite.addTest(it)
        }
        xcSuite
    }
}

private fun NSInvocation.selectorString() = NSStringFromSelector(selector)
